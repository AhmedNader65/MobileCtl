package com.mobilectl.builder

import java.io.File
import java.security.MessageDigest

object BuildCache {
    private const val CACHE_DIR = ".mobilectl"
    private const val CACHE_FILE = "build.cache"

    data class ValidationResult(
        val needsRebuild: Boolean,
        val reason: String,
        val filesChecked: Int,
        val currentHash: String?,
        val cachedHash: String?
    )

    fun validateCache(baseDir: String, sourceFiles: List<String>): ValidationResult {
        val cacheFile = File(baseDir, "$CACHE_DIR/$CACHE_FILE")

        if (!cacheFile.exists()) {
            return ValidationResult(
                needsRebuild = true,
                reason = "No cache found",
                filesChecked = sourceFiles.size,
                currentHash = null,
                cachedHash = null
            )
        }

        val currentHash = calculateSourceHash(baseDir, sourceFiles)
        val cachedHash = cacheFile.readText().trim()

        val needsRebuild = currentHash != cachedHash

        return ValidationResult(
            needsRebuild = needsRebuild,
            reason = if (needsRebuild) "Source files changed" else "Cache valid",
            filesChecked = sourceFiles.size,
            currentHash = currentHash,
            cachedHash = cachedHash
        )
    }

    fun shouldRebuild(baseDir: String, sourceFiles: List<String>): Boolean {
        return validateCache(baseDir, sourceFiles).needsRebuild
    }

    fun updateCache(baseDir: String, sourceFiles: List<String>) {
        val cacheDir = File(baseDir, CACHE_DIR)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val cacheFile = File(cacheDir, CACHE_FILE)
        val currentHash = calculateSourceHash(baseDir, sourceFiles)
        cacheFile.writeText(currentHash)
    }

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

    fun getAndroidSourceFiles(baseDir: String): List<String> {
        val sources = mutableListOf<String>()

        val commonDirs = listOf(
            "app/src",
            "app/build.gradle",
            "app/build.gradle.kts",
            "build.gradle",
            "build.gradle.kts",
            "gradle.properties",
            "settings.gradle",
            "settings.gradle.kts"
        )

        commonDirs.forEach { path ->
            val file = File(baseDir, path)
            when {
                file.isFile -> sources.add(path)
                file.isDirectory -> {
                    file.walk()
                        .filter { it.isFile && (it.extension == "kt" || it.extension == "java" || it.extension == "xml") }
                        .forEach { sources.add(it.relativeTo(File(baseDir)).path) }
                }
            }
        }

        return sources
    }
}
