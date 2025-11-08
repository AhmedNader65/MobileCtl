package com.mobilectl.builder.android

import com.mobilectl.builder.BuildOutput
import com.mobilectl.builder.PlatformBuilder
import com.mobilectl.builder.android.signing.AndroidSdkFinder
import com.mobilectl.builder.android.signing.SigningOrchestrator
import com.mobilectl.builder.android.signing.SigningOrchestratorFactory
import com.mobilectl.config.Config
import com.mobilectl.model.Platform
import com.mobilectl.util.ArtifactDetector
import com.mobilectl.util.PremiumLogger
import com.mobilectl.util.ProcessExecutor
import java.io.File

/**
 * Orchestrates Android builds
 *
 * Responsibilities:
 * - Determine what to build (delegates to ArtifactStrategy)
 * - Execute Gradle tasks (delegates to GradleExecutor)
 * - Sign artifacts (delegates to SigningOrchestrator)
 * - Validate for deployment (delegates to ArtifactValidator)
 *
 * Follows Single Responsibility Principle
 */
class AndroidBuilder(
    private val processExecutor: ProcessExecutor,
    private val artifactStrategy: ArtifactStrategy = ArtifactStrategy(),
    private val signingOrchestrator: SigningOrchestrator = SigningOrchestratorFactory.create(
        processExecutor,
        AndroidSdkFinder()
    ),
    private val artifactValidator: ArtifactValidator = ArtifactValidator(),
    private val cacheManager: BuildCacheManager = BuildCacheManager()
) : PlatformBuilder {

    override suspend fun build(baseDir: String, config: Config): BuildOutput {
        val startTime = System.currentTimeMillis()

        return try {
            // Step 1: Determine what to build
            val requirements = artifactStrategy.determineRequirements(config)
            if (requirements.isEmpty()) {
                return failedBuild(startTime, "No deployment destinations enabled")
            }

            val artifactTypes = artifactStrategy.getArtifactTypesToBuild(requirements)

            PremiumLogger.section("Building Android")
            showBuildPlan(config, requirements, artifactTypes)

            // Step 2: Check cache
            val cacheValidation = cacheManager.validateCache(baseDir)
            showCacheStatus(cacheValidation)

            // Step 3: Build artifacts
            val builtArtifacts = buildArtifacts(
                baseDir = baseDir,
                config = config,
                artifactTypes = artifactTypes,
                useCache = !cacheValidation.needsRebuild
            )

            if (builtArtifacts.isEmpty()) {
                return failedBuild(startTime, "No artifacts were built")
            }

            // Step 4: Sign artifacts (if needed)
            val signedArtifacts = signArtifactsIfNeeded(
                artifacts = builtArtifacts,
                config = config,
                baseDir = baseDir,
                requirements = requirements
            )

            // Step 5: Validate for deployment
            val validation = artifactValidator.validate(signedArtifacts, requirements)

            if (!validation.isValid) {
                PremiumLogger.warning("Validation issues detected")
            }

            if (artifactValidator.needsUserConfirmation(validation)) {
                val message = artifactValidator.formatValidationMessage(validation)
                println(message)

                if (!validation.canDeploy) {
                    return failedBuild(startTime, "Cannot deploy to any destination")
                }

                if (!askUserConfirmation()) {
                    return failedBuild(startTime, "Deployment cancelled by user")
                }
            }

            // Step 6: Update cache
            if (cacheValidation.needsRebuild) {
                cacheManager.updateCache(baseDir)
            }

            PremiumLogger.sectionEnd()

            // Step 7: Return result
            val duration = System.currentTimeMillis() - startTime
            val primaryArtifact = signedArtifacts.values.first()

            BuildOutput(
                success = true,
                platform = Platform.ANDROID,
                outputPath = primaryArtifact.path,
                isSigned = primaryArtifact.isSigned,
                durationMs = duration,
                warnings = validation.warnings
            )

        } catch (e: Exception) {
            PremiumLogger.error("Build error: ${e.message}")
            failedBuild(startTime, e.message ?: "Unknown error")
        }
    }

    /**
     * Build artifacts (APK and/or AAB)
     */
    private suspend fun buildArtifacts(
        baseDir: String,
        config: Config,
        artifactTypes: Set<ArtifactStrategy.ArtifactType>,
        useCache: Boolean
    ): Map<ArtifactStrategy.ArtifactType, ArtifactInfo> {
        val artifacts = mutableMapOf<ArtifactStrategy.ArtifactType, ArtifactInfo>()
        val flavor = config.build.android.defaultFlavor
        val buildType = config.build.android.defaultType

        for (type in artifactTypes) {
            // Check cache first
            if (useCache) {
                val cached = findCachedArtifact(baseDir, flavor, buildType, type)
                if (cached != null) {
                    PremiumLogger.success("Using cached ${type.name}")
                    artifacts[type] = cached
                    continue
                }
            }

            // Build it
            val built = buildArtifact(baseDir, config, type)
            if (built != null) {
                artifacts[type] = built
            }
        }

        return artifacts
    }

    /**
     * Build single artifact (APK or AAB)
     */
    private suspend fun buildArtifact(
        baseDir: String,
        config: Config,
        type: ArtifactStrategy.ArtifactType
    ): ArtifactInfo? {
        val flavor = config.build.android.defaultFlavor
        val buildType = config.build.android.defaultType
        val isAab = type == ArtifactStrategy.ArtifactType.AAB

        // Find Gradle wrapper
        val gradlewPath = findGradleWrapper(baseDir)
            ?: throw Exception("Gradle wrapper not found")

        // Build
        val taskName = buildGradleTask(flavor, buildType, isAab)
        PremiumLogger.progress("Running $taskName")

        val startTime = System.currentTimeMillis()
        val result = processExecutor.executeWithProgress(
            command = gradlewPath,
            args = listOf(taskName),
            workingDir = baseDir,
            onProgress = { print("\r\u001B[90m│\u001B[0m  \u001B[36m⋯\u001B[0m  \u001B[2m$it\u001B[0m") }
        )

        print("\r" + " ".repeat(80) + "\r")
        val duration = (System.currentTimeMillis() - startTime) / 1000
        PremiumLogger.detail("Duration", "${duration}s", dim = true)

        if (!result.success) {
            PremiumLogger.error("Build failed: $taskName")
            return null
        }

        PremiumLogger.success("${type.name} built successfully")

        // Find artifact
        val artifactFile = if (isAab) {
            ArtifactDetector.findAab(File(baseDir), flavor, buildType)
        } else {
            ArtifactDetector.findUnsignedApk(File(baseDir), flavor, buildType)
                ?: ArtifactDetector.findSignedApk(File(baseDir), flavor, buildType)
        }

        if (artifactFile == null) {
            PremiumLogger.error("${type.name} not found after build")
            return null
        }

        return ArtifactInfo(
            type = type,
            path = artifactFile.absolutePath,
            isSigned = isArtifactSigned(artifactFile, type),
            sizeBytes = artifactFile.length()
        )
    }

    /**
     * Determine if artifact is signed
     */
    private fun isArtifactSigned(file: File, type: ArtifactStrategy.ArtifactType): Boolean {
        return when (type) {
            ArtifactStrategy.ArtifactType.AAB -> {
                // AABs from Gradle bundleRelease are UNSIGNED by default
                // Only signed if signingConfigs is configured in build.gradle
                // We'll sign them via SigningOrchestrator
                false
            }
            ArtifactStrategy.ArtifactType.APK -> {
                // APKs have explicit "-unsigned" in filename
                !file.name.contains("unsigned")
            }
        }
    }
    /**
     * Sign artifacts if needed
     */
    private suspend fun signArtifactsIfNeeded(
        artifacts: Map<ArtifactStrategy.ArtifactType, ArtifactInfo>,
        config: Config,
        baseDir: String,
        requirements: List<ArtifactStrategy.ArtifactRequirement>
    ): Map<ArtifactStrategy.ArtifactType, ArtifactInfo> {
        // Check if any destination requires signing
        val needsSigning = artifactStrategy.requiresSigning(requirements)
        if (!needsSigning) {
            PremiumLogger.info("Signing not required for selected destinations")
            return artifacts
        }

        val signedArtifacts = mutableMapOf<ArtifactStrategy.ArtifactType, ArtifactInfo>()

        for ((type, artifact) in artifacts) {
            if (artifact.isSigned) {
                signedArtifacts[type] = artifact
                continue
            }

            // Sign it
            val signResult = signingOrchestrator.signArtifact(
                artifactPath = artifact.path,
                config = config,
                baseDir = baseDir
            )

            if (signResult.isSigned) {
                PremiumLogger.success("${type.name} signed successfully")
                signedArtifacts[type] = artifact.copy(isSigned = true)
            } else {
                PremiumLogger.warning("${type.name} signing failed: ${signResult.error ?: "Unknown error"}")
                signedArtifacts[type] = artifact  // Keep unsigned
            }
        }
        return signedArtifacts
    }

    // Helper methods

    private fun showBuildPlan(
        config: Config,
        requirements: List<ArtifactStrategy.ArtifactRequirement>,
        artifactTypes: Set<ArtifactStrategy.ArtifactType>
    ) {
        val flavor = config.build.android.defaultFlavor
        val buildType = config.build.android.defaultType

        PremiumLogger.detail("Variant", "$flavor/$buildType")
        PremiumLogger.detail("Artifacts", artifactTypes.joinToString(", ") { it.name })
        PremiumLogger.detail("Destinations", requirements.joinToString(", ") { it.destination })
    }

    private fun showCacheStatus(validation: BuildCacheManager.CacheValidation) {
        if (validation.needsRebuild) {
            PremiumLogger.info("Cache: ${validation.reason}")
        } else {
            PremiumLogger.success("Cache: ${validation.reason}")
        }
    }

    private fun buildGradleTask(flavor: String, buildType: String, isAab: Boolean): String {
        val variant = "${flavor.lowercase()}${buildType.replaceFirstChar { it.uppercase() }}"
        val prefix = if (isAab) "bundle" else "assemble"
        return "$prefix${variant.replaceFirstChar { it.uppercase() }}"
    }

    private fun findCachedArtifact(
        baseDir: String,
        flavor: String,
        buildType: String,
        type: ArtifactStrategy.ArtifactType
    ): ArtifactInfo? {
        val file = if (type == ArtifactStrategy.ArtifactType.AAB) {
            ArtifactDetector.findAab(File(baseDir), flavor, buildType)
        } else {
            ArtifactDetector.findSignedApk(File(baseDir), flavor, buildType)
                ?: ArtifactDetector.findUnsignedApk(File(baseDir), flavor, buildType)
        } ?: return null

        return ArtifactInfo(
            type = type,
            path = file.absolutePath,
            isSigned = !file.name.contains("unsigned"),
            sizeBytes = file.length()
        )
    }

    private fun findGradleWrapper(baseDir: String): String? {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val candidates = if (isWindows) {
            listOf("$baseDir/gradlew.bat", "$baseDir/gradlew")
        } else {
            listOf("$baseDir/gradlew")
        }

        return candidates.firstOrNull { File(it).exists() }
    }

    private fun askUserConfirmation(): Boolean {
        print("\nContinue with deployment? (Y/n): ")
        val input = readLine()?.trim()?.lowercase() ?: "y"
        return input != "n" && input != "no"
    }

    private fun failedBuild(startTime: Long, message: String): BuildOutput {
        val duration = System.currentTimeMillis() - startTime
        PremiumLogger.error(message)
        return BuildOutput(
            success = false,
            platform = Platform.ANDROID,
            error = message,
            durationMs = duration
        )
    }
}