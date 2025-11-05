package com.mobilectl

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.mobilectl.commands.BuildCommand
import com.mobilectl.commands.ChangelogCommand
import com.mobilectl.commands.ChangelogGenerateCommand
import com.mobilectl.commands.ChangelogRestoreCommand
import com.mobilectl.commands.ChangelogShowCommand
import com.mobilectl.commands.ChangelogUpdateCommand
import com.mobilectl.commands.DeployCommand
import com.mobilectl.commands.InfoCommand
import com.mobilectl.commands.VersionBumpCommand
import com.mobilectl.commands.VersionCommand
import com.mobilectl.commands.VersionRestoreCommand
import com.mobilectl.commands.VersionShowCommand

fun main(args: Array<String>) {
    try {
        // Handle --version and --help directly
        when {
            args.contains("--version") -> {
                println("mobilectl v0.1.0")
                return
            }

            args.isEmpty() || args.contains("--help") -> {
                showHelp()
                return
            }
        }

        MobileCtl()
            .subcommands(
                BuildCommand(),
                DeployCommand(),
                VersionCommand().subcommands(
                    VersionShowCommand(),
                    VersionBumpCommand(),
                    VersionRestoreCommand()
                ),
                ChangelogCommand().subcommands(
                    ChangelogGenerateCommand(),
                    ChangelogShowCommand(),
                    ChangelogRestoreCommand(),
                    ChangelogUpdateCommand()
                ),
                InfoCommand()
            )
            .main(args)
    } catch (e: Exception) {
        System.err.println("❌ Error: ${e.message}")
        if (args.contains("--verbose")) {
            e.printStackTrace()
        }
        System.exit(1)
    }
}

private fun showHelp() {
    println(
        """
        ╭────────────────────────────────────────────────────────────╮
        │                      🔧 mobilectl v0.1.0                  │
        │          Modern DevOps automation for mobile apps          │
        ╰────────────────────────────────────────────────────────────╯
        
        Usage: mobilectl [OPTIONS] COMMAND [ARGS]...
        
        ✨ Commands:
          build              Build Android or iOS app
          deploy             Deploy build artifacts
          version            Manage app version
          changelog          Generate changelog
          info               Show project configuration
          
        📋 Options:
          --help             Show this message
          --version          Show version
          --verbose          Verbose output
          --dry-run          Show what would be done without doing it
    """.trimIndent()
    )
}