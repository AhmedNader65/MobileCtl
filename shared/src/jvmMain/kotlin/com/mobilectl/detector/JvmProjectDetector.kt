package com.mobilectl.detector

import com.mobilectl.model.Platform
import com.mobilectl.util.FileOperations
import com.mobilectl.util.createFileOperations
import com.mobilectl.util.createLogger
import java.io.File

fun createProjectDetector(): ProjectDetector = JvmProjectDetectorImpl()

/**
 * JVM implementation
 */
class JvmProjectDetectorImpl() : ProjectDetector {

    private val logger = createLogger("ProjectDetector")

    override fun detectPlatforms(
        baseDir: String,androidEnabled: Boolean, iosEnabled: Boolean): Set<Platform> {
        val detected = mutableSetOf<Platform>()

        logger.debug("Detecting platforms (androidEnabled=$androidEnabled, iosEnabled=$iosEnabled)")
        logger.debug("Working directory: ${File(".").absolutePath}")

        if (androidEnabled) {
            if (isAndroidProject(baseDir)) {
                logger.info("✅ Android project detected")
                detected.add(Platform.ANDROID)
            } else {
                logger.debug("❌ Android project not detected")
            }
        }

        if (iosEnabled) {
            if (isIosProject(baseDir)) {
                logger.info("✅ iOS project detected")
                detected.add(Platform.IOS)
            } else {
                logger.debug("❌ iOS project not detected")
            }
        }

        return detected
    }
        override fun isAndroidProject(baseDir: String): Boolean {
            logger.debug("Scanning for Android project in: $baseDir")

            val baseFile = File(baseDir)
            if (!baseFile.exists() || !baseFile.isDirectory) {
                logger.debug("Base directory doesn't exist: $baseDir")
                return false
            }

            // Look for any build.gradle or build.gradle.kts that has Android configuration
            val allBuildFiles = findFilesRecursively(baseDir, maxDepth = 2) { file ->
                file.name == "build.gradle" || file.name == "build.gradle.kts"
            }

            logger.debug("Found ${allBuildFiles.size} build.gradle files")

            // Look for AndroidManifest.xml (indicates Android project)
            val hasAndroidManifest = findFilesRecursively(baseDir, maxDepth = 3) { file ->
                file.name == "AndroidManifest.xml"
            }.isNotEmpty()

            logger.debug("Has AndroidManifest.xml: $hasAndroidManifest")

            // Check for settings.gradle (indicates multi-module project)
            val hasSettingsGradle = File(baseDir, "settings.gradle").exists() ||
                    File(baseDir, "settings.gradle.kts").exists()

            logger.debug("Has settings.gradle: $hasSettingsGradle")

            // Android project if:
            // - Has build.gradle/build.gradle.kts AND AndroidManifest.xml, OR
            // - Has settings.gradle (multi-module like flutter/react-native)
            val isAndroid = (allBuildFiles.isNotEmpty() && hasAndroidManifest) || hasSettingsGradle

            logger.debug("Is Android project: $isAndroid")

            return isAndroid
        }

        override fun isIosProject(baseDir: String): Boolean {
            logger.debug("Scanning for iOS project in: $baseDir")

            val baseFile = File(baseDir)
            if (!baseFile.exists() || !baseFile.isDirectory) {
                logger.debug("Base directory doesn't exist: $baseDir")
                return false
            }

            // Look for .xcodeproj or .xcworkspace anywhere in directory tree
            val xcodeProjects = findFilesRecursively(baseDir, maxDepth = 2) { file ->
                file.isDirectory && (file.name.endsWith(".xcodeproj") || file.name.endsWith(".xcworkspace"))
            }

            logger.debug("Found ${xcodeProjects.size} Xcode projects")

            // Look for Info.plist (iOS app configuration)
            val hasInfoPlist = findFilesRecursively(baseDir, maxDepth = 3) { file ->
                file.name == "Info.plist"
            }.isNotEmpty()

            logger.debug("Has Info.plist: $hasInfoPlist")

            // Look for podfile (CocoaPods dependency management)
            val hasPodfile = File(baseDir, "Podfile").exists() ||
                    File(baseDir, "Podfile.lock").exists() ||
                    findFilesRecursively(baseDir, maxDepth = 2) { file ->
                        file.name == "Podfile" || file.name == "Podfile.lock"
                    }.isNotEmpty()

            logger.debug("Has Podfile: $hasPodfile")

            // iOS project if:
            // - Has .xcodeproj/.xcworkspace, OR
            // - Has Info.plist, OR
            // - Has Podfile (likely iOS)
            val isIos = xcodeProjects.isNotEmpty() || hasInfoPlist || hasPodfile

            logger.debug("Is iOS project: $isIos")

            return isIos
        }

        /**
         * Recursively find files matching a predicate up to maxDepth
         */
        private fun findFilesRecursively(
            dir: String,
            maxDepth: Int,
            currentDepth: Int = 0,
            predicate: (File) -> Boolean
        ): List<File> {
            if (currentDepth >= maxDepth) return emptyList()

            val results = mutableListOf<File>()
            val currentDir = File(dir)

            try {
                currentDir.listFiles()?.forEach { file ->
                    if (predicate(file)) {
                        results.add(file)
                    }
                    if (file.isDirectory && !file.name.startsWith(".")) {
                        results.addAll(findFilesRecursively(file.absolutePath, maxDepth, currentDepth + 1, predicate))
                    }
                }
            } catch (e: Exception) {
                logger.debug("Error scanning directory: ${e.message}")
            }

            return results
        }
}