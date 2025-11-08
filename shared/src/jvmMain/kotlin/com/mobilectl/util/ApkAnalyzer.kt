package com.mobilectl.util

import com.mobilectl.util.PremiumLogger
import java.io.File
import java.util.zip.ZipFile

/**
 * Analyzes Android artifacts (APK/AAB) to extract metadata
 */
object ApkAnalyzer {

    /**
     * Extract package ID from APK/AAB using aapt/aapt2
     */
    fun getPackageId(artifactFile: File): String? {
        if (!artifactFile.exists()) {
            PremiumLogger.error("File does not exist: ${artifactFile.absolutePath}")
            return null
        }

        // Validate file type
        if (!isValidAndroidArtifact(artifactFile)) {
            PremiumLogger.error("File is not a valid APK or AAB: ${artifactFile.absolutePath}")
            return null
        }

        val isAab = artifactFile.extension.lowercase() == "aab"

        // For AAB files, extract manifest from the bundle
        // For APK files, use aapt2 directly
        val packageId = if (isAab) {
            extractPackageFromAab(artifactFile)
        } else {
            tryGetPackageWithAapt2(artifactFile)
                ?: tryGetPackageWithAapt2Badging(artifactFile)
                ?: tryGetPackageWithAapt(artifactFile)
        }
        return packageId
    }

    /**
     * Check if file is a valid Android artifact (APK or AAB)
     */
    private fun isValidAndroidArtifact(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension == "apk" || extension == "aab"
    }

    /**
     * Extract version code from APK/AAB
     */
    fun getVersionCode(artifactFile: File): String? {
        if (!artifactFile.exists()) {
            return null
        }

        if (!isValidAndroidArtifact(artifactFile)) {
            return null
        }

        val isAab = artifactFile.extension.lowercase() == "aab"

        return if (isAab) {
            extractVersionCodeFromAab(artifactFile)
        } else {
            tryGetVersionCodeWithAapt2(artifactFile)
                ?: tryGetVersionCodeWithAapt(artifactFile)
        }
    }

    /**
     * Extract version name from APK/AAB
     */
    fun getVersionName(artifactFile: File): String? {
        if (!artifactFile.exists()) {
            return null
        }

        if (!isValidAndroidArtifact(artifactFile)) {
            return null
        }

        val isAab = artifactFile.extension.lowercase() == "aab"

        return if (isAab) {
            extractVersionNameFromAab(artifactFile)
        } else {
            tryGetVersionNameWithAapt2(artifactFile)
                ?: tryGetVersionNameWithAapt(artifactFile)
        }
    }

