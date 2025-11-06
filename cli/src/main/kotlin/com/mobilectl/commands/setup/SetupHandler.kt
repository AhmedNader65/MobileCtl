package com.mobilectl.commands.setup

import com.github.ajalt.mordant.terminal.Terminal
import com.mobilectl.config.createConfigParser
import com.mobilectl.util.PremiumLogger
import java.io.File

/**
 * Handler for the setup command.
 * Orchestrates the comprehensive setup wizard and configuration file generation.
 *
 * Architecture:
 * - ProjectDetector: Auto-detection logic
 * - SetupWizard: Interactive wizard
 * - ConfigParser: Config serialization
 * - WorkflowGenerator: GitHub Actions/GitLab CI generation
 */
class SetupHandler(
    private val force: Boolean = false,
    private val skipWizard: Boolean = false,
    private val outputPath: String? = null
) {
    private val workingDir = System.getProperty("user.dir")
    private val terminal = Terminal()

    // Dependency injection
    private val projectDetector = ProjectDetector(workingDir)
    private val wizard = SetupWizard(terminal, projectDetector)
    private val configParser = createConfigParser()
    private val workflowGenerator = WorkflowGenerator()

    /**
     * Main entry point for setup command.
     */
    fun handle(): Int {
        PremiumLogger.section("mobilectl Setup")

        // Check if config already exists
        val configPath = resolveConfigPath()
        if (checkExistingConfig(configPath) && !force) {
            return 1
        }

        // Run the comprehensive wizard
        val result = wizard.run()

        when (result) {
            is SetupResult.Cancelled -> {
                PremiumLogger.info("Setup cancelled by user")
                return 0
            }

            is SetupResult.Success -> {
                return handleSuccessfulSetup(result, configPath)
            }
        }
    }

    /**
     * Checks if config already exists and prompts user.
     */
    private fun checkExistingConfig(configPath: String): Boolean {
        val configFile = File(configPath)
        if (!configFile.exists()) return false

        PremiumLogger.warning("Configuration file already exists: $configPath")

        if (force) {
            PremiumLogger.info("--force flag set, overwriting existing config")
            return false
        }

        terminal.println("\nOptions:")
        terminal.println("  1. Overwrite (backup will be created)")
        terminal.println("  2. Cancel")
        terminal.println("  3. Specify different output path")
        terminal.print("\nChoice [1/2/3]: ")

        return when (terminal.readLineOrNull()?.trim()) {
            "1" -> {
                createBackup(configFile)
                false
            }
            "3" -> {
                terminal.print("Enter new path: ")
                val newPath = terminal.readLineOrNull()?.trim()
                if (newPath.isNullOrBlank()) {
                    PremiumLogger.error("Invalid path")
                    true
                } else {
                    // TODO: Handle custom path
                    false
                }
            }
            else -> true // Cancel
        }
    }

    /**
     * Creates a backup of existing config file.
     */
    private fun createBackup(file: File) {
        val timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val backupFile = File(file.parent, "${file.nameWithoutExtension}.backup.$timestamp.${file.extension}")

        file.copyTo(backupFile, overwrite = false)
        PremiumLogger.success("Backup created: ${backupFile.name}")
    }

    /**
     * Handles successful setup by generating all files.
     */
    private fun handleSuccessfulSetup(result: SetupResult.Success, configPath: String): Int {
        val config = result.config

        // 1. Generate and save mobileops.yaml
        try {
            val yamlContent = configParser.toYaml(config)
            File(configPath).writeText(yamlContent)
            PremiumLogger.success("✅ Configuration saved to: $configPath")
        } catch (e: Exception) {
            PremiumLogger.error("Failed to save configuration: ${e.message}")
            return 1
        }

        // 2. Generate GitHub Actions workflow if requested
        if (result.generateGitHubActions) {
            try {
                generateGitHubActions(result)
                PremiumLogger.success("✅ GitHub Actions workflow created")
            } catch (e: Exception) {
                PremiumLogger.warning("Failed to generate GitHub Actions: ${e.message}")
            }
        }

        // 3. Generate GitLab CI pipeline if requested
        if (result.generateGitLabCI) {
            try {
                generateGitLabCI(result)
                PremiumLogger.success("✅ GitLab CI pipeline created")
            } catch (e: Exception) {
                PremiumLogger.warning("Failed to generate GitLab CI: ${e.message}")
            }
        }

        // 4. Generate setup summary
        try {
            val docsDir = File(workingDir, "docs")
            if (!docsDir.exists()) {
                docsDir.mkdirs()
            }

            val summaryFile = File(docsDir, "SETUP.md")
            summaryFile.writeText(result.setupSummary)
            PremiumLogger.success("✅ Setup summary saved to: docs/SETUP.md")
        } catch (e: Exception) {
            PremiumLogger.warning("Failed to save setup summary: ${e.message}")
        }

        // 5. Show final instructions
        showFinalInstructions(config)

        return 0
    }

    /**
     * Generates GitHub Actions workflow file.
     */
    private fun generateGitHubActions(result: SetupResult.Success) {
        val workflowContent = workflowGenerator.generateGitHubActions(result.config)

        val workflowDir = File(workingDir, ".github/workflows")
        workflowDir.mkdirs()

        val workflowFile = File(workflowDir, "mobilectl-deploy.yml")
        workflowFile.writeText(workflowContent)
    }

    /**
     * Generates GitLab CI pipeline file.
     */
    private fun generateGitLabCI(result: SetupResult.Success) {
        val pipelineContent = workflowGenerator.generateGitLabCI(result.config)

        val pipelineFile = File(workingDir, ".gitlab-ci.yml")
        pipelineFile.writeText(pipelineContent)
    }

    /**
     * Shows final instructions to the user.
     */
    private fun showFinalInstructions(config: com.mobilectl.config.Config) {
        terminal.println()
        terminal.println("═".repeat(60))
        terminal.println("🎉 Setup Complete!")
        terminal.println("═".repeat(60))
        terminal.println()

        terminal.println("📋 Next Steps:")
        terminal.println()

        // Environment variables
        if (config.build?.android?.useEnvForPasswords == true) {
            terminal.println("1️⃣  Set required environment variables:")
            terminal.println("   export ANDROID_KEY_PASSWORD=your-key-password")
            terminal.println("   export ANDROID_STORE_PASSWORD=your-store-password")
            terminal.println()
        }

        // Build
        terminal.println("2️⃣  Build your app:")
        terminal.println("   mobilectl build")
        terminal.println()

        // Deploy
        terminal.println("3️⃣  Deploy your app:")
        if (config.deploy?.flavorGroups?.isNotEmpty() == true) {
            val defaultGroup = config.deploy?.defaultGroup
            if (defaultGroup != null) {
                terminal.println("   mobilectl deploy --group $defaultGroup")
            } else {
                terminal.println("   mobilectl deploy --all-variants")
            }
        } else {
            terminal.println("   mobilectl deploy")
        }
        terminal.println()

        // Documentation
        terminal.println("4️⃣  Learn more:")
        terminal.println("   • Read docs/SETUP.md for setup details")
        terminal.println("   • Run 'mobilectl --help' for all commands")
        terminal.println("   • Check generated CI/CD workflows")
        terminal.println()

        terminal.println("═".repeat(60))
    }

    /**
     * Resolves the output path for the config file.
     */
    private fun resolveConfigPath(): String {
        return outputPath ?: File(workingDir, "mobileops.yaml").absolutePath
    }
}
