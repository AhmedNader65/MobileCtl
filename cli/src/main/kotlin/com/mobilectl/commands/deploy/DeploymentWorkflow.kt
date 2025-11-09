package com.mobilectl.commands.deploy

import com.mobilectl.builder.BuildOrchestrator
import com.mobilectl.builder.BuildResult
import com.mobilectl.builder.JvmBuildManager
import com.mobilectl.builder.android.AndroidBuilder
import com.mobilectl.builder.android.BuildCacheManager
import com.mobilectl.builder.IosBuilder
import com.mobilectl.commands.changelog.ChangelogGenerateHandler
import com.mobilectl.commands.version.VersionBumpHandler
import com.mobilectl.config.Config
import com.mobilectl.deploy.DeployOrchestrator  // ✅ This is a concrete class
import com.mobilectl.deploy.createDeployOrchestrator
import com.mobilectl.detector.ProjectDetector
import com.mobilectl.model.Platform
import com.mobilectl.model.deploy.DeployResult
import com.mobilectl.util.*
import java.io.File

/**
 * Orchestrates the deployment workflow
 */
class DeploymentWorkflow(
    private val workingPath: String,
    private val detector: ProjectDetector,
    private val verbose: Boolean = false
) {
    private val deployOrchestrator = createDeployOrchestrator()
    private val processExecutor = createProcessExecutor()

    // Strategy helpers
    private val flavorSelector = FlavorSelector()
    private val platformSelector = PlatformSelector()
    private val buildCacheManager = BuildCacheManager()

    /**
     * Execute version bump before deployment
     */
    suspend fun bumpVersionBeforeDeploy(strategy: String, config: Config) {
        PremiumLogger.header("Pre-Flight: Version Bump ($strategy)", "🔧")

        if (!isValidStrategy(strategy)) {
            throw IllegalArgumentException("Invalid strategy: $strategy")
        }

        if (strategy == "manual") {
            PremiumLogger.simpleInfo("Skipping version bump (manual mode)")
            return
        }

        val versionHandler = VersionBumpHandler(
            level = strategy,
            verbose = verbose,
            dryRun = false,
            skipBackup = false
        )
        versionHandler.execute()
    }

    /**
     * Execute changelog generation before deployment
     */
    suspend fun executeChangelogGeneration(config: Config) {
        PremiumLogger.header("Pre-Flight: Changelog Generation", "📝")

        val generator = ChangelogGenerateHandler(
            config.changelog.fromTag,
            verbose,
            false,
            config.changelog.append,
            config.changelog.useLastState
        )
        generator.execute()

        PremiumLogger.simpleSuccess("Changelog generated")
        println()
    }

    /**
     * Validate version bump strategy
     */
    fun isValidStrategy(strategy: String): Boolean {
        return strategy in listOf("patch", "minor", "major", "auto", "manual")
    }

    /**
     * Parse platforms from argument
     */
    fun parsePlatforms(platformArg: String?, config: Config): Set<Platform>? {
        return platformSelector.parsePlatforms(platformArg, config)
    }

    /**
     * Select flavors to deploy
     * ✅ Fixed signature with FlavorOptions
     */
    fun selectFlavorsToDeploy(config: Config, options: FlavorOptions): List<String> {
        return flavorSelector.selectFlavors(config, options)
    }

    /**
     * Check if build is needed using cache validation
     */
    fun checkIfBuildNeeded(config: Config, platforms: Set<Platform>): Boolean {
        platforms.forEach { platform ->
            when (platform) {
                Platform.ANDROID -> {
                    // Check artifact exists
                    val artifactPath = config.deploy?.android?.artifactPath
                    if (artifactPath == null) {
                        return true
                    }

                    val artifactFile = if (File(artifactPath).isAbsolute) {
                        File(artifactPath)
                    } else {
                        File(workingPath, artifactPath)
                    }

                    if (!artifactFile.exists()) {
                        if (verbose) {
                            PremiumLogger.info("Artifact missing: $artifactPath")
                        }
                        return true
                    }

                    // Use BuildCacheManager for accurate hash-based detection
                    val cacheValidation = buildCacheManager.validateCache(workingPath)
                    if (cacheValidation.needsRebuild) {
                        if (verbose) {
                            PremiumLogger.info("Cache: ${cacheValidation.reason}")
                        }
                        return true
                    }
                }
                Platform.IOS -> {
                    // For iOS, still check if artifact exists
                    // TODO: Implement iOS cache validation when iOS support is added
                    val artifactPath = config.deploy?.ios?.artifactPath
                    if (artifactPath == null) {
                        return true
                    }

                    val artifactFile = if (File(artifactPath).isAbsolute) {
                        File(artifactPath)
                    } else {
                        File(workingPath, artifactPath)
                    }

                    if (!artifactFile.exists()) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /**
     * Build artifacts for platforms
     */
    suspend fun buildArtifacts(config: Config, platforms: Set<Platform>): BuildResult {
        return try {
            val androidBuilder = AndroidBuilder(processExecutor)
            val iosBuilder = IosBuilder(processExecutor)
            val buildManager = JvmBuildManager(androidBuilder, iosBuilder)
            val orchestrator = BuildOrchestrator(detector, buildManager)

            val result = orchestrator.build(
                config = config,
                platforms = platforms,
                verbose = verbose,
                dryRun = false
            )

            if (result.success) {
                PremiumLogger.header("Build Completed", "✓")
                result.outputs.forEach { output ->
                    if (output.success && output.outputPath != null) {
                        PremiumLogger.info("${output.platform}: ${output.outputPath}")
                    }
                }
            } else {
                PremiumLogger.simpleError("Build failed")
            }

            result
        } catch (e: Exception) {
            PremiumLogger.simpleError("Build error: ${e.message}")
            if (verbose) {
                e.printStackTrace()
            }
            BuildResult(success = false, outputs = emptyList())
        }
    }

    /**
     * Validate signing requirements
     */
    fun validateSigningRequirements(config: Config, buildResult: BuildResult) {
        val androidConfig = config.deploy?.android ?: return
        val androidBuild = buildResult.outputs.find { it.platform == Platform.ANDROID } ?: return

        if (!androidBuild.isSigned && androidConfig.playConsole.enabled) {
            showSigningWarning()

            print("  Proceed with unsigned APK? (y/n): ")
            val input = readLine()?.trim()?.lowercase()
            if (input != "y" && input != "yes") {
                PremiumLogger.simpleInfo("Deployment cancelled")
                throw Exception("Signing required for Play Console deployment")
            }

            PremiumLogger.simpleWarning("Disabling Play Console deployment")
        }

        if (androidBuild.warnings.isNotEmpty()) {
            println()
            PremiumLogger.simpleWarning("Build Warnings:")
            androidBuild.warnings.forEach { warning ->
                PremiumLogger.info("• $warning")
            }
            println()
        }
    }

    /**
     * Execute deployment to platforms
     */
    suspend fun executeDeploy(
        config: Config,
        platforms: Set<Platform>
    ): List<DeployResult> {
        val baseDir = File(workingPath)
        val allResults = mutableListOf<DeployResult>()

        platforms.forEach { platform ->
            when (platform) {
                Platform.ANDROID -> {
                    val results = deployAndroid(config, baseDir)
                    allResults.addAll(results)
                }

                Platform.IOS -> {
                    val results = deployIos(config, baseDir)
                    allResults.addAll(results)
                }
            }
        }

        return allResults
    }

    /**
     * Deploy Android artifacts
     */
    private suspend fun deployAndroid(config: Config, baseDir: File): List<DeployResult> {
        val androidConfig = config.deploy?.android ?: return emptyList()
        if (!androidConfig.enabled) return emptyList()

        val buildFlavor = config.build.android.defaultFlavor
        val buildType = config.build.android.defaultType

        // Collect artifacts
        val artifacts = mutableListOf<File>()
        var containsApk = false

        // APK if needed
        if (config.build.android.firebaseOutputType == "apk") {
            containsApk = true
            val apk = ArtifactDetector.resolveArtifact(
                path = androidConfig.artifactPath,
                artifactType = ArtifactType.APK,
                baseDir = baseDir,
                flavor = buildFlavor,
                type = buildType
            )
            if (apk != null) artifacts.add(apk)
        }

        // AAB if needed
        if (config.build.android.firebaseOutputType == "aab" ||
            androidConfig.playConsole.enabled) {
            val aab = ArtifactDetector.resolveArtifact(
                path = androidConfig.artifactPath,
                artifactType = ArtifactType.AAB,
                baseDir = baseDir,
                flavor = buildFlavor,
                type = buildType
            )
            if (aab != null) artifacts.add(aab)
        }

        if (artifacts.isEmpty()) {
            PremiumLogger.simpleError("Android artifact not found!")
            return emptyList()
        }

        // ✅ Use the concrete DeployOrchestrator
        val deploymentResults = deployOrchestrator.deployAndroid(
            androidConfig,
            artifacts,
            containsApk
        )

        return deploymentResults.individual
    }

    /**
     * Deploy iOS artifacts
     */
    private suspend fun deployIos(config: Config, baseDir: File): List<DeployResult> {
        val iosConfig = config.deploy?.ios ?: return emptyList()
        if (!iosConfig.enabled) return emptyList()

        val artifactFile = ArtifactDetector.resolveArtifact(
            path = iosConfig.artifactPath,
            artifactType = ArtifactType.IPA,
            baseDir = baseDir
        )

        if (artifactFile == null) {
            PremiumLogger.simpleError("iOS artifact not found!")
            return emptyList()
        }

        val deploymentResults = deployOrchestrator.deployIos(
            iosConfig,
            artifactFile.absolutePath
        )

        return deploymentResults.individual
    }

    private fun showSigningWarning() {
        PremiumLogger.box("Signing Required for Play Console",mapOf(
            "" to "Google Play Console requires signed APKs.",
            "" to "Unsigned APKs can only be deployed to Firebase.",
            "" to "",
            "" to "💡  Options:",
            "" to "   1. Set up keystore and rebuild",
            "" to "   2. Disable Play Console in mobilectl.yaml",
            "" to "   3. Continue with Firebase only"
        ), false)

    }
}

/**
 * Flavor selection options
 */
data class FlavorOptions(
    val allFlavors: Boolean = false,
    val group: String? = null,
    val flavors: String? = null,
    val exclude: String? = null
)
