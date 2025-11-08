package com.mobilectl.builder.android.signing

import com.mobilectl.util.PremiumLogger
import com.mobilectl.util.ProcessExecutor

/**
 * JVM implementation of AAB signer
 *
 * Uses jarsigner (comes with JDK)
 */
class JvmAabSigner(
    private val processExecutor: ProcessExecutor
) : ArtifactSigner {

    override suspend fun sign(
        artifactPath: String,
        signingConfig: SigningConfig,
        baseDir: String
    ): SigningOrchestrator.SigningResult {
        return try {
            print("Signing AAB: $artifactPath\n")
            // jarsigner signs in-place
            val result = processExecutor.executeWithProgress(
                command = "jarsigner",
                args = listOf(
                    "-verbose",
                    "-sigalg", "SHA256withRSA",
                    "-digestalg", "SHA-256",
                    "-keystore", signingConfig.keystorePath,
                    "-storepass", signingConfig.storePassword,
                    "-keypass", signingConfig.keyPassword,
                    artifactPath,
                    signingConfig.keyAlias
                ),
                workingDir = baseDir,
                onProgress = { print("\r[BUILD] 🔐 Signing AAB...") }
            )

            print("\r" + " ".repeat(80) + "\r")

            if (result.success) {
                // Verify signature
                val verified = verifyAabSignature(artifactPath)
                if (verified) {
                    SigningOrchestrator.SigningResult(
                        success = true,
                        isSigned = true
                    )
                } else {
                    SigningOrchestrator.SigningResult(
                        success = false,
                        isSigned = false,
                        error = "AAB signature verification failed"
                    )
                }
            } else {
                SigningOrchestrator.SigningResult(
                    success = false,
                    isSigned = false,
                    error = "AAB signing failed"
                )
            }
        } catch (e: Exception) {
            PremiumLogger.error("AAB signing error: ${e.message}")
            SigningOrchestrator.SigningResult(
                success = false,
                isSigned = false,
                error = e.message ?: "Unknown AAB signing error"
            )
        }
    }

    private suspend fun verifyAabSignature(aabPath: String): Boolean {
        return try {
            val result = processExecutor.execute(
                command = "jarsigner",
                args = listOf("-verify", "-verbose", "-certs", aabPath)
            )
            result.stdout.contains("jar verified")
        } catch (e: Exception) {
            false
        }
    }
}
