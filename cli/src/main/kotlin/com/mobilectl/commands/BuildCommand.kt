package com.mobilectl.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.obj
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.mobilectl.config.ConfigLoader
import com.mobilectl.util.createFileUtil
import kotlinx.coroutines.runBlocking

class BuildCommand : CliktCommand(name = "build") {
    private val platform by argument(name = "platform").optional()
    private val verbose by option("--verbose", help = "Verbose output").flag()
    private val dryRun by option("--dry-run", help = "Show what would be done").flag()


    override fun run() {

        val target = platform ?: "all"
        echo("🏗️  Building: $target")

        if (dryRun) {
            echo("📋 DRY-RUN mode - nothing will actually be built")
        }

        if (verbose) {
            echo("🔍 Verbose mode enabled")
            echo("   Platform: $target")
            echo("   Working directory: ${currentContext.obj}")
        }
        val fileUtil = createFileUtil()
        val configLoader = ConfigLoader(fileUtil)
        runBlocking {

            // Load config (with auto-fallback to defaults)
            val result = configLoader.loadConfig("mobileops.yml")

            result.fold(
                onSuccess = { config ->
                    println("✅ Config loaded successfully")
                    println("App: ${config.app.name}")
                    println("Android enabled: ${config.build.android.enabled}")
                },
                onFailure = { error ->
                    println("❌ Failed to load config: ${error.message}")
                }
            )
        }
    }
}
