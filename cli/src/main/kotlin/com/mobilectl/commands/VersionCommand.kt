package com.mobilectl.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.mobilectl.commands.version.VersionBumpHandler
import com.mobilectl.commands.version.VersionRestoreHandler
import com.mobilectl.commands.version.VersionShowHandler
import kotlinx.coroutines.runBlocking

class VersionCommand : CliktCommand(name = "version") {
    override fun run() = Unit
}

class VersionShowCommand : CliktCommand(name = "show") {
    private val verbose by option("--verbose", "-v", help = "Show detailed version info").flag()

    override fun run() {
        runBlocking {
            VersionShowHandler(verbose = verbose).execute()
        }
    }
}

class VersionBumpCommand : CliktCommand(name = "bump") {
    private val level by argument(
        name = "level",
        help = "major, minor, or patch"
    )
    private val verbose by option("--verbose", "-v", help = "Verbose output").flag()
    private val dryRun by option("--dry-run", help = "Show what would be done").flag()
    private val skipBackup by option("--skip-backup", help = "Skip creating backup").flag()

    override fun run() {
        runBlocking {
            VersionBumpHandler(
                level = level,
                verbose = verbose,
                dryRun = dryRun,
                skipBackup = skipBackup
            ).execute()
        }
    }
}

class VersionRestoreCommand : CliktCommand(name = "restore") {
    private val backupName by argument(
        name = "backup",
        help = "backup name to restore (leave empty to list)"
    ).optional()

    override fun run() {
        VersionRestoreHandler(backupName = backupName).execute()
    }
}
