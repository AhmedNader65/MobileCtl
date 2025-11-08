package com.mobilectl.deploy.firebase

import com.mobilectl.deploy.BaseUploadStrategy
import com.mobilectl.model.deploy.DeployResult
import com.mobilectl.util.ApkAnalyzer
import com.mobilectl.util.PremiumLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Firebase Android App Distribution uploader
 * SOLID: Single Responsibility - only handles upload orchestration
 */
class FirebaseAndroidUploader(
    private val firebaseClientProvider: suspend (File, File) -> FirebaseClient = { serviceAccountFile, apkFile ->
        FirebaseHttpClient.create(serviceAccountFile, apkFile = apkFile)
    }
) : BaseUploadStrategy() {

    override suspend fun upload(
        artifactFile: File,
        config: Map<String, String>
    ): DeployResult {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()

                // Validate artifact
                val validFile = validateFile(artifactFile).getOrNull()
                    ?: return@withContext createFailureResult(
                        validateFile(artifactFile).exceptionOrNull()?.message ?: "Invalid file",
                        validateFile(artifactFile).exceptionOrNull() as Exception?
                            ?: IllegalArgumentException("Invalid file")
                    )

                // Get service account path
                val serviceAccountPath = getRequiredConfig(config, "serviceAccount").getOrNull()
                    ?: return@withContext createFailureResult(
                        "Missing 'serviceAccount' config",
                        IllegalArgumentException("Missing 'serviceAccount' config")
                    )

                val serviceAccountFile = File(serviceAccountPath)
                if (!serviceAccountFile.exists()) {
                    return@withContext createFailureResult(
                        "Service account file not found: $serviceAccountPath",
                        IllegalArgumentException("Service account file not found")
                    )
                }

                PremiumLogger.section("Firebase App Distribution")
                PremiumLogger.detail(
                    "File",
                    "${validFile.name} (${validFile.length() / (1024 * 1024)} MB)"
                )

                // Show APK info
                try {
                    val apkInfo = ApkAnalyzer.getApkInfo(validFile)
                    if (apkInfo != null) {
                        PremiumLogger.detail("Package ID", apkInfo.packageId)
                    }
                } catch (e: Exception) {
                    // Ignore if we can't extract APK info
                }

                // Check if artifact type is AAB (requires Google Play connection)
                val isAab = validFile.extension.lowercase() == "aab"
                if (isAab) {
                    PremiumLogger.warning("Uploading AAB (requires Google Play connection)")
                }

                // Ensure APK is aligned before upload (Firebase requirement)
                val fileToUpload = if (validFile.extension.lowercase() == "apk") {
                    ensureApkAligned(validFile)
                } else {
                    validFile
                }

                // Create Firebase client
                val firebaseClient = firebaseClientProvider(serviceAccountFile, fileToUpload)

                // Extract upload parameters
                val releaseNotes = config["releaseNotes"]
                val testGroups = config["testGroups"]?.split(",")?.map { it.trim() } ?: emptyList()

                // Upload
                val uploadResponse = firebaseClient.uploadBuild(
                    file = validFile,
                    releaseNotes = releaseNotes,
                    testGroups = testGroups
                )

                val duration = System.currentTimeMillis() - startTime

                // Return result
                return@withContext if (uploadResponse.success) {
                    PremiumLogger.success("Upload complete")
                    PremiumLogger.detail("Release ID", uploadResponse.buildId ?: "N/A")
                    PremiumLogger.sectionEnd()

                    DeployResult(
                        success = true,
                        platform = "android",
                        destination = "firebase",
                        message = uploadResponse.message,
                        buildId = uploadResponse.buildId,
                        buildUrl = uploadResponse.buildUrl,
                        duration = duration
                    )
                } else {
                    // Check if this is the AAB + Google Play linking error
                    val isAabError =
                        uploadResponse.message.contains("app bundle", ignoreCase = true) &&
                                uploadResponse.message.contains("Google Play", ignoreCase = true)

                    if (isAabError) {
                        PremiumLogger.sectionEnd()
                        showAabFirebaseGuidance(validFile)
                    }

                    DeployResult(
                        success = false,
                        platform = "android",
                        destination = "firebase",
                        message = uploadResponse.message,
                        duration = duration
                    )
                }

            } catch (e: Exception) {
                return@withContext createFailureResult(e.message ?: "Unknown error", e)
            }
        }
    }

    override fun validateConfig(config: Map<String, String>): List<String> {
        val errors = mutableListOf<String>()

        if (config["serviceAccount"].isNullOrBlank()) {
            errors.add("'serviceAccount' path is required in config")
        }

        return errors
    }

    private fun createFailureResult(message: String, error: Exception): DeployResult {
        return DeployResult(
            success = false,
            platform = "android",
            error = error,
            destination = "firebase",
            message = message
        )
    }

    /**
     * Ensure APK is aligned before uploading to Firebase
     * APKs signed with apksigner are already aligned
     * APKs from other sources may need alignment
     */
    private fun ensureApkAligned(apkFile: File): File {
        // Check if already aligned using apksigner (most reliable)
        if (isApkAlignedWithApksigner(apkFile)) {
            return apkFile
        }

        // Fallback to zipalign check
        if (isApkAligned(apkFile)) {
            return apkFile
        }

        PremiumLogger.progress("Aligning APK for Firebase...")

        // Find zipalign tool
        val zipalignPath = findZipalign()
        if (zipalignPath == null) {
            PremiumLogger.warning("zipalign not found - uploading unaligned APK (may fail)")
            return apkFile
        }

        // Create aligned APK in temp directory
        val alignedApk = File.createTempFile("aligned-", "-${apkFile.name}")
        alignedApk.deleteOnExit()

        try {
            val process = ProcessBuilder(
                zipalignPath,
                "-f",  // Force overwrite
                "4",   // Alignment in bytes
                apkFile.absolutePath,
                alignedApk.absolutePath
            ).start()

            process.waitFor()

            if (process.exitValue() == 0) {
                PremiumLogger.success("APK aligned successfully")
                return alignedApk
            } else {
                val error = process.errorStream.bufferedReader().readText()
                PremiumLogger.warning("zipalign failed: $error")
                return apkFile
            }
        } catch (e: Exception) {
            PremiumLogger.warning("zipalign error: ${e.message}")
            return apkFile
        }
    }

    /**
     * Check if APK is aligned using apksigner verify (most reliable)
     */
    private fun isApkAlignedWithApksigner(apkFile: File): Boolean {
        val apksignerPath = findApksigner() ?: return false

        try {
            val process = ProcessBuilder(
                apksignerPath,
                "verify",
                "--verbose",
                apkFile.absolutePath
            ).start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            // apksigner verify checks alignment as part of verification
            // If it exits with 0 and doesn't warn about alignment, it's aligned
            return process.exitValue() == 0 && !output.contains("not zip aligned", ignoreCase = true)
        } catch (e: Exception) {
            return false
        }
    }

    private fun findApksigner(): String? {
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val apksigner = findToolInSdk(androidHome, "apksigner")
            if (apksigner != null) return apksigner
        }

        val localPropsPath = findLocalProperties()
        if (localPropsPath != null) {
            val sdkDir = readSdkDirFromLocalProperties(localPropsPath)
            if (sdkDir != null) {
                val apksigner = findToolInSdk(sdkDir, "apksigner")
                if (apksigner != null) return apksigner
            }
        }

        val commonPaths = getCommonAndroidSdkPaths()
        for (path in commonPaths) {
            val apksigner = findToolInSdk(path, "apksigner")
            if (apksigner != null) return apksigner
        }

        return null
    }

    /**
     * Check if APK is already aligned
     */
    private fun isApkAligned(apkFile: File): Boolean {
        val zipalignPath = findZipalign() ?: return false

        try {
            val process = ProcessBuilder(
                zipalignPath,
                "-c",  // Check alignment
                "4",   // Alignment in bytes
                apkFile.absolutePath
            ).start()

            process.waitFor()
            return process.exitValue() == 0
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Find zipalign in Android SDK
     */
    private fun findZipalign(): String? {
        // Strategy 1: Check environment variables
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val zipalign = findToolInSdk(androidHome, "zipalign")
            if (zipalign != null) return zipalign
        }

        // Strategy 2: Read from local.properties
        val localPropsPath = findLocalProperties()
        if (localPropsPath != null) {
            val sdkDir = readSdkDirFromLocalProperties(localPropsPath)
            if (sdkDir != null) {
                val zipalign = findToolInSdk(sdkDir, "zipalign")
                if (zipalign != null) return zipalign
            }
        }

        // Strategy 3: Check common installation paths
        val commonPaths = getCommonAndroidSdkPaths()
        for (path in commonPaths) {
            val zipalign = findToolInSdk(path, "zipalign")
            if (zipalign != null) return zipalign
        }

        return null
    }

    private fun findToolInSdk(sdkPath: String, toolName: String): String? {
        val buildToolsDir = File(sdkPath, "build-tools")
        if (!buildToolsDir.exists()) return null

        val latestVersion = buildToolsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.name }
            ?.firstOrNull()

        if (latestVersion != null) {
            val isWindows = System.getProperty("os.name").lowercase().contains("windows")

            val possibleFiles = if (toolName == "apksigner") {
                // apksigner is a script (.bat on Windows, shell script on Unix)
                if (isWindows) {
                    listOf(File(latestVersion, "apksigner.bat"), File(latestVersion, "apksigner"))
                } else {
                    listOf(File(latestVersion, "apksigner"))
                }
            } else {
                // For other tools (zipalign, etc.)
                if (isWindows) {
                    listOf(File(latestVersion, "$toolName.exe"))
                } else {
                    listOf(File(latestVersion, toolName))
                }
            }

            for (toolFile in possibleFiles) {
                if (toolFile.exists()) {
                    return toolFile.absolutePath
                }
            }
        }

        return null
    }

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

    private fun readSdkDirFromLocalProperties(file: File): String? {
        return try {
            file.readLines()
                .firstOrNull { it.trim().startsWith("sdk.dir") }
                ?.substringAfter("=")
                ?.trim()
                ?.replace("\\\\", "/")
                ?.replace("\\", "/")
        } catch (e: Exception) {
            null
        }
    }

    private fun getCommonAndroidSdkPaths(): List<String> {
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
            else -> listOf(
                "$userHome/Android/Sdk",
                "/opt/android-sdk"
            )
        }
    }

    /**
     * Show guidance when Firebase rejects AAB upload due to missing Google Play connection
     */
    private fun showAabFirebaseGuidance(artifactFile: File) {
        val cyan = "\u001B[36m"
        val yellow = "\u001B[33m"
        val gray = "\u001B[90m"
        val white = "\u001B[97m"
        val reset = "\u001B[0m"
        val bold = "\u001B[1m"
        val dim = "\u001B[2m"

        println()
        println("$gray╭─────────────────────────────────────────────────────────────────────╮$reset")
        println("$gray│$reset                                                                     $gray│$reset")
        println("$gray│$reset  $yellow⚠$reset  ${bold}${white}Firebase Cannot Process AAB Files Without Google Play$reset      $gray│$reset")
        println("$gray│$reset                                                                     $gray│$reset")
        println("$gray╰─────────────────────────────────────────────────────────────────────╯$reset")
        println()
        println("  ${dim}Firebase App Distribution requires a Google Play connection to$reset")
        println("  ${dim}process AAB (Android App Bundle) files.$reset")
        println()
        println("  ${cyan}►$reset ${bold}Option 1: Connect Firebase to Google Play$reset")
        println("    ${gray}1. Go to Firebase Console → App Distribution$reset")
        println("    ${gray}2. Follow the setup to link your Google Play developer account$reset")
        println("    ${gray}3. Note: Requires a Google Play Console account (\$25 one-time fee)$reset")
        println()
        println("  ${cyan}►$reset ${bold}Option 2: Use APK for Firebase (Recommended for Testing)$reset")
        println("    ${gray}Add to your .mobilectl.yaml:$reset")
        println()
        println("    ${white}build:$reset")
        println("    ${white}  android:$reset")
        println("    ${white}    outputType: aab              ${dim}# Default for Play Store$reset")
        println("    ${white}    firebaseOutputType: apk      ${dim}# Override for Firebase$reset")
        println()
        println("    ${dim}This will build APK for Firebase and AAB for Play Store automatically.$reset")
        println()
        println("  ${dim}${gray}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$reset")
        println("  ℹ️  ${dim}For testing, APK is usually sufficient and doesn't require Play Console$reset")
        println("     ${dim}For production Play Store releases, use AAB$reset")
        println()
    }
}