package com.mobilectl.deploy

import com.mobilectl.model.deploy.DeployResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Local filesystem uploader for testing and development
 * Simply copies artifact to a local directory
 */
class LocalAndroidUploader : BaseUploadStrategy() {

    override suspend fun upload(
        artifactFile: File,
        config: Map<String, String>
    ): DeployResult {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()

                // Validate artifact
                val validFile = validateFile(artifactFile).getOrNull()
                    ?: return@withContext createFailureResult("Invalid artifact file")

                // Get output directory
                val outputDir = config["outputDir"]?.let { File(it) }
                    ?: File("build/deploy")

                // Create directory if needed
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }

                if (!outputDir.isDirectory) {
                    return@withContext createFailureResult(
                        "Output path is not a directory: ${outputDir.absolutePath}"
                    )
                }

                println("📁 Copying to local directory...")
                println("   From: ${validFile.absolutePath}")
                println("   To: ${outputDir.absolutePath}")

                // Copy file
                val destFile = File(outputDir, validFile.name)
                validFile.copyTo(destFile, overwrite = true)

                println("✅ Artifact copied successfully")
                println("   Location: ${destFile.absolutePath}")

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
                return@withContext createFailureResult("Copy failed: ${e.message}")
            }
        }
    }

    override fun validateConfig(config: Map<String, String>): List<String> {
        // No required config for local uploader
        return emptyList()
    }

    private fun createFailureResult(message: String): DeployResult {
        return DeployResult(
            success = false,
            platform = "android",
            destination = "local",
            message = message
        )
    }
}
