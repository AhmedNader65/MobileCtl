package com.mobilectl.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.obj
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.mobilectl.builder.AndroidBuilder
import com.mobilectl.builder.BuildOrchestrator
import com.mobilectl.builder.IosBuilder
import com.mobilectl.builder.JvmBuildManager
import com.mobilectl.config.ConfigLoader
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.model.Platform
import com.mobilectl.util.createFileUtil
import com.mobilectl.util.createProcessExecutor
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths

class BuildCommand : CliktCommand(name = "build") {
    private val platform by argument(name = "platform").optional()
    private val flavor by argument("flavor").optional()  // empty, staging, production, etc
    private val type by argument("type").optional()      // debug, release
    private val verbose by option("--verbose", help = "Verbose output").flag()
    private val dryRun by option("--dry-run", help = "Show what would be done").flag()
    private val customDir by option(
        "--dir",
        help = "Working directory (default: current working directory)"
    )


    override fun run() {

        val target = platform ?: "all"
        echo("🏗️  Building: $target")

        if (dryRun) {
            echo("📋 DRY-RUN mode - nothing will actually be built")
        }

        if (verbose) {
            echo("🔍 Verbose mode enabled")
            echo("   Platform: $target")
        }
        val fileUtil = createFileUtil()
        val configLoader = ConfigLoader(fileUtil)
        runBlocking {
            val workingPath = if (customDir != null) {
                File(customDir!!).absolutePath
            } else {
                File(".").absolutePath
            }

            echo("📍 Working directory: $workingPath")

            // Verify directory exists
            if (!File(workingPath).exists()) {
                echo("❌ Directory does not exist: $workingPath")
                return@runBlocking
            }
            // Load config (with auto-fallback to defaults)
            val configFile = File(workingPath, "mobileops.yml").absolutePath
            val configResult = configLoader.loadConfig(configFile)


            val config = configResult.getOrNull() ?: run {
                if (configResult.isFailure) {
                    echo("⚠️  Config not found, using defaults")
                }
                // Return default config
                com.mobilectl.config.Config()
            }

            // Parse platform argument
            val targetPlatforms = when {
                platform == null -> null  // Auto-detect
                platform == "all" -> setOf(Platform.ANDROID, Platform.IOS)
                platform == "android" -> setOf(Platform.ANDROID)
                platform == "ios" -> setOf(Platform.IOS)
                else -> {
                    echo("❌ Unknown platform: $platform. Use 'android', 'ios', or 'all'")
                    return@runBlocking
                }
            }
            config.apply {
                if (flavor != null) {
                    this.build.android.defaultFlavor = flavor.toString()
                }
                if (type != null) {
                    this.build.android.defaultType = type.toString()
                }
            }
            echo("Target platforms: $targetPlatforms")
            // Create builders
            val processExecutor = createProcessExecutor()
            val androidBuilder = AndroidBuilder(processExecutor)
            val iosBuilder = IosBuilder(processExecutor)
            val buildManager = JvmBuildManager(androidBuilder, iosBuilder)

            // Create orchestrator
            val detector = createProjectDetector()
            val orchestrator = BuildOrchestrator(detector, buildManager)

            // Execute build
            echo("🏗️  Starting build...")
            val result = orchestrator.build(
                baseDir = workingPath,
                config = config,
                platforms = targetPlatforms,
                verbose = verbose,
                dryRun = dryRun
            )

            // Print results
            echo("")
            result.outputs.forEach { output ->
                val status = if (output.success) "✅" else "❌"
                echo("$status ${output.platform.name}: ${output.message}")
                if (verbose && output.outputPath != null) {
                    echo("   Output: ${output.outputPath}")
                }
                if (output.durationMs > 0) {
                    val seconds = output.durationMs / 1000.0
                    echo("   Duration: ${String.format("%.2f", seconds)}s")
                }
            }

            echo("")
            if (result.success) {
                echo("✅ ${result.message}")
            } else {
                echo("❌ ${result.message}")
            }

            if (verbose && result.totalDurationMs > 0) {
                val seconds = result.totalDurationMs / 1000.0
                echo("📊 Total time: ${String.format("%.2f", seconds)}s")
            }
        }
    }
}
