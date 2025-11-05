package com.mobilectl.validation

import com.mobilectl.config.Config
import com.mobilectl.detector.ProjectDetector
import com.mobilectl.model.ValidationError
import com.mobilectl.model.ValidationSeverity


interface ComponentValidator {
    fun validate(config: Config): List<ValidationError>
}

class BuildConfigValidator(
    private val projectDetector: ProjectDetector
) : ComponentValidator {

    override fun validate(config: Config): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        config.build.let { buildConfig ->
            val androidEnabled = buildConfig.android.enabled
                ?: projectDetector.isAndroidProject()
            val iosEnabled = buildConfig.ios.enabled
                ?: projectDetector.isIosProject()

            if (androidEnabled) {
                val defaultType = buildConfig.android.defaultType.trim()
                if (defaultType.isBlank()) {
                    errors.add(ValidationError(
                        field = "build.android.default_type",
                        message = "Cannot be empty",
                        severity = ValidationSeverity.ERROR,
                        suggestion = "Set to 'debug' or 'release'"
                    ))
                }
            }

            if (iosEnabled) {
                val scheme = buildConfig.ios.scheme.trim()
                if (scheme.isBlank()) {
                    errors.add(ValidationError(
                        field = "build.ios.scheme",
                        message = "Cannot be empty",
                        severity = ValidationSeverity.ERROR,
                        suggestion = "Set the iOS scheme name"
                    ))
                }
            }
        }

        return errors
    }
}

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

class VersionConfigValidator : ComponentValidator {

    override fun validate(config: Config): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        config.version?.let { versionConfig ->
            if (versionConfig.enabled) {
                if (versionConfig.filesToUpdate.isEmpty()) {
                    errors.add(ValidationError(
                        field = "version.filesToUpdate",
                        message = "At least one file must be specified",
                        severity = ValidationSeverity.ERROR,
                        suggestion = "Add files like 'pubspec.yaml', 'package.json', 'build.gradle'"
                    ))
                }

                if (versionConfig.bumpStrategy !in listOf("semver", "manual")) {
                    errors.add(ValidationError(
                        field = "version.bumpStrategy",
                        message = "Invalid strategy '${versionConfig.bumpStrategy}'",
                        severity = ValidationSeverity.ERROR,
                        suggestion = "Use 'semver' or 'manual'"
                    ))
                }

                val versionPattern = """^\d+\.\d+\.\d+(-[a-zA-Z0-9.]+)?(\+[a-zA-Z0-9.]+)?$""".toRegex()
                if (!versionPattern.matches(versionConfig.current)) {
                    errors.add(ValidationError(
                        field = "version.current",
                        message = "Invalid version format '${versionConfig.current}'",
                        severity = ValidationSeverity.WARNING,
                        suggestion = "Use semantic versioning like '1.0.0'"
                    ))
                }
            }
        }

        return errors
    }
}