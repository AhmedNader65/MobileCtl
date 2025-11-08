package com.mobilectl.builder.android

import java.io.File
import java.security.MessageDigest

/**
 * Manages build cache to avoid unnecessary rebuilds
 *
 * Uses SHA-256 hash of source files to determine if rebuild is needed
 */
class BuildCacheManager {

    private val cacheDir = ".mobilectl"
    private val cacheFile = "build.cache"

    data class CacheValidation(
        val needsRebuild: Boolean,
        val reason: String,
        val filesChecked: Int,
        val currentHash: String? = null,
        val cachedHash: String? = null
    )

    /**
     * Validate cache and determine if rebuild is needed
     */
    fun validateCache(baseDir: String): CacheValidation {
        val sourceFiles = getAndroidSourceFiles(baseDir)
        val cacheFilePath = File(baseDir, "$cacheDir/$cacheFile")

        if (!cacheFilePath.exists()) {
            return CacheValidation(
                needsRebuild = true,
                reason = "No cache found",
                filesChecked = sourceFiles.size
            )
        }

        val currentHash = calculateSourceHash(baseDir, sourceFiles)
        val cachedHash = cacheFilePath.readText().trim()

        val needsRebuild = currentHash != cachedHash

        return CacheValidation(
            needsRebuild = needsRebuild,
            reason = if (needsRebuild) "Source files changed" else "Cache valid",
            filesChecked = sourceFiles.size,
            currentHash = currentHash,
            cachedHash = cachedHash
        )
    }

    /**
     * Update cache with current source hash
     */
    fun updateCache(baseDir: String) {
        val sourceFiles = getAndroidSourceFiles(baseDir)
        val cacheDirPath = File(baseDir, cacheDir)

        if (!cacheDirPath.exists()) {
            cacheDirPath.mkdirs()
        }

        val cacheFilePath = File(cacheDirPath, cacheFile)
        val currentHash = calculateSourceHash(baseDir, sourceFiles)
        cacheFilePath.writeText(currentHash)
    }

    /**
     * Get all Android source files to track
     */
    private fun getAndroidSourceFiles(baseDir: String): List<String> {
        val sources = mutableListOf<String>()

        val trackedPaths = listOf(
            "app/src",
            "app/build.gradle",
            "app/build.gradle.kts",
            "build.gradle",
            "build.gradle.kts",
            "gradle.properties",
            "settings.gradle",
            "settings.gradle.kts"
        )

        trackedPaths.forEach { path ->
            val file = File(baseDir, path)
            when {
                file.isFile -> sources.add(path)
                file.isDirectory -> {
                    file.walk()
                        .filter { it.isFile }
                        .filter { it.extension in setOf("kt", "java", "xml") }
                        .forEach { sources.add(it.relativeTo(File(baseDir)).path) }
                }
            }
        }

        return sources
    }

    /**
     * Calculate SHA-256 hash of source files
     */
    private fun calculateSourceHash(baseDir: String, sourceFiles: List<String>): String {
        val digest = MessageDigest.getInstance("SHA-256")

        sourceFiles.forEach { relativePath ->
            val file = File(baseDir, relativePath)
            if (file.exists() && file.isFile) {
                digest.update(file.readBytes())
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
