package com.mobilectl.builder

import com.mobilectl.builder.android.AndroidBuilder
import com.mobilectl.config.Config
import com.mobilectl.model.Platform
import com.mobilectl.util.PremiumLogger
import com.mobilectl.util.ProcessExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JvmBuildManager(
    private val androidBuilder: AndroidBuilder,
    private val iosBuilder: IosBuilder
) : BuildManager {
    override suspend fun build(platforms: Set<Platform>,config: Config): BuildResult = withContext(Dispatchers.Default) {
        val baseDir = System.getProperty("user.dir")
        val outputs = mutableListOf<BuildOutput>()
        val startTime = System.currentTimeMillis()

        for (platform in platforms) {
            val output = when (platform) {
                Platform.ANDROID -> androidBuilder.build(baseDir, config)
                Platform.IOS -> iosBuilder.build(baseDir, config)
            }
            outputs.add(output)
        }

        val totalDuration = System.currentTimeMillis() - startTime

        BuildResult(
            success = outputs.all { it.success },
            outputs = outputs,
            totalDurationMs = totalDuration
        )
    }
}

class IosBuilder(
    private val processExecutor: ProcessExecutor
) : PlatformBuilder {
    override suspend fun build(baseDir: String, config: Config): BuildOutput {  // ← RECEIVE baseDir
        val startTime = System.currentTimeMillis()

        return try {
            PremiumLogger.info("Building iOS from: $baseDir")

            val result = processExecutor.execute(
                command = "xcodebuild",
                args = listOf(
                    "-scheme", "MyApp",
                    "-configuration", "Release",
                    "-destination", "generic/platform=iOS",
                    "build"
                ),
                workingDir = baseDir  // ← USE baseDir
            )

            val duration = System.currentTimeMillis() - startTime

            if (result.success) {
                PremiumLogger.info("✅ iOS build succeeded")
                BuildOutput(
                    success = true,
                    platform = Platform.IOS,
                    outputPath = "build/outputs/ipa/",
                    durationMs = duration
                )
            } else {
                PremiumLogger.warning("❌ iOS build failed: ${result.stderr}")
                BuildOutput(
                    success = false,
                    platform = Platform.IOS,
                    error = result.stderr.takeIf { it.isNotEmpty() } ?: "Build failed",
                    durationMs = duration
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            PremiumLogger.error("iOS build error: ${e.message}")

            BuildOutput(
                success = false,
                platform = Platform.IOS,
                error = e.message ?: "Unknown error",
                durationMs = duration
            )
        }
    }
}