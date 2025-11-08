package com.mobilectl.builder.android

import com.mobilectl.config.Config

/**
 * Determines which artifacts (APK/AAB) to build based on deployment destinations
 *
 * Rules:
 * - Firebase: APK or AAB (configurable, unsigned OK)
 * - Play Console: AAB only (must be signed)
 * - Local: APK or AAB (unsigned OK)
 */
class ArtifactStrategy {

    data class ArtifactRequirement(
        val type: ArtifactType,
        val mustBeSigned: Boolean,
        val destination: String
    )

    enum class ArtifactType {
        APK,
        AAB
    }

    /**
     * Determine what artifacts to build based on enabled destinations
     */
    fun determineRequirements(config: Config): List<ArtifactRequirement> {
        val requirements = mutableListOf<ArtifactRequirement>()
        val deployConfig = config.deploy.android ?: return requirements

        // Firebase
        if (deployConfig.firebase.enabled == true) {
            val firebaseType = when (config.build.android.firebaseOutputType?.lowercase()) {
                "aab" -> ArtifactType.AAB
                else -> ArtifactType.APK
            }
            requirements.add(
                ArtifactRequirement(
                    type = firebaseType,
                    mustBeSigned = false,  // Firebase accepts unsigned
                    destination = "Firebase"
                )
            )
        }

        // Play Console (AAB only, must be signed)
        if (deployConfig.playConsole?.enabled == true) {
            requirements.add(
                ArtifactRequirement(
                    type = ArtifactType.AAB,
                    mustBeSigned = true,  // Play Console requires signed
                    destination = "Play Console"
                )
            )
        }

        // Local
        if (deployConfig.local?.enabled == true) {
            val localType = when (config.build.android.outputType.lowercase()) {
                "aab" -> ArtifactType.AAB
                else -> ArtifactType.APK
            }
            requirements.add(
                ArtifactRequirement(
                    type = localType,
                    mustBeSigned = false,  // Local accepts unsigned
                    destination = "Local"
                )
            )
        }

        return requirements
    }

    /**
     * Get unique artifact types to build (no duplicates)
     */
    fun getArtifactTypesToBuild(requirements: List<ArtifactRequirement>): Set<ArtifactType> {
        return requirements.map { it.type }.toSet()
    }

    /**
     * Check if any destination requires signing
     */
    fun requiresSigning(requirements: List<ArtifactRequirement>): Boolean {
        return requirements.any { it.mustBeSigned }
    }
}