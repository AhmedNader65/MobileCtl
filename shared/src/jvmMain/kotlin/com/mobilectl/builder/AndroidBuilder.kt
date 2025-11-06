package com.mobilectl.builder

import com.mobilectl.config.Config
import com.mobilectl.model.Platform
import com.mobilectl.util.Logger
import com.mobilectl.util.ProcessExecutor
import com.mobilectl.util.createLogger
import java.io.File

class AndroidBuilder(
    private val processExecutor: ProcessExecutor
) : PlatformBuilder {

    private val logger = createLogger("AndroidBuilder")

    override suspend fun build(baseDir: String, config: Config): BuildOutput {
        val startTime = System.currentTimeMillis()

        return try {
            val buildFlavor = config.build.android.defaultFlavor
            val buildType = config.build.android.defaultType

            logger.info("🔨 Building Android: $buildFlavor/$buildType")

            // Step 1: Find gradlew
            val gradlewPath = findGradleWrapper(baseDir)
                ?: return failedBuild(startTime, "gradlew not found")

            logger.info("Using gradlew: $gradlewPath")

            // Step 2: Build APK
            val buildSuccess = buildApk(gradlewPath, buildFlavor, buildType, baseDir)
            if (!buildSuccess) {
                return failedBuild(startTime, "APK build failed")
            }

            logger.info("✅ APK built successfully")

            // Step 3: Sign APK (or warn if skipping)
            val signResult = attemptSign(baseDir, config, buildFlavor, buildType)
            if (!signResult.success && signResult.isFatal) {
                return failedBuild(startTime, signResult.message)
            }

            // Log signing result
            if (signResult.isSigned) {
                logger.info("✅ APK signed successfully")
            } else {
                logger.warn("⚠️  ${signResult.message}")
                showSigningSetupGuide()
            }

            // Step 4: Find artifact (signed or unsigned)
            val outputPath = if (signResult.isSigned) {
                findSignedApkPath(baseDir, buildFlavor, buildType)
            } else {
                findUnsignedApkPath(baseDir, buildFlavor, buildType)
            }

            val duration = System.currentTimeMillis() - startTime
            BuildOutput(
                success = true,
                platform = Platform.ANDROID,
                outputPath = outputPath,
                isSigned = signResult.isSigned,  // ✅ NEW: Track if signed
                durationMs = duration
            )

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error("Build error: ${e.message}", e)

            BuildOutput(
                success = false,
                platform = Platform.ANDROID,
                error = e.message ?: "Unknown error",
                durationMs = duration
            )
        }
    }

    /**
     * Attempt to sign APK
     * Returns result with success, isSigned, and message
     */
    private suspend fun attemptSign(
        baseDir: String,
        config: Config,
        flavor: String,
        type: String
    ): SignResult {
        val keystoreConfig = config.build.android
        val keyStore = keystoreConfig.keyStore
        val keyAlias = keystoreConfig.keyAlias

        // Check if keystore is configured
        if (keyStore.isBlank() || keyAlias.isBlank()) {
            return SignResult(
                success = true,
                isSigned = false,
                isFatal = false,
                message = "Keystore not configured, deploying unsigned APK"
            )
        }

        // Resolve keystore path
        val keystorePath = if (File(keyStore).isAbsolute) {
            keyStore
        } else {
            File(baseDir, keyStore).absolutePath
        }

        if (!File(keystorePath).exists()) {
            return SignResult(
                success = false,
                isSigned = false,
                isFatal = true,
                message = "Keystore file not found: $keystorePath"
            )
        }

        // Get passwords
        val keyPassword = keystoreConfig.keyPassword.ifBlank {
            System.getenv("MOBILECTL_KEY_PASSWORD") ?: ""
        }
        val storePassword = keystoreConfig.storePassword.ifBlank {
            System.getenv("MOBILECTL_STORE_PASSWORD") ?: ""
        }

        if (keyPassword.isBlank() || storePassword.isBlank()) {
            return SignResult(
                success = true,
                isSigned = false,
                isFatal = false,
                message = "Keystore passwords not set, deploying unsigned APK"
            )
        }

        // Find unsigned APK
        val unsignedApk = findUnsignedApkPath(baseDir, flavor, type)
            ?: return SignResult(
                success = false,
                isSigned = false,
                isFatal = true,
                message = "Unsigned APK not found"
            )

        // Sign it
        val signSuccess = signApk(
            unsignedApk,
            keystorePath,
            keyPassword,
            storePassword,
            keyAlias,
            baseDir
        )

        return if (signSuccess) {
            SignResult(
                success = true,
                isSigned = true,
                isFatal = false,
                message = "APK signed successfully"
            )
        } else {
            SignResult(
                success = true,  // Not fatal, can still deploy unsigned
                isSigned = false,
                isFatal = false,
                message = "Signing failed, deploying unsigned APK"
            )
        }
    }

    /**
     * Show setup guide for keystore
     */
    private fun showSigningSetupGuide() {
        logger.warn("")
        logger.warn("═══════════════════════════════════════════════════")
        logger.warn("📋 To enable APK signing, set up your keystore:")
        logger.warn("═══════════════════════════════════════════════════")
        logger.warn("")
        logger.warn("Option 1: Generate new keystore")
        logger.warn("  keytool -genkey -v -keystore keystore.jks \\")
        logger.warn("    -keyalg RSA -keysize 2048 -validity 10000 \\")
        logger.warn("    -alias my-app-key -storepass password1 \\")
        logger.warn("    -keypass password1")
        logger.warn("")
        logger.warn("Option 2: Configure in mobilectl.yaml")
        logger.warn("  build:")
        logger.warn("    android:")
        logger.warn("      keyStore: keystore.jks")
        logger.warn("      keyAlias: my-app-key")
        logger.warn("      keyPassword: password1")
        logger.warn("      storePassword: password1")
        logger.warn("")
        logger.warn("Option 3: Set environment variables")
        logger.warn("  export MOBILECTL_KEY_ALIAS=my-app-key")
        logger.warn("  export MOBILECTL_KEY_PASSWORD=password1")
        logger.warn("  export MOBILECTL_STORE_PASSWORD=password1")
        logger.warn("")
        logger.warn("⚠️  WARNING: Unsigned APKs cannot be uploaded to Google Play Console")
        logger.warn("           They are only for Firebase App Distribution")
        logger.warn("═══════════════════════════════════════════════════")
        logger.warn("")
    }

    /**
     * Sign the APK
     */
    private suspend fun signApk(
        unsignedApkPath: String,
        keystorePath: String,
        keyPassword: String,
        storePassword: String,
        keyAlias: String,
        baseDir: String
    ): Boolean {
        return try {
            val signedApkPath = unsignedApkPath.replace("-unsigned.apk", ".apk")

            val result = processExecutor.execute(
                command = "jarsigner",
                args = listOf(
                    "-verbose",
                    "-sigalg", "SHA256withRSA",
                    "-digestalg", "SHA-256",
                    "-keystore", keystorePath,
                    "-storepass", storePassword,
                    "-keypass", keyPassword,
                    "-signedjar", signedApkPath,
                    unsignedApkPath,
                    keyAlias
                ),
                workingDir = baseDir
            )

            result.success

        } catch (e: Exception) {
            logger.error("Signing error: ${e.message}")
            false
        }
    }

    /**
     * Build APK using Gradle
     */
    private suspend fun buildApk(
        gradlewPath: String,
        flavor: String,
        type: String,
        baseDir: String
    ): Boolean {
        return try {
            val variantName = "${flavor.lowercase()}${type.replaceFirstChar { it.uppercase() }}"
            val gradleTask = "assemble${variantName.replaceFirstChar { it.uppercase() }}"

            logger.info("Running: $gradleTask")

            val result = processExecutor.execute(
                command = gradlewPath,
                args = listOf(gradleTask),
                workingDir = baseDir
            )

            result.success

        } catch (e: Exception) {
            logger.error("Build execution failed: ${e.message}")
            false
        }
    }

    private fun findUnsignedApkPath(baseDir: String, flavor: String, type: String): String? {
        val apkDir = File(baseDir, "build/outputs/apk/$flavor/$type")
        return if (apkDir.exists()) {
            apkDir.walk()
                .filter { it.name.endsWith("-unsigned.apk") }
                .maxByOrNull { it.lastModified() }
                ?.absolutePath
        } else null
    }

    private fun findSignedApkPath(baseDir: String, flavor: String, type: String): String? {
        val apkDir = File(baseDir, "build/outputs/apk/$flavor/$type")
        return if (apkDir.exists()) {
            apkDir.walk()
                .filter { it.name.endsWith(".apk") && !it.name.endsWith("-unsigned.apk") }
                .maxByOrNull { it.lastModified() }
                ?.absolutePath
        } else null
    }

    private fun findGradleWrapper(baseDir: String): String? {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val candidates = if (isWindows) {
            listOf("$baseDir/gradlew.bat", "$baseDir/gradlew")
        } else {
            listOf("$baseDir/gradlew")
        }

        for (candidate in candidates) {
            val file = File(candidate)
            if (file.exists() && file.isFile) {
                logger.debug("Found gradlew: $candidate")
                return candidate
            }
        }
        return null
    }

    private fun failedBuild(startTime: Long, message: String): BuildOutput {
        val duration = System.currentTimeMillis() - startTime
        logger.error("❌ $message")
        return BuildOutput(
            success = false,
            platform = Platform.ANDROID,
            error = message,
            durationMs = duration
        )
    }
}

/**
 * Result of signing attempt
 */
data class SignResult(
    val success: Boolean,      // Operation succeeded
    val isSigned: Boolean,     // APK is signed
    val isFatal: Boolean,      // Should stop build?
    val message: String
)
