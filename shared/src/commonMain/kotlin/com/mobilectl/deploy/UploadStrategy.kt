package com.mobilectl.deploy

import com.mobilectl.model.deploy.DeployResult
import java.io.File

/**
 * Strategy for uploading artifacts to different services
 * Open for extension, closed for modification (SOLID: OCP)
 */
interface UploadStrategy {
    /**
     * Upload artifact to destination
     * @param artifactFile The file to upload (APK, AAB, or IPA)
     * @param config Configuration (token, appId, etc)
     * @return DeployResult with status and details
     */
    suspend fun upload(
        artifactFile: File,
        config: Map<String, String>
    ): DeployResult

    /**
     * Validate that config is complete before uploading
     */
    fun validateConfig(config: Map<String, String>): List<String>
}

abstract class BaseUploadStrategy : UploadStrategy {

    protected fun validateFile(file: File): Result<File> {
        return when {
            !file.exists() -> Result.failure(Exception("Artifact file not found: ${file.absolutePath}"))
            !file.isFile -> Result.failure(Exception("Path is not a file: ${file.absolutePath}"))
            file.length() == 0L -> Result.failure(Exception("Artifact file is empty"))
            else -> Result.success(file)
        }
    }

    protected fun getRequiredConfig(config: Map<String, String>, key: String): Result<String> {
        return config[key]?.takeIf { it.isNotBlank() }?.let { Result.success(it) }
            ?: Result.failure(Exception("Missing required config: $key"))
    }
}