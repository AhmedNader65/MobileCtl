package com.mobilectl.validation

import com.mobilectl.config.Config
import com.mobilectl.model.ValidationSeverity
import com.mobilectl.model.deploy.AndroidDeployConfig
import com.mobilectl.model.deploy.DeployConfig
import com.mobilectl.model.deploy.IosDeployConfig
import org.junit.jupiter.api.Assertions.assertFalse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeployConfigValidatorTest {

    private val validator = DeployConfigValidator()

    @Test
    fun testValidAndroidDeployConfig() {
        val config = Config(
            deploy = DeployConfig(
                enabled = true,
                android = AndroidDeployConfig(
                    enabled = true,
                    destination = "firebase",
                    appId = "com.example.app",
                    token = "firebase-token-123",
                    artifactPath = "build/outputs/apk/release/app-release.apk"
                )
            )
        )

        val errors = validator.validate(config)
        assertEquals(0, errors.size, "Valid Android config should have no errors")
    }

    @Test
    fun testValidIosDeployConfig() {
        val config = Config(
            deploy = DeployConfig(
                enabled = true,
                ios = IosDeployConfig(
                    enabled = true,
                    destination = "testflight",
                    appId = "com.example.app",
                    teamId = "ABC123XYZ",
                    apiKey = "app-store-key",
                    artifactPath = "build/outputs/app.ipa"
                )
            )
        )

        val errors = validator.validate(config)
        assertEquals(0, errors.size, "Valid iOS config should have no errors")
    }

    @Test
    fun testAndroidInvalidDestination() {
        val config = Config(
            deploy = DeployConfig(
                enabled = true,
                android = AndroidDeployConfig(
                    enabled = true,
                    destination = "invalid-service",
                    appId = "com.example.app",
                    token = "token",
                    artifactPath = "app.apk"
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.field == "deploy.android.destination" && it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun testIosInvalidDestination() {
        val config = Config(
            deploy = DeployConfig(
                enabled = true,
                ios = IosDeployConfig(
                    enabled = true,
                    destination = "invalid-service",
                    appId = "com.example.app",
                    teamId = "ABC123",
                    apiKey = "key",
                    artifactPath = "app.ipa"
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.field == "deploy.ios.destination" && it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun testFirebaseAndroidMissingToken() {
        val config = Config(
            deploy = DeployConfig(
                enabled = true,
                android = AndroidDeployConfig(
                    enabled = true,
                    destination = "firebase",
                    appId = "com.example.app",
                    token = "",  // Empty!
                    artifactPath = "app.apk"
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.field == "deploy.android.token" && it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun testTestFlightMissingApiKey() {
        val config = Config(
            deploy = DeployConfig(
                enabled = true,
                ios = IosDeployConfig(
                    enabled = true,
                    destination = "testflight",
                    appId = "com.example.app",
                    teamId = "ABC123",
                    apiKey = "",  // Empty!
                    artifactPath = "app.ipa"
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.field == "deploy.ios.apiKey" && it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun testLocalAndroidNeedsNoToken() {
        val config = Config(
            deploy = DeployConfig(
                enabled = true,
                android = AndroidDeployConfig(
                    enabled = true,
                    destination = "local",
                    appId = "com.example.app",
                    token = "",  // Empty, but OK for local
                    artifactPath = "app.apk"
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(
            errors.none { it.field == "deploy.android.token" },
            "Local upload should not require token"
        )
    }

    @Test
    fun testLocalIosNeedsNoApiKey() {
        val config = Config(
            deploy = DeployConfig(
                enabled = true,
                ios = IosDeployConfig(
                    enabled = true,
                    destination = "local",
                    appId = "com.example.app",
                    teamId = "",
                    apiKey = "",  // Empty, but OK for local
                    artifactPath = "app.ipa"
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(
            errors.none { it.field == "deploy.ios.apiKey" },
            "Local upload should not require API key"
        )
    }

    @Test
    fun testAndroidEmptyArtifactPath() {
        val config = Config(
            deploy = DeployConfig(
                enabled = true,
                android = AndroidDeployConfig(
                    enabled = true,
                    destination = "firebase",
                    appId = "com.example.app",
                    token = "token",
                    artifactPath = ""  // Empty!
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.field == "deploy.android.artifactPath" && it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun testIosEmptyArtifactPath() {
        val config = Config(
            deploy = DeployConfig(
                enabled = true,
                ios = IosDeployConfig(
                    enabled = true,
                    destination = "testflight",
                    appId = "com.example.app",
                    teamId = "ABC123",
                    apiKey = "key",
                    artifactPath = ""  // Empty!
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.field == "deploy.ios.artifactPath" && it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun testBothPlatformsEnabled() {
        val config = Config(
            deploy = DeployConfig(
                enabled = true,
                android = AndroidDeployConfig(
                    enabled = true,
                    destination = "firebase",
                    appId = "com.example.app",
                    token = "token",
                    artifactPath = "app.apk"
                ),
                ios = IosDeployConfig(
                    enabled = true,
                    destination = "testflight",
                    appId = "com.example.app",
                    teamId = "ABC123",
                    apiKey = "key",
                    artifactPath = "app.ipa"
                )
            )
        )

        val errors = validator.validate(config)
        assertEquals(0, errors.size, "Both platforms valid should have no errors")
    }

    @Test
    fun testAndroidDisabled() {
        val config = Config(
            deploy = DeployConfig(
                enabled = true,
                android = AndroidDeployConfig(
                    enabled = false,  // Disabled
                    destination = "invalid",
                    appId = "",
                    token = "",
                    artifactPath = ""
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(
            errors.isEmpty(),
            "Disabled Android should not be validated"
        )
    }

    @Test
    fun testIosDisabled() {
        val config = Config(
            deploy = DeployConfig(
                enabled = true,
                ios = IosDeployConfig(
                    enabled = false,  // Disabled
                    destination = "invalid",
                    appId = "",
                    teamId = "",
                    apiKey = "",
                    artifactPath = ""
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(
            errors.isEmpty(),
            "Disabled iOS should not be validated"
        )
    }

    @Test
    fun testDeployConfigNull() {
        val config = Config()

        val errors = validator.validate(config)
        assertEquals(0, errors.size, "empty deploy config should have no errors")
    }

    @Test
    fun testErrorHasSuggestion() {
        val config = Config(
            deploy = DeployConfig(
                enabled = true,
                android = AndroidDeployConfig(
                    enabled = true,
                    destination = "firebase",
                    appId = "app",
                    token = "",
                    artifactPath = "app.apk"
                )
            )
        )

        val errors = validator.validate(config)
        val tokenError = errors.firstOrNull { it.field == "deploy.android.token" }

        assertTrue(tokenError?.suggestion != null, "Error should have suggestion")
        assertTrue(tokenError!!.suggestion!!.contains("FIREBASE_TOKEN"))
    }

    @Test
    fun testMultipleErrors() {
        val config = Config(
            deploy = DeployConfig(
                enabled = true,
                android = AndroidDeployConfig(
                    enabled = true,
                    destination = "invalid",
                    appId = "app",
                    token = "",
                    artifactPath = ""
                )
            )
        )

        val errors = validator.validate(config)
        assertEquals(2, errors.count { it.severity == ValidationSeverity.ERROR })
        print(errors)
        assertTrue(errors.any { it.field == "deploy.android.destination" })
        assertTrue(errors.any { it.field == "deploy.android.artifactPath" })

        assertFalse(errors.any { it.field == "deploy.android.token" })
    }
}
