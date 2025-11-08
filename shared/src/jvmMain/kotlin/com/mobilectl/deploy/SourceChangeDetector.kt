package com.mobilectl.commands.deploy

import com.mobilectl.model.Platform
import java.io.File

/**
 * Detects source changes to determine rebuild necessity
 */
class SourceChangeDetector(
    private val baseDir: File,
    private val verbose: Boolean = false
) {

    fun needsRebuild(artifactPath: String, platform: Platform): Boolean {
        val artifactFile = if (File(artifactPath).isAbsolute) {
            File(artifactPath)
        } else {
            File(baseDir, artifactPath)
        }

        if (!artifactFile.exists()) {
            if (verbose) println("   Artifact missing: $artifactPath")
            return true
        }

        val artifactTime = artifactFile.lastModified()

        if (hasUncommittedChanges()) {
            if (verbose) println("   Uncommitted changes detected")
            return true
        }

        return hasRecentSourceChanges(artifactTime, platform)
    }

    private fun hasUncommittedChanges(): Boolean {
        return try {
            val process = ProcessBuilder("git", "status", "--porcelain")
                .directory(baseDir)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            exitCode == 0 && output.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }

    private fun hasRecentSourceChanges(lastArtifactTime: Long, platform: Platform): Boolean {
        val sourcePatterns = when (platform) {
            Platform.ANDROID -> listOf(
                "src/**/*.kt", "src/**/*.java",
                "app/**/*.kt", "app/**/*.java",
                "build.gradle.kts", "build.gradle",
                "app/build.gradle.kts", "gradle.properties"
            )
            Platform.IOS -> listOf(
                "ios/**/*.swift", "ios/**/*.h", "ios/**/*.m",
                "ios/**/*.pbxproj", "ios/Podfile"
            )
            else -> emptyList()
        }

        return sourcePatterns.any { pattern ->
            hasChangesInPattern(pattern, lastArtifactTime)
        }
    }

    private fun hasChangesInPattern(pattern: String, lastArtifactTime: Long): Boolean {
        val dir = if (pattern.contains("**")) {
            baseDir.resolve(pattern.substringBefore("**").trimEnd('/'))
        } else {
            baseDir.resolve(pattern)
        }

        return when {
            dir.isDirectory -> {
                dir.walk()
                    .filter { it.isFile }
                    .any { it.lastModified() > lastArtifactTime }
            }
            dir.isFile -> {
                dir.lastModified() > lastArtifactTime
            }
            else -> false
        }
    }
}
