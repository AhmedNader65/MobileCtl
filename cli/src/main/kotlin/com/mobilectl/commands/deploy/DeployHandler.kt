package com.mobilectl.commands.deploy

import com.mobilectl.detector.createProjectDetector
import com.mobilectl.util.PremiumLogger
import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

/**
 * Thin coordinator for deployment command.
 * Delegates to specialized classes following Single Responsibility Principle.
 *
 * Architecture:
 * - ConfigurationService: Config loading and merging
 * - SmartDefaultsProvider: Auto-detection logic
 * - DeploymentPresenter: UI and output formatting
 * - InteractiveDeploymentWizard: Interactive mode
 * - DeploymentWorkflow: Business logic orchestration
 */
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
    private val confirm: Boolean,
    val bumpVersion: String?,
    private val changelog: Boolean,
    private val allFlavors: Boolean = false,
    private val group: String? = null,
    private val flavors: String? = null,
    private val exclude: String? = null,
) {
    private val out = PrintWriter(System.out, true, StandardCharsets.UTF_8)
    private val workingPath = System.getProperty("user.dir")
    private val detector = createProjectDetector()

    // Dependency injection of specialized services
    private val smartDefaultsProvider = SmartDefaultsProvider(workingPath, verbose)
    private val configurationService = ConfigurationService(workingPath, smartDefaultsProvider, verbose)
    private val presenter = DeploymentPresenter(
        out, allFlavors, group, flavors, exclude,
        bumpVersion, changelog, releaseNotes, testGroups, skipBuild
    )
    private val workflow = DeploymentWorkflow(
        workingPath = workingPath,
        detector = detector,
        verbose = verbose
    )
    private val wizard = InteractiveDeploymentWizard(out)

    suspend fun execute() {
        try {
            // Validate working directory
            if (!File(workingPath).exists()) {
                PremiumLogger.simpleError("Directory does not exist: $workingPath")
                return
            }

            // 1. Load configuration
            val baseConfig = configurationService.loadConfigOrUseDefaults()
            var config = configurationService.applyCommandLineOverrides(
                baseConfig, destination, releaseNotes, testGroups
            )

            // 2. Interactive mode
            if (interactive) {
                config = runInteractiveWizard(config) ?: return
            }

            // 3. Parse platforms
            var targetPlatforms = workflow.parsePlatforms(platform, config)
            if (targetPlatforms == null) {
                targetPlatforms = detector.detectPlatforms(
                    config.build.android.enabled,
                    config.build.ios.enabled == true
                )
            }

            // 4. Detect environment
            val actualEnvironment = environment ?: smartDefaultsProvider.detectEnvironment()

            // 5. Show summary
            presenter.printSummary(config, targetPlatforms, actualEnvironment)

            // 6. Ask for confirmation
            if (!confirm && !dryRun && !interactive) {
                if (!presenter.askForConfirmation()) {
                    PremiumLogger.simpleInfo("Deployment cancelled")
                    return
                }
            }

            // 7. Dry-run mode
            if (dryRun) {
                PremiumLogger.simpleInfo("DRY-RUN mode - nothing will be deployed")
                presenter.showDryRunDetails(config, targetPlatforms, actualEnvironment)
                return
            }

            // 8. Version bump and changelog (pre-flight)
            if (!skipBuild) {
                val shouldBumpVersion = config.version?.autoIncrement == true || bumpVersion != null
                val shouldGenerateChangelog = config.changelog.enabled == true || changelog
                val strategy = bumpVersion ?: config.version?.bumpStrategy

                if (shouldBumpVersion && strategy != null) {
                    if (!workflow.isValidStrategy(strategy)) {
                        PremiumLogger.simpleError("Invalid strategy: $strategy")
                        PremiumLogger.info("Valid: patch, minor, major, auto, manual")
                        return
                    }

                    if (strategy == "manual") {
                        PremiumLogger.simpleInfo("Skipping version bump (manual mode)")
                    } else {
                        workflow.bumpVersionBeforeDeploy(strategy, config)
                        config = configurationService.loadConfigOrUseDefaults()
                    }
                }

                if (shouldGenerateChangelog) {
                    workflow.executeChangelogGeneration(config)
                }
            }

            // 9. Handle flavor selection and exclusion
            val flavorOptions = FlavorOptions(
                allFlavors = allFlavors,
                group = group,
                flavors = flavors,
                exclude = exclude
            )
            val flavorsToDeploy = workflow.selectFlavorsToDeploy(config, flavorOptions)

            val allResultsGlobal = mutableListOf<com.mobilectl.model.deploy.DeployResult>()

            // 10. Deploy each flavor
            flavorsToDeploy.forEachIndexed { index, flavor ->
                out.println()
                out.println("┌─────────────────────────────────────────────────────────┐")
                out.println("│  ▶  Flavor ${index + 1}/${flavorsToDeploy.size}: ${if (flavor == "") "default flavor" else flavor}")
                out.println("└─────────────────────────────────────────────────────────┘")
                out.println()

                config = config.copy(
                    build = config.build.copy(
                        android = config.build.android.copy(defaultFlavor = flavor)
                    )
                )

                // 11. Build if needed
                if (!skipBuild) {
                    val needsBuild = workflow.checkIfBuildNeeded(config, targetPlatforms)
                    if (needsBuild) {
                        PremiumLogger.header("Building Artifacts", "🏗️")
                        val buildResult = workflow.buildArtifacts(config, targetPlatforms)

                        if (!buildResult.success) {
                            PremiumLogger.simpleError("Build failed!")
                            return
                        }

                        // Check signing requirements
                        workflow.validateSigningRequirements(config, buildResult)
                    } else {
                        PremiumLogger.simpleSuccess(
                            "Artifacts up-to-date (no source changes detected)"
                        )
                    }
                }

                // 12. Execute deployment
                presenter.showDeploymentHeader(actualEnvironment, targetPlatforms)
                allResultsGlobal.addAll(
                    workflow.executeDeploy(config, targetPlatforms)
                )
            }

            // 13. Print results
            printDeployResults(allResultsGlobal, verbose, workingPath)

        } catch (e: Exception) {
            PremiumLogger.simpleError("Error: ${e.message}")
            if (verbose) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Run interactive deployment wizard
     */
    private fun runInteractiveWizard(baseConfig: com.mobilectl.config.Config): com.mobilectl.config.Config? {
        val availablePlatforms = mutableSetOf<com.mobilectl.model.Platform>()
        if (baseConfig.deploy?.android?.enabled == true) availablePlatforms.add(com.mobilectl.model.Platform.ANDROID)
        if (baseConfig.deploy?.ios?.enabled == true) availablePlatforms.add(com.mobilectl.model.Platform.IOS)

        // Step 1: Select platforms
        val selectedPlatforms = wizard.selectPlatforms(availablePlatforms) ?: return null

        // Step 2: Select destinations per platform
        val destinationMap = mutableMapOf<com.mobilectl.model.Platform, List<String>>()

        selectedPlatforms.forEach { platform ->
            val availableDests = when (platform) {
                com.mobilectl.model.Platform.ANDROID -> AvailableDestinations.forAndroid()
                com.mobilectl.model.Platform.IOS -> AvailableDestinations.forIos()
                else -> emptyList()
            }

            val selectedDests = wizard.selectDestinations(platform, availableDests)
            if (selectedDests.isNotEmpty()) {
                destinationMap[platform] = selectedDests
            }
        }

        if (destinationMap.isEmpty()) {
            PremiumLogger.simpleError("No destinations selected")
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
            environment = environment ?: smartDefaultsProvider.detectEnvironment()
        )

        if (!confirmed) {
            PremiumLogger.simpleInfo("Deployment cancelled")
            return null
        }

        // Apply interactive selections to config
        var config = baseConfig

        // Update destinations
        selectedPlatforms.forEach { platform ->
            val destinations = destinationMap[platform] ?: emptyList()

            when (platform) {
                com.mobilectl.model.Platform.ANDROID -> {
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

                com.mobilectl.model.Platform.IOS -> {
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
}
