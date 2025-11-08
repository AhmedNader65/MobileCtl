package com.mobilectl.builder.android.signing

import com.mobilectl.config.Config

/**
 * Orchestrates artifact signing (APK/AAB)
 *
 * Platform-agnostic interface
 */
interface SigningOrchestrator {

    /**
     * Result of signing operation
     */
    data class SigningResult(
        val success: Boolean,
        val isSigned: Boolean,
        val error: String? = null,
        val warnings: List<String> = emptyList()
    )

    /**
     * Sign artifact (APK or AAB)
     *
     * @param artifactPath Path to artifact file
     * @param config Build configuration
     * @param baseDir Project base directory
     * @return Signing result
     */
    suspend fun signArtifact(
        artifactPath: String,
        config: Config,
        baseDir: String
    ): SigningResult

    /**
     * Check if signing is configured and available
     */
    fun isSigningAvailable(config: Config, baseDir: String): Boolean
}
