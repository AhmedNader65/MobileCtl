package com.mobilectl.commands.buildCommand

import com.mobilectl.builder.AndroidBuilder
import com.mobilectl.builder.BuildOrchestrator
import com.mobilectl.builder.IosBuilder
import com.mobilectl.builder.JvmBuildManager
import com.mobilectl.config.ConfigLoader
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.model.Platform
import com.mobilectl.util.createFileUtil
import com.mobilectl.util.createProcessExecutor
import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

class BuildHandler(
    private val platform: String?,
    private val flavor: String?,
    private val type: String?,
    private val verbose: Boolean,
    private val dryRun: Boolean
) {
    private val out = PrintWriter(System.out, true, StandardCharsets.UTF_8)
    private val workingPath = System.getProperty("user.dir")
    private val configFile = File(workingPath, "mobileops.yaml").absolutePath

    suspend fun execute() {
        try {
            // Validate directory
            if (!File(workingPath).exists()) {
                out.println("❌ Directory does not exist: $workingPath")
                return
            }

            // Load config
            val fileUtil = createFileUtil()
            val detector = createProjectDetector()
            val configLoader = ConfigLoader(fileUtil, detector)
            val configResult = configLoader.loadConfig(configFile)

            val config = configResult.getOrNull() ?: run {
                if (configResult.isFailure) {
                    out.println("⚠️  Config not found, using defaults")
                }
                com.mobilectl.config.Config()
            }

            // Parse and validate inputs
            val targetPlatforms = parsePlatforms(platform)
            if (targetPlatforms == null) return

            // Apply CLI overrides to config
            applyOverrides(config, flavor, type)

            // Show summary
            printSummary(targetPlatforms)

            if (dryRun) {
                com.mobilectl.util.PremiumLogger.simpleInfo("DRY-RUN mode - nothing will actually be built")
                return
            }

            // Execute build
            executeBuild(config, targetPlatforms, detector)
        } catch (e: Exception) {
            out.println("❌ Error: ${e.message}")
        }
    }

    private fun parsePlatforms(platformArg: String?): Set<Platform>? {
        return when {
            platformArg == null -> null  // Auto-detect
            platformArg == "all" -> setOf(Platform.ANDROID, Platform.IOS)
            platformArg == "android" -> setOf(Platform.ANDROID)
            platformArg == "ios" -> setOf(Platform.IOS)
            else -> {
                out.println("❌ Unknown platform: $platformArg. Use 'android', 'ios', or 'all'")
                null
            }
        }
    }

    private fun applyOverrides(
        config: com.mobilectl.config.Config,
        flavorArg: String?,
        typeArg: String?
    ) {
        config.apply {
            if (flavorArg != null) {
                this.build.android.defaultFlavor = flavorArg
            }
            if (typeArg != null) {
                this.build.android.defaultType = typeArg
            }
        }
    }

    private fun printSummary(targetPlatforms: Set<Platform>?) {
        val target = targetPlatforms?.joinToString(", ") { it.name } ?: "auto-detect"
        val items = mutableMapOf<String, String>()
        items["Platforms"] = target

        if (verbose) {
            if (flavor != null) items["Flavor"] = flavor!!
            if (type != null) items["Type"] = type!!
        }

        com.mobilectl.util.PremiumLogger.box("Build Configuration", items, success = true)
    }

    private suspend fun executeBuild(
        config: com.mobilectl.config.Config,
        targetPlatforms: Set<Platform>?,
        detector: com.mobilectl.detector.ProjectDetector
    ) {
        val processExecutor = createProcessExecutor()
        val androidBuilder = AndroidBuilder(processExecutor)
        val iosBuilder = IosBuilder(processExecutor)
        val buildManager = JvmBuildManager(androidBuilder, iosBuilder)

        val orchestrator = BuildOrchestrator(detector, buildManager)

        val result = orchestrator.build(
            config = config,
            platforms = targetPlatforms,
            verbose = verbose,
            dryRun = false
        )

        printResults(result)
    }

    private fun printResults(result: com.mobilectl.builder.BuildResult) {
        result.outputs.forEach { output ->
            val items = mutableMapOf<String, String>()
            items["Platform"] = output.platform.name
            items["Status"] = output.message

            if (verbose) {
                if (output.outputPath != null) {
                    items["Output"] = output.outputPath!!
                }
                if (output.durationMs > 0) {
                    val seconds = output.durationMs / 1000.0
                    items["Duration"] = "${String.format("%.2f", seconds)}s"
                }
            }

            com.mobilectl.util.PremiumLogger.box(
                if (output.success) "Build Successful" else "Build Failed",
                items,
                success = output.success
            )
        }

        if (result.success) {
            com.mobilectl.util.PremiumLogger.simpleSuccess(result.message)
        } else {
            com.mobilectl.util.PremiumLogger.simpleError(result.message)
        }

        if (verbose && result.totalDurationMs > 0) {
            val seconds = result.totalDurationMs / 1000.0
            com.mobilectl.util.PremiumLogger.info("Total time: ${String.format("%.2f", seconds)}s")
        }
        out.println()
    }
}
