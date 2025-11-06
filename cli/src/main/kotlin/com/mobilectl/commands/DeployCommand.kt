package com.mobilectl.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.mobilectl.commands.deploy.DeployHandler
import kotlinx.coroutines.runBlocking

class DeployCommand : CliktCommand(
    name = "deploy"
) {
    private val platform by argument(
        name = "platform",
        help = "android, ios, or all (leave empty for auto-detect)"
    ).optional()

    private val destination by argument(
        name = "destination",
        help = "Deployment destination (firebase, local, testflight, etc.)"
    ).optional()

    private val environment by option(
        "--env", "-e",
        help = "Environment to deploy to (dev, staging, production)"
    )

    private val releaseNotes by option(
        "--notes", "-n",
        help = "Release notes for this deployment"
    )

    private val testGroups by option(
        "--groups", "-g",
        help = "Comma-separated list of test groups"
    )
    private val allFlavors by option(
        "-A", "--all-flavors",
        help = "Deploy all flavors"
    ).flag()

    private val group by option(
        "-G", "--flavor-group",
        help = "Deploy flavor group: production, testing, etc"
    )

    private val flavors by option(
        "-f", "--flavors",
        help = "Deploy specific flavors (comma-separated)"
    )

    private val exclude by option(
        "-E", "--exclude",
        help = "Exclude flavors (comma-separated)"
    )
    private val verbose by option(
        "--verbose", "-v",
        help = "Verbose output"
    ).flag()

    private val dryRun by option(
        "--dry-run",
        help = "Show what would be done without deploying"
    ).flag()

    private val skipBuild by option(
        "--skip-build",
        help = "Skip building and deploy existing artifacts"
    ).flag()


    private val interactive by option(
        "--interactive", "-i",
        help = "Interactive mode: let user choose platforms/destinations"
    ).flag()

    private val confirm by option(
        "--confirm", "-y",
        help = "Skip confirmation prompt"
    ).flag()

    // Bump version strategy: major, minor, patch, none
    private val bumpVersion by option(
        "--bump-version", "-B",
        metavar = "STRATEGY",
        help = "Bump version before deploying (major, minor, patch, none)"
    )
    private val changelog by option(
        "-C", "--changelog",
        help = "Generate changelog before deploy (auto if bumping version and enabled)"
    ).flag()
    override fun run() {
        runBlocking {
            DeployHandler(
                platform = platform,
                destination = destination,
                environment = environment,
                releaseNotes = releaseNotes,
                testGroups = testGroups,
                verbose = verbose,
                dryRun = dryRun,
                skipBuild = skipBuild,
                interactive = interactive,
                confirm = confirm,
                bumpVersion = bumpVersion,
                changelog = changelog,
                allFlavors = allFlavors,
                group = group,
                flavors = flavors,
                exclude = exclude
            ).execute()
        }
    }
}
