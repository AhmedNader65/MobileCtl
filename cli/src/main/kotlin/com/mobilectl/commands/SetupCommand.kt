package com.mobilectl.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.mobilectl.commands.setup.SetupHandler

/**
 * Setup command - comprehensive wizard to generate complete mobileops.yaml configuration.
 *
 * Usage:
 *   mobilectl setup                    # Run interactive wizard
 *   mobilectl setup --force            # Overwrite existing config
 *   mobilectl setup --output path.yaml # Custom output path
 *
 * Features:
 *   • Interactive wizard through 8 setup phases
 *   • Auto-detects project configuration
 *   • Generates complete mobileops.yaml
 *   • Optionally creates CI/CD workflows
 *   • Creates setup documentation
 */
class SetupCommand : CliktCommand(
    name = "setup",
    help = """
        🚀 Comprehensive setup wizard for mobilectl

        Guides you through complete project configuration and generates:
        • mobileops.yaml (complete config file)
        • .github/workflows/mobilectl-deploy.yml (optional)
        • .gitlab-ci.yml (optional)
        • docs/SETUP.md (setup summary)

        The wizard covers:
        1. Project information (name, package, type)
        2. Build configuration (flavors, signing)
        3. Deployment destinations (Firebase, Play Console, TestFlight)
        4. Version management (auto-increment, files)
        5. Changelog generation
        6. Deployment groups
        7. CI/CD setup
        8. Review and confirmation
    """.trimIndent()
) {
    private val force by option(
        "--force", "-f",
        help = "Overwrite existing configuration without prompting"
    ).flag()

    private val skipWizard by option(
        "--skip-wizard",
        help = "Skip wizard and generate minimal config (not recommended)"
    ).flag()

    private val output by option(
        "--output", "-o",
        help = "Custom output path for config file (default: mobileops.yaml)"
    )

    override fun run() {
        val exitCode = SetupHandler(
            force = force,
            skipWizard = skipWizard,
            outputPath = output
        ).handle()

        if (exitCode != 0) {
            System.exit(exitCode)
        }
    }
}
