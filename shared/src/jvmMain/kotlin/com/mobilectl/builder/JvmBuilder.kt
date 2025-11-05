package com.mobilectl.builder

import com.mobilectl.config.Config
import com.mobilectl.model.Platform
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.util.ProcessExecutor
import com.mobilectl.util.createLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = createLogger("BuildManager")

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

class AndroidBuilder(
    private val processExecutor: ProcessExecutor
) : PlatformBuilder {
    override suspend fun build(baseDir: String, config: Config): BuildOutput {
        val startTime = System.currentTimeMillis()

        return try {
            val buildFlavor = config.build.android.defaultFlavor
            val buildType = config.build.android.defaultType

            logger.info("Building Android from: $baseDir")
            logger.info("Flavor: $buildFlavor, Type: $buildType")

            // Find the correct gradlew executable
            val gradlewPath = findGradleWrapper(baseDir)

            if (gradlewPath == null) {
                logger.error("gradlew not found in $baseDir")
                return BuildOutput(
                    success = false,
                    platform = Platform.ANDROID,
                    error = "gradlew not found. Make sure this is a Gradle-based Android project.",
                    durationMs = System.currentTimeMillis() - startTime
                )
            }

            logger.info("Using gradlew at: $gradlewPath")

            // Build variant name (e.g., stagingDebug, productionRelease)
            val variantName = "${buildFlavor.lowercase()}${buildType.replaceFirstChar { it.uppercase() }}"
            val gradleTask = "assemble${variantName.replaceFirstChar { it.uppercase() }}"

            logger.info("Running gradle task: $gradleTask")

            // Execute gradlew from the base directory
            val result = processExecutor.execute(
                command = gradlewPath,
                args = listOf(gradleTask),
                workingDir = baseDir
            )

            val duration = System.currentTimeMillis() - startTime

            if (result.success) {
                logger.info("✅ Android build succeeded")
                BuildOutput(
                    success = true,
                    platform = Platform.ANDROID,
                    outputPath = "$baseDir/build/outputs/apk/$buildFlavor/$buildType/",
                    durationMs = duration
                )
            } else {
                logger.warn("❌ Android build failed: ${result.stderr}")
                BuildOutput(
                    success = false,
                    platform = Platform.ANDROID,
                    error = result.stderr.takeIf { it.isNotEmpty() } ?: "Build failed",
                    durationMs = duration
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error("Android build error: ${e.message}")

            BuildOutput(
                success = false,
                platform = Platform.ANDROID,
                error = e.message ?: "Unknown error",
                durationMs = duration
            )
        }
    }

    /**
     * Find the gradlew executable in the project
     */
    private fun findGradleWrapper(baseDir: String): String? {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")

        val candidates = if (isWindows) {
            listOf(
                "$baseDir/gradlew.bat",
                "$baseDir/gradlew",
                "$baseDir\\gradlew.bat",
                "$baseDir\\gradlew"
            )
        } else {
            listOf(
                "$baseDir/gradlew",
                "$baseDir/./gradlew"
            )
        }

        for (candidate in candidates) {
            val file = java.io.File(candidate)
            if (file.exists() && file.isFile) {
                logger.debug("Found gradlew at: $candidate")
                return candidate
            }
        }

        logger.debug("gradlew candidates checked: $candidates")
        return null
    }
}

class IosBuilder(
    private val processExecutor: ProcessExecutor
) : PlatformBuilder {
    override suspend fun build(baseDir: String, config: Config): BuildOutput {  // ← RECEIVE baseDir
        val startTime = System.currentTimeMillis()

        return try {
            logger.info("Building iOS from: $baseDir")

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
                logger.info("✅ iOS build succeeded")
                BuildOutput(
                    success = true,
                    platform = Platform.IOS,
                    outputPath = "build/outputs/ipa/",
                    durationMs = duration
                )
            } else {
                logger.warn("❌ iOS build failed: ${result.stderr}")
                BuildOutput(
                    success = false,
                    platform = Platform.IOS,
                    error = result.stderr.takeIf { it.isNotEmpty() } ?: "Build failed",
                    durationMs = duration
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error("iOS build error: ${e.message}")

            BuildOutput(
                success = false,
                platform = Platform.IOS,
                error = e.message ?: "Unknown error",
                durationMs = duration
            )
        }
    }
}