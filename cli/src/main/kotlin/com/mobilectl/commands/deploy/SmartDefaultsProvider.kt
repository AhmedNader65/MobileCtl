package com.mobilectl.commands.deploy

import com.mobilectl.config.Config
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.deploy.AndroidDeployConfig
import com.mobilectl.model.deploy.AppStoreDestination
import com.mobilectl.model.deploy.DeployConfig
import com.mobilectl.model.deploy.FirebaseAndroidDestination
import com.mobilectl.model.deploy.IosDeployConfig
import com.mobilectl.model.deploy.LocalAndroidDestination
import com.mobilectl.model.deploy.PlayConsoleAndroidDestination
import com.mobilectl.model.deploy.TestFlightDestination
import com.mobilectl.model.versionManagement.VersionConfig
import com.mobilectl.util.ArtifactDetector
import com.mobilectl.util.ArtifactType
import com.mobilectl.util.PremiumLogger
import java.io.File

/**
 * Provides smart defaults by auto-detecting project configuration.
 * Single Responsibility: Auto-detection logic
 */
class SmartDefaultsProvider(
    private val workingPath: String,
    private val verbose: Boolean
) {
    /**
     * Creates smart defaults by auto-detecting Firebase config, artifacts, etc.
     */
    fun createSmartDefaults(): Config {
        val baseDir = File(workingPath)

        val firebaseConfig = autoDetectFirebaseConfig()
        val androidArtifact = detectAndroidArtifact(baseDir)
        val iosArtifact = detectIosArtifact(baseDir)

        val androidArtifactPath = androidArtifact?.absolutePath
            ?: "build/outputs/apk/release/app-release.apk"

        val iosArtifactPath = iosArtifact?.absolutePath
            ?: "build/outputs/ipa/release/app.ipa"

        val androidConfig = AndroidDeployConfig(
            enabled = true,
            artifactPath = androidArtifactPath,
            firebase = firebaseConfig,
            playConsole = PlayConsoleAndroidDestination(enabled = false),
            local = LocalAndroidDestination(enabled = false)
        )

        val iosConfig = IosDeployConfig(
            enabled = false,
            artifactPath = iosArtifactPath,
            testflight = TestFlightDestination(enabled = false),
            appStore = AppStoreDestination(enabled = false)
        )

        val deployConfig = DeployConfig(
            android = androidConfig,
            ios = iosConfig
        )

        return Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = deployConfig
        )
    }

    /**
     * Detects environment based on git branch.
     */
    fun detectEnvironment(): String {
        return try {
            val process = ProcessBuilder("git", "branch", "--show-current")
                .directory(File(workingPath))
                .redirectErrorStream(true)
                .start()
            val branch = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()

            // If git command failed (e.g., not a git repo), default to "dev"
            if (exitCode != 0) {
                return "dev"
            }

            when {
                branch == "main" || branch == "master" -> "production"
                branch.startsWith("staging") -> "staging"
                else -> "dev"
            }
        } catch (e: Exception) {
            "dev"
        }
    }

    private fun detectAndroidArtifact(baseDir: File): File? {
        val artifact = ArtifactDetector.resolveArtifact(
            path = null,
            artifactType = ArtifactType.APK,
            baseDir = baseDir
        )

        if (artifact != null && ArtifactDetector.isDebugBuild(artifact)) {
            PremiumLogger.simpleWarning("Debug APK detected!")
            PremiumLogger.info("This is meant for testing only")
            PremiumLogger.info("For production: ./gradlew bundleRelease")
            println()
        }

        if (verbose && artifact != null) {
            PremiumLogger.simpleSuccess("Auto-detected Android artifact: ${artifact.name}")
        }

        return artifact
    }

    private fun detectIosArtifact(baseDir: File): File? {
        val artifact = ArtifactDetector.resolveArtifact(
            path = null,
            artifactType = ArtifactType.IPA,
            baseDir = baseDir
        )

        if (verbose && artifact != null) {
            PremiumLogger.simpleSuccess("Auto-detected iOS artifact: ${artifact.name}")
        }

        return artifact
    }

    private fun autoDetectFirebaseConfig(): FirebaseAndroidDestination {
        return try {
            val googleServicesFile = findGoogleServicesJson()
            val serviceAccountFile = findServiceAccountJson()

            if (serviceAccountFile == null) {
                if (verbose) {
                    PremiumLogger.info("Firebase: Service account not found (will fail at deploy)")
                }
                return FirebaseAndroidDestination(
                    enabled = true,
                    serviceAccount = "credentials/firebase-service-account.json"
                )
            }

            if (verbose) {
                PremiumLogger.simpleSuccess("Auto-detected Firebase: ${serviceAccountFile.absolutePath}")
                if (googleServicesFile != null) {
                    PremiumLogger.simpleSuccess("Auto-detected google-services.json: ${googleServicesFile.absolutePath}")
                }
            }

            FirebaseAndroidDestination(
                enabled = true,
                serviceAccount = serviceAccountFile.absolutePath,
                googleServices = googleServicesFile?.absolutePath
            )
        } catch (e: Exception) {
            if (verbose) {
                PremiumLogger.simpleWarning("Firebase auto-detection failed: ${e.message}")
            }
            FirebaseAndroidDestination()
        }
    }

    private fun findGoogleServicesJson(): File? {
        val knownPaths = listOf(
            "app/google-services.json",
            "app/src/main/google-services.json",
            "google-services.json"
        )
        return knownPaths
            .map { File(workingPath, it) }
            .firstOrNull { it.exists() }
    }

    private fun findServiceAccountJson(): File? {
        val knownPaths = listOf(
            "credentials/firebase-service-account.json",
            "credentials/firebase-account.json",
            "credentials/account.json",
            "firebase-service-account.json",
            "firebase-account.json"
        )
        return knownPaths
            .map { File(workingPath, it) }
            .firstOrNull { it.exists() }
    }
}
