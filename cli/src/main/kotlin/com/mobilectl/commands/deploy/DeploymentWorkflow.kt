package com.mobilectl.commands.deploy

import com.mobilectl.builder.AndroidBuilder
import com.mobilectl.builder.BuildOrchestrator
import com.mobilectl.builder.BuildResult
import com.mobilectl.builder.IosBuilder
import com.mobilectl.builder.JvmBuildManager
import com.mobilectl.commands.changelog.ChangelogGenerateHandler
import com.mobilectl.commands.version.VersionBumpHandler
import com.mobilectl.config.Config
import com.mobilectl.deploy.DeployOrchestrator
import com.mobilectl.detector.ProjectDetector
import com.mobilectl.model.Platform
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.deploy.DeployResult
import com.mobilectl.util.ArtifactDetector
import com.mobilectl.util.ArtifactType
import com.mobilectl.util.createProcessExecutor
import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

/**
 * Orchestrates the deployment workflow.
 * Single Responsibility: Workflow orchestration and business logic
 */
class DeploymentWorkflow(
    private val workingPath: String,
    private val detector: ProjectDetector,
    private val verbose: Boolean = false,
    private val allFlavors: Boolean = false,
    private val group: String? = null,
    private val flavors: String? = null,
    private val exclude: String? = null
) {
    private val out = PrintWriter(System.out, true, StandardCharsets.UTF_8)

    /**
     * Executes version bump before deployment
     */
    suspend fun bumpVersionBeforeDeploy(strategy: String, config: Config) {
        com.mobilectl.util.PremiumLogger.header("Pre-Flight: Version Bump ($strategy)", "🔧")

        val versionHandler = VersionBumpHandler(
            level = strategy,
            verbose = verbose,
            dryRun = false,
            skipBackup = false
        )
        versionHandler.execute()
    }

    /**
     * Executes changelog generation before deployment
     */
    suspend fun executeChangelogGeneration(config: Config) {
        com.mobilectl.util.PremiumLogger.header("Pre-Flight: Changelog Generation", "📝")

        val generator = ChangelogGenerateHandler(
            config.changelog.fromTag,
            verbose,
            false,
            config.changelog.append,
            config.changelog.useLastState
        )
        generator.execute()

        com.mobilectl.util.PremiumLogger.simpleSuccess("Changelog generated")
        println()
    }

    /**
     * Validates that a version bump strategy is valid
     */
    fun isValidStrategy(strategy: String): Boolean {
        return strategy in listOf("patch", "minor", "major", "auto", "manual")
    }

    /**
     * Parses platform string to Platform enum set
     */
    fun parsePlatforms(platformArg: String?, config: Config): Set<Platform>? {
        return when {
            platformArg != null -> {
                when (platformArg) {
                    "all" -> setOf(Platform.ANDROID, Platform.IOS)
                    "android" -> setOf(Platform.ANDROID)
                    "ios" -> setOf(Platform.IOS)
                    else -> {
                        com.mobilectl.util.PremiumLogger.simpleError("Unknown platform: $platformArg. Use 'android', 'ios', or 'all'")
                        null
                    }
                }
            }

            else -> {
                // Auto-detect from config
                val detected = mutableSetOf<Platform>()
                if (config.deploy?.android?.enabled == true) {
                    detected.add(Platform.ANDROID)
                }
                if (config.deploy?.ios?.enabled == true) {
                    detected.add(Platform.IOS)
                }

                if (detected.isEmpty()) {
                    com.mobilectl.util.PremiumLogger.simpleError("No platform is specified in config, smart detector will try to identify")
                    null
                } else {
                    detected
                }
            }
        }
    }

    /**
     * Selects flavors to deploy based on command-line options
     */
    fun selectFlavorsToDeploy(config: Config): List<String> {
        return when {
            allFlavors -> getFlavors(config.build)
            group != null -> config.deploy.flavorGroups[group]?.flavors ?: emptyList()
            flavors != null -> flavors.split(",").map { it.trim() }
            config.deploy.defaultGroup != null ->
                config.deploy.flavorGroups[config.deploy.defaultGroup]?.flavors
                    ?: emptyList()
            else -> listOf(config.build.android.defaultFlavor)
        }
    }

    private fun getFlavors(config: BuildConfig): List<String> {
        val allConfiguredFlavors = config.android.flavors
        return if (allConfiguredFlavors.isEmpty()) {
            out.println("⚠️  No flavors configured in config.build.android.flavors")
            listOf(config.android.defaultFlavor)
        } else {
            allConfiguredFlavors
        }
    }

    /**
     * Checks if build is needed by comparing source changes
     */
    fun checkIfBuildNeeded(config: Config, platforms: Set<Platform>): Boolean {
        platforms.forEach { platform ->
            val artifactPath = when (platform) {
                Platform.ANDROID -> config.deploy?.android?.artifactPath
                Platform.IOS -> config.deploy?.ios?.artifactPath
                else -> null
            } ?: return true

            val artifact = if (File(artifactPath).isAbsolute) {
                File(artifactPath)
            } else {
                File(File(workingPath), artifactPath)
            }

            // If artifact doesn't exist, need to build
            if (!artifact.exists()) {
                if (verbose) {
                    com.mobilectl.util.PremiumLogger.info("Artifact missing: $artifactPath")
                }
                return true
            }

            // Check if source files are newer than artifact
            val artifactTime = artifact.lastModified()
            val sourceHasChanges = hasSourceChanges(artifactTime, platform)

            if (sourceHasChanges) {
                if (verbose) {
                    com.mobilectl.util.PremiumLogger.info("Source files modified since last build")
                }
                return true
            }
        }

        return false
    }

    /**
     * Check if source has been modified since artifact was built
     */
    private fun hasSourceChanges(lastArtifactTime: Long, platform: Platform): Boolean {
        return try {
            // Try git first (works for both platforms)
            val process = ProcessBuilder("git", "status", "--porcelain")
                .directory(File(workingPath))
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                if (output.isNotBlank()) {
                    if (verbose) {
                        out.println("   Uncommitted changes detected")
                    }
                    return true
                }

                hasRecentSourceChanges(lastArtifactTime, platform)
            } else {
                if (verbose) {
                    out.println("   Git check failed, assuming rebuild needed")
                }
                true
            }
        } catch (e: Exception) {
            if (verbose) {
                out.println("   Fallback: checking file timestamps")
            }
            hasRecentSourceChanges(lastArtifactTime, platform)
        }
    }

    /**
     * Check if any platform-specific source files are newer than artifact
     */
    private fun hasRecentSourceChanges(lastArtifactTime: Long, platform: Platform): Boolean {
        val baseDir = File(workingPath)

        val sourcePatterns = when (platform) {
            Platform.ANDROID -> listOf(
                "src/**/*.kt", "src/**/*.java", "app/**/*.kt", "app/**/*.java",
                "build.gradle.kts", "build.gradle", "app/build.gradle.kts",
                "app/build.gradle", "settings.gradle.kts", "gradle.properties"
            )
            Platform.IOS -> listOf(
                "ios/**/*.swift", "ios/**/*.h", "ios/**/*.m",
                "ios/**/*.xcconfig", "ios/**/*.pbxproj",
                "ios/Podfile", "ios/Podfile.lock", "Podfile", "Podfile.lock"
            )
            else -> emptyList()
        }

        return sourcePatterns.any { pattern ->
            val glob = pattern.replace("**", "*")
            val dir = if (pattern.contains("**")) {
                baseDir.resolve(pattern.substringBefore("**").trimEnd('/'))
            } else {
                baseDir.resolve(pattern)
            }

            when {
                dir.isDirectory -> {
                    dir.walk()
                        .filter { it.isFile }
                        .any { it.lastModified() > lastArtifactTime }
                }
                dir.isFile -> {
                    dir.lastModified() > lastArtifactTime
                }
                else -> false
            }
        }
    }

    /**
     * Builds artifacts for specified platforms
     */
    suspend fun buildArtifacts(config: Config, platforms: Set<Platform>): BuildResult {
        return try {
            val processExecutor = createProcessExecutor()
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
                com.mobilectl.util.PremiumLogger.header("Build Completed", "✓")
                result.outputs.forEach { output ->
                    if (output.success && output.outputPath != null) {
                        com.mobilectl.util.PremiumLogger.info("${output.platform}: ${output.outputPath}")
                    }
                }
            } else {
                com.mobilectl.util.PremiumLogger.simpleError("Build failed")
            }

            result
        } catch (e: Exception) {
            com.mobilectl.util.PremiumLogger.simpleError("Build error: ${e.message}")
            if (verbose) {
                e.printStackTrace()
            }
            BuildResult(success = false, outputs = emptyList())
        }
    }

    /**
     * Validates signing requirements before deployment
     */
    fun validateSigningRequirements(config: Config, buildResult: BuildResult) {
        val androidConfig = config.deploy?.android ?: return
        val androidBuild = buildResult.outputs.find { it.platform == Platform.ANDROID } ?: return

        if (!androidBuild.isSigned && androidConfig.playConsole.enabled) {
            val red = "\u001B[31m"
            val gray = "\u001B[90m"
            val white = "\u001B[97m"
            val yellow = "\u001B[33m"
            val cyan = "\u001B[36m"
            val dim = "\u001B[2m"
            val bold = "\u001B[1m"
            val reset = "\u001B[0m"

            println()
            println("$gray┌─────────────────────────────────────────────────────────┐$reset")
            println("$gray│$reset  $red✗$reset  ${bold}${white}Signing Required for Play Console$reset")
            println("$gray├─────────────────────────────────────────────────────────┤$reset")
            println("$gray│$reset")
            println("$gray│$reset  Google Play Console requires signed APKs.")
            println("$gray│$reset  Unsigned APKs can only be deployed to Firebase.")
            println("$gray│$reset")
            println("$gray│$reset  ${yellow}💡$reset  ${bold}Options:$reset")
            println("$gray│$reset     ${dim}1. Set up keystore and rebuild$reset")
            println("$gray│$reset     ${dim}2. Disable Play Console in mobileops.yaml$reset")
            println("$gray│$reset     ${dim}3. Continue with Firebase only$reset")
            println("$gray└─────────────────────────────────────────────────────────┘$reset")
            println()

            out.print("  $cyan?$reset ${dim}Proceed with unsigned APK?$reset (y/n): ")
            out.flush()

            val input = readLine()?.trim()?.lowercase()
            if (input != "y" && input != "yes") {
                com.mobilectl.util.PremiumLogger.simpleInfo("Deployment cancelled")
                throw Exception("Signing required for Play Console deployment")
            }

            com.mobilectl.util.PremiumLogger.simpleWarning("Disabling Play Console deployment")
        }

        if (androidBuild.warnings.isNotEmpty()) {
            println()
            com.mobilectl.util.PremiumLogger.simpleWarning("Build Warnings:")
            androidBuild.warnings.forEach { warning ->
                com.mobilectl.util.PremiumLogger.info("• $warning")
            }
            println()
        }
    }

    /**
     * Executes deployment to platforms
     */
    suspend fun executeDeploy(config: Config, platforms: Set<Platform>, env: String): MutableList<DeployResult> {
        val orchestrator = DeployOrchestrator()
        val baseDir = File(workingPath)
        val allResults = mutableListOf<DeployResult>()

        platforms.forEach { platform ->
            when (platform) {
                Platform.ANDROID -> {
                    val androidConfig = config.deploy?.android
                    if (androidConfig != null && androidConfig.enabled) {
                        // Get flavor and type from build config for cache lookup
                        val buildFlavor = config.build.android.defaultFlavor
                        val buildType = config.build.android.defaultType

                        val artifactFile = ArtifactDetector.resolveArtifact(
                            path = androidConfig.artifactPath,
                            artifactType = ArtifactType.APK,
                            baseDir = baseDir,
                            flavor = buildFlavor,
                            type = buildType
                        )

                        if (artifactFile == null) {
                            com.mobilectl.util.PremiumLogger.simpleError("Android artifact not found!")
                            return@forEach
                        }

                        val deploymentResults = orchestrator.deployAndroid(
                            androidConfig,
                            artifactFile.absolutePath
                        )
                        allResults.addAll(deploymentResults.individual)
                    }
                }

                Platform.IOS -> {
                    val iosConfig = config.deploy?.ios
                    if (iosConfig != null && iosConfig.enabled) {
                        val artifactFile = ArtifactDetector.resolveArtifact(
                            path = iosConfig.artifactPath,
                            artifactType = ArtifactType.IPA,
                            baseDir = baseDir
                        )

                        if (artifactFile == null) {
                            com.mobilectl.util.PremiumLogger.simpleError("iOS artifact not found!")
                            return@forEach
                        }

                        val deploymentResults = orchestrator.deployIos(
                            iosConfig,
                            artifactFile.absolutePath
                        )
                        allResults.addAll(deploymentResults.individual)
                    }
                }

                else -> {}
            }
        }

        return allResults
    }
}
