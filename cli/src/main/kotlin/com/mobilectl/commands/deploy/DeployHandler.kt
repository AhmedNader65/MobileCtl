package com.mobilectl.commands.deploy

import com.mobilectl.builder.AndroidBuilder
import com.mobilectl.builder.BuildOrchestrator
import com.mobilectl.builder.BuildResult
import com.mobilectl.builder.IosBuilder
import com.mobilectl.builder.JvmBuildManager
import com.mobilectl.config.Config
import com.mobilectl.config.ConfigLoader
import com.mobilectl.deploy.DeployOrchestrator
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.model.Platform
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.deploy.AndroidDeployConfig
import com.mobilectl.model.deploy.AppStoreDestination
import com.mobilectl.model.deploy.DeployConfig
import com.mobilectl.model.deploy.DeployResult
import com.mobilectl.model.deploy.FirebaseAndroidDestination
import com.mobilectl.model.deploy.IosDeployConfig
import com.mobilectl.model.deploy.LocalAndroidDestination
import com.mobilectl.model.deploy.PlayConsoleAndroidDestination
import com.mobilectl.model.deploy.TestFlightDestination
import com.mobilectl.model.versionManagement.VersionConfig
import com.mobilectl.util.ArtifactDetector
import com.mobilectl.util.ArtifactType
import com.mobilectl.util.createFileUtil
import com.mobilectl.util.createProcessExecutor
import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

class DeployHandler(
    private val platform: String?,
    private val destination: String?,
    private val environment: String?,
    private val releaseNotes: String?,
    private val testGroups: String?,
    private val verbose: Boolean,
    private val dryRun: Boolean,
    private val skipBuild: Boolean,
    private val interactive: Boolean,
    private val confirm: Boolean
) {
    private val out = PrintWriter(System.out, true, StandardCharsets.UTF_8)
    private val workingPath = System.getProperty("user.dir")

    suspend fun execute() {
        try {
            if (!File(workingPath).exists()) {
                out.println("❌ Directory does not exist: $workingPath")
                return
            }

            // 1. Load config
            val baseConfig = loadConfigOrUseDefaults()
            var config = applyCommandLineOverrides(baseConfig)

            // 2. Interactive mode
            if (interactive) {
                config = runInteractiveWizard(config) ?: return
            }

            // 3. Parse platforms
            val targetPlatforms = parsePlatforms(platform, config)
            if (targetPlatforms == null) {
                out.println("❌ Could not determine platforms")
                return
            }

            // 4. Detect environment
            val actualEnvironment = environment ?: detectEnvironment()

            // 5. Show summary
            printSummary(targetPlatforms, actualEnvironment)

            // 6. Ask for confirmation
            if (!confirm && !dryRun && !interactive) {
                if (!askForConfirmation()) {
                    out.println("⏭️  Deployment cancelled")
                    return
                }
            }

            // 7. Dry-run mode
            if (dryRun) {
                out.println("📋 DRY-RUN mode - nothing will be deployed")
                showDryRunDetails(config, targetPlatforms, actualEnvironment)
                return
            }

            // 8. Build if needed
            if (!skipBuild) {
                val needsBuild = checkIfBuildNeeded(config, targetPlatforms)
                if (needsBuild) {
                    out.println()
                    out.println("🏗️  Building artifacts...")
                    val buildResult = buildArtifacts(config, targetPlatforms)

                    if (!buildResult.success) {
                        out.println("❌ Build failed!")
                        return
                    }

                    // Check signing requirements
                    validateSigningRequirements(config, buildResult)
                } else {
                    out.println()
                    out.println("✅ Artifacts up-to-date (no source changes detected)")
                }
            }

            // 9. Deploy
            executeDeploy(config, targetPlatforms, actualEnvironment)

        } catch (e: Exception) {
            out.println("❌ Error: ${e.message}")
            if (verbose) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Validate signing requirements before deployment
     */
    private fun validateSigningRequirements(config: Config, buildResult: BuildResult) {
        val androidConfig = config.deploy?.android ?: return
        val androidBuild = buildResult.outputs.find { it.platform == Platform.ANDROID } ?: return

        // Check if unsigned APK being deployed to Play Console
        if (!androidBuild.isSigned && androidConfig.playConsole.enabled) {
            out.println()
            out.println("═══════════════════════════════════════════════════")
            out.println("❌ SIGNING REQUIRED FOR PLAY CONSOLE")
            out.println("═══════════════════════════════════════════════════")
            out.println()
            out.println("Google Play Console requires signed APKs.")
            out.println("Unsigned APKs can only be deployed to Firebase.")
            out.println()

            out.println("💡 Options:")
            out.println("   1. Set up keystore and rebuild")
            out.println("   2. Disable Play Console in mobilectl.yaml")
            out.println("   3. Continue with Firebase only (press Enter)")
            out.println()

            out.print("? Proceed with unsigned APK? (y/n): ")
            out.flush()

            val input = readLine()?.trim()?.lowercase()
            if (input != "y" && input != "yes") {
                out.println("⏭️  Deployment cancelled")
                throw Exception("Signing required for Play Console deployment")
            }

            // Disable Play Console
            out.println("⚠️  Disabling Play Console deployment")
        }

        // Show warnings
        if (androidBuild.warnings.isNotEmpty()) {
            out.println()
            out.println("⚠️  Build Warnings:")
            androidBuild.warnings.forEach { warning ->
                out.println("   • $warning")
            }
            out.println()
        }
    }

    /**
     * Check if build is needed by comparing git timestamps
     */
    private fun checkIfBuildNeeded(config: Config, platforms: Set<Platform>): Boolean {
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
                    out.println("ℹ️  Artifact missing: $artifactPath")
                }
                return true
            }

            // Check if source files are newer than artifact
            val artifactTime = artifact.lastModified()
            val sourceHasChanges = hasSourceChanges(artifactTime, platform)

            if (sourceHasChanges) {
                if (verbose) {
                    out.println("ℹ️  Source files modified since last build")
                }
                return true
            }
        }

        return false
    }

    /**
     * Check if source has been modified since artifact was built
     * Platform-specific checking
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
                // Kotlin/Java source
                "src/**/*.kt",
                "src/**/*.java",
                "app/**/*.kt",
                "app/**/*.java",

                // Gradle config
                "build.gradle.kts",
                "build.gradle",
                "app/build.gradle.kts",
                "app/build.gradle",
                "settings.gradle.kts",
                "gradle.properties"
            )

            Platform.IOS -> listOf(
                // Swift source
                "ios/**/*.swift",
                "ios/**/*.h",
                "ios/**/*.m",

                // Xcode config
                "ios/**/*.xcconfig",
                "ios/**/*.pbxproj",
                "ios/Podfile",
                "ios/Podfile.lock",
                "Podfile",
                "Podfile.lock"
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
     * Build using existing BuildOrchestrator
     */
    private suspend fun buildArtifacts(
        config: Config,
        platforms: Set<Platform>
    ): BuildResult {
        return try {
            val detector = createProjectDetector()
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
            out.println("❌ Build error: ${e.message}")
            if (verbose) {
                e.printStackTrace()
            }
            BuildResult(success = false, outputs = emptyList())
        }
    }

    /**
     * Load config or create defaults
     */
    private suspend fun loadConfigOrUseDefaults(): Config {
        val configFile = File(workingPath, "mobilectl.yaml")

        return if (configFile.exists()) {
            try {
                val fileUtil = createFileUtil()
                val detector = createProjectDetector()
                val configLoader = ConfigLoader(fileUtil, detector)
                configLoader.loadConfig(configFile.absolutePath).getOrNull() ?: createSmartDefaults()
            } catch (e: Exception) {
                if (verbose) out.println("⚠️  Failed to load config: ${e.message}")
                createSmartDefaults()
            }
        } else {
            if (verbose) out.println("ℹ️  No config file found, using auto-detected defaults")
            createSmartDefaults()
        }
    }

    /**
     * Apply command-line overrides
     */
    private fun applyCommandLineOverrides(config: Config): Config {
        var updated = config

        // Override destination
        if (destination != null) {
            updated = applyDestinationOverride(updated, destination)
        }

        // Override test groups
        if (testGroups != null) {
            val groups = testGroups.split(",").map { it.trim() }
            updated.deploy.android?.firebase?.testGroups = groups
        }

        return updated
    }

    /**
     * Apply destination override
     */
    private fun applyDestinationOverride(config: Config, dest: String): Config {
        val normalizedDest = dest.lowercase()
        var updated = config

        // Android
        updated.deploy.android?.let { android ->
            updated = updated.copy(
                deploy = updated.deploy.copy(
                    android = android.copy(
                        firebase = android.firebase.copy(enabled = normalizedDest == "firebase"),
                        playConsole = android.playConsole.copy(enabled = normalizedDest == "play-console"),
                        local = android.local.copy(enabled = normalizedDest == "local")
                    )
                )
            )
        }

        // iOS
        updated.deploy.ios?.let { ios ->
            updated = updated.copy(
                deploy = updated.deploy.copy(
                    ios = ios.copy(
                        testflight = ios.testflight.copy(enabled = normalizedDest == "testflight"),
                        appStore = ios.appStore.copy(enabled = normalizedDest == "app-store")
                    )
                )
            )
        }

        return updated
    }
    private fun parsePlatforms(platformArg: String?, config: Config): Set<Platform>? {
        return when {
            platformArg != null -> {
                when (platformArg) {
                    "all" -> setOf(Platform.ANDROID, Platform.IOS)
                    "android" -> setOf(Platform.ANDROID)
                    "ios" -> setOf(Platform.IOS)
                    else -> {
                        out.println("❌ Unknown platform: $platformArg. Use 'android', 'ios', or 'all'")
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
                    out.println("❌ No platforms detected. Specify 'android', 'ios', or 'all'")
                    null
                } else {
                    detected
                }
            }
        }
    }

    private fun showDryRunDetails(config: Config, platforms: Set<Platform>, env: String) {
        out.println()
        out.println("📋 Deployment Plan:")
        out.println()

        platforms.forEach { platform ->
            when (platform) {
                Platform.ANDROID -> showAndroidPlan(config, env)
                Platform.IOS -> showIosPlan(config, env)
                else -> {}
            }
        }
    }

    private fun showAndroidPlan(config: Config, env: String) {
        out.println("📱 Android:")

        val androidConfig = config.deploy?.android
        if (androidConfig == null) {
            out.println("   ⚠️  No Android deployment configured")
            return
        }

        if (androidConfig.firebase.enabled) {
            out.println("   ✓ Firebase App Distribution")
            out.println("     - Service Account: ${androidConfig.firebase.serviceAccount}")
            out.println("     - Test Groups: ${androidConfig.firebase.testGroups.joinToString()}")
        }

        if (androidConfig.playConsole.enabled) {
            out.println("   ✓ Google Play Console")
        }

        if (androidConfig.local.enabled) {
            out.println("   ✓ Local filesystem")
            out.println("     - Output: ${androidConfig.local.outputDir}")
        }

        out.println()
    }

    private fun showIosPlan(config: Config, env: String) {
        out.println("🍎 iOS:")
        val iosConfig = config.deploy?.ios
        if (iosConfig == null) {
            out.println("   ⚠️  No iOS deployment configured")
            return
        }

        if (iosConfig.testflight.enabled) {
            out.println("   ✓ TestFlight")
        }

        if (iosConfig.appStore.enabled) {
            out.println("   ✓ App Store")
        }

        out.println()
    }

    private suspend fun executeDeploy(config: Config, platforms: Set<Platform>, env: String) {
        com.mobilectl.util.PremiumLogger.header("Starting Deployment", "🚀")
        com.mobilectl.util.PremiumLogger.info("Environment: $env")
        com.mobilectl.util.PremiumLogger.info("Platforms: ${platforms.joinToString(", ")}")
        println()

        val orchestrator = DeployOrchestrator()
        val baseDir = File(workingPath)
        val allResults = mutableListOf<DeployResult>()

        platforms.forEach { platform ->
            when (platform) {
                Platform.ANDROID -> {
                    val androidConfig = config.deploy?.android
                    if (androidConfig != null && androidConfig.enabled) {
                        val artifactFile = ArtifactDetector.resolveArtifact(
                            path = androidConfig.artifactPath,
                            artifactType = ArtifactType.APK,
                            baseDir = baseDir
                        )

                        if (artifactFile == null) {
                            out.println("❌ Android artifact not found!")
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
                            out.println("❌ iOS artifact not found!")
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

        printDeployResults(allResults, verbose, workingPath)
    }

    private fun resolveArtifactPath(path: String): File {
        return if (File(path).isAbsolute) {
            File(path)
        } else {
            File(workingPath, path)
        }
    }

    private fun createSmartDefaults(): Config {
        val baseDir = File(workingPath)

        val firebaseConfig = autoDetectFirebaseConfig()

        val androidArtifact = ArtifactDetector.resolveArtifact(
            path = null,
            artifactType = ArtifactType.APK,
            baseDir = baseDir
        )

        if (androidArtifact != null && ArtifactDetector.isDebugBuild(androidArtifact)) {
            out.println("⚠️  WARNING: Debug APK detected!")
            out.println("   This is meant for testing only")
            out.println("   For production, build a release APK: ./gradlew assembleRelease")
            out.println()
        }

        val iosArtifact = ArtifactDetector.resolveArtifact(
            path = null,
            artifactType = ArtifactType.IPA,
            baseDir = baseDir
        )

        val androidArtifactPath = androidArtifact?.absolutePath
            ?: "build/outputs/apk/release/app-release.apk"

        val iosArtifactPath = iosArtifact?.absolutePath
            ?: "build/outputs/ipa/release/app.ipa"

        if (verbose) {
            if (androidArtifact != null) {
                out.println("✓ Auto-detected Android artifact: ${androidArtifact.name}")
            }
            if (iosArtifact != null) {
                out.println("✓ Auto-detected iOS artifact: ${iosArtifact.name}")
            }
        }

        val androidConfig = AndroidDeployConfig(
            enabled = true,
            artifactPath = androidArtifactPath,
            firebase = firebaseConfig,
            playConsole = PlayConsoleAndroidDestination(enabled = false),
            local = LocalAndroidDestination(enabled = false)
        )

        val iosConfig = IosDeployConfig(
            enabled = false,
            artifactPath = iosArtifactPath,
            testflight = TestFlightDestination(enabled = false),
            appStore = AppStoreDestination(enabled = false)
        )

        val deployConfig = DeployConfig(
            android = androidConfig,
            ios = iosConfig
        )

        return Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = deployConfig
        )
    }

    private fun autoDetectFirebaseConfig(): FirebaseAndroidDestination {
        return try {
            val googleServicesFile = findGoogleServicesJson()
            val serviceAccountFile = findServiceAccountJson()

            if (serviceAccountFile == null) {
                if (verbose) {
                    out.println("ℹ️  Firebase: Service account not found (will fail at deploy)")
                }
                return FirebaseAndroidDestination(
                    enabled = true,
                    serviceAccount = "credentials/firebase-service-account.json"
                )
            }

            if (verbose) {
                out.println("✓ Auto-detected Firebase: ${serviceAccountFile.absolutePath}")
                if (googleServicesFile != null) {
                    out.println("✓ Auto-detected google-services.json: ${googleServicesFile.absolutePath}")
                }
            }

            FirebaseAndroidDestination(
                enabled = true,
                serviceAccount = serviceAccountFile.absolutePath,
                googleServices = googleServicesFile?.absolutePath
            )

        } catch (e: Exception) {
            if (verbose) {
                out.println("⚠️  Firebase auto-detection failed: ${e.message}")
            }
            FirebaseAndroidDestination()
        }
    }

    private fun findGoogleServicesJson(): File? {
        val knownPaths = listOf(
            "app/google-services.json",
            "app/src/main/google-services.json",
            "google-services.json"
        )
        return knownPaths
            .map { File(workingPath, it) }
            .firstOrNull { it.exists() }
    }

    private fun findServiceAccountJson(): File? {
        val knownPaths = listOf(
            "credentials/firebase-service-account.json",
            "credentials/firebase-account.json",
            "credentials/account.json",
            "firebase-service-account.json",
            "firebase-account.json"
        )
        return knownPaths
            .map { File(workingPath, it) }
            .firstOrNull { it.exists() }
    }

    private fun detectEnvironment(): String {
        return try {
            val process = ProcessBuilder("git", "branch", "--show-current")
                .redirectErrorStream(true)
                .start()
            val branch = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            when {
                branch == "main" || branch == "master" -> "production"
                branch.startsWith("staging") -> "staging"
                else -> "dev"
            }
        } catch (e: Exception) {
            "dev"
        }
    }

    private fun printSummary(targetPlatforms: Set<Platform>, env: String) {
        out.println("🚀 Deploying to: ${targetPlatforms.joinToString(", ") { it.name }}")
        out.println("🌍 Environment: $env")

        if (destination != null) {
            out.println("📍 Destination: $destination")
        }

        if (releaseNotes != null) {
            out.println("📝 Release notes: $releaseNotes")
        }

        if (testGroups != null) {
            out.println("👥 Test groups: $testGroups")
        }

        if (skipBuild) {
            out.println("⏭️  Skipping build (using existing artifacts)")
        }

        out.println()
    }


    /**
     * Run interactive deployment wizard
     */
    private fun runInteractiveWizard(baseConfig: Config): Config? {
        val wizard = InteractiveDeploymentWizard(out)

        val availablePlatforms = mutableSetOf<Platform>()
        if (baseConfig.deploy?.android?.enabled == true) availablePlatforms.add(Platform.ANDROID)
        if (baseConfig.deploy?.ios?.enabled == true) availablePlatforms.add(Platform.IOS)

        // Step 1: Select platforms
        val selectedPlatforms = wizard.selectPlatforms(availablePlatforms) ?: return null

        // Step 2: Select destinations per platform
        val destinationMap = mutableMapOf<Platform, List<String>>()

        selectedPlatforms.forEach { platform ->
            val availableDests = when (platform) {
                Platform.ANDROID -> AvailableDestinations.forAndroid()
                Platform.IOS -> AvailableDestinations.forIos()
                else -> emptyList()
            }

            val selectedDests = wizard.selectDestinations(platform, availableDests) ?: emptyList()
            if (selectedDests.isNotEmpty()) {
                destinationMap[platform] = selectedDests
            }
        }

        if (destinationMap.isEmpty()) {
            out.println("❌ No destinations selected")
            return null
        }

        // Step 3: Get optional parameters
        val newReleaseNotes = wizard.getReleaseNotes(releaseNotes)
        val newTestGroups = wizard.getTestGroups(
            testGroups?.split(",")?.map { it.trim() } ?: emptyList()
        )

        // Step 4: Confirm
        val confirmed = wizard.confirmDeployment(
            platforms = selectedPlatforms,
            destinations = destinationMap,
            releaseNotes = newReleaseNotes,
            testGroups = newTestGroups,
            environment = environment ?: detectEnvironment()
        )

        if (!confirmed) {
            out.println("⏭️  Deployment cancelled")
            return null
        }

        // Apply interactive selections to config
        var config = baseConfig

        // Update destinations
        selectedPlatforms.forEach { platform ->
            val destinations = destinationMap[platform] ?: emptyList()

            when (platform) {
                Platform.ANDROID -> {
                    val androidConfig = config.deploy.android ?: return@forEach
                    config = config.copy(
                        deploy = config.deploy.copy(
                            android = androidConfig.copy(
                                firebase = androidConfig.firebase.copy(
                                    enabled = "firebase" in destinations
                                ),
                                playConsole = androidConfig.playConsole.copy(
                                    enabled = "play-console" in destinations
                                ),
                                local = androidConfig.local.copy(
                                    enabled = "local" in destinations
                                )
                            )
                        )
                    )
                }
                Platform.IOS -> {
                    val iosConfig = config.deploy.ios ?: return@forEach
                    config = config.copy(
                        deploy = config.deploy.copy(
                            ios = iosConfig.copy(
                                testflight = iosConfig.testflight.copy(
                                    enabled = "testflight" in destinations
                                ),
                                appStore = iosConfig.appStore.copy(
                                    enabled = "app-store" in destinations
                                )
                            )
                        )
                    )
                }
                else -> {}
            }
        }

        // Apply new parameters
        if (newReleaseNotes != null) {
            config.deploy.android?.firebase?.releaseNotes = newReleaseNotes
        }

        return config
    }

    private fun askForConfirmation(): Boolean {
        out.println()
        out.print("? Proceed with deployment? (Y/n) ")
        out.flush()

        val input = readLine()?.trim()?.lowercase() ?: "y"
        return input != "n" && input != "no"
    }
}
