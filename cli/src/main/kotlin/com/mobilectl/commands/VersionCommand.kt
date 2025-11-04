package com.mobilectl.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option


class VersionCommand : CliktCommand(name = "version") {
    private val action by argument(name = "action").optional()
    private val level by argument(name = "level").optional()
    private val verbose by option("--verbose", help = "Verbose output").flag()
    private val dryRun by option("--dry-run", help = "Show what would be done").flag()


    override fun run() {

        when (action) {
            "bump" -> {
                echo("🔢 Bumping version: $level")
                if (dryRun) {
                    echo("📋 DRY-RUN: Version would be bumped from 1.0.0 → 1.0.1")
                }
            }
            "get" -> echo("📍 Current version: 1.0.0")
            else -> echo("❌ Unknown action: $action")
        }

        if (verbose) {
            echo("🔍 Verbose: Action=$action, Level=$level")
        }
    }
}