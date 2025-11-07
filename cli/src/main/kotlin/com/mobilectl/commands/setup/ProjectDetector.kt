package com.mobilectl.commands.setup

import java.io.File

/**
 * Auto-detects project configuration and settings.
 * Used by SetupWizard to provide intelligent defaults.
 */
class ProjectDetector(
    private val workingDir: String = System.getProperty("user.dir")
) {
    private val baseDir = File(workingDir)

    // ========================================================================
    // PROJECT TYPE DETECTION
    // ========================================================================

    /**
     * Detects the project type (Android Native, Flutter, React Native, iOS).
     */
    fun detectProjectType(): ProjectType? {
        return when {
            isFlutterProject() -> ProjectType.FLUTTER
            isReactNativeProject() -> ProjectType.REACT_NATIVE
            isAndroidNativeProject() -> ProjectType.ANDROID_NATIVE
            isIosNativeProject() -> ProjectType.IOS_NATIVE
            else -> null
        }
    }

    private fun isFlutterProject(): Boolean {
        return File(baseDir, "pubspec.yaml").exists() &&
                File(baseDir, "lib").exists()
    }

    private fun isReactNativeProject(): Boolean {
        val packageJson = File(baseDir, "package.json")
        if (!packageJson.exists()) return false

        val content = packageJson.readText()
        return content.contains("react-native")
    }

    private fun isAndroidNativeProject(): Boolean {
        return File(baseDir, "app/build.gradle").exists() ||
                File(baseDir, "app/build.gradle.kts").exists() ||
                File(baseDir, "build.gradle").exists() ||
                File(baseDir, "build.gradle.kts").exists()
    }

    private fun isIosNativeProject(): Boolean {
        return baseDir.listFiles()?.any { file ->
            file.extension == "xcodeproj" || file.extension == "xcworkspace"
        } == true
    }

    // ========================================================================
    // APP METADATA DETECTION
    // ========================================================================

    /**
     * Detects the app name from various sources.
     */
    fun detectAppName(): String? {
        if(!baseDir.exists()) return null
        return detectFlutterAppName()
            ?: detectAndroidAppName()
            ?: detectIosAppName()
            ?: detectFromDirectoryName()
    }

    private fun detectFlutterAppName(): String? {
        val pubspec = File(baseDir, "pubspec.yaml")
        if (!pubspec.exists()) return null

        return pubspec.readLines()
            .firstOrNull { it.trim().startsWith("name:") }
            ?.substringAfter("name:")
            ?.trim()
    }

    private fun detectAndroidAppName(): String? {
        val stringsXml = findFile("app/src/main/res/values/strings.xml")
            ?: return null

        val appNameRegex = """<string name="app_name">(.*?)</string>""".toRegex()
        return appNameRegex.find(stringsXml.readText())?.groupValues?.get(1)
    }

    private fun detectIosAppName(): String? {
        val infoPlist = findFile("ios/*/Info.plist")
            ?: findFile("*/Info.plist")
            ?: return null

        val content = infoPlist.readText()
        val bundleNameRegex = """<key>CFBundleName</key>\s*<string>(.*?)</string>""".toRegex()
        return bundleNameRegex.find(content)?.groupValues?.get(1)
    }

    private fun detectFromDirectoryName(): String? {
        val dirName = baseDir.name
        return if (dirName.isNotBlank()) {
            dirName.replace("-", " ").replace("_", " ")
                .split(" ")
                .joinToString(" ") { it.capitalize() }
        } else null
    }

    /**
     * Detects the package name / bundle identifier.
     */
    fun detectPackageName(): String? {
        return detectAndroidPackageName()
            ?: detectIosBundleId()
            ?: detectFlutterPackageName()
    }

    private fun detectAndroidPackageName(): String? {
        // Try build.gradle.kts first
        val buildGradleKts = findFile("app/build.gradle.kts")
        if (buildGradleKts != null) {
            val content = buildGradleKts.readText()
            val packageRegex = """applicationId\s*=\s*["'](.*?)["']""".toRegex()
            val match = packageRegex.find(content)
            if (match != null) return match.groupValues[1]
        }

        // Try build.gradle (Groovy)
        val buildGradle = findFile("app/build.gradle")
        if (buildGradle != null) {
            val content = buildGradle.readText()
            val packageRegex = """applicationId\s+["'](.*?)["']""".toRegex()
            val match = packageRegex.find(content)
            if (match != null) return match.groupValues[1]
        }

        // Try AndroidManifest.xml
        val manifest = findFile("app/src/main/AndroidManifest.xml")
        if (manifest != null) {
            val content = manifest.readText()
            val packageRegex = """package\s*=\s*["'](.*?)["']""".toRegex()
            val match = packageRegex.find(content)
            if (match != null) return match.groupValues[1]
        }

        return null
    }

    private fun detectIosBundleId(): String? {
        val infoPlist = findFile("ios/*/Info.plist")
            ?: findFile("*/Info.plist")
            ?: return null

        val content = infoPlist.readText()
        val bundleIdRegex = """<key>CFBundleIdentifier</key>\s*<string>(.*?)</string>""".toRegex()
        return bundleIdRegex.find(content)?.groupValues?.get(1)
    }

    private fun detectFlutterPackageName(): String? {
        // For Flutter, check Android package name
        return detectAndroidPackageName()
    }

    /**
     * Detects the current version.
     */
    fun detectVersion(): String? {
        return detectFlutterVersion()
            ?: detectAndroidVersion()
            ?: detectIosVersion()
    }

    private fun detectFlutterVersion(): String? {
        val pubspec = File(baseDir, "pubspec.yaml")
        if (!pubspec.exists()) return null

        return pubspec.readLines()
            .firstOrNull { it.trim().startsWith("version:") }
            ?.substringAfter("version:")
            ?.trim()
            ?.substringBefore("+") // Remove build number
    }

    private fun detectAndroidVersion(): String? {
        val buildGradleKts = findFile("app/build.gradle.kts")
        if (buildGradleKts != null) {
            val content = buildGradleKts.readText()
            val versionNameRegex = """versionName\s*=\s*["'](.*?)["']""".toRegex()
            val match = versionNameRegex.find(content)
            if (match != null) return match.groupValues[1]
        }

        val buildGradle = findFile("app/build.gradle")
        if (buildGradle != null) {
            val content = buildGradle.readText()
            val versionNameRegex = """versionName\s+["'](.*?)["']""".toRegex()
            val match = versionNameRegex.find(content)
            if (match != null) return match.groupValues[1]
        }

        return null
    }

    private fun detectIosVersion(): String? {
        val infoPlist = findFile("ios/*/Info.plist")
            ?: findFile("*/Info.plist")
            ?: return null

        val content = infoPlist.readText()
        val versionRegex = """<key>CFBundleShortVersionString</key>\s*<string>(.*?)</string>""".toRegex()
        return versionRegex.find(content)?.groupValues?.get(1)
    }

    // ========================================================================
    // BUILD CONFIGURATION DETECTION
    // ========================================================================

    /**
     * Detects Android product flavors.
     */
    fun detectAndroidFlavors(): List<String> {
        val buildGradleKts = findFile("app/build.gradle.kts")
        if (buildGradleKts != null) {
            return detectFlavorsFromKotlinDsl(buildGradleKts)
        }

        val buildGradle = findFile("app/build.gradle")
        if (buildGradle != null) {
            return detectFlavorsFromGroovy(buildGradle)
        }

        return emptyList()
    }

    private fun detectFlavorsFromKotlinDsl(file: File): List<String> {
        val content = file.readText()
        val flavors = mutableListOf<String>()

        // Find the start of productFlavors block
        val startIndex = content.indexOf("productFlavors")
        if (startIndex == -1) return emptyList()

        val openBraceIndex = content.indexOf('{', startIndex)
        if (openBraceIndex == -1) return emptyList()

        // Count braces to find the matching closing brace
        var braceCount = 1
        var currentIndex = openBraceIndex + 1
        var closeBraceIndex = -1

        while (currentIndex < content.length && braceCount > 0) {
            when (content[currentIndex]) {
                '{' -> braceCount++
                '}' -> {
                    braceCount--
                    if (braceCount == 0) {
                        closeBraceIndex = currentIndex
                        break
                    }
                }
            }
            currentIndex++
        }

        if (closeBraceIndex == -1) return emptyList()

        val flavorBlock = content.substring(openBraceIndex + 1, closeBraceIndex)

        // Extract flavor names like: create("free") or register("free")
        val flavorRegex = """(?:create|register)\s*\(\s*["'](.*?)["']\s*\)""".toRegex()
        flavorRegex.findAll(flavorBlock).forEach { match ->
            flavors.add(match.groupValues[1])
        }

        return flavors
    }

    private fun detectFlavorsFromGroovy(file: File): List<String> {
        val content = file.readText()
        val flavors = mutableListOf<String>()

        // Find the start of productFlavors block
        val startIndex = content.indexOf("productFlavors")
        if (startIndex == -1) return emptyList()

        val openBraceIndex = content.indexOf('{', startIndex)
        if (openBraceIndex == -1) return emptyList()

        // Count braces to find the matching closing brace
        var braceCount = 1
        var currentIndex = openBraceIndex + 1
        var closeBraceIndex = -1

        while (currentIndex < content.length && braceCount > 0) {
            when (content[currentIndex]) {
                '{' -> braceCount++
                '}' -> {
                    braceCount--
                    if (braceCount == 0) {
                        closeBraceIndex = currentIndex
                        break
                    }
                }
            }
            currentIndex++
        }

        if (closeBraceIndex == -1) return emptyList()

        val flavorBlock = content.substring(openBraceIndex + 1, closeBraceIndex)

        // Extract flavor names like: free {} or free { ... }
        val flavorRegex = """(\w+)\s*\{""".toRegex()
        flavorRegex.findAll(flavorBlock).forEach { match ->
            val flavorName = match.groupValues[1]
            // Exclude common Gradle keywords
            if (flavorName !in setOf("android", "productFlavors", "buildTypes")) {
                flavors.add(flavorName)
            }
        }

        return flavors
    }

    /**
     * Detects iOS project/workspace path.
     */
    fun detectIosProjectPath(): String? {
        // Prefer workspace over project
        baseDir.listFiles()?.forEach { file ->
            if (file.extension == "xcworkspace") {
                return file.name
            }
        }

        baseDir.listFiles()?.forEach { file ->
            if (file.extension == "xcodeproj") {
                return file.name
            }
        }

        // Check ios/ subdirectory
        val iosDir = File(baseDir, "ios")
        if (iosDir.exists()) {
            iosDir.listFiles()?.forEach { file ->
                if (file.extension == "xcworkspace") {
                    return "ios/${file.name}"
                }
            }
            iosDir.listFiles()?.forEach { file ->
                if (file.extension == "xcodeproj") {
                    return "ios/${file.name}"
                }
            }
        }

        return null
    }

    /**
     * Detects iOS scheme.
     */
    fun detectIosScheme(): String? {
        // Usually the scheme name is the same as the app name
        return detectAppName()
    }

    // ========================================================================
    // DEPLOYMENT CONFIGURATION DETECTION
    // ========================================================================

    /**
     * Detects Firebase credentials.
     */
    fun detectFirebaseCredentials(): String? {
        val knownPaths = listOf(
            "credentials/firebase-service-account.json",
            "credentials/firebase-adminsdk.json",
            "credentials/firebase-account.json",
            "credentials/account.json",
            "firebase-service-account.json",
            "firebase-adminsdk.json",
            "firebase-account.json"
        )
        return knownPaths
            .map { File(baseDir, it) }
            .firstOrNull { it.exists() }
            ?.path
    }

    /**
     * Detects google-services.json.
     */
    fun detectGoogleServicesJson(): String? {
        val knownPaths = listOf(
            "app/google-services.json",
            "app/src/main/google-services.json",
            "android/app/google-services.json",
            "google-services.json"
        )
        return knownPaths
            .map { File(baseDir, it) }
            .firstOrNull { it.exists() }
            ?.path
    }

    /**
     * Detects Play Console credentials.
     */
    fun detectPlayConsoleCredentials(): String? {
        val knownPaths = listOf(
            "credentials/play-console.json",
            "credentials/play-console-service-account.json",
            "credentials/google-play.json",
            "play-console.json",
            "play-console-service-account.json"
        )
        return knownPaths
            .map { File(baseDir, it) }
            .firstOrNull { it.exists() }
            ?.path
    }

    /**
     * Detects App Store Connect API key.
     */
    fun detectAppStoreConnectApiKey(): String? {
        val knownPaths = listOf(
            "credentials/app-store-connect-api-key.json",
            "credentials/appstore-api-key.json",
            "credentials/asc-api-key.json",
            "app-store-connect-api-key.json",
            "appstore-api-key.json"
        )
        return knownPaths
            .map { File(baseDir, it) }
            .firstOrNull { it.exists() }
            ?.path
    }

    // ========================================================================
    // VERSION MANAGEMENT DETECTION
    // ========================================================================

    /**
     * Detects files that contain version information.
     */
    fun detectVersionFiles(): List<String> {
        val files = mutableListOf<String>()

        // Flutter
        if (File(baseDir, "pubspec.yaml").exists()) {
            files.add("pubspec.yaml")
        }

        // Android
        if (File(baseDir, "app/build.gradle.kts").exists()) {
            files.add("app/build.gradle.kts")
        } else if (File(baseDir, "app/build.gradle").exists()) {
            files.add("app/build.gradle")
        }

        // iOS
        findFile("ios/*/Info.plist")?.let {
            files.add(it.path.removePrefix(baseDir.path + "/"))
        }

        // React Native
        if (File(baseDir, "package.json").exists()) {
            files.add("package.json")
        }

        return files
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    /**
     * Finds a file using a glob-like pattern (supports single wildcard).
     */
    private fun findFile(pattern: String): File? {
        val parts = pattern.split("*")
        if (parts.size == 1) {
            // No wildcard, direct path
            val file = File(baseDir, pattern)
            return if (file.exists()) file else null
        }

        // Simple wildcard support (only one level)
        val prefix = parts[0].removeSuffix("/")
        val suffix = parts[1].removePrefix("/")

        val parentDir = if (prefix.isEmpty()) baseDir else File(baseDir, prefix)
        if (!parentDir.exists() || !parentDir.isDirectory) return null

        return parentDir.listFiles()
            ?.firstOrNull { file ->
                file.isDirectory && File(file, suffix).exists()
            }
            ?.let { File(it, suffix) }
    }

    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