    /**
     * Extract package name from AAB file by building a minimal APK and analyzing it
     */
    private fun extractPackageFromAab(aabFile: File): String? {
        return try {
            ZipFile(aabFile).use { zip ->
                // First, try to extract the base.apk (some AABs have this)
                var baseApkEntry = zip.getEntry("base.apk")

                // If not found, look for any APK in the bundle
                if (baseApkEntry == null) {
                    baseApkEntry = zip.entries().asSequence()
                        .firstOrNull { it.name.endsWith(".apk") && it.name.contains("base") }
                }

                // If we found a base APK, extract and analyze it
                if (baseApkEntry != null) {
                    val tempApk = File.createTempFile("base", ".apk")
                    tempApk.deleteOnExit()

                    zip.getInputStream(baseApkEntry).use { input ->
                        tempApk.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    // Use standard APK analysis
                    val packageName = tryGetPackageWithAapt2(tempApk)
                        ?: tryGetPackageWithAapt2Badging(tempApk)
                        ?: tryGetPackageWithAapt(tempApk)

                    tempApk.delete()

                    if (packageName != null) {
                        return packageName
                    }
                }

                // Fallback 1: Try bundletool to extract universal APK
                val bundletoolPackage = extractPackageUsingBundletool(aabFile)
                if (bundletoolPackage != null) {
                    return bundletoolPackage
                }

                // Fallback 2: Build a minimal APK from AAB contents
                val packageName = buildMinimalApkAndExtract(zip)

                if (packageName == null) {
                    PremiumLogger.error("Could not extract package name from AAB")
                }

                packageName
            }
        } catch (e: Exception) {
            PremiumLogger.error("Error extracting package from AAB: ${e.message}")
            null
        }
    }

    /**
     * Extract package name using bundletool
     * Bundletool is Google's official tool for working with AABs
     */
    private fun extractPackageUsingBundletool(aabFile: File): String? {
        return try {
            val bundletoolPath = findBundletool() ?: return null

            // Create temporary APKs output file
            val tempApks = File.createTempFile("universal", ".apks")
            tempApks.deleteOnExit()

            // Build universal APK using bundletool
            val buildProcess = ProcessBuilder(
                "java",
                "-jar",
                bundletoolPath,
                "build-apks",
                "--bundle=${aabFile.absolutePath}",
                "--output=${tempApks.absolutePath}",
                "--mode=universal"
            ).start()

            buildProcess.waitFor()

            if (buildProcess.exitValue() != 0) {
                val errorOutput = buildProcess.errorStream.bufferedReader().readText()
                PremiumLogger.error("Bundletool build-apks failed: $errorOutput")
                tempApks.delete()
                return null
            }

            // Extract the universal APK from the .apks file (which is a ZIP)
            val universalApk = File.createTempFile("universal", ".apk")
            universalApk.deleteOnExit()

            ZipFile(tempApks).use { apksZip ->
                val universalEntry = apksZip.getEntry("universal.apk")
                if (universalEntry != null) {
                    apksZip.getInputStream(universalEntry).use { input ->
                        universalApk.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } else {
                    PremiumLogger.error("universal.apk not found in .apks file")
                    tempApks.delete()
                    universalApk.delete()
                    return null
                }
            }

            tempApks.delete()

            // Analyze the universal APK
            val packageName = tryGetPackageWithAapt2(universalApk)
                ?: tryGetPackageWithAapt2Badging(universalApk)
                ?: tryGetPackageWithAapt(universalApk)

            universalApk.delete()
            packageName

        } catch (e: Exception) {
            PremiumLogger.error("Error using bundletool: ${e.message}")
            null
        }
    }

    /**
     * Find bundletool.jar
     * First checks ANDROID_HOME/bundletool, then common locations
     */
    private fun findBundletool(): String? {
        // Check ANDROID_HOME
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val bundletoolPath = File(androidHome, "bundletool/bundletool.jar")
            if (bundletoolPath.exists()) {
                return bundletoolPath.absolutePath
            }
        }

        // Check common locations
        val userHome = System.getProperty("user.home")
        val commonPaths = listOf(
            "$userHome/.android/bundletool.jar",
            "$userHome/bundletool.jar",
            "/usr/local/bin/bundletool.jar",
            "C:/Program Files/Android/bundletool/bundletool.jar"
        )

        for (path in commonPaths) {
            val file = File(path)
            if (file.exists()) {
                return file.absolutePath
            }
        }

        // Silently return null - bundletool is optional
        return null
    }

    /**
     * Build a minimal APK from AAB contents and extract package info
     */
    private fun buildMinimalApkAndExtract(aabZip: ZipFile): String? {
        return try {
            val minimalApk = buildMinimalApkFromAab(aabZip) ?: return null

            // Now analyze this minimal APK
            val packageName = tryGetPackageWithAapt2(minimalApk)
                ?: tryGetPackageWithAapt2Badging(minimalApk)
                ?: tryGetPackageWithAapt(minimalApk)

            minimalApk.delete()
            packageName
        } catch (e: Exception) {
            PremiumLogger.error("Error extracting package from minimal APK: ${e.message}")
            null
        }
    }

    /**
     * Extract package name from AndroidManifest.xml using aapt2
     */
    private fun extractPackageFromManifest(manifestFile: File): String? {
        return try {
            val aapt2Path = findAapt2()
            if (aapt2Path == null) {
                PremiumLogger.error("aapt2 not found")
                return null
            }

            val process = ProcessBuilder(
                aapt2Path,
                "dump",
                "xmltree",
                "--file",
                manifestFile.absolutePath
            ).start()

            val output = process.inputStream.bufferedReader().readText()
            val errorOutput = process.errorStream.bufferedReader().readText()
            process.waitFor()

            if (process.exitValue() == 0) {
                // Look for package attribute in the manifest tag
                // Format: A: package="com.example.app" (Raw: "com.example.app")
                val packageRegex = """A: package="([^"]+)"""".toRegex()
                val match = packageRegex.find(output)
                match?.groupValues?.get(1)
            } else {
                PremiumLogger.error("aapt2 xmltree failed with exit code ${process.exitValue()}")
                if (errorOutput.isNotBlank()) {
                    PremiumLogger.error("aapt2 xmltree error: $errorOutput")
                }
                null
            }
        } catch (e: Exception) {
            PremiumLogger.error("Error extracting package from manifest: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Extract version code from AAB file
     */
    private fun extractVersionCodeFromAab(aabFile: File): String? {
        return try {
            ZipFile(aabFile).use { zip ->
                // Try base APK first
                var baseApkEntry = zip.getEntry("base.apk")
                if (baseApkEntry == null) {
                    baseApkEntry = zip.entries().asSequence()
                        .firstOrNull { it.name.endsWith(".apk") && it.name.contains("base") }
                }

                if (baseApkEntry != null) {
                    val tempApk = File.createTempFile("base", ".apk")
                    tempApk.deleteOnExit()

                    zip.getInputStream(baseApkEntry).use { input ->
                        tempApk.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    val versionCode = tryGetVersionCodeWithAapt2(tempApk)
                        ?: tryGetVersionCodeWithAapt(tempApk)

                    tempApk.delete()

                    if (versionCode != null) {
                        return versionCode
                    }
                }

                // Fallback 1: Try bundletool
                val bundletoolVersion = extractVersionUsingBundletool(aabFile, "code")
                if (bundletoolVersion != null) {
                    return bundletoolVersion
                }

                // Fallback 2: Build minimal APK and extract
                val minimalApk = buildMinimalApkFromAab(zip)
                if (minimalApk != null) {
                    val versionCode = tryGetVersionCodeWithAapt2(minimalApk)
                        ?: tryGetVersionCodeWithAapt(minimalApk)
                    minimalApk.delete()
                    return versionCode
                }

                null
            }
        } catch (e: Exception) {
            PremiumLogger.error("Error extracting version code from AAB: ${e.message}")
            null
        }
    }

    /**
     * Extract version name from AAB file
     */
    private fun extractVersionNameFromAab(aabFile: File): String? {
        return try {
            ZipFile(aabFile).use { zip ->
                // Try base APK first
                var baseApkEntry = zip.getEntry("base.apk")
                if (baseApkEntry == null) {
                    baseApkEntry = zip.entries().asSequence()
                        .firstOrNull { it.name.endsWith(".apk") && it.name.contains("base") }
                }

                if (baseApkEntry != null) {
                    val tempApk = File.createTempFile("base", ".apk")
                    tempApk.deleteOnExit()

                    zip.getInputStream(baseApkEntry).use { input ->
                        tempApk.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    val versionName = tryGetVersionNameWithAapt2(tempApk)
                        ?: tryGetVersionNameWithAapt(tempApk)

                    tempApk.delete()

                    if (versionName != null) {
                        return versionName
                    }
                }

                // Fallback 1: Try bundletool
                val bundletoolVersion = extractVersionUsingBundletool(aabFile, "name")
                if (bundletoolVersion != null) {
                    return bundletoolVersion
                }

                // Fallback 2: Build minimal APK and extract
                val minimalApk = buildMinimalApkFromAab(zip)
                if (minimalApk != null) {
                    val versionName = tryGetVersionNameWithAapt2(minimalApk)
                        ?: tryGetVersionNameWithAapt(minimalApk)
                    minimalApk.delete()
                    return versionName
                }

                null
            }
        } catch (e: Exception) {
            PremiumLogger.error("Error extracting version name from AAB: ${e.message}")
            null
        }
    }

    /**
     * Extract version information using bundletool
     */
    private fun extractVersionUsingBundletool(aabFile: File, versionType: String): String? {
        return try {
            val bundletoolPath = findBundletool() ?: return null

            val tempApks = File.createTempFile("universal", ".apks")
            tempApks.deleteOnExit()

            val buildProcess = ProcessBuilder(
                "java",
                "-jar",
                bundletoolPath,
                "build-apks",
                "--bundle=${aabFile.absolutePath}",
                "--output=${tempApks.absolutePath}",
                "--mode=universal"
            ).start()

            buildProcess.waitFor()

            if (buildProcess.exitValue() != 0) {
                tempApks.delete()
                return null
            }

            val universalApk = File.createTempFile("universal", ".apk")
            universalApk.deleteOnExit()

            ZipFile(tempApks).use { apksZip ->
                val universalEntry = apksZip.getEntry("universal.apk")
                if (universalEntry != null) {
                    apksZip.getInputStream(universalEntry).use { input ->
                        universalApk.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } else {
                    tempApks.delete()
                    universalApk.delete()
                    return null
                }
            }

            tempApks.delete()

            val result = when (versionType) {
                "code" -> tryGetVersionCodeWithAapt2(universalApk)
                    ?: tryGetVersionCodeWithAapt(universalApk)
                "name" -> tryGetVersionNameWithAapt2(universalApk)
                    ?: tryGetVersionNameWithAapt(universalApk)
                else -> null
            }

            universalApk.delete()
            result

        } catch (e: Exception) {
            null
        }
    }

    /**
     * Build a minimal APK from AAB contents (helper method)
     */
    private fun buildMinimalApkFromAab(aabZip: ZipFile): File? {
        return try {
            // Create a temporary APK file
            val tempApk = File.createTempFile("minimal", ".apk")
            tempApk.deleteOnExit()

            // Create a ZIP (APK is just a ZIP)
            java.util.zip.ZipOutputStream(tempApk.outputStream()).use { apkOut ->
                // Copy manifest from AAB
                val manifestEntry = aabZip.getEntry("base/manifest/AndroidManifest.xml")
                if (manifestEntry != null) {
                    apkOut.putNextEntry(java.util.zip.ZipEntry("AndroidManifest.xml"))
                    aabZip.getInputStream(manifestEntry).copyTo(apkOut)
                    apkOut.closeEntry()
                } else {
                    PremiumLogger.error("Manifest not found in AAB")
                    return null
                }

                // Note: We intentionally do NOT copy resources.pb as resources.arsc
                // because they use incompatible formats (protobuf vs binary ARSC).
                // aapt2 can parse APKs with just a manifest for package name extraction.
            }

            tempApk
        } catch (e: Exception) {
            PremiumLogger.error("Error building minimal APK: ${e.message}")
            null
        }
    }

    /**
     * Extract version code from AndroidManifest.xml
     */
    private fun extractVersionCodeFromManifest(manifestFile: File): String? {
        return try {
            val aapt2Path = findAapt2() ?: return null

            val process = ProcessBuilder(
                aapt2Path,
                "dump",
                "xmltree",
                "--file",
                manifestFile.absolutePath
            ).start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            if (process.exitValue() == 0) {
                // Look for versionCode attribute
                // Format: A: android:versionCode(0x0101021b)=(type 0x10)0x1
                val versionCodeRegex = """android:versionCode[^=]*=\(type 0x10\)0x([0-9a-fA-F]+)""".toRegex()
                val match = versionCodeRegex.find(output)
                match?.groupValues?.get(1)?.toInt(16)?.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extract version name from AndroidManifest.xml
     */
    private fun extractVersionNameFromManifest(manifestFile: File): String? {
        return try {
            val aapt2Path = findAapt2() ?: return null

            val process = ProcessBuilder(
                aapt2Path,
                "dump",
                "xmltree",
                "--file",
                manifestFile.absolutePath
            ).start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            if (process.exitValue() == 0) {
                // Look for versionName attribute
                // Format: A: android:versionName(0x0101021c)="1.0.0" (Raw: "1.0.0")
                val versionNameRegex = """android:versionName[^=]*="([^"]+)"""".toRegex()
                val match = versionNameRegex.find(output)
                match?.groupValues?.get(1)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get comprehensive APK/AAB info
     */
    fun getApkInfo(artifactFile: File): ApkInfo? {
        if (!artifactFile.exists()) {
            return null
        }

        if (!isValidAndroidArtifact(artifactFile)) {
            return null
        }

        val packageId = getPackageId(artifactFile)
        val versionCode = getVersionCode(artifactFile)
        val versionName = getVersionName(artifactFile)

        val fileType = when (artifactFile.extension.lowercase()) {
            "aab" -> "AAB"
            "apk" -> "APK"
            else -> "Android artifact"
        }

        return if (packageId != null) {
            ApkInfo(
                packageId = packageId,
                versionCode = versionCode,
                versionName = versionName,
                fileSizeBytes = artifactFile.length(),
                fileType = fileType
            )
        } else {
            null
        }
    }

    private fun tryGetPackageWithAapt2(apkFile: File): String? {
        return try {
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

    /**
     * Extract package name using aapt2 badging (works for both APK and AAB)
     */
    private fun tryGetPackageWithAapt2Badging(artifactFile: File): String? {
        return try {
            val process = ProcessBuilder(
                findAapt2() ?: return null,
                "dump",
                "badging",
                artifactFile.absolutePath
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
            PremiumLogger.error("Error executing aapt: ${e.message}")
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
        PremiumLogger.info("aapt2 not found in common SDK paths.")
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
 * Android artifact (APK/AAB) metadata information
 */
data class ApkInfo(
    val packageId: String,
    val versionCode: String?,
    val versionName: String?,
    val fileSizeBytes: Long,
    val fileType: String = "APK"  // "APK" or "AAB"
) {
    val fileSizeMB: Double
        get() = fileSizeBytes / (1024.0 * 1024.0)
}
