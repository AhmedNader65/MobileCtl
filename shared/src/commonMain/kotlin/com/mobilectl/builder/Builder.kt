package com.mobilectl.builder

import com.mobilectl.config.Config
import com.mobilectl.model.Platform

/**
 * Interface for platform-specific builders
 */
interface PlatformBuilder {
    suspend fun build(
        baseDir: String,
        config: Config
    ): BuildOutput
}

data class BuildOutput(
    val success: Boolean,
    val platform: Platform,
    val outputPath: String? = null,
    val warnings: List<String> = emptyList(),
    val error: String? = null,
    val durationMs: Long = 0
) {
    val message: String
        get() = when {
            success -> "✅ Build successful"
            error != null -> "❌ Build failed: $error"
            else -> "❌ Build failed"
        }
}

interface BuildManager {
    suspend fun build(platforms: Set<Platform>,
                      config: Config,
                      baseDir: String): BuildResult
}

data class BuildResult(
    val success: Boolean,
    val outputs: List<BuildOutput>,
    val totalDurationMs: Long = 0
) {
    val message: String
        get() = if (success) "✅ All builds completed successfully"
        else "❌ One or more builds failed"
}
