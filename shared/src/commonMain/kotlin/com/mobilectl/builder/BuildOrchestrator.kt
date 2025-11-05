package com.mobilectl.builder

import com.mobilectl.config.Config
import com.mobilectl.detector.ProjectDetector
import com.mobilectl.model.Platform
import com.mobilectl.util.Logger
import com.mobilectl.util.createLogger

/**
 * High-level build orchestration (platform-independent)
 */
class BuildOrchestrator(
    private val detector: ProjectDetector,
    private val buildManager: BuildManager,

) {
    private val logger = createLogger("BuildOrchestrator")

    suspend fun build(
        config: Config,
        platforms: Set<Platform>? = null,
        verbose: Boolean = false,
        dryRun: Boolean = false
    ): BuildResult {
        return try {
            // Detect platforms if not specified
            val targetPlatforms = platforms ?: detector.detectPlatforms(
                config.build.android.enabled == true,
                config.build.ios.enabled == true
            )

            if (targetPlatforms.isEmpty()) {
                logger.error("No platforms to build. Enable Android or iOS in config.")
                return BuildResult(
                    success = false,
                    outputs = emptyList()
                )
            }

            if (verbose) {
                logger.info("Building platforms: ${targetPlatforms.joinToString(", ")}")
            }

            if (dryRun) {
                logger.info("DRY-RUN mode: showing what would be built")
                return BuildResult(
                    success = true,
                    outputs = targetPlatforms.map { platform ->
                        BuildOutput(
                            success = true,
                            platform = platform,
                            outputPath = when (platform) {
                                Platform.ANDROID -> "build/outputs/apk/release/"
                                Platform.IOS -> "build/outputs/ipa/"
                            }
                        )
                    }
                )
            }

            buildManager.build(targetPlatforms, config)
        } catch (e: Exception) {
            logger.error("Build failed", e)
            BuildResult(
                success = false,
                outputs = emptyList()
            )
        }
    }
}
