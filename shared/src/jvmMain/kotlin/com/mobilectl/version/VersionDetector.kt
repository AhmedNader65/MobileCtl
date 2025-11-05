package com.mobilectl.version

import com.mobilectl.util.createLogger
import java.io.File

fun createVersionDetector(): VersionDetector = JvmVersionDetector()

class JvmVersionDetector : VersionDetector {

    private val logger = createLogger("VersionDetector")

    /**
     * Auto-detect version from app files
     * Returns null if no version found
     */
    override fun detectVersionFromApp(baseDir: String): SemanticVersion? {
        // Try Android first (most common)
        detectFromBuildGradle(baseDir)?.let {
            logger.debug("Detected version from build.gradle: $it")
            return it
        }

        // Try package.json (React Native)
        detectFromPackageJson(baseDir)?.let {
            logger.debug("Detected version from package.json: $it")
            return it
        }

        // Try iOS Info.plist
        detectFromPlist(baseDir)?.let {
            logger.debug("Detected version from Info.plist: $it")
            return it
        }

        logger.warn("Could not auto-detect version from app files")
        return null
    }

    private fun detectFromBuildGradle(baseDir: String): SemanticVersion? {
        val gradleFiles = listOf(
            File(baseDir, "app/build.gradle.kts"),
            File(baseDir, "app/build.gradle"),
            File(baseDir, "build.gradle.kts"),
            File(baseDir, "build.gradle")
        )

        for (file in gradleFiles) {
            if (!file.exists()) continue

            val content = file.readText()

            // Try: versionName = "1.0.0"
            val versionNameRegex = """versionName\s*=\s*["']([0-9.]+)["']""".toRegex()
            versionNameRegex.find(content)?.let { match ->
                return SemanticVersion.parse(match.groupValues[1])
            }

            // Try: version = "1.0.0"
            val versionRegex = """version\s*=\s*["']([0-9.]+)["']""".toRegex()
            versionRegex.find(content)?.let { match ->
                return SemanticVersion.parse(match.groupValues[1])
            }
        }

        return null
    }

    private fun detectFromPackageJson(baseDir: String): SemanticVersion? {
        val file = File(baseDir, "package.json")
        if (!file.exists()) return null

        val content = file.readText()
        val regex = """"version"\s*:\s*"([0-9.]+)"""".toRegex()

        return regex.find(content)?.let { match ->
            SemanticVersion.parse(match.groupValues[1])
        }
    }

    private fun detectFromPlist(baseDir: String): SemanticVersion? {
        val plistFile = findPlistFile(baseDir) ?: return null
        val content = plistFile.readText()

        val regex = """<key>CFBundleShortVersionString</key>\s*<string>([0-9.]+)</string>""".toRegex()

        return regex.find(content)?.let { match ->
            SemanticVersion.parse(match.groupValues[1])
        }
    }

    private fun findPlistFile(baseDir: String): File? {
        return File(baseDir).walkTopDown()
            .find { it.name == "Info.plist" }
    }

}