package com.mobilectl.util

import java.io.File

/**
 * Auto-detects build artifacts in common locations
 *
 * Uses simple file-based caching to track artifact locations for faster resolution
 */
object ArtifactDetector {

    /**
     * Find artifact by path (with cache-first strategy)
     *
     * Resolution order:
     * 1. Check cache for previously found artifact path
     * 2. Use explicitly provided path
     * 3. Fall back to auto-detection
     */
    fun resolveArtifact(
        path: String?,
        artifactType: ArtifactType = ArtifactType.APK,
        baseDir: File = File(System.getProperty("user.dir")),
        flavor: String? = null,
        type: String? = null
    ): File? {
        // 1. Check cache first if flavor and type are provided
        if (flavor != null && type != null) {
            val cachedPath = getCachedArtifactPath(baseDir, flavor, type, artifactType)
            if (cachedPath != null) {
                val cachedFile = File(cachedPath)
                if (cachedFile.exists()) {
                    return cachedFile
                }
            }
        }

        // 2. If path specified, use it
        if (!path.isNullOrBlank()) {
            val file = if (File(path).isAbsolute) {
                File(path)
            } else {
                File(baseDir, path)
            }

            if (file.exists()) {
                // Cache this path for next time
                if (flavor != null && type != null) {
                    cacheArtifactPath(baseDir, flavor, type, file.absolutePath, artifactType)
                }
                return file
            }
        }

        // 3. Fall back to auto-detection
        val detected = when (artifactType) {
            ArtifactType.APK -> {
                if (flavor != null && type != null) {
                    findSignedApk(baseDir, flavor, type)
                        ?: findUnsignedApk(baseDir, flavor, type)
                } else {
                    findAndroidApk(baseDir)
                }
            }
            ArtifactType.AAB -> {
                if (flavor != null && type != null) {
                    findAab(baseDir, flavor, type)
                } else {
                    findAndroidAab(baseDir)
                }
            }
            ArtifactType.IPA -> findIosIpa(baseDir)
        }

        // Cache detected artifact
        if (detected != null && flavor != null && type != null) {
            cacheArtifactPath(baseDir, flavor, type, detected.absolutePath, artifactType)
        }

        return detected
    }

    /**
     * Find signed APK for specific flavor and type
     */
    fun findSignedApk(baseDir: File, flavor: String, type: String): File? {
        val moduleDir = findAndroidModuleDir(baseDir) ?: baseDir
        val apkDir = File(moduleDir, "build/outputs/apk/$flavor/$type")

        return if (apkDir.exists()) {
            apkDir.walk()
                .filter { it.name.endsWith(".apk") && !it.name.endsWith("-unsigned.apk") }
                .maxByOrNull { it.lastModified() }
        } else {
            null
        }
    }

    /**
     * Find unsigned APK for specific flavor and type
     */
    fun findUnsignedApk(baseDir: File, flavor: String, type: String): File? {
        val moduleDir = findAndroidModuleDir(baseDir) ?: baseDir
        val apkDir = File(moduleDir, "build/outputs/apk/$flavor/$type")

        return if (apkDir.exists()) {
            apkDir.walk()
                .filter { it.name.endsWith("-unsigned.apk") }
                .maxByOrNull { it.lastModified() }
        } else {
            null
        }
    }

    /**
     * Find AAB (App Bundle) for specific flavor and type
     */
    fun findAab(baseDir: File, flavor: String, type: String): File? {
        val moduleDir = findAndroidModuleDir(baseDir) ?: baseDir

        // Try standard path first
        val bundleDir = File(moduleDir, "build/outputs/bundle/$flavor${type.replaceFirstChar { it.uppercase() }}")
        if (bundleDir.exists()) {
            val aabFile = bundleDir.walk()
                .filter { it.name.endsWith(".aab") }
                .maxByOrNull { it.lastModified() }
            if (aabFile != null) return aabFile
        }

        // Try alternate path (some Gradle versions use this)
        val alternateBundleDir = File(moduleDir, "build/outputs/bundle/$flavor/$type")
        if (alternateBundleDir.exists()) {
            return alternateBundleDir.walk()
                .filter { it.name.endsWith(".aab") }
                .maxByOrNull { it.lastModified() }
        }

        return null
    }

    /**
     * Find Android APK in common locations (generic fallback)
     */
    fun findAndroidApk(baseDir: File = File(System.getProperty("user.dir"))): File? {
        val commonPaths = listOf(
            // Gradle release builds
            "app/build/outputs/apk/release/app-release.apk",
            "app/build/outputs/apk/release/app-release-unsigned.apk",
            "android/build/outputs/apk/release/app-release.apk",
            "android/build/outputs/apk/release/app-release-unsigned.apk",

            // Flavor-specific builds
            "app/build/outputs/apk/staging/release/app-staging-release.apk",
            "app/build/outputs/apk/production/release/app-production-release.apk",

            // Root level
            "app-release.apk",

            // Debug builds
            "app/build/outputs/apk/debug/app-debug.apk",
            "app-debug.apk"
        )

        return findArtifact(baseDir, commonPaths)
    }

