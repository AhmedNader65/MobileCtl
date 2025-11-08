package com.mobilectl.builder.android.signing

import com.mobilectl.util.PremiumLogger
import java.io.File

/**
 * Finds Android SDK tools (apksigner, zipalign, aapt, etc.)
 *
 * Strategy:
 * 1. Environment variables (ANDROID_HOME, ANDROID_SDK_ROOT)
 * 2. local.properties (sdk.dir)
 * 3. Common installation paths (platform-specific)
 */
class AndroidSdkFinder {

    /**
     * Find apksigner tool
     */
    fun findApksigner(): String? {
        return findTool("apksigner", isScript = true)
    }

    /**
     * Find zipalign tool
     */
    fun findZipalign(): String? {
        return findTool("zipalign", isScript = false)
    }

    /**
     * Find aapt tool (for APK analysis)
     */
    fun findAapt(): String? {
        return findTool("aapt", isScript = false)
    }

    /**
     * Generic tool finder
     */
    private fun findTool(toolName: String, isScript: Boolean): String? {
        // Strategy 1: Environment variables
        val sdkFromEnv = getSdkPathFromEnvironment()
        if (sdkFromEnv != null) {
            val tool = findToolInSdk(sdkFromEnv, toolName, isScript)
            if (tool != null) {
                PremiumLogger.info("Found $toolName in ANDROID_HOME: $tool")
                return tool
            }
        }

        // Strategy 2: local.properties
        val sdkFromProps = getSdkPathFromLocalProperties()
        if (sdkFromProps != null) {
            val tool = findToolInSdk(sdkFromProps, toolName, isScript)
            if (tool != null) {
                PremiumLogger.info("Found $toolName in local.properties: $tool")
                return tool
            }
        }

        // Strategy 3: Common paths
        val commonPaths = getCommonSdkPaths()
        for (path in commonPaths) {
            val tool = findToolInSdk(path, toolName, isScript)
            if (tool != null) {
                PremiumLogger.info("Found $toolName in common path: $tool")
                return tool
            }
        }

        PremiumLogger.warning("$toolName not found in Android SDK")
        return null
    }

    /**
     * Find tool in specific SDK directory
     */
    private fun findToolInSdk(sdkPath: String, toolName: String, isScript: Boolean): String? {
        val buildToolsDir = File(sdkPath, "build-tools")
        if (!buildToolsDir.exists()) return null

        // Get latest build-tools version
        val latestVersion = buildToolsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.name }
            ?.firstOrNull()
            ?: return null

        val isWindows = isWindows()

        // Build list of possible file names
        val possibleFiles = if (isScript) {
            // For scripts (apksigner)
            if (isWindows) {
                listOf(
                    File(latestVersion, "$toolName.bat"),
                    File(latestVersion, toolName)
                )
            } else {
                listOf(File(latestVersion, toolName))
            }
        } else {
            // For executables (zipalign, aapt)
            if (isWindows) {
                listOf(File(latestVersion, "$toolName.exe"))
            } else {
                listOf(File(latestVersion, toolName))
            }
        }

        // Return first existing file
        return possibleFiles.firstOrNull { it.exists() }?.absolutePath
    }

    /**
     * Get SDK path from environment variables
     */
    private fun getSdkPathFromEnvironment(): String? {
        return System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
    }

    /**
     * Get SDK path from local.properties
     */
    private fun getSdkPathFromLocalProperties(): String? {
        val localProps = findLocalProperties() ?: return null
        return readSdkDirFromFile(localProps)
    }

    /**
     * Find local.properties file (search up to 5 parent directories)
     */
    private fun findLocalProperties(): File? {
        var current = File(System.getProperty("user.dir")).absoluteFile
        repeat(5) {
            val localProps = File(current, "local.properties")
            if (localProps.exists()) {
                return localProps
            }
            current = current.parentFile ?: return null
        }
        return null
    }

    /**
     * Read sdk.dir from local.properties file
     */
    private fun readSdkDirFromFile(file: File): String? {
        return try {
            file.readLines()
                .firstOrNull { it.trim().startsWith("sdk.dir") }
                ?.substringAfter("=")
                ?.trim()
                ?.replace("\\\\", "/")
                ?.replace("\\", "/")
        } catch (e: Exception) {
            PremiumLogger.warning("Failed to read local.properties: ${e.message}")
            null
        }
    }

    /**
     * Get common Android SDK installation paths (platform-specific)
     */
    private fun getCommonSdkPaths(): List<String> {
        val userHome = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()

        return when {
            os.contains("windows") -> listOf(
                "$userHome\\AppData\\Local\\Android\\Sdk",
                "C:\\Android\\Sdk",
                "C:\\Program Files\\Android\\Sdk"
            )

            os.contains("mac") -> listOf(
                "$userHome/Library/Android/sdk"
            )

            else -> listOf(  // Linux/Unix
                "$userHome/Android/Sdk",
                "/opt/android-sdk",
                "/usr/local/android-sdk"
            )
        }
    }

    /**
     * Check if running on Windows
     */
    private fun isWindows(): Boolean {
        return System.getProperty("os.name").lowercase().contains("windows")
    }
}