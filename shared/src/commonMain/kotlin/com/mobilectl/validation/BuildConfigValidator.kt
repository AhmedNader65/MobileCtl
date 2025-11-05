package com.mobilectl.validation

import com.mobilectl.config.Config
import com.mobilectl.detector.ProjectDetector
import com.mobilectl.model.ValidationError
import com.mobilectl.model.ValidationSeverity

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