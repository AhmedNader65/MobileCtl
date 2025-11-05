package com.mobilectl.validation

import com.mobilectl.config.Config
import com.mobilectl.model.ValidationError
import com.mobilectl.model.ValidationSeverity

class DeployConfigValidator : ComponentValidator {

    override fun validate(config: Config): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        config.deploy.let { deployConfig ->
            // Android validation
            deployConfig.android?.let { android ->
                if (android.enabled) {
                    if (android.destination !in listOf("firebase", "google-play", "local")) {
                        errors.add(ValidationError(
                            field = "deploy.android.destination",
                            message = "Invalid destination '${android.destination}'",
                            severity = ValidationSeverity.ERROR,
                            suggestion = "Use 'firebase', 'google-play', or 'local'"
                        ))
                    }

                    if (android.destination == "firebase" && android.token.isBlank()) {
                        errors.add(ValidationError(
                            field = "deploy.android.token",
                            message = "Firebase token required",
                            severity = ValidationSeverity.ERROR,
                            suggestion = "Set FIREBASE_TOKEN environment variable"
                        ))
                    }

                    if (android.artifactPath.isBlank()) {
                        errors.add(ValidationError(
                            field = "deploy.android.artifactPath",
                            message = "Artifact path cannot be empty",
                            severity = ValidationSeverity.ERROR,
                            suggestion = "Set path to APK or AAB file"
                        ))
                    }
                }
            }

            // iOS validation
            deployConfig.ios?.let { ios ->
                if (ios.enabled) {
                    if (ios.destination !in listOf("testflight", "app-store", "local")) {
                        errors.add(ValidationError(
                            field = "deploy.ios.destination",
                            message = "Invalid destination '${ios.destination}'",
                            severity = ValidationSeverity.ERROR,
                            suggestion = "Use 'testflight', 'app-store', or 'local'"
                        ))
                    }

                    if (ios.destination == "testflight" && ios.apiKey.isBlank()) {
                        errors.add(ValidationError(
                            field = "deploy.ios.apiKey",
                            message = "App Store Connect API key required",
                            severity = ValidationSeverity.ERROR,
                            suggestion = "Set APP_STORE_KEY environment variable"
                        ))
                    }

                    if (ios.artifactPath.isBlank()) {
                        errors.add(ValidationError(
                            field = "deploy.ios.artifactPath",
                            message = "Artifact path cannot be empty",
                            severity = ValidationSeverity.ERROR,
                            suggestion = "Set path to IPA file"
                        ))
                    }
                }
            }
        }

        return errors
    }
}
