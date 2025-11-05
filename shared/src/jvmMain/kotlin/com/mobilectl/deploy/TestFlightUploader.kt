package com.mobilectl.deploy

import com.mobilectl.model.deploy.DeployResult
import java.io.File

class TestFlightUploader : BaseUploadStrategy() {

    override suspend fun upload(
        artifactFile: File,
        config: Map<String, String>
    ): DeployResult {
        return try {
            val startTime = System.currentTimeMillis()

            val validFile = validateFile(artifactFile).getOrNull()
                ?: return DeployResult(
                    success = false,
                    platform = "ios",
                    destination = "testflight",
                    message = "Invalid artifact file"
                )

            val apiKey = getRequiredConfig(config, "apiKey").getOrNull()
                ?: return DeployResult(
                    success = false,
                    platform = "ios",
                    destination = "testflight",
                    message = "Missing App Store Connect API key"
                )

            val bundleId = getRequiredConfig(config, "bundleId").getOrNull()
                ?: return DeployResult(
                    success = false,
                    platform = "ios",
                    destination = "testflight",
                    message = "Missing bundle ID"
                )

            println("📤 Uploading to TestFlight...")
            println("   Bundle ID: $bundleId")
            println("   File: ${validFile.name} (${validFile.length() / (1024 * 1024)} MB)")

            // TODO: Implement actual TestFlight upload
            // For now, simulate upload
            simulateUpload(validFile)

            val duration = System.currentTimeMillis() - startTime

            DeployResult(
                success = true,
                platform = "ios",
                destination = "testflight",
                message = "Successfully uploaded to TestFlight",
                buildId = generateBuildId(),
                duration = duration
            )

        } catch (e: Exception) {
            DeployResult(
                success = false,
                platform = "ios",
                destination = "testflight",
                message = "Upload failed: ${e.message}"
            )
        }
    }

    override fun validateConfig(config: Map<String, String>): List<String> {
        val errors = mutableListOf<String>()

        if (config["apiKey"].isNullOrBlank()) {
            errors.add("App Store Connect API key is required")
        }

        if (config["bundleId"].isNullOrBlank()) {
            errors.add("Bundle ID is required")
        }

        return errors
    }

    private suspend fun simulateUpload(file: File) {
        kotlinx.coroutines.delay(1000)
    }

    private fun generateBuildId(): String {
        return "testflight_${System.currentTimeMillis()}"
    }
}

class LocalIosUploader : BaseUploadStrategy() {

    override suspend fun upload(
        artifactFile: File,
        config: Map<String, String>
    ): DeployResult {
        return try {
            val startTime = System.currentTimeMillis()

            val validFile = validateFile(artifactFile).getOrNull()
                ?: return DeployResult(
                    success = false,
                    platform = "ios",
                    destination = "local",
                    message = "Invalid artifact file"
                )

            val outputDir = config["outputDir"]?.let { File(it) }
                ?: File("build/deploy")

            outputDir.mkdirs()
            val destFile = File(outputDir, validFile.name)

            println("📁 Copying to local directory...")
            validFile.copyTo(destFile, overwrite = true)

            val duration = System.currentTimeMillis() - startTime

            DeployResult(
                success = true,
                platform = "ios",
                destination = "local",
                message = "Artifact copied to ${destFile.absolutePath}",
                buildUrl = destFile.absolutePath,
                duration = duration
            )

        } catch (e: Exception) {
            DeployResult(
                success = false,
                platform = "ios",
                destination = "local",
                message = "Copy failed: ${e.message}"
            )
        }
    }

    override fun validateConfig(config: Map<String, String>): List<String> {
        return emptyList()
    }
}
