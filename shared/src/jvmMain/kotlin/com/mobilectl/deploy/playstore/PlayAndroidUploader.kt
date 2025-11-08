package com.mobilectl.deploy.playstore

import com.mobilectl.deploy.BaseUploadStrategy
import com.mobilectl.model.deploy.DeployResult
import com.mobilectl.util.ApkAnalyzer
import com.mobilectl.util.PremiumLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Google Play Console uploader
 * SOLID: Single Responsibility - only handles upload orchestration
 */
class PlayAndroidUploader(
    private val playClientProvider: suspend (File, File) -> PlayClient = { serviceAccountFile, aabFile ->
        PlayHttpClient.create(serviceAccountFile, aabFile = aabFile)
    }
) : BaseUploadStrategy() {

    override suspend fun upload(
        artifactFile: File,
        config: Map<String, String>
    ): DeployResult {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()

                // Validate artifact - must be AAB for Play Console
                val validFile = validateFile(artifactFile).getOrNull()
                    ?: return@withContext createFailureResult(
                        validateFile(artifactFile).exceptionOrNull()?.message ?: "Invalid file",
                        validateFile(artifactFile).exceptionOrNull() as Exception?
                            ?: IllegalArgumentException("Invalid file")
                    )

                // AAB-only validation for Play Console
                if (validFile.extension != "aab") {
                    return@withContext createFailureResult(
                        "Google Play Console requires AAB format. Build with bundleRelease task instead of bundleRelease.",
                        IllegalArgumentException("AAB format required")
                    )
                }

                // Get service account path
                val serviceAccountPath = getRequiredConfig(config, "serviceAccount").getOrNull()
                    ?: return@withContext createFailureResult(
                        "Missing 'serviceAccount' config",
                        IllegalArgumentException("Missing 'serviceAccount' config")
                    )

                val serviceAccountFile = File(serviceAccountPath)
                if (!serviceAccountFile.exists()) {
                    return@withContext createFailureResult(
                        "Service account file not found: $serviceAccountPath",
                        IllegalArgumentException("Service account file not found")
                    )
                }

                PremiumLogger.section("Google Play Console")
                PremiumLogger.detail(
                    "File",
                    "${validFile.name} (${validFile.length() / (1024 * 1024)} MB)"
                )

                // Show AAB info
                try {
                    val aabInfo = ApkAnalyzer.getApkInfo(validFile)
                    if (aabInfo != null) {
                        PremiumLogger.detail("Package ID", aabInfo.packageId)
                        PremiumLogger.detail("Version Code", aabInfo.versionCode.toString())
                        PremiumLogger.detail("Version Name", aabInfo.versionName.toString())
                    }
                } catch (e: Exception) {
                    // Ignore if we can't extract AAB info
                }

                // Create Play Console client
                val playClient = playClientProvider(serviceAccountFile, validFile)

                // Extract upload parameters
                val track = config["track"] ?: "internal"  // internal, alpha, beta, production
                val status = config["status"] ?: "draft"  // draft, completed, halted, inProgress
                val releaseNotes = config["releaseNotes"]
                val rolloutPercentage = config["rolloutPercentage"]?.toDoubleOrNull()

                PremiumLogger.detail("Track", track)
                PremiumLogger.detail("Status", status)
                if (rolloutPercentage != null) {
                    PremiumLogger.detail("Rollout", "$rolloutPercentage%")
                }

                // Execute Play Console edit session workflow
                PremiumLogger.detail("Status", "Creating edit session...")
                val editId = playClient.createEdit()

                PremiumLogger.detail("Status", "Uploading bundle...")
                val versionCode = playClient.uploadBundle(editId, validFile)

                PremiumLogger.detail("Status", "Assigning to track...")
                playClient.assignToTrack(
                    editId = editId,
                    track = track,
                    versionCode = versionCode,
                    releaseNotes = releaseNotes,
                    rolloutPercentage = rolloutPercentage,
                    status = status
                )

                PremiumLogger.detail("Status", "Committing changes...")
                playClient.commitEdit(editId)

                val duration = System.currentTimeMillis() - startTime

                // Return success result
                PremiumLogger.success("Upload complete")
                PremiumLogger.detail("Release", "Version $versionCode published to $track")
                PremiumLogger.sectionEnd()

                return@withContext DeployResult(
                    success = true,
                    platform = "android",
                    destination = "playstore",
                    message = "Successfully published version $versionCode to $track track",
                    buildId = versionCode.toString(),
                    buildUrl = "https://play.google.com/console",  // Could be made more specific
                    duration = duration
                )

            } catch (e: Exception) {
                PremiumLogger.error("Play Store upload failed: ${e.message}")
                return@withContext createFailureResult(e.message ?: "Unknown error", e)
            }
        }
    }

    override fun validateConfig(config: Map<String, String>): List<String> {
        val errors = mutableListOf<String>()

        if (config["serviceAccount"].isNullOrBlank()) {
            errors.add("'serviceAccount' path is required in config")
        }

        val track = config["track"]
        if (track != null && track !in listOf("internal", "alpha", "beta", "production")) {
            errors.add("'track' must be one of: internal, alpha, beta, production")
        }

        val status = config["status"]
        if (status != null && status !in listOf("draft", "completed", "halted", "inProgress")) {
            errors.add("'status' must be one of: draft, completed, halted, inProgress")
        }

        val rollout = config["rolloutPercentage"]?.toDoubleOrNull()
        if (rollout != null && (rollout < 0.0 || rollout > 100.0)) {
            errors.add("'rolloutPercentage' must be between 0 and 100")
        }

        return errors
    }

    private fun createFailureResult(message: String, error: Exception): DeployResult {
        return DeployResult(
            success = false,
            platform = "android",
            error = error,
            destination = "playstore",
            message = message
        )
    }
}