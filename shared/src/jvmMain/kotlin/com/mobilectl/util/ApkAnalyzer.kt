package com.mobilectl.util

import java.io.File

/**
 * Analyzes APK files to extract metadata
 */
object ApkAnalyzer {

    /**
     * Extract package ID from APK using aapt/aapt2
     */
    fun getPackageId(apkFile: File): String? {
        if (!apkFile.exists()) {
            return null
        }

        // Try aapt2 first (newer)
        val packageId = tryGetPackageWithAapt2(apkFile)
            ?: tryGetPackageWithAapt(apkFile)
        return packageId
    }

    /**
     * Extract version code from APK
     */
    fun getVersionCode(apkFile: File): String? {
        if (!apkFile.exists()) {
            return null
        }

        return tryGetVersionCodeWithAapt2(apkFile)
            ?: tryGetVersionCodeWithAapt(apkFile)
    }

    /**
     * Extract version name from APK
     */
    fun getVersionName(apkFile: File): String? {
        if (!apkFile.exists()) {
            return null
        }

        return tryGetVersionNameWithAapt2(apkFile)
            ?: tryGetVersionNameWithAapt(apkFile)
    }

    /**
     * Get comprehensive APK info
     */
    fun getApkInfo(apkFile: File): ApkInfo? {
        if (!apkFile.exists()) {
            return null
        }

        val packageId = getPackageId(apkFile)
        val versionCode = getVersionCode(apkFile)
        val versionName = getVersionName(apkFile)
        PremiumLogger.info("APK Package ID: $packageId, Version Code: $versionCode, Version Name: $versionName")

        return if (packageId != null) {
            ApkInfo(
                packageId = packageId,
                versionCode = versionCode,
                versionName = versionName,
                fileSizeBytes = apkFile.length()
            )
        } else {
            null
        }
    }

