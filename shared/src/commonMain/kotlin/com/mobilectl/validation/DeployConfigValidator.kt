package com.mobilectl.validation

import com.mobilectl.config.Config
import com.mobilectl.model.ValidationError
import com.mobilectl.model.ValidationSeverity
import com.mobilectl.model.deploy.AndroidDeployConfig
import com.mobilectl.model.deploy.AppStoreDestination
import com.mobilectl.model.deploy.FirebaseAndroidDestination
import com.mobilectl.model.deploy.IosDeployConfig
import com.mobilectl.model.deploy.LocalAndroidDestination
import com.mobilectl.model.deploy.PlayConsoleAndroidDestination
import com.mobilectl.model.deploy.TestFlightDestination
import java.io.File

class DeployConfigValidator : ComponentValidator {

    override fun validate(config: Config): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        config.deploy.let { deployConfig ->
            // Validate Android
            deployConfig.android?.let { android ->
                errors.addAll(validateAndroid(android))
                print(errors)
            }

            // Validate iOS
            deployConfig.ios?.let { ios ->
                errors.addAll(validateIos(ios))
                print(errors)
            }
        }

        return errors
    }

    /**
     * Validate Android deployment configuration
     */
    private fun validateAndroid(android: AndroidDeployConfig): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (!android.enabled) {
            return errors  // Skip validation if disabled
        }

        // Validate artifact path
        if (android.artifactPath.isBlank()) {
            errors.add(ValidationError(
                field = "deploy.android.artifactPath",
                message = "Artifact path cannot be empty",
                severity = ValidationSeverity.ERROR,
                suggestion = "Set path to APK or AAB file (e.g., 'build/outputs/apk/release/app-release.apk')"
            ))
        } else {
            val artifactFile = File(android.artifactPath)
            if (!artifactFile.exists() && !File(System.getProperty("user.dir"), android.artifactPath).exists()) {
                errors.add(ValidationError(
                    field = "deploy.android.artifactPath",
                    message = "Artifact file not found: ${android.artifactPath}",
                    severity = ValidationSeverity.WARNING,
                    suggestion = "Build the app first or check the path"
                ))
            }
        }

        // Validate Firebase destination
        errors.addAll(validateFirebaseAndroid(android.firebase))

        // Validate Play Console destination
        errors.addAll(validatePlayConsoleAndroid(android.playConsole))

        // Validate Local destination
        errors.addAll(validateLocalAndroid(android.local))

        // Check at least one destination is enabled
        val anyDestinationEnabled = android.firebase.enabled ||
                android.playConsole.enabled ||
                android.local.enabled

        if (!anyDestinationEnabled) {
            errors.add(ValidationError(
                field = "deploy.android",
                message = "No Android deployment destinations are enabled",
                severity = ValidationSeverity.WARNING,
                suggestion = "Enable at least one destination (firebase, play-console, or local)"
            ))
        }

        return errors
    }

    /**
     * Validate Firebase Android destination
     */
    private fun validateFirebaseAndroid(
        firebase: FirebaseAndroidDestination
    ): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (!firebase.enabled) {
            return errors
        }

        // Validate service account
        if (firebase.serviceAccount.isBlank()) {
            errors.add(ValidationError(
                field = "deploy.android.firebase.serviceAccount",
                message = "Firebase service account path is required",
                severity = ValidationSeverity.ERROR,
                suggestion = "Set path to Firebase service account JSON file"
            ))
        } else {
            val serviceAccountFile = File(firebase.serviceAccount)
            val workingDir = File(System.getProperty("user.dir"))
            val resolvedFile = if (serviceAccountFile.isAbsolute) {
                serviceAccountFile
            } else {
                File(workingDir, firebase.serviceAccount)
            }

            if (!resolvedFile.exists()) {
                errors.add(ValidationError(
                    field = "deploy.android.firebase.serviceAccount",
                    message = "Service account file not found: ${firebase.serviceAccount}",
                    severity = ValidationSeverity.ERROR,
                    suggestion = "Download from Firebase Console → Project Settings → Service Accounts"
                ))
            } else if (!resolvedFile.name.endsWith(".json")) {
                errors.add(ValidationError(
                    field = "deploy.android.firebase.serviceAccount",
                    message = "Service account must be a JSON file",
                    severity = ValidationSeverity.ERROR,
                    suggestion = "Use a valid Firebase service account JSON file"
                ))
            }
        }

        // Validate test groups
        if (firebase.testGroups.isEmpty()) {
            errors.add(ValidationError(
                field = "deploy.android.firebase.testGroups",
                message = "At least one test group is required",
                severity = ValidationSeverity.WARNING,
                suggestion = "Add test groups or use default 'qa-team'"
            ))
        }

        return errors
    }

    /**
     * Validate Play Console Android destination
     */
    private fun validatePlayConsoleAndroid(
        playConsole: PlayConsoleAndroidDestination?
    ): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        playConsole ?: return errors

        if (!playConsole.enabled) {
            return errors
        }

        // Validate service account
        if (playConsole.serviceAccount.isBlank()) {
            errors.add(ValidationError(
                field = "deploy.android.playConsole.serviceAccount",
                message = "Play Console service account is required",
                severity = ValidationSeverity.ERROR,
                suggestion = "Set path to Play Console service account JSON"
            ))
        } else {
            val serviceAccountFile = File(playConsole.serviceAccount)
            if (!serviceAccountFile.exists()) {
                errors.add(ValidationError(
                    field = "deploy.android.playConsole.serviceAccount",
                    message = "Service account file not found",
                    severity = ValidationSeverity.ERROR,
                    suggestion = "Download from Google Play Console"
                ))
            }
        }

        // Validate package name
        if (playConsole.packageName.isBlank()) {
            errors.add(ValidationError(
                field = "deploy.android.playConsole.packageName",
                message = "Package name is required",
                severity = ValidationSeverity.ERROR,
                suggestion = "Set your app's package name (e.g., 'com.example.app')"
            ))
        }

        return errors
    }

    /**
     * Validate Local Android destination
     */
    private fun validateLocalAndroid(
        local: LocalAndroidDestination
    ): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (!local.enabled) {
            return errors
        }

        if (local.outputDir.isBlank()) {
            errors.add(ValidationError(
                field = "deploy.android.local.outputDir",
                message = "Output directory cannot be empty",
                severity = ValidationSeverity.ERROR,
                suggestion = "Set output directory path"
            ))
        }

        return errors
    }

    /**
     * Validate iOS deployment configuration
     */
    private fun validateIos(ios: IosDeployConfig): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (!ios.enabled) {
            return errors
        }

        // Validate artifact path
        if (ios.artifactPath.isBlank()) {
            errors.add(ValidationError(
                field = "deploy.ios.artifactPath",
                message = "Artifact path cannot be empty",
                severity = ValidationSeverity.ERROR,
                suggestion = "Set path to IPA file"
            ))
        } else {
            val artifactFile = File(ios.artifactPath)
            if (!artifactFile.exists() && !File(System.getProperty("user.dir"), ios.artifactPath).exists()) {
                errors.add(ValidationError(
                    field = "deploy.ios.artifactPath",
                    message = "IPA file not found: ${ios.artifactPath}",
                    severity = ValidationSeverity.WARNING,
                    suggestion = "Build the app first"
                ))
            }
        }

        // Validate TestFlight destination
        errors.addAll(validateTestFlightIos(ios.testflight))

        // Validate App Store destination
        errors.addAll(validateAppStoreIos(ios.appStore))

        // Check at least one destination is enabled
        val anyDestinationEnabled = ios.testflight.enabled || ios.appStore.enabled

        if (!anyDestinationEnabled) {
            errors.add(ValidationError(
                field = "deploy.ios",
                message = "No iOS deployment destinations are enabled",
                severity = ValidationSeverity.WARNING,
                suggestion = "Enable TestFlight or App Store"
            ))
        }

        return errors
    }

    /**
     * Validate TestFlight iOS destination
     */
    private fun validateTestFlightIos(
        testflight: TestFlightDestination
    ): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (!testflight.enabled) {
            return errors
        }

        // Validate API key
        if (testflight.apiKeyPath.isBlank()) {
            errors.add(ValidationError(
                field = "deploy.ios.testflight.apiKeyPath",
                message = "App Store Connect API key path is required",
                severity = ValidationSeverity.ERROR,
                suggestion = "Set path to API key JSON file"
            ))
        } else {
            val keyFile = File(testflight.apiKeyPath)
            if (!keyFile.exists()) {
                errors.add(ValidationError(
                    field = "deploy.ios.testflight.apiKeyPath",
                    message = "API key file not found",
                    severity = ValidationSeverity.ERROR,
                    suggestion = "Download from App Store Connect"
                ))
            }
        }

        // Validate bundle ID
        if (testflight.bundleId.isBlank()) {
            errors.add(ValidationError(
                field = "deploy.ios.testflight.bundleId",
                message = "Bundle ID is required",
                severity = ValidationSeverity.ERROR,
                suggestion = "Set your app's bundle ID"
            ))
        }

        // Validate team ID
        if (testflight.teamId.isBlank()) {
            errors.add(ValidationError(
                field = "deploy.ios.testflight.teamId",
                message = "Team ID is required",
                severity = ValidationSeverity.ERROR,
                suggestion = "Set your Apple Team ID"
            ))
        }

        return errors
    }

    /**
     * Validate App Store iOS destination
     */
    private fun validateAppStoreIos(
        appStore: AppStoreDestination?
    ): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        appStore ?: return errors

        if (!appStore.enabled) {
            return errors
        }

        // Validate API key
        if (appStore.apiKeyPath.isBlank()) {
            errors.add(ValidationError(
                field = "deploy.ios.appStore.apiKeyPath",
                message = "App Store Connect API key path is required",
                severity = ValidationSeverity.ERROR,
                suggestion = "Download from App Store Connect"
            ))
        }

        // Validate bundle ID
        if (appStore.bundleId.isBlank()) {
            errors.add(ValidationError(
                field = "deploy.ios.appStore.bundleId",
                message = "Bundle ID is required",
                severity = ValidationSeverity.ERROR,
                suggestion = "Set your app's bundle ID"
            ))
        }

        // Validate team ID
        if (appStore.teamId.isBlank()) {
            errors.add(ValidationError(
                field = "deploy.ios.appStore.teamId",
                message = "Team ID is required",
                severity = ValidationSeverity.ERROR,
                suggestion = "Set your Apple Team ID"
            ))
        }

        return errors
    }
}
