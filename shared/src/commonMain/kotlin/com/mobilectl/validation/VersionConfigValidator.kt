package com.mobilectl.validation

import com.mobilectl.config.Config
import com.mobilectl.model.ValidationError
import com.mobilectl.model.ValidationSeverity

class VersionConfigValidator : ComponentValidator {

    override fun validate(config: Config): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        config.version?.let { versionConfig ->
            if (versionConfig.enabled) {
                if (versionConfig.bumpStrategy !in listOf("patch", "minor", "major", "auto", "manual")) {
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