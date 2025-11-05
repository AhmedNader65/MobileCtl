package com.mobilectl.validation

import com.mobilectl.config.Config
import com.mobilectl.model.ValidationError
import com.mobilectl.model.ValidationSeverity


class ChangelogConfigValidator : ComponentValidator {

    override fun validate(config: Config): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        config.changelog.let { changelogConfig ->
            if (changelogConfig.enabled) {
                val format = changelogConfig.format.trim()
                if (format !in listOf("markdown")) {
                    errors.add(ValidationError(
                        field = "changelog.format",
                        message = "Invalid format '$format'",
                        severity = ValidationSeverity.ERROR,
                        suggestion = "Use 'markdown'"
                    ))
                }

                val outputFile = changelogConfig.outputFile.trim()
                if (outputFile.isBlank()) {
                    errors.add(ValidationError(
                        field = "changelog.output_file",
                        message = "Cannot be empty",
                        severity = ValidationSeverity.ERROR,
                        suggestion = "Set to 'CHANGELOG.md'"
                    ))
                }

                if (!outputFile.endsWith(".md")) {
                    errors.add(ValidationError(
                        field = "changelog.output_file",
                        message = "Should be .md file",
                        severity = ValidationSeverity.WARNING,
                        suggestion = "Change to CHANGELOG.md"
                    ))
                }

                if (changelogConfig.commitTypes.isEmpty()) {
                    errors.add(ValidationError(
                        field = "changelog.commit_types",
                        message = "At least one must be defined",
                        severity = ValidationSeverity.ERROR,
                        suggestion = "Add commit types like 'feat', 'fix', 'docs'"
                    ))
                }
            }
        }

        return errors
    }
}