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
    private val firebaseClientProvider: suspend (File) -> FirebaseClient = { serviceAccountFile ->
        FirebaseHttpClient.create(serviceAccountFile)
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

                println("📤 Uploading to Firebase App Distribution...")
                println("   File: ${validFile.name} (${validFile.length() / (1024 * 1024)} MB)")

                // Create Firebase client
                val firebaseClient = firebaseClientProvider(serviceAccountFile)

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
                    println("✅ Build uploaded successfully")
                    println("   Release ID: ${uploadResponse.buildId}")

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
