package com.mobilectl.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option


class ChangelogCommand : CliktCommand(name = "changelog") {
    private val format by option(
        "--format",
        help = "Output format (markdown, html)"
    ).default("markdown")

    private val output by option(
        "--output",
        help = "Output file path"
    ).default("CHANGELOG.md")
    private val verbose by option("--verbose", help = "Verbose output").flag()
    private val dryRun by option("--dry-run", help = "Show what would be done").flag()



    override fun run() {
        echo("📝 Generating changelog...")
        echo("📄 Format: $format")
        echo("💾 Output: $output")

        if (dryRun) {
            echo("📋 DRY-RUN: Changelog would be written to $output")
        }

        if (verbose) {
            echo("🔍 Verbose: Parsing commits from git log...")
        }
    }
}