package com.mobilectl.builder.android

import com.mobilectl.builder.android.signing.SigningOrchestrator
import java.io.File

/**
 * Validates artifacts against destination requirements
 *
 * Rules:
 * - Firebase: Accepts unsigned APK/AAB (warns but allows)
 * - Play Console: Requires signed AAB only
 * - Local: Accepts anything
 */
class ArtifactValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val canDeploy: Boolean,
        val warnings: List<String> = emptyList(),
        val errors: List<String> = emptyList(),
        val allowedDestinations: List<String> = emptyList()
    )

    /**
     * Validate artifacts against all destination requirements
     */
    fun validate(
        artifacts: Map<ArtifactStrategy.ArtifactType, ArtifactInfo>,
        requirements: List<ArtifactStrategy.ArtifactRequirement>
    ): ValidationResult {
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val allowedDestinations = mutableListOf<String>()

        for (requirement in requirements) {
            val artifact = artifacts[requirement.type]

            if (artifact == null) {
                errors.add("${requirement.destination} requires ${requirement.type} but it wasn't built")
                continue
            }

            // Check signing requirement
            if (requirement.mustBeSigned && !artifact.isSigned) {
                errors.add("${requirement.destination} requires signed ${requirement.type}")
                continue
            }

            // Warn about unsigned (but allow for Firebase/Local)
            if (!requirement.mustBeSigned && !artifact.isSigned) {
                warnings.add("${requirement.destination} will receive unsigned ${requirement.type}")
            }

            allowedDestinations.add(requirement.destination)
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            canDeploy = allowedDestinations.isNotEmpty(),
            warnings = warnings,
            errors = errors,
            allowedDestinations = allowedDestinations
        )
    }

    /**
     * Check if user confirmation is needed
     */
    fun needsUserConfirmation(validation: ValidationResult): Boolean {
        // Need confirmation if:
        // 1. Has errors but can still deploy somewhere (partial deployment)
        // 2. Has warnings (unsigned artifacts)
        return (validation.errors.isNotEmpty() && validation.canDeploy) ||
                validation.warnings.isNotEmpty()
    }

    /**
     * Generate user-friendly validation message
     */
    fun formatValidationMessage(validation: ValidationResult): String {
        val lines = mutableListOf<String>()

        if (validation.errors.isNotEmpty()) {
            lines.add("⚠️  Deployment Issues:")
            validation.errors.forEach { lines.add("   ❌ $it") }
            lines.add("")
        }

        if (validation.warnings.isNotEmpty()) {
            lines.add("⚠️  Warnings:")
            validation.warnings.forEach { lines.add("   ⚠️  $it") }
            lines.add("")
        }

        if (validation.canDeploy) {
            lines.add("✅ Can deploy to:")
            validation.allowedDestinations.forEach { lines.add("   • $it") }
        } else {
            lines.add("❌ Cannot deploy to any destination")
        }

        return lines.joinToString("\n")
    }
}

/**
 * Information about a built artifact
 */
data class ArtifactInfo(
    val type: ArtifactStrategy.ArtifactType,
    val path: String,
    val isSigned: Boolean,
    val sizeBytes: Long
) {
    val sizeMB: Double
        get() = sizeBytes / (1024.0 * 1024.0)
}
