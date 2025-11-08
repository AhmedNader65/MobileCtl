package com.mobilectl.builder.android.signing

import com.mobilectl.util.PremiumLogger
import com.mobilectl.util.ProcessExecutor

/**
 * Signs APK files using apksigner
 */
class JvmApkSigner(
    private val processExecutor: ProcessExecutor,
    private val sdkFinder: AndroidSdkFinder
) : ArtifactSigner {

    override suspend fun sign(
        artifactPath: String,
        signingConfig: SigningConfig,
        baseDir: String
    ): SigningOrchestrator.SigningResult {
        // Find apksigner
        val apksignerPath = sdkFinder.findApksigner()
        if (apksignerPath == null) {
            return SigningOrchestrator.SigningResult(
                success = false,
                isSigned = false,
                error = "apksigner not found in Android SDK"
            )
        }

        val signedApkPath = artifactPath.replace("-unsigned.apk", ".apk")

        return try {
            val result = processExecutor.executeWithProgress(
                command = apksignerPath,
                args = listOf(
                    "sign",
                    "--ks", signingConfig.keystorePath,
                    "--ks-key-alias", signingConfig.keyAlias,
                    "--ks-pass", "pass:${signingConfig.storePassword}",
                    "--key-pass", "pass:${signingConfig.keyPassword}",
                    "--out", signedApkPath,
                    artifactPath
                ),
                workingDir = baseDir,
                onProgress = { print("\r[BUILD] 🔐 Signing APK...") }
            )

            print("\r" + " ".repeat(80) + "\r")

            if (result.success) {
                SigningOrchestrator.SigningResult(
                    success = true,
                    isSigned = true
                )
            } else {
                SigningOrchestrator.SigningResult(
                    success = false,
                    isSigned = false,
                    error = "APK signing failed"
                )
            }
        } catch (e: Exception) {
            PremiumLogger.error("Signing error: ${e.message}")
            SigningOrchestrator.SigningResult(
                success = false,
                isSigned = false,
                error = e.message ?: "Unknown signing error"
            )
        }
    }
}
