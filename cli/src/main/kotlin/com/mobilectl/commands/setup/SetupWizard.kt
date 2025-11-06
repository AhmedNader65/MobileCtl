package com.mobilectl.commands.setup

import com.github.ajalt.mordant.terminal.Terminal
import com.mobilectl.config.Config
import com.mobilectl.model.appMetadata.AppConfig
import com.mobilectl.model.buildConfig.AndroidBuildConfig
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.buildConfig.IosBuildConfig
import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.deploy.DeployConfig
import com.mobilectl.model.deploy.AndroidDeployConfig
import com.mobilectl.model.deploy.FirebaseAndroidDestination
import com.mobilectl.model.deploy.PlayConsoleAndroidDestination
import com.mobilectl.model.deploy.LocalAndroidDestination
import com.mobilectl.model.deploy.IosDeployConfig
import com.mobilectl.model.deploy.TestFlightDestination
import com.mobilectl.model.deploy.AppStoreDestination
import com.mobilectl.model.deploy.FlavorGroup
import com.mobilectl.model.notifications.NotifyConfig
import com.mobilectl.model.report.ReportConfig
import com.mobilectl.model.versionManagement.VersionConfig

/**
 * Comprehensive setup wizard that guides users through complete mobilectl configuration.
 * Generates a full mobileops.yaml file from scratch in 8 phases:
 * 1. Project Information
 * 2. Build Configuration
 * 3. Deployment Destinations
 * 4. Version Management
 * 5. Changelog
 * 6. Deployment Groups
 * 7. CI/CD Setup
 * 8. Review & Confirm
 */