    private fun tryGetPackageWithAapt2(apkFile: File): String? {
        return try {
            val aapt2 = findAapt()
            val process = ProcessBuilder(
                findAapt2() ?: return null,
                "dump",
                "packagename",
                apkFile.absolutePath
            ).start()

            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            if (process.exitValue() == 0 && output.isNotBlank()) {
                output
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun tryGetPackageWithAapt(apkFile: File): String? {
        return try {
            val process = ProcessBuilder(
                findAapt() ?: return null,
                "dump",
                "badging",
                apkFile.absolutePath
            ).start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            if (process.exitValue() == 0) {
                // Parse: package: name='com.example.app' versionCode='1' versionName='1.0'
                val packageLine = output.lines().firstOrNull { it.startsWith("package:") }
                val packageMatch = Regex("name='([^']+)'").find(packageLine ?: "")
                packageMatch?.groupValues?.get(1)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun tryGetVersionCodeWithAapt2(apkFile: File): String? {
        return try {
            val process = ProcessBuilder(
                findAapt2() ?: return null,
                "dump",
                "badging",
                apkFile.absolutePath
            ).start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            if (process.exitValue() == 0) {
                val versionMatch = Regex("versionCode='([^']+)'").find(output)
                versionMatch?.groupValues?.get(1)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun tryGetVersionCodeWithAapt(apkFile: File): String? {
        return try {
            val process = ProcessBuilder(
                findAapt() ?: return null,
                "dump",
                "badging",
                apkFile.absolutePath
            ).start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            if (process.exitValue() == 0) {
                val versionMatch = Regex("versionCode='([^']+)'").find(output)
                versionMatch?.groupValues?.get(1)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun tryGetVersionNameWithAapt2(apkFile: File): String? {
        return try {
            val process = ProcessBuilder(
                findAapt2() ?: return null,
                "dump",
                "badging",
                apkFile.absolutePath
            ).start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            if (process.exitValue() == 0) {
                val versionMatch = Regex("versionName='([^']+)'").find(output)
                versionMatch?.groupValues?.get(1)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun tryGetVersionNameWithAapt(apkFile: File): String? {
        return try {
            val process = ProcessBuilder(
                findAapt() ?: return null,
                "dump",
                "badging",
                apkFile.absolutePath
            ).start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            if (process.exitValue() == 0) {
                val versionMatch = Regex("versionName='([^']+)'").find(output)
                versionMatch?.groupValues?.get(1)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Find aapt2 in Android SDK with multiple detection strategies
     */
    private fun findAapt2(): String? {
        // Strategy 1: Check environment variables
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val aapt2 = findAaptInSdk(androidHome, "aapt2")
            if (aapt2 != null) return aapt2
        }

        // Strategy 2: Read from local.properties (Android project standard)
        val localPropsPath = findLocalProperties()
        if (localPropsPath != null) {
            val sdkDir = readSdkDirFromLocalProperties(localPropsPath)
            if (sdkDir != null) {
                val aapt2 = findAaptInSdk(sdkDir, "aapt2")
                if (aapt2 != null) return aapt2
            }
        }

        // Strategy 3: Check common installation paths
        val commonPaths = getCommonAndroidSdkPaths()
        for (path in commonPaths) {
            val aapt2 = findAaptInSdk(path, "aapt2")
            if (aapt2 != null) return aapt2
        }

        // Strategy 4: Try to find in PATH
        return findInPath("aapt2")
    }

    /**
     * Find aapt in Android SDK (fallback for older versions)
     */
    private fun findAapt(): String? {
        // Strategy 1: Check environment variables
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val aapt = findAaptInSdk(androidHome, "aapt")
            if (aapt != null) return aapt
        }

        // Strategy 2: Read from local.properties
        val localPropsPath = findLocalProperties()
        if (localPropsPath != null) {
            val sdkDir = readSdkDirFromLocalProperties(localPropsPath)
            if (sdkDir != null) {
                val aapt = findAaptInSdk(sdkDir, "aapt")
                if (aapt != null) return aapt
            }
        }

        // Strategy 3: Check common installation paths
        val commonPaths = getCommonAndroidSdkPaths()
        for (path in commonPaths) {
            val aapt = findAaptInSdk(path, "aapt")
            if (aapt != null) return aapt
        }

        // Strategy 4: Try to find in PATH
        return findInPath("aapt")
    }

    /**
     * Find aapt/aapt2 in a given SDK directory
     */
    private fun findAaptInSdk(sdkPath: String, toolName: String): String? {
        val buildToolsDir = File(sdkPath, "build-tools")
        if (!buildToolsDir.exists()) return null

        // Get latest build tools version
        val latestVersion = buildToolsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.name }
            ?.firstOrNull()

        if (latestVersion != null) {
            val toolFile = if (System.getProperty("os.name").lowercase().contains("windows")) {
                File(latestVersion, "$toolName.exe")
            } else {
                File(latestVersion, toolName)
            }

            if (toolFile.exists()) {
                return toolFile.absolutePath
            }
        }

        return null
    }

    /**
     * Find local.properties file (Android project standard)
     */
    private fun findLocalProperties(): File? {
        // Start from current directory and search up
        var current = File(System.getProperty("user.dir")).absoluteFile
        repeat(5) {  // Search up to 5 levels
            val localProps = File(current, "local.properties")
            if (localProps.exists()) {
                return localProps
            }
            current = current.parentFile ?: return null
        }
        return null
    }

    /**
     * Read sdk.dir from local.properties
     */
    private fun readSdkDirFromLocalProperties(file: File): String? {
        return try {
            file.readLines()
                .firstOrNull { it.trim().startsWith("sdk.dir") }
                ?.substringAfter("=")
                ?.trim()
                ?.replace("\\\\", "/")  // Windows path fix
                ?.replace("\\", "/")     // Windows path fix
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get common Android SDK installation paths by platform
     */
    private fun getCommonAndroidSdkPaths(): List<String> {
        val userHome = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()

        return when {
            os.contains("windows") -> listOf(
                "$userHome\\AppData\\Local\\Android\\Sdk",
                "C:\\Android\\Sdk",
                "C:\\Program Files\\Android\\Sdk",
                "C:\\Program Files (x86)\\Android\\Sdk"
            )
            os.contains("mac") -> listOf(
                "$userHome/Library/Android/sdk",
                "/Applications/Android Studio.app/Contents/sdk"
            )
            else -> listOf(  // Linux
                "$userHome/Android/Sdk",
                "/opt/android-sdk",
                "/usr/local/android-sdk"
            )
        }
    }

    /**
     * Find tool in system PATH
     */
    private fun findInPath(toolName: String): String? {
        return try {
            val which = if (System.getProperty("os.name").lowercase().contains("windows")) "where" else "which"
            val process = ProcessBuilder(which, toolName).start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            if (process.exitValue() == 0 && output.isNotBlank()) {
                output.lines().firstOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * APK metadata information
 */
data class ApkInfo(
    val packageId: String,
    val versionCode: String?,
    val versionName: String?,
    val fileSizeBytes: Long
) {
    val fileSizeMB: Double
        get() = fileSizeBytes / (1024.0 * 1024.0)
}