    /**
     * Find Android AAB in common locations (generic fallback)
     */
    fun findAndroidAab(baseDir: File = File(System.getProperty("user.dir"))): File? {
        val commonPaths = listOf(
            // Gradle release builds
            "app/build/outputs/bundle/release/app-release.aab",
            "app/build/outputs/bundle/releaseBundle/app-release.aab",

            // Flavor-specific builds
            "app/build/outputs/bundle/staging/release/app-staging-release.aab",
            "app/build/outputs/bundle/production/release/app-production-release.aab",

            // Root level
            "app-release.aab"
        )

        return findArtifact(baseDir, commonPaths)
    }

    /**
     * Find iOS IPA in common locations
     */
    fun findIosIpa(baseDir: File = File(System.getProperty("user.dir"))): File? {
        val commonPaths = listOf(
            // Xcode builds
            "build/outputs/ipa/release/app.ipa",
            "build/iphoneos/app.ipa",
            "build/Release-iphoneos/app.ipa",

            // Archive exports
            "build/archive/app.ipa",
            "build/export/app.ipa",

            // Root level
            "app.ipa",
            "app-release.ipa",

            // Fastlane builds
            "fastlane/builds/app.ipa",
            "fastlane/builds/*.ipa"
        )

        return findArtifact(baseDir, commonPaths)
    }

    /**
     * Find most recent APK (useful if multiple exist)
     */
    fun findMostRecentApk(baseDir: File = File(System.getProperty("user.dir"))): File? {
        return findAndroidApk(baseDir)?.takeIf { it.exists() }
            ?: findAllApks(baseDir).maxByOrNull { it.lastModified() }
    }

    /**
     * Find all APKs in common locations
     */
    fun findAllApks(baseDir: File = File(System.getProperty("user.dir"))): List<File> {
        val moduleDir = findAndroidModuleDir(baseDir) ?: baseDir
        val buildDir = File(moduleDir, "build/outputs/apk")

        return if (buildDir.exists()) {
            buildDir.walk()
                .filter { it.name.endsWith(".apk") }
                .toList()
        } else {
            emptyList()
        }
    }

    /**
     * Check if artifact is a debug build
     */
    fun isDebugBuild(artifactFile: File): Boolean {
        return artifactFile.name.contains("debug", ignoreCase = true)
    }

    // ===== PRIVATE HELPERS =====

    /**
     * Find the Android app module directory
     */
    private fun findAndroidModuleDir(baseDir: File): File? {
        // Try common module names first (fast path)
        val commonNames = listOf("app", "android", "mobile")
        for (name in commonNames) {
            val moduleDir = File(baseDir, name)
            if (moduleDir.exists() && isAndroidModule(moduleDir)) {
                return moduleDir
            }
        }

        // Fall back to scanning all directories
        baseDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory && !dir.name.startsWith(".") && isAndroidModule(dir)) {
                return dir
            }
        }

        return null
    }

    /**
     * Check if a directory is an Android module
     */
    private fun isAndroidModule(dir: File): Boolean {
        return File(dir, "build.gradle.kts").exists()
                || File(dir, "build.gradle").exists()
    }

    /**
     * Find first existing artifact from list
     */
    private fun findArtifact(baseDir: File, paths: List<String>): File? {
        return paths
            .map { path ->
                if (path.contains("*")) {
                    // Handle glob patterns
                    val dir = File(baseDir, path.substringBefore("*"))
                    val pattern = path.substringAfterLast("/")

                    if (dir.exists() && dir.isDirectory) {
                        dir.listFiles { file ->
                            file.name.matches(pattern.replace("*", ".*").toRegex())
                        }?.firstOrNull()
                    } else {
                        null
                    }
                } else {
                    // Direct path
                    File(baseDir, path)
                }
            }
            .firstOrNull { it != null && it.exists() }
    }

    // ===== SIMPLE FILE-BASED CACHING =====

    /**
     * Get cached artifact path
     */
    private fun getCachedArtifactPath(
        baseDir: File,
        flavor: String,
        type: String,
        artifactType: ArtifactType
    ): String? {
        return try {
            val cacheFile = File(baseDir, ".mobilectl/artifacts.cache")
            if (!cacheFile.exists()) return null

            val cacheKey = buildCacheKey(flavor, type, artifactType)

            cacheFile.readLines()
                .firstOrNull { it.startsWith("$cacheKey=") }
                ?.substringAfter("=")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Cache artifact path
     */
    private fun cacheArtifactPath(
        baseDir: File,
        flavor: String,
        type: String,
        path: String,
        artifactType: ArtifactType
    ) {
        try {
            val cacheDir = File(baseDir, ".mobilectl")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val cacheFile = File(cacheDir, "artifacts.cache")
            val cacheKey = buildCacheKey(flavor, type, artifactType)

            // Read existing cache
            val lines = if (cacheFile.exists()) {
                cacheFile.readLines()
                    .filter { !it.startsWith("$cacheKey=") }
                    .toMutableList()
            } else {
                mutableListOf()
            }

            // Add new entry
            lines.add("$cacheKey=$path")

            // Write back
            cacheFile.writeText(lines.joinToString("\n"))
        } catch (e: Exception) {
            // Silent fail - caching is optional
        }
    }

    /**
     * Build cache key for artifact
     */
    private fun buildCacheKey(flavor: String, type: String, artifactType: ArtifactType): String {
        return "${flavor}_${type}_${artifactType.name.lowercase()}"
    }
}

/**
 * Artifact type enumeration
 */
enum class ArtifactType {
    APK,   // Android Package
    AAB,   // Android App Bundle
    IPA    // iOS App
}