class SetupWizard(
    private val terminal: Terminal,
    private val projectDetector: ProjectDetector
) {

    private val wizardState = WizardState()

    /**
     * Run the complete setup wizard
     */
    fun run(): SetupResult {
        showWelcome()

        // Phase 1: Project Information
        wizardState.projectInfo = phaseProjectInformation()

        // Phase 2: Build Configuration
        wizardState.buildConfig = phaseBuildConfiguration()

        // Phase 3: Deployment Destinations
        wizardState.deployConfig = phaseDeploymentDestinations()

        // Phase 4: Version Management
        wizardState.versionConfig = phaseVersionManagement()

        // Phase 5: Changelog
        wizardState.changelogConfig = phaseChangelog()

        // Phase 6: Deployment Groups
        wizardState.flavorGroups = phaseDeploymentGroups()

        // Phase 7: CI/CD Setup
        wizardState.cicdConfig = phaseCICD()

        // Phase 8: Review & Confirm
        val confirmed = phaseReviewAndConfirm()

        if (!confirmed) {
            terminal.println("\n❌ Setup cancelled.")
            return SetupResult.Cancelled
        }

        // Generate the config
        val config = generateConfig()

        return SetupResult.Success(
            config = config,
            generateGitHubActions = wizardState.cicdConfig.generateGitHubActions,
            generateGitLabCI = wizardState.cicdConfig.generateGitLabCI,
            setupSummary = generateSetupSummary()
        )
    }

    // ========================================================================
    // PHASE 1: PROJECT INFORMATION
    // ========================================================================

    private fun phaseProjectInformation(): ProjectInfo {
        terminal.println("\n$PHASE_HEADER")
        terminal.println("1️⃣  PROJECT INFORMATION")
        terminal.println(SEPARATOR)

        // Detect project type
        val detectedType = projectDetector.detectProjectType()
        terminal.println("\n📁 Project Type")
        val projectType = if (detectedType != null) {
            terminal.println("Auto-detected: ${detectedType.displayName}")
            if (promptYesNo("Use this?", default = true)) {
                detectedType
            } else {
                promptProjectType()
            }
        } else {
            promptProjectType()
        }

        // App name
        terminal.println("\n📝 App Name")
        val detectedName = projectDetector.detectAppName()
        val appName = if (detectedName != null) {
            promptWithDefault("App name", detectedName)
        } else {
            promptRequired("App name")
        }

        // Package name
        terminal.println("\n📦 Package Name")
        val detectedPackage = projectDetector.detectPackageName()
        val packageName = if (detectedPackage != null) {
            terminal.println("Auto-detected: $detectedPackage")
            if (promptYesNo("Use this?", default = true)) {
                detectedPackage
            } else {
                promptRequired("Package name (e.g., com.example.myapp)")
            }
        } else {
            promptRequired("Package name (e.g., com.example.myapp)")
        }

        // Organization
        terminal.println("\n🏢 Organization")
        val organization = promptWithDefault("Organization", "My Company")

        // Current version
        terminal.println("\n📍 Current Version")
        val detectedVersion = projectDetector.detectVersion()
        val version = if (detectedVersion != null) {
            promptWithDefault("Version", detectedVersion)
        } else {
            promptWithDefault("Version", "1.0.0")
        }

        return ProjectInfo(
            type = projectType,
            appName = appName,
            packageName = packageName,
            bundleId = packageName, // Same as package for iOS
            organization = organization,
            version = version
        )
    }

    // ========================================================================
    // PHASE 2: BUILD CONFIGURATION
    // ========================================================================

    private fun phaseBuildConfiguration(): BuildConfigData {
        terminal.println("\n$PHASE_HEADER")
        terminal.println("2️⃣  BUILD SETUP")
        terminal.println(SEPARATOR)

        val androidConfig = if (wizardState.projectInfo.type.supportsAndroid) {
            configureAndroidBuild()
        } else null

        val iosConfig = if (wizardState.projectInfo.type.supportsIos) {
            configureIosBuild()
        } else null

        return BuildConfigData(
            android = androidConfig,
            ios = iosConfig
        )
    }

    private fun configureAndroidBuild(): AndroidBuildData {
        terminal.println("\n🤖 Android Build Configuration")

        // Detect flavors
        val detectedFlavors = projectDetector.detectAndroidFlavors()
        val flavors = if (detectedFlavors.isNotEmpty()) {
            terminal.println("\n🎯 Product Flavors")
            terminal.println("Detected from build.gradle.kts:")
            detectedFlavors.forEach { terminal.println("  ✓ $it") }

            val additionalFlavors = mutableListOf<String>()
            if (promptYesNo("Add more?", default = false)) {
                terminal.println("Enter flavor names (comma-separated):")
                val input = readLine()?.trim() ?: ""
                additionalFlavors.addAll(input.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            }

            detectedFlavors + additionalFlavors
        } else {
            terminal.println("\n🎯 Product Flavors")
            terminal.println("No flavors detected.")

            if (promptYesNo("Add flavors?", default = false)) {
                terminal.println("Enter flavor names (comma-separated):")
                val input = readLine()?.trim() ?: ""
                input.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                emptyList()
            }
        }

        // Default flavor
        val defaultFlavor = if (flavors.isNotEmpty()) {
            terminal.println("\n🔐 Default Flavor")
            promptChoice("Which is default?", flavors, default = flavors.first())
        } else {
            null
        }

        // Signing configuration
        terminal.println("\n🔒 Signing Configuration")
        val keyStorePath = promptWithDefault("Keystore path", "release.jks")
        val keyAlias = promptWithDefault("Key alias", "release-key")
        terminal.println("(Passwords will be set via environment variables)")

        return AndroidBuildData(
            enabled = true,
            flavors = flavors,
            defaultFlavor = defaultFlavor ?: "release",
            defaultType = "release",
            keyStorePath = keyStorePath,
            keyAlias = keyAlias,
            useEnvForPasswords = true
        )
    }

    private fun configureIosBuild(): IosBuildData {
        terminal.println("\n🍎 iOS Build Configuration")

        // Detect project path
        val detectedProjectPath = projectDetector.detectIosProjectPath()
        val projectPath = if (detectedProjectPath != null) {
            promptWithDefault("Project/Workspace path", detectedProjectPath)
        } else {
            promptRequired("Project/Workspace path (e.g., ios/MyApp.xcworkspace)")
        }

        // Scheme
        val detectedScheme = projectDetector.detectIosScheme()
        val scheme = if (detectedScheme != null) {
            promptWithDefault("Scheme", detectedScheme)
        } else {
            promptRequired("Scheme")
        }

        // Configuration
        val configuration = promptChoice(
            "Build configuration",
            listOf("Release", "Debug"),
            default = "Release"
        )

        // Code signing
        terminal.println("\n🔐 Code Signing")
        val codeSignIdentity = promptWithDefault(
            "Code sign identity",
            "iPhone Distribution"
        )
        val provisioningProfile = promptRequired("Provisioning profile name")

        return IosBuildData(
            enabled = true,
            projectPath = projectPath,
            scheme = scheme,
            configuration = configuration,
            codeSignIdentity = codeSignIdentity,
            provisioningProfile = provisioningProfile
        )
    }

    // ========================================================================
    // PHASE 3: DEPLOYMENT DESTINATIONS
    // ========================================================================

    private fun phaseDeploymentDestinations(): DeployConfigData {
        terminal.println("\n$PHASE_HEADER")
        terminal.println("3️⃣  DEPLOYMENT DESTINATIONS")
        terminal.println(SEPARATOR)
        terminal.println("\nSelect where to deploy:")

        val androidDestinations = if (wizardState.projectInfo.type.supportsAndroid) {
            configureAndroidDestinations()
        } else null

        val iosDestinations = if (wizardState.projectInfo.type.supportsIos) {
            configureIosDestinations()
        } else null

        return DeployConfigData(
            android = androidDestinations,
            ios = iosDestinations
        )
    }

    private fun configureAndroidDestinations(): AndroidDestinationsData {
        terminal.println("\n🤖 Android Deployment")

        // Firebase
        terminal.println("\n🔥 Firebase App Distribution")
        val firebaseCredentials = projectDetector.detectFirebaseCredentials()
        val firebaseEnabled = if (firebaseCredentials != null) {
            terminal.println("Credentials found: $firebaseCredentials ✓")
            promptYesNo("Enable?", default = true)
        } else {
            terminal.println("No credentials found.")
            promptYesNo("Enable?", default = false)
        }

        val firebaseData = if (firebaseEnabled) {
            val serviceAccount = if (firebaseCredentials != null) {
                firebaseCredentials
            } else {
                promptRequired("Service account JSON path")
            }

            val googleServices = projectDetector.detectGoogleServicesJson()
                ?: "app/google-services.json"

            terminal.println("Test groups (comma-separated):")
            val testGroups = readLine()?.trim()
                ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                ?: listOf("qa-team")

            FirebaseDestinationData(
                enabled = true,
                serviceAccount = serviceAccount,
                googleServices = googleServices,
                testGroups = testGroups
            )
        } else null

        // Play Console
        terminal.println("\n🎮 Google Play Console")
        val playCredentials = projectDetector.detectPlayConsoleCredentials()
        val playEnabled = if (playCredentials != null) {
            terminal.println("Credentials found: $playCredentials ✓")
            promptYesNo("Enable?", default = true)
        } else {
            terminal.println("No credentials found.")
            promptYesNo("Enable?", default = false)
        }

        val playData = if (playEnabled) {
            val serviceAccount = playCredentials ?: promptRequired("Service account JSON path")

            PlayConsoleDestinationData(
                enabled = true,
                serviceAccount = serviceAccount,
                packageName = wizardState.projectInfo.packageName
            )
        } else null

        // Local
        terminal.println("\n📁 Local Filesystem")
        val localEnabled = promptYesNo("Enable for testing?", default = false)
        val localData = if (localEnabled) {
            LocalDestinationData(
                enabled = true,
                outputDir = promptWithDefault("Output directory", "build/deploy")
            )
        } else null

        return AndroidDestinationsData(
            firebase = firebaseData,
            playConsole = playData,
            local = localData
        )
    }

    private fun configureIosDestinations(): IosDestinationsData {
        terminal.println("\n🍎 iOS Deployment")

        // TestFlight
        terminal.println("\n✈️ TestFlight")
        val testFlightApiKey = projectDetector.detectAppStoreConnectApiKey()
        val testFlightEnabled = if (testFlightApiKey != null) {
            terminal.println("API key found: $testFlightApiKey ✓")
            promptYesNo("Enable?", default = true)
        } else {
            terminal.println("No API key found.")
            promptYesNo("Enable?", default = false)
        }

        val testFlightData = if (testFlightEnabled) {
            val apiKeyPath = testFlightApiKey ?: promptRequired("App Store Connect API key path")
            val teamId = promptRequired("Team ID")

            TestFlightDestinationData(
                enabled = true,
                apiKeyPath = apiKeyPath,
                bundleId = wizardState.projectInfo.bundleId,
                teamId = teamId
            )
        } else null

        // App Store
        terminal.println("\n🏪 App Store")
        val appStoreEnabled = promptYesNo("Enable?", default = false)
        val appStoreData = if (appStoreEnabled) {
            val apiKeyPath = testFlightApiKey ?: promptRequired("App Store Connect API key path")
            val teamId = promptRequired("Team ID")

            AppStoreDestinationData(
                enabled = true,
                apiKeyPath = apiKeyPath,
                bundleId = wizardState.projectInfo.bundleId,
                teamId = teamId
            )
        } else null

        return IosDestinationsData(
            testFlight = testFlightData,
            appStore = appStoreData
        )
    }

    // ========================================================================
    // PHASE 4: VERSION MANAGEMENT
    // ========================================================================

    private fun phaseVersionManagement(): VersionConfigData {
        terminal.println("\n$PHASE_HEADER")
        terminal.println("4️⃣  VERSION MANAGEMENT")
        terminal.println(SEPARATOR)

        terminal.println("\n📌 Auto-Increment on Deploy")
        val autoIncrement = promptYesNo("Automatically bump version?", default = true)

        val bumpStrategy = if (autoIncrement) {
            promptChoice(
                "Default strategy",
                listOf("patch", "minor", "major", "auto"),
                default = "patch"
            )
        } else {
            "manual"
        }

        // Files to update
        terminal.println("\n📄 Files to Update")
        val detectedFiles = projectDetector.detectVersionFiles()
        detectedFiles.forEach { terminal.println("  ✓ $it") }

        val additionalFiles = if (promptYesNo("Add more?", default = false)) {
            terminal.println("Enter file paths (comma-separated):")
            val input = terminal.readLineOrNull()?.trim() ?: ""
            input.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        return VersionConfigData(
            enabled = autoIncrement,
            current = wizardState.projectInfo.version,
            autoIncrement = autoIncrement,
            bumpStrategy = bumpStrategy,
            filesToUpdate = detectedFiles + additionalFiles
        )
    }

    // ========================================================================
    // PHASE 5: CHANGELOG
    // ========================================================================

    private fun phaseChangelog(): ChangelogConfigData {
        terminal.println("\n$PHASE_HEADER")
        terminal.println("5️⃣  CHANGELOG")
        terminal.println(SEPARATOR)

        terminal.println("\n📝 Generate Changelogs")
        val enabled = promptYesNo("Generate on deploy?", default = true)

        if (!enabled) {
            return ChangelogConfigData(enabled = false)
        }

        val format = promptChoice(
            "Format",
            listOf("markdown", "html", "json"),
            default = "markdown"
        )

        val outputFile = promptWithDefault(
            "Output file",
            if (format == "markdown") "CHANGELOG.md" else "CHANGELOG.$format"
        )

        val fromTagStrategy = promptChoice(
            "Start from",
            listOf("auto-detect", "specific-tag", "last-release"),
            default = "auto-detect"
        )

        val fromTag = if (fromTagStrategy == "specific-tag") {
            promptRequired("Tag name")
        } else {
            null
        }

        val append = promptYesNo("Append to existing $outputFile?", default = true)

        return ChangelogConfigData(
            enabled = true,
            format = format,
            outputFile = outputFile,
            fromTag = fromTag,
            append = append,
            includeBreakingChanges = true,
            includeContributors = true,
            includeStats = true
        )
    }

    // ========================================================================
    // PHASE 6: DEPLOYMENT GROUPS
    // ========================================================================

    private fun phaseDeploymentGroups(): List<FlavorGroupData> {
        terminal.println("\n$PHASE_HEADER")
        terminal.println("6️⃣  DEPLOYMENT GROUPS")
        terminal.println(SEPARATOR)

        val androidFlavors = wizardState.buildConfig.android?.flavors ?: emptyList()

        if (androidFlavors.isEmpty()) {
            terminal.println("\nNo flavors configured. Skipping deployment groups.")
            return emptyList()
        }

        terminal.println("\n🎯 Create Flavor Groups for Easy Deployment")
        terminal.println("Groups let you deploy multiple flavors together.")

        val groups = mutableListOf<FlavorGroupData>()

        // Suggest default groups
        if (androidFlavors.size > 1) {
            terminal.println("\nSuggested groups:")
            terminal.println("  1. Production Release (all flavors)")
            terminal.println("  2. QA Testing (debug builds)")
            terminal.println("  3. Individual flavor groups")

            if (promptYesNo("Create suggested groups?", default = true)) {
                // All flavors group
                groups.add(FlavorGroupData(
                    name = "production",
                    description = "All production flavors",
                    flavors = androidFlavors
                ))

                // QA group (if there's a debug/qa flavor)
                val qaFlavors = androidFlavors.filter { it.contains("qa") || it.contains("debug") }
                if (qaFlavors.isNotEmpty()) {
                    groups.add(FlavorGroupData(
                        name = "qa",
                        description = "QA testing flavors",
                        flavors = qaFlavors
                    ))
                }
            }
        }

        // Custom groups
        while (promptYesNo("Add custom group?", default = false)) {
            val name = promptRequired("Group name")
            val description = promptRequired("Description")

            terminal.println("Select flavors for this group:")
            val selectedFlavors = mutableListOf<String>()
            androidFlavors.forEach { flavor ->
                if (promptYesNo("  Include $flavor?", default = false)) {
                    selectedFlavors.add(flavor)
                }
            }

            if (selectedFlavors.isNotEmpty()) {
                groups.add(FlavorGroupData(name, description, selectedFlavors))
            }
        }

        return groups
    }

    // ========================================================================
    // PHASE 7: CI/CD SETUP
    // ========================================================================

    private fun phaseCICD(): CICDConfigData {
        terminal.println("\n$PHASE_HEADER")
        terminal.println("7️⃣  CI/CD SETUP")
        terminal.println(SEPARATOR)

        // GitHub Actions
        terminal.println("\n🤖 GitHub Actions")
        val generateGitHub = promptYesNo("Generate example workflow?", default = true)

        val gitHubTriggers = if (generateGitHub) {
            terminal.println("\nTriggers:")
            val triggers = mutableListOf<String>()
            if (promptYesNo("  On tag push (v*.*.*)?", default = true)) {
                triggers.add("tag")
            }
            if (promptYesNo("  Manual dispatch?", default = true)) {
                triggers.add("manual")
            }
            if (promptYesNo("  On PR (test only)?", default = true)) {
                triggers.add("pr")
            }
            triggers
        } else {
            emptyList()
        }

        // GitLab CI
        terminal.println("\n🦊 GitLab CI")
        val generateGitLab = promptYesNo("Generate example pipeline?", default = false)

        return CICDConfigData(
            generateGitHubActions = generateGitHub,
            gitHubTriggers = gitHubTriggers,
            generateGitLabCI = generateGitLab
        )
    }

    // ========================================================================
    // PHASE 8: REVIEW & CONFIRM
    // ========================================================================

    private fun phaseReviewAndConfirm(): Boolean {
        terminal.println("\n$PHASE_HEADER")
        terminal.println("8️⃣  REVIEW & CONFIRM")
        terminal.println(SEPARATOR)

        terminal.println("\n📋 Configuration Summary\n")

        // Project
        terminal.println("Project:")
        terminal.println("  • Name: ${wizardState.projectInfo.appName}")
        terminal.println("  • Package: ${wizardState.projectInfo.packageName}")
        terminal.println("  • Type: ${wizardState.projectInfo.type.displayName}")
        terminal.println("  • Version: ${wizardState.projectInfo.version}")

        // Build
        if (wizardState.buildConfig.android != null) {
            terminal.println("\nAndroid Build:")
            val android = wizardState.buildConfig.android!!
            if (android.flavors.isNotEmpty()) {
                terminal.println("  • Flavors: ${android.flavors.joinToString(", ")}")
                terminal.println("  • Default: ${android.defaultFlavor}")
            }
            terminal.println("  • Signing: ${android.keyStorePath}")
        }

        if (wizardState.buildConfig.ios != null) {
            terminal.println("\niOS Build:")
            val ios = wizardState.buildConfig.ios!!
            terminal.println("  • Project: ${ios.projectPath}")
            terminal.println("  • Scheme: ${ios.scheme}")
        }

        // Deployment
        terminal.println("\nDeployment:")
        wizardState.deployConfig.android?.let { android ->
            if (android.firebase?.enabled == true) {
                terminal.println("  • Firebase (${android.firebase!!.testGroups.joinToString(", ")})")
            }
            if (android.playConsole?.enabled == true) {
                terminal.println("  • Play Console (${android.playConsole!!.track} track)")
            }
        }
        wizardState.deployConfig.ios?.let { ios ->
            if (ios.testFlight?.enabled == true) {
                terminal.println("  • TestFlight")
            }
            if (ios.appStore?.enabled == true) {
                terminal.println("  • App Store")
            }
        }

        // Version
        if (wizardState.versionConfig.enabled) {
            terminal.println("\nVersion:")
            terminal.println("  • Auto-increment: ${wizardState.versionConfig.bumpStrategy}")
            terminal.println("  • Files: ${wizardState.versionConfig.filesToUpdate.size}")
        }

        // Changelog
        if (wizardState.changelogConfig.enabled) {
            terminal.println("\nChangelog:")
            terminal.println("  • Auto-generate: ${wizardState.changelogConfig.format}")
            terminal.println("  • Output: ${wizardState.changelogConfig.outputFile}")
        }

        // Groups
        if (wizardState.flavorGroups.isNotEmpty()) {
            terminal.println("\nGroups:")
            wizardState.flavorGroups.forEach { group ->
                terminal.println("  • ${group.name} (${group.flavors.size} flavors)")
            }
        }

        // CI/CD
        if (wizardState.cicdConfig.generateGitHubActions) {
            terminal.println("\nCI/CD:")
            terminal.println("  • GitHub Actions configured")
        }

        terminal.println()
        return promptYesNo("Everything looks good?", default = true)
    }

    // ========================================================================
    // CONFIG GENERATION
    // ========================================================================

    private fun generateConfig(): Config {
        val projectInfo = wizardState.projectInfo
        val buildConfig = wizardState.buildConfig
        val deployConfig = wizardState.deployConfig
        val versionConfig = wizardState.versionConfig
        val changelogConfig = wizardState.changelogConfig
        val flavorGroups = wizardState.flavorGroups

        return Config(
            app = AppConfig(
                name = projectInfo.appName,
                identifier = projectInfo.packageName,
                version = projectInfo.version
            ),
            build = BuildConfig(
                android = if (buildConfig.android != null) {
                    val android = buildConfig.android!!
                    AndroidBuildConfig(
                        enabled = android.enabled,
                        defaultFlavor = android.defaultFlavor,
                        defaultType = android.defaultType,
                        flavors = android.flavors,
                        keyStore = android.keyStorePath,
                        keyAlias = android.keyAlias,
                        keyPassword = if (android.useEnvForPasswords) "\${ANDROID_KEY_PASSWORD}" else "",
                        storePassword = if (android.useEnvForPasswords) "\${ANDROID_STORE_PASSWORD}" else "",
                        useEnvForPasswords = android.useEnvForPasswords
                    )
                } else null,
                ios = if (buildConfig.ios != null) {
                    val ios = buildConfig.ios!!
                    IosBuildConfig(
                        enabled = ios.enabled,
                        projectPath = ios.projectPath,
                        scheme = ios.scheme,
                        configuration = ios.configuration,
                        codeSignIdentity = ios.codeSignIdentity,
                        provisioningProfile = ios.provisioningProfile
                    )
                } else null
            ),
            version = VersionConfig(
                enabled = versionConfig.enabled,
                current = versionConfig.current,
                autoIncrement = versionConfig.autoIncrement,
                bumpStrategy = versionConfig.bumpStrategy,
                filesToUpdate = versionConfig.filesToUpdate
            ),
            changelog = ChangelogConfig(
                enabled = changelogConfig.enabled,
                format = changelogConfig.format,
                outputFile = changelogConfig.outputFile,
                fromTag = changelogConfig.fromTag,
                append = changelogConfig.append,
                includeBreakingChanges = changelogConfig.includeBreakingChanges,
                includeContributors = changelogConfig.includeContributors,
                includeStats = changelogConfig.includeStats
            ),
            deploy = DeployConfig(
                enabled = true,
                android = if (deployConfig.android != null) {
                    val android = deployConfig.android!!
                    AndroidDeployConfig(
                        artifactPath = "build/outputs/apk/\${flavor}/release/app-\${flavor}-release.apk",
                        firebase = if (android.firebase != null) {
                            val firebase = android.firebase!!
                            FirebaseAndroidDestination(
                                enabled = firebase.enabled,
                                serviceAccount = firebase.serviceAccount,
                                googleServices = firebase.googleServices,
                                testGroups = firebase.testGroups
                            )
                        } else FirebaseAndroidDestination(enabled = false),
                        playConsole = if (android.playConsole != null) {
                            val play = android.playConsole!!
                            PlayConsoleAndroidDestination(
                                enabled = play.enabled,
                                serviceAccount = play.serviceAccount,
                                packageName = play.packageName
                            )
                        } else PlayConsoleAndroidDestination(enabled = false),
                        local = if (android.local != null) {
                            val local = android.local!!
                            LocalAndroidDestination(
                                enabled = local.enabled,
                                outputDir = local.outputDir
                            )
                        } else LocalAndroidDestination(enabled = false)
                    )
                } else null,
                ios = if (deployConfig.ios != null) {
                    val ios = deployConfig.ios!!
                    IosDeployConfig(
                        artifactPath = "build/outputs/ipa/\${scheme}.ipa",
                        testflight = if (ios.testFlight != null) {
                            val tf = ios.testFlight!!
                            TestFlightDestination(
                                enabled = tf.enabled,
                                apiKeyPath = tf.apiKeyPath,
                                bundleId = tf.bundleId,
                                teamId = tf.teamId
                            )
                        } else TestFlightDestination(enabled = false),
                        appStore = if (ios.appStore != null) {
                            val store = ios.appStore!!
                            AppStoreDestination(
                                enabled = store.enabled,
                                apiKeyPath = store.apiKeyPath,
                                bundleId = store.bundleId,
                                teamId = store.teamId
                            )
                        } else AppStoreDestination(enabled = false)
                    )
                } else null,
                flavorGroups = flavorGroups.associate { group ->
                    group.name to FlavorGroup(
                        name = group.name,
                        description = group.description,
                        flavors = group.flavors
                    )
                },
                defaultGroup = flavorGroups.firstOrNull()?.name
            ),
            notify = NotifyConfig(),
            report = ReportConfig(
                enabled = false
            ),
            env = emptyMap()
        )
    }

    private fun generateSetupSummary(): String {
        return buildString {
            appendLine("# mobilectl Setup Summary")
            appendLine()
            appendLine("Generated: ${java.time.LocalDateTime.now()}")
            appendLine()
            appendLine("## Project")
            appendLine("- Name: ${wizardState.projectInfo.appName}")
            appendLine("- Package: ${wizardState.projectInfo.packageName}")
            appendLine("- Type: ${wizardState.projectInfo.type.displayName}")
            appendLine("- Version: ${wizardState.projectInfo.version}")
            appendLine()
            appendLine("## Configuration")
            appendLine("- Config file: `mobileops.yaml`")
            appendLine("- Build flavors: ${wizardState.buildConfig.android?.flavors?.size ?: 0}")
            appendLine("- Deployment groups: ${wizardState.flavorGroups.size}")
            appendLine()
            appendLine("## Next Steps")
            appendLine("1. Review the generated `mobileops.yaml`")
            appendLine("2. Set environment variables for passwords:")
            appendLine("   - `ANDROID_KEY_PASSWORD`")
            appendLine("   - `ANDROID_STORE_PASSWORD`")
            appendLine("3. Try deploying: `mobilectl deploy --all-variants -y`")
            appendLine()
            appendLine("## Documentation")
            appendLine("- See `docs/` for more information")
            appendLine("- Run `mobilectl --help` for available commands")
        }
    }

    // ========================================================================
    // UI HELPERS
    // ========================================================================

    private fun showWelcome() {
        terminal.println("\n🚀 mobilectl Setup Wizard")
        terminal.println("═".repeat(60))
        terminal.println()
        terminal.println("📱 Welcome to mobilectl!")
        terminal.println("Let's set up your project for mobile deployment.")
        terminal.println()
        terminal.print("Press Enter to continue...")
        readLine()
    }

    private fun promptProjectType(): ProjectType {
        terminal.println("[1] Android (native)")
        terminal.println("[2] Flutter")
        terminal.println("[3] React Native")
        terminal.println("[4] iOS (native)")
        terminal.print("> ")

        return when (readLine()?.trim()) {
            "1" -> ProjectType.ANDROID_NATIVE
            "2" -> ProjectType.FLUTTER
            "3" -> ProjectType.REACT_NATIVE
            "4" -> ProjectType.IOS_NATIVE
            else -> ProjectType.ANDROID_NATIVE
        }
    }

    private fun promptRequired(prompt: String): String {
        while (true) {
            terminal.print("$prompt: ")
            val input = readLine()?.trim()
            if (!input.isNullOrBlank()) {
                return input
            }
            terminal.println("  ⚠ This field is required")
        }
    }

    private fun promptWithDefault(prompt: String, default: String): String {
        terminal.print("$prompt [$default]: ")
        val input = readLine()?.trim()
        return if (input.isNullOrBlank()) default else input
    }

    private fun promptYesNo(prompt: String, default: Boolean = true): Boolean {
        val hint = if (default) "(Y/n)" else "(y/N)"
        terminal.print("$prompt $hint: ")
        return when (readLine()?.trim()?.lowercase()) {
            "y", "yes" -> true
            "n", "no" -> false
            "" -> default
            else -> default
        }
    }

    private fun <T> promptChoice(prompt: String, choices: List<T>, default: T): T {
        choices.forEachIndexed { index, choice ->
            terminal.println("[${index + 1}] $choice")
        }
        terminal.print("$prompt > ")

        val input = readLine()?.trim()?.toIntOrNull()
        return if (input != null && input in 1..choices.size) {
            choices[input - 1]
        } else {
            default
        }
    }

    companion object {
        private const val PHASE_HEADER = "\n"
        private const val SEPARATOR = "────────────────────────────────────────────────────────────"
    }
}

// ============================================================================
// DATA CLASSES
// ============================================================================

private class WizardState {
    lateinit var projectInfo: ProjectInfo
    lateinit var buildConfig: BuildConfigData
    lateinit var deployConfig: DeployConfigData
    lateinit var versionConfig: VersionConfigData
    lateinit var changelogConfig: ChangelogConfigData
    var flavorGroups: List<FlavorGroupData> = emptyList()
    lateinit var cicdConfig: CICDConfigData
}

data class ProjectInfo(
    val type: ProjectType,
    val appName: String,
    val packageName: String,
    val bundleId: String,
    val organization: String,
    val version: String
)

enum class ProjectType(
    val displayName: String,
    val supportsAndroid: Boolean,
    val supportsIos: Boolean
) {
    ANDROID_NATIVE("Android (native)", true, false),
    IOS_NATIVE("iOS (native)", false, true),
    FLUTTER("Flutter", true, true),
    REACT_NATIVE("React Native", true, true)
}

data class BuildConfigData(
    val android: AndroidBuildData?,
    val ios: IosBuildData?
)

data class AndroidBuildData(
    val enabled: Boolean,
    val flavors: List<String>,
    val defaultFlavor: String,
    val defaultType: String,
    val keyStorePath: String,
    val keyAlias: String,
    val useEnvForPasswords: Boolean
)

data class IosBuildData(
    val enabled: Boolean,
    val projectPath: String,
    val scheme: String,
    val configuration: String,
    val codeSignIdentity: String,
    val provisioningProfile: String
)

data class DeployConfigData(
    val android: AndroidDestinationsData?,
    val ios: IosDestinationsData?
)

data class AndroidDestinationsData(
    val firebase: FirebaseDestinationData?,
    val playConsole: PlayConsoleDestinationData?,
    val local: LocalDestinationData?
)

data class IosDestinationsData(
    val testFlight: TestFlightDestinationData?,
    val appStore: AppStoreDestinationData?
)

data class FirebaseDestinationData(
    val enabled: Boolean,
    val serviceAccount: String,
    val googleServices: String,
    val testGroups: List<String>
)

data class PlayConsoleDestinationData(
    val enabled: Boolean,
    val serviceAccount: String,
    val packageName: String
)

data class LocalDestinationData(
    val enabled: Boolean,
    val outputDir: String
)

data class TestFlightDestinationData(
    val enabled: Boolean,
    val apiKeyPath: String,
    val bundleId: String,
    val teamId: String
)

data class AppStoreDestinationData(
    val enabled: Boolean,
    val apiKeyPath: String,
    val bundleId: String,
    val teamId: String
)

data class VersionConfigData(
    val enabled: Boolean,
    val current: String = "1.0.0",
    val autoIncrement: Boolean = false,
    val bumpStrategy: String = "manual",
    val filesToUpdate: List<String> = emptyList()
)

data class ChangelogConfigData(
    val enabled: Boolean,
    val format: String = "markdown",
    val outputFile: String = "CHANGELOG.md",
    val fromTag: String? = null,
    val append: Boolean = true,
    val includeBreakingChanges: Boolean = true,
    val includeContributors: Boolean = true,
    val includeStats: Boolean = true
)

data class FlavorGroupData(
    val name: String,
    val description: String,
    val flavors: List<String>
)

data class CICDConfigData(
    val generateGitHubActions: Boolean,
    val gitHubTriggers: List<String>,
    val generateGitLabCI: Boolean
)

sealed class SetupResult {
    data class Success(
        val config: Config,
        val generateGitHubActions: Boolean,
        val generateGitLabCI: Boolean,
        val setupSummary: String
    ) : SetupResult()

    object Cancelled : SetupResult()
}
