package com.mobilectl.deploy

import com.mobilectl.deploy.firebase.FirebaseClient
import com.mobilectl.deploy.firebase.FirebaseHttpClient
import com.mobilectl.model.deploy.DeployResult
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
                        validateFile(artifactFile).exceptionOrNull() as Exception? ?: IllegalArgumentException("Invalid file")
                    )

                // Get service account path
                val serviceAccountPath = getRequiredConfig(config, "serviceAccount").getOrNull()
                    ?: return@withContext createFailureResult("Missing 'serviceAccount' config",
                        IllegalArgumentException("Missing 'serviceAccount' config")
                    )

                val serviceAccountFile = File(serviceAccountPath)
                if (!serviceAccountFile.exists()) {
                    return@withContext createFailureResult(
                        "Service account file not found: $serviceAccountPath",
                        IllegalArgumentException("Service account file not found")
                    )
                }

                com.mobilectl.util.PremiumLogger.section("Firebase App Distribution")
                com.mobilectl.util.PremiumLogger.detail("File", "${validFile.name} (${validFile.length() / (1024 * 1024)} MB)")

                // Show APK info
                try {
                    val apkInfo = com.mobilectl.util.ApkAnalyzer.getApkInfo(validFile)
                    if (apkInfo != null) {
                        com.mobilectl.util.PremiumLogger.detail("Package ID", apkInfo.packageId)
                    }
                } catch (e: Exception) {
                    // Ignore if we can't extract APK info
                }

                // Create Firebase client
                val firebaseClient = firebaseClientProvider(serviceAccountFile, validFile)

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
                    com.mobilectl.util.PremiumLogger.success("Upload complete")
                    com.mobilectl.util.PremiumLogger.detail("Release ID", uploadResponse.buildId ?: "N/A")
                    com.mobilectl.util.PremiumLogger.sectionEnd()

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
}
