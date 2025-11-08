package com.mobilectl.commands.deploy

import com.mobilectl.config.Config
import com.mobilectl.config.ConfigLoader
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.model.deploy.DeployConfig
import com.mobilectl.util.PremiumLogger
import com.mobilectl.util.createFileUtil
import java.io.File

/**
 * Service responsible for loading and merging configurations.
 * Single Responsibility: Configuration management
 */
class ConfigurationService(
    private val workingPath: String,
    private val smartDefaultsProvider: SmartDefaultsProvider,
    private val verbose: Boolean
) {
    /**
     * Loads configuration from file or uses smart defaults.
     * Merges user config with smart defaults for missing sections.
     */
    suspend fun loadConfigOrUseDefaults(): Config {
        val configFile = File(workingPath, "mobileops.yaml")

        return if (configFile.exists()) {
            loadAndMergeConfig(configFile)
        } else {
            if (verbose) {
                PremiumLogger.info("No config file found, using auto-detected defaults")
            }
            smartDefaultsProvider.createSmartDefaults()
        }
    }

    private suspend fun loadAndMergeConfig(configFile: File): Config {
        return try {
            val fileUtil = createFileUtil()
            val detector = createProjectDetector()
            val configLoader = ConfigLoader(fileUtil, detector)
            val loadedConfig = configLoader.loadConfig(configFile.absolutePath).getOrNull()

            if (loadedConfig != null) {
                // Merge loaded config with smart defaults
                val defaults = smartDefaultsProvider.createSmartDefaults()
                mergeWithDefaults(loadedConfig, defaults)
            } else {
                smartDefaultsProvider.createSmartDefaults()
            }
        } catch (e: Exception) {
            if (verbose) {
                PremiumLogger.simpleWarning("Failed to load config: ${e.message}")
            }
            smartDefaultsProvider.createSmartDefaults()
        }
    }

    /**
     * Merges user configuration with smart defaults.
     * User config takes precedence, but null/missing sections are filled from defaults.
     */
    private fun mergeWithDefaults(userConfig: Config, defaults: Config): Config {
        return Config(
            app = userConfig.app,
            build = userConfig.build,
            version = userConfig.version ?: defaults.version,
            changelog = userConfig.changelog,
            deploy = mergeDeployConfig(userConfig.deploy, defaults.deploy),
            notify = userConfig.notify,
            report = userConfig.report,
            env = if (userConfig.env.isNotEmpty()) userConfig.env else defaults.env
        )
    }

    /**
     * Merges deploy configurations intelligently.
     * If user hasn't specified android/ios config, use smart defaults.
     */
    private fun mergeDeployConfig(user: DeployConfig, default: DeployConfig): DeployConfig {
        return DeployConfig(
            enabled = user.enabled,
            android = user.android ?: default.android,
            ios = user.ios ?: default.ios,
            flavorGroups = if (user.flavorGroups.isNotEmpty()) user.flavorGroups else default.flavorGroups,
            defaultGroup = user.defaultGroup ?: default.defaultGroup
        )
    }

    /**
     * Applies command-line overrides to configuration.
     */
    fun applyCommandLineOverrides(
        config: Config,
        destination: String?,
        releaseNotes: String?,
        testGroups: String?
    ): Config {
        var updated = config

        // Override destination
        if (destination != null) {
            updated = applyDestinationOverride(updated, destination)
        }

        // Override release notes
        if (releaseNotes != null) {
            updated = updated.copy(
                deploy = updated.deploy.copy(
                    android = updated.deploy.android?.copy(
                        firebase = updated.deploy.android!!.firebase.copy(
                            releaseNotes = releaseNotes
                        )
                    )
                )
            )
        }

        // Override test groups
        if (testGroups != null) {
            val groups = testGroups.split(",").map { it.trim() }
            updated = updated.copy(
                deploy = updated.deploy.copy(
                    android = updated.deploy.android?.copy(
                        firebase = updated.deploy.android!!.firebase.copy(
                            testGroups = groups
                        )
                    )
                )
            )
        }

        return updated
    }

    private fun applyDestinationOverride(config: Config, dest: String): Config {
        val destinations = dest.lowercase().split(",").map { it.trim() }

        return when {
            "firebase" in destinations -> enableFirebase(config)
            "play" in destinations || "playstore" in destinations -> enablePlayConsole(config)
            "local" in destinations -> enableLocal(config)
            "testflight" in destinations -> enableTestFlight(config)
            "appstore" in destinations -> enableAppStore(config)
            else -> config
        }
    }

    private fun enableFirebase(config: Config): Config {
        return config.copy(
            deploy = config.deploy.copy(
                android = config.deploy.android?.copy(
                    firebase = config.deploy.android!!.firebase.copy(enabled = true)
                )
            )
        )
    }

    private fun enablePlayConsole(config: Config): Config {
        return config.copy(
            deploy = config.deploy.copy(
                android = config.deploy.android?.copy(
                    playConsole = config.deploy.android!!.playConsole.copy(enabled = true)
                )
            )
        )
    }

    private fun enableLocal(config: Config): Config {
        return config.copy(
            deploy = config.deploy.copy(
                android = config.deploy.android?.copy(
                    local = config.deploy.android!!.local.copy(enabled = true)
                )
            )
        )
    }

    private fun enableTestFlight(config: Config): Config {
        return config.copy(
            deploy = config.deploy.copy(
                ios = config.deploy.ios?.copy(
                    testflight = config.deploy.ios!!.testflight.copy(enabled = true)
                )
            )
        )
    }

    private fun enableAppStore(config: Config): Config {
        return config.copy(
            deploy = config.deploy.copy(
                ios = config.deploy.ios?.copy(
                    appStore = config.deploy.ios!!.appStore.copy(enabled = true)
                )
            )
        )
    }
}
