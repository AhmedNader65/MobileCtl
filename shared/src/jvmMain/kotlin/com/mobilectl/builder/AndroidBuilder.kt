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

            com.mobilectl.util.PremiumLogger.section("Building Android")
            com.mobilectl.util.PremiumLogger.detail("Variant", "$buildFlavor/$buildType")

            val gradlewPath = findGradleWrapper(baseDir)
                ?: return failedBuild(
                    startTime,
                    "Gradle wrapper not found",
                    "Ensure gradlew or gradlew.bat exists in project root"
                )

            com.mobilectl.util.PremiumLogger.detail("Gradle", gradlewPath)

            val sourceFiles = BuildCache.getAndroidSourceFiles(baseDir)
            val validation = BuildCache.validateCache(baseDir, sourceFiles)

            showCacheValidationResult(validation)

            if (!validation.needsRebuild) {
                val existingArtifact = findSignedApkPath(baseDir, buildFlavor, buildType)
                    ?: findUnsignedApkPath(baseDir, buildFlavor, buildType)

                if (existingArtifact != null) {
                    val duration = System.currentTimeMillis() - startTime
                    return BuildOutput(
                        success = true,
                        platform = Platform.ANDROID,
                        outputPath = existingArtifact,
                        isSigned = !existingArtifact.contains("unsigned"),
                        durationMs = duration
                    )
                }
            }

            val buildSuccess = buildApk(gradlewPath, buildFlavor, buildType, baseDir)
            if (!buildSuccess) {
                return failedBuild(
                    startTime,
                    "Android build failed: assemble${buildFlavor.replaceFirstChar { it.uppercase() }}${buildType.replaceFirstChar { it.uppercase() }} exited with non-zero code",
                    "Run with DEBUG=1 for verbose output. Common causes: Missing dependencies, incorrect Java version, gradle.properties misconfiguration"
                )
            }

            BuildCache.updateCache(baseDir, sourceFiles)
            com.mobilectl.util.PremiumLogger.success("APK built successfully")

            val signResult = attemptSign(baseDir, config, buildFlavor, buildType)
            if (!signResult.success && signResult.isFatal) {
                com.mobilectl.util.PremiumLogger.sectionEnd()
                return failedBuild(startTime, signResult.message)
            }

            if (signResult.isSigned) {
                com.mobilectl.util.PremiumLogger.success("APK signed successfully")
            } else {
                com.mobilectl.util.PremiumLogger.warning(signResult.message)
            }

            com.mobilectl.util.PremiumLogger.sectionEnd()

            if (!signResult.isSigned) {
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

    private fun showCacheValidationResult(validation: BuildCache.ValidationResult) {
        val green = "\u001B[32m"
        val cyan = "\u001B[36m"
        val yellow = "\u001B[33m"
        val gray = "\u001B[90m"
        val white = "\u001B[97m"
        val reset = "\u001B[0m"
        val bold = "\u001B[1m"
        val dim = "\u001B[2m"

        println()
        println("$gray┌─────────────────────────────────────────────────────────┐$reset")

        if (validation.needsRebuild) {
            println("$gray│$reset  ${yellow}⟳$reset  ${bold}${white}Build Cache Validation$reset                            $gray│$reset")
            println("$gray├─────────────────────────────────────────────────────────┤$reset")
            println("$gray│$reset  ${dim}Status$reset          ${yellow}●$reset ${yellow}${validation.reason}$reset")
            println("$gray│$reset  ${dim}Files Checked$reset   ${validation.filesChecked} source files")

            if (validation.cachedHash != null && validation.currentHash != null) {
                println("$gray│$reset  ${dim}Cache Hash$reset      ${gray}${validation.cachedHash.take(12)}...$reset")
                println("$gray│$reset  ${dim}Current Hash$reset    ${gray}${validation.currentHash.take(12)}...$reset")
            }

            println("$gray│$reset  ${dim}Action$reset          Rebuilding from source")
        } else {
            println("$gray│$reset  ${green}✓$reset  ${bold}${white}Build Cache Validation$reset                            $gray│$reset")
            println("$gray├─────────────────────────────────────────────────────────┤$reset")
            println("$gray│$reset  ${dim}Status$reset          ${green}●$reset ${green}${validation.reason}$reset")
            println("$gray│$reset  ${dim}Files Checked$reset   ${validation.filesChecked} source files")
            println("$gray│$reset  ${dim}Hash Match$reset      ${gray}${validation.currentHash?.take(12)}...$reset")
            println("$gray│$reset  ${dim}Action$reset          ${green}Using cached artifact$reset")
        }

        println("$gray└─────────────────────────────────────────────────────────┘$reset")
        println()
    }

    private fun showSigningSetupGuide() {
        val cyan = "\u001B[36m"
        val yellow = "\u001B[33m"
        val gray = "\u001B[90m"
        val white = "\u001B[97m"
        val reset = "\u001B[0m"
        val bold = "\u001B[1m"
        val dim = "\u001B[2m"

        println()
        println("$gray╭─────────────────────────────────────────────────────────╮$reset")
        println("$gray│$reset                                                           $gray│$reset")
        println("$gray│$reset  $yellow⚡$reset ${bold}${white}Enable APK Signing for Production Builds$reset       $gray│$reset")
        println("$gray│$reset                                                           $gray│$reset")
        println("$gray╰─────────────────────────────────────────────────────────╯$reset")
        println()
        println("  ${cyan}►$reset ${bold}Step 1$reset ${dim}— Generate Keystore$reset")
        println("    ${gray}keytool -genkey -v -keystore release.jks -keyalg RSA \\$reset")
        println("    ${gray}  -keysize 2048 -validity 10000 -alias release-key$reset")
        println()
        println("  ${cyan}►$reset ${bold}Step 2$reset ${dim}— Configure mobilectl.yaml$reset")
        println("    ${gray}build:$reset")
        println("    ${gray}  android:$reset")
        println("    ${gray}    keyStore: release.jks$reset")
        println("    ${gray}    keyAlias: release-key$reset")
        println("    ${gray}    keyPassword: \${KEY_PASSWORD}      ${dim}# Use env vars$reset")
        println("    ${gray}    storePassword: \${STORE_PASSWORD}$reset")
        println()
        println("  ${cyan}►$reset ${bold}Step 3$reset ${dim}— Set Environment Variables$reset")
        println("    ${gray}export KEY_PASSWORD=your_secure_password$reset")
        println("    ${gray}export STORE_PASSWORD=your_secure_password$reset")
        println()
        println("  ${dim}${gray}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$reset")
        println("  ℹ️  ${dim}Google Play Console requires signed APKs$reset")
        println("     ${dim}Firebase App Distribution accepts unsigned builds$reset")
        println()
    }

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

            val result = processExecutor.executeWithProgress(
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
                workingDir = baseDir,
                onProgress = { progress ->
                    print("\r[BUILD] 🔐 $progress")
                    System.out.flush()
                }
            )

            print("\r" + " ".repeat(80) + "\r")
            result.success

        } catch (e: Exception) {
            logger.error("Signing error: ${e.message}")
            false
        }
    }

    private suspend fun buildApk(
        gradlewPath: String,
        flavor: String,
        type: String,
        baseDir: String
    ): Boolean {
        return try {
            val variantName = "${flavor.lowercase()}${type.replaceFirstChar { it.uppercase() }}"
            val gradleTask = "assemble${variantName.replaceFirstChar { it.uppercase() }}"

            com.mobilectl.util.PremiumLogger.progress("Running $gradleTask")
            val startTime = System.currentTimeMillis()

            val result = processExecutor.executeWithProgress(
                command = gradlewPath,
                args = listOf(gradleTask),
                workingDir = baseDir,
                onProgress = { progress ->
                    print("\r\u001B[90m│\u001B[0m  \u001B[36m⋯\u001B[0m  \u001B[2m$progress\u001B[0m")
                    System.out.flush()
                }
            )

            print("\r" + " ".repeat(80) + "\r")
            val elapsed = (System.currentTimeMillis() - startTime) / 1000
            com.mobilectl.util.PremiumLogger.detail("Duration", "${elapsed}s", dim = true)

            result.success

        } catch (e: Exception) {
            com.mobilectl.util.PremiumLogger.error("Build execution failed: ${e.message}")
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

    private fun failedBuild(startTime: Long, message: String, tip: String? = null): BuildOutput {
        val duration = System.currentTimeMillis() - startTime
        logger.error(message)
        if (tip != null) {
            logger.info("💡 $tip")
        }
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
