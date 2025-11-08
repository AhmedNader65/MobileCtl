package com.mobilectl.builder.android.signing

import com.mobilectl.config.Config
import java.io.File

/**
 * JVM implementation of SigningOrchestrator
 *
 * Delegates to platform-specific signers (APK/AAB)
 */
class JvmSigningOrchestrator(
    private val apkSigner: ArtifactSigner,
    private val aabSigner: ArtifactSigner
) : SigningOrchestrator {

    override suspend fun signArtifact(
        artifactPath: String,
        config: Config,
        baseDir: String
    ): SigningOrchestrator.SigningResult {
        // Validate configuration
        val validation = validateSigningConfig(config, baseDir)
        if (!validation.isValid) {
            return SigningOrchestrator.SigningResult(
                success = true,  // Not fatal
                isSigned = false,
                warnings = listOf(validation.reason)
            )
        }

        // Delegate to appropriate signer
        return when {
            artifactPath.endsWith(".apk") -> apkSigner.sign(
                artifactPath = artifactPath,
                signingConfig = validation.config!!,
                baseDir = baseDir
            )

            artifactPath.endsWith(".aab") -> aabSigner.sign(
                artifactPath = artifactPath,
                signingConfig = validation.config!!,
                baseDir = baseDir
            )

            else -> SigningOrchestrator.SigningResult(
                success = false,
                isSigned = false,
                error = "Unknown artifact type: $artifactPath"
            )
        }
    }

    override fun isSigningAvailable(config: Config, baseDir: String): Boolean {
        val validation = validateSigningConfig(config, baseDir)
        return validation.isValid
    }

    /**
     * Validate signing configuration
     */
    private fun validateSigningConfig(config: Config, baseDir: String): SigningValidation {
        val androidConfig = config.build.android

        // Check if keystore is configured
        if (androidConfig.keyStore.isBlank() || androidConfig.keyAlias.isBlank()) {
            return SigningValidation(
                isValid = false,
                reason = "Keystore not configured in mobilectl.yaml"
            )
        }

        // Resolve keystore path
        val keystorePath = if (File(androidConfig.keyStore).isAbsolute) {
            androidConfig.keyStore
        } else {
            File(baseDir, androidConfig.keyStore).absolutePath
        }

        // Check if keystore exists
        if (!File(keystorePath).exists()) {
            return SigningValidation(
                isValid = false,
                reason = "Keystore file not found: $keystorePath"
            )
        }

        // Get passwords
        val keyPassword = androidConfig.keyPassword.ifBlank {
            System.getenv("MOBILECTL_KEY_PASSWORD") ?: ""
        }
        val storePassword = androidConfig.storePassword.ifBlank {
            System.getenv("MOBILECTL_STORE_PASSWORD") ?: ""
        }

        if (keyPassword.isBlank() || storePassword.isBlank()) {
            return SigningValidation(
                isValid = false,
                reason = "Keystore passwords not set (use environment variables)"
            )
        }

        return SigningValidation(
            isValid = true,
            config = SigningConfig(
                keystorePath = keystorePath,
                keyAlias = androidConfig.keyAlias,
                keyPassword = keyPassword,
                storePassword = storePassword
            )
        )
    }
}
