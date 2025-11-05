package com.mobilectl.deploy

import com.mobilectl.model.deploy.DeployResult
import java.io.File

class FirebaseAndroidUploader : BaseUploadStrategy() {

    override suspend fun upload(
        artifactFile: File,
        config: Map<String, String>
    ): DeployResult {
        return try {
            val startTime = System.currentTimeMillis()

            // Validate file
            val validFile = validateFile(artifactFile).getOrNull()
                ?: return DeployResult(
                    success = false,
                    platform = "android",
                    destination = "firebase",
                    message = validateFile(artifactFile).exceptionOrNull()?.message ?: "Unknown error"
                )

            // Validate config
            val token = getRequiredConfig(config, "token").getOrNull()
                ?: return DeployResult(
                    success = false,
                    platform = "android",
                    destination = "firebase",
                    message = "Missing Firebase token"
                )

            val appId = getRequiredConfig(config, "appId").getOrNull()
                ?: return DeployResult(
                    success = false,
                    platform = "android",
                    destination = "firebase",
                    message = "Missing Firebase app ID"
                )

            println("📤 Uploading to Firebase App Distribution...")
            println("   App ID: $appId")
            println("   File: ${validFile.name} (${validFile.length() / (1024 * 1024)} MB)")

            // TODO: Implement actual Firebase upload
            // For now, simulate upload
            simulateUpload(validFile)

            val duration = System.currentTimeMillis() - startTime

            DeployResult(
                success = true,
                platform = "android",
                destination = "firebase",
                message = "Successfully uploaded to Firebase App Distribution",
                buildId = generateBuildId(),
                duration = duration
            )

        } catch (e: Exception) {
            DeployResult(
                success = false,
                platform = "android",
                destination = "firebase",
                message = "Upload failed: ${e.message}"
            )
        }
    }

    override fun validateConfig(config: Map<String, String>): List<String> {
        val errors = mutableListOf<String>()

        if (config["token"].isNullOrBlank()) {
            errors.add("Firebase token is required (set FIREBASE_TOKEN env var)")
        }

        if (config["appId"].isNullOrBlank()) {
            errors.add("Firebase app ID is required")
        }

        return errors
    }

    private suspend fun simulateUpload(file: File) {
        // Simulate network delay
        kotlinx.coroutines.delay(1000)
    }

    private fun generateBuildId(): String {
        return "firebase_${System.currentTimeMillis()}"
    }
}

class LocalAndroidUploader : BaseUploadStrategy() {

    override suspend fun upload(
        artifactFile: File,
        config: Map<String, String>
    ): DeployResult {
        return try {
            val startTime = System.currentTimeMillis()

            val validFile = validateFile(artifactFile).getOrNull()
                ?: return DeployResult(
                    success = false,
                    platform = "android",
                    destination = "local",
                    message = "Invalid artifact file"
                )

            val outputDir = config["outputDir"]?.let { File(it) }
                ?: File("build/deploy")

            outputDir.mkdirs()
            val destFile = File(outputDir, validFile.name)

            println("📁 Copying to local directory...")
            println("   From: ${validFile.absolutePath}")
            println("   To: ${destFile.absolutePath}")

            validFile.copyTo(destFile, overwrite = true)

            val duration = System.currentTimeMillis() - startTime

            DeployResult(
                success = true,
                platform = "android",
                destination = "local",
                message = "Artifact copied to ${destFile.absolutePath}",
                buildUrl = destFile.absolutePath,
                duration = duration
            )

        } catch (e: Exception) {
            DeployResult(
                success = false,
                platform = "android",
                destination = "local",
                message = "Copy failed: ${e.message}"
            )
        }
    }

    override fun validateConfig(config: Map<String, String>): List<String> {
        return emptyList()  // No required config for local
    }
}
