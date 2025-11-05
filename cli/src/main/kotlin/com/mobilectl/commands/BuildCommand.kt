package com.mobilectl.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking

class BuildCommand : CliktCommand(name = "build") {
    private val platform by argument(
        name = "platform",
        help = "android, ios, or all (leave empty for auto-detect)"
    ).optional()

    private val flavor by argument(
        name = "flavor",
        help = "build flavor (staging, production, etc)"
    ).optional()

    private val type by argument(
        name = "type",
        help = "build type (debug, release)"
    ).optional()

    private val verbose by option("--verbose", "-v", help = "Verbose output").flag()
    private val dryRun by option("--dry-run", help = "Show what would be done").flag()

    override fun run() {
        runBlocking {
            BuildHandler(
                platform = platform,
                flavor = flavor,
                type = type,
                verbose = verbose,
                dryRun = dryRun
            ).execute()
        }
    }
}
