package com.mobilectl.builder.android.signing

/**
 * Signs a specific artifact type
 */
interface ArtifactSigner {

    /**
     * Sign artifact
     */
    suspend fun sign(
        artifactPath: String,
        signingConfig: SigningConfig,
        baseDir: String
    ): SigningOrchestrator.SigningResult
}
