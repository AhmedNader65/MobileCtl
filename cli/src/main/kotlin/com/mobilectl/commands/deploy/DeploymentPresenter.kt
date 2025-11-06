package com.mobilectl.commands.deploy

import com.mobilectl.config.Config
import com.mobilectl.model.Platform
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

/**
 * Handles all presentation/output logic for deployment.
 * Single Responsibility: User interface and output formatting
 */
class DeploymentPresenter(
    private val out: PrintWriter = PrintWriter(System.out, true, StandardCharsets.UTF_8),
    private val allFlavors: Boolean = false,
    private val group: String? = null,
    private val flavors: String? = null,
    private val exclude: String? = null,
    private val bumpVersion: String? = null,
    private val changelog: Boolean = false,
    private val releaseNotes: String? = null,
    private val testGroups: String? = null,
    private val skipBuild: Boolean = false
) {

    /**
     * Prints deployment configuration summary
     */
    fun printSummary(config: Config, targetPlatforms: Set<Platform>, env: String) {
        val items = mutableMapOf<String, String>()
        items["Platforms"] = targetPlatforms.joinToString(", ") { it.name }
        items["Environment"] = env

        // Show deployment destinations
        val destinations = mutableListOf<String>()
        if (Platform.ANDROID in targetPlatforms) {
            config.deploy?.android?.let { android ->
                if (android.firebase.enabled) destinations.add("Firebase")
                if (android.playConsole.enabled) destinations.add("Play Console")
                if (android.local.enabled) destinations.add("Local")
            }
        }
        if (Platform.IOS in targetPlatforms) {
            config.deploy?.ios?.let { ios ->
                if (ios.testflight.enabled) destinations.add("TestFlight")
                if (ios.appStore.enabled) destinations.add("App Store")
            }
        }
        if (destinations.isNotEmpty()) {
            items["Destinations"] = destinations.joinToString(", ")
        }

        // Show flavor/group info
        if (allFlavors) {
            items["Flavors"] = "All configured flavors"
        } else if (group != null) {
            val flavorList = config.deploy.flavorGroups[group]?.flavors?.joinToString(", ") ?: "none"
            items["Flavor Group"] = "$group ($flavorList)"
        } else if (flavors != null) {
            items["Flavors"] = flavors!!
        } else {
            items["Flavor"] = config.build.android.defaultFlavor.ifEmpty { "default" }
        }

        if (exclude != null) {
            items["Excluding"] = exclude!!
        }

        // Show version management
        if (config.version?.autoIncrement == true || bumpVersion != null) {
            val strategy = bumpVersion ?: config.version?.bumpStrategy ?: "auto"
            items["Version Bump"] = strategy
        }

        // Show changelog
        if (config.changelog?.enabled == true || changelog) {
            items["Changelog"] = "Generate"
        }

        if (releaseNotes != null) {
            items["Release Notes"] = "\"${releaseNotes!!}\""
        }

        if (testGroups != null) {
            items["Test Groups"] = testGroups!!
        }

        if (skipBuild) {
            items["Build"] = "Skipped (existing artifacts)"
        } else {
            items["Build Type"] = config.build.android.defaultType
        }

        com.mobilectl.util.PremiumLogger.box("Deployment Configuration", items, success = true)
    }

    /**
     * Shows detailed dry-run plan
     */
    fun showDryRunDetails(config: Config, platforms: Set<Platform>, env: String) {
        com.mobilectl.util.PremiumLogger.header("Deployment Plan (DRY-RUN)", "📋")

        platforms.forEach { platform ->
            when (platform) {
                Platform.ANDROID -> showAndroidPlan(config, env)
                Platform.IOS -> showIosPlan(config, env)
                else -> {}
            }
        }
    }

    /**
     * Shows Android deployment plan
     */
    private fun showAndroidPlan(config: Config, env: String) {
        val androidConfig = config.deploy?.android
        if (androidConfig == null) {
            com.mobilectl.util.PremiumLogger.info("Android: No deployment configured")
            return
        }

        com.mobilectl.util.PremiumLogger.simpleInfo("Android Platform:")

        if (config.version?.autoIncrement == true) {
            com.mobilectl.util.PremiumLogger.info("  • Version Auto-Increment: Enabled")
        }
        if (androidConfig.firebase.enabled) {
            com.mobilectl.util.PremiumLogger.info("  • Firebase App Distribution")
            com.mobilectl.util.PremiumLogger.info("    - Service Account: ${androidConfig.firebase.serviceAccount}")
            com.mobilectl.util.PremiumLogger.info("    - Test Groups: ${androidConfig.firebase.testGroups.joinToString()}")
        }

        if (androidConfig.playConsole.enabled) {
            com.mobilectl.util.PremiumLogger.info("  • Google Play Console")
        }

        if (androidConfig.local.enabled) {
            com.mobilectl.util.PremiumLogger.info("  • Local filesystem: ${androidConfig.local.outputDir}")
        }

        println()
    }

    /**
     * Shows iOS deployment plan
     */
    private fun showIosPlan(config: Config, env: String) {
        val iosConfig = config.deploy?.ios
        if (iosConfig == null) {
            com.mobilectl.util.PremiumLogger.info("iOS: No deployment configured")
            return
        }

        com.mobilectl.util.PremiumLogger.simpleInfo("iOS Platform:")

        if (iosConfig.testflight.enabled) {
            com.mobilectl.util.PremiumLogger.info("  • TestFlight")
        }

        if (iosConfig.appStore.enabled) {
            com.mobilectl.util.PremiumLogger.info("  • App Store")
        }

        println()
    }

    /**
     * Asks user for deployment confirmation
     */
    fun askForConfirmation(): Boolean {
        val cyan = "\u001B[36m"
        val dim = "\u001B[2m"
        val reset = "\u001B[0m"

        out.println()
        out.print("  $cyan?$reset ${dim}Proceed with deployment?$reset (Y/n) ")
        out.flush()

        val input = readLine()?.trim()?.lowercase() ?: "y"
        return input != "n" && input != "no"
    }

    /**
     * Shows header for deployment start
     */
    fun showDeploymentHeader(env: String, platforms: Set<Platform>) {
        com.mobilectl.util.PremiumLogger.header("Starting Deployment", "🚀")
        com.mobilectl.util.PremiumLogger.info("Environment: $env")
        com.mobilectl.util.PremiumLogger.info("Platforms: ${platforms.joinToString(", ")}")
        println()
    }
}
