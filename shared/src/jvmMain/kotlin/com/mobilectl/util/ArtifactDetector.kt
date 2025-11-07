package com.mobilectl.util

import com.mobilectl.builder.ArtifactCache
import java.io.File

/**
 * Auto-detects build artifacts in common locations
 */
object ArtifactDetector {

    /**
     * Find Android APK in common locations
     */
    fun findAndroidApk(baseDir: File = File(System.getProperty("user.dir"))): File? {
        val commonPaths = listOf(
            // Gradle release builds
            "app/release/app-release.apk",
            "app/build/outputs/apk/release/app-release.apk",
            "app/build/outputs/apk/release/app-release-unsigned.apk",
            "android/release/app-release.apk",
            "android/build/outputs/apk/release/app-release.apk",
            "android/build/outputs/apk/release/app-release-unsigned.apk",


            // Flavor-specific builds
            "app/build/outputs/apk/staging/release/app-staging-release.apk",
            "app/build/outputs/apk/production/release/app-production-release.apk",

            "android/build/outputs/apk/staging/release/app-staging-release.apk",
            "android/build/outputs/apk/production/release/app-production-release.apk",

            // Root level
            "app-release.apk",

            // Alternative outputs
            "outputs/apk/app-release.apk",
            "outputs/apk/release/app.apk",

            // Gradle debug builds
            "build/outputs/apk/debug/app-debug.apk",
            "app-debug.apk",
        )

        return findArtifact(baseDir, commonPaths)
    }

    /**
     * Find Android AAB (App Bundle) in common locations
     */
    fun findAndroidAab(baseDir: File = File(System.getProperty("user.dir"))): File? {
        val commonPaths = listOf(
            // Gradle release builds
            "build/outputs/bundle/release/app-release.aab",
            "build/outputs/bundle/releaseBundle/app-release.aab",

            // Flavor-specific builds
            "build/outputs/bundle/staging/release/app-staging-release.aab",
            "build/outputs/bundle/production/release/app-production-release.aab",

            // Root level
            "app-release.aab",

            // Alternative outputs
            "outputs/bundle/app-release.aab"
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
     * Find all APKs in common locations using smart module detection
     */
    fun findAllApks(baseDir: File = File(System.getProperty("user.dir"))): List<File> {
        // Try to find Android module directory
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
     * Find the Android app module directory
     * Same logic as AndroidBuilder for consistency
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
        val gradleFiles = listOf(
            File(dir, "build.gradle.kts"),
            File(dir, "build.gradle")
        )

        for (gradleFile in gradleFiles) {
            if (gradleFile.exists()) {
                try {
                    val content = gradleFile.readText()
                    if (content.contains("com.android.application") ||
                        content.contains("com.android.library") ||
                        content.contains("id(\"com.android.application\")") ||
                        content.contains("id 'com.android.application'")) {
                        return true
                    }
                } catch (e: Exception) {
                    // Ignore read errors
                }
            }
        }

        return false
    }

    /**
     * Find artifact by path (with auto-detection as fallback)
     * Now checks build cache first before falling back to filesystem search
     */
    fun resolveArtifact(
        path: String?,
        artifactType: ArtifactType = ArtifactType.APK,
        baseDir: File = File(System.getProperty("user.dir")),
        flavor: String? = null,
        type: String? = null
    ): File? {
        // 1. Check cache first if flavor and type are provided
        if (flavor != null && type != null && artifactType == ArtifactType.APK) {
            val cachedPath = ArtifactCache.getArtifactPath(flavor, type)
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
                return file
            }
        }

        // 3. Fall back to auto-detection
        return when (artifactType) {
            ArtifactType.APK -> findAndroidApk(baseDir)
            ArtifactType.AAB -> findAndroidAab(baseDir)
            ArtifactType.IPA -> findIosIpa(baseDir)
        }
    }

    /**
     * Private helper: Find first existing artifact from list
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

    /**
     * Check if artifact is a debug build
     */
    fun isDebugBuild(artifactFile: File): Boolean {
        return artifactFile.name.contains("debug", ignoreCase = true)
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
