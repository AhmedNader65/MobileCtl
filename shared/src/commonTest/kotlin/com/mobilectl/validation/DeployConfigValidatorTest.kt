package com.mobilectl.validation

import com.mobilectl.config.Config
import com.mobilectl.model.ValidationSeverity
import com.mobilectl.model.deploy.*
import com.mobilectl.validation.ValidationTestHelpers.createTempApk
import com.mobilectl.validation.ValidationTestHelpers.createTempAab
import com.mobilectl.validation.ValidationTestHelpers.createTempAppStoreApiKey
import com.mobilectl.validation.ValidationTestHelpers.createTempFirebaseServiceAccount
import com.mobilectl.validation.ValidationTestHelpers.createTempIpa
import com.mobilectl.validation.ValidationTestHelpers.createTempPlayConsoleServiceAccount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeployConfigValidatorTest {


    private fun createValidator(): DeployConfigValidator {
        return DeployConfigValidator()
    }

    /**
     * Test valid Firebase Android config
     */
    @Test
    fun testValidFirebaseAndroidConfig() {
        val validator = createValidator()
        val apk = createTempApk()
        val firebaseServiceAccount = createTempFirebaseServiceAccount()

        try {
            val config = Config(
                deploy = DeployConfig(
                    android = AndroidDeployConfig(
                        enabled = true,
                        artifactPath = apk.absolutePath,
                        firebase = FirebaseAndroidDestination(
                            enabled = true,
                            serviceAccount = firebaseServiceAccount.absolutePath,
                            testGroups = listOf("qa-team")
                        ),
                        playConsole = PlayConsoleAndroidDestination(enabled = false),
                        local = LocalAndroidDestination(enabled = false)
                    )
                )
            )

            val errors = validator.validate(config)
            assertEquals(0, errors.size, "Valid Firebase config should have no errors. Errors: ${errors.map { "${it.field}: ${it.message}" }}")
        } finally {
            apk.delete()
            firebaseServiceAccount.delete()
        }
    }

    /**
     * Test valid Play Console Android config
     */
    @Test
    fun testValidPlayConsoleAndroidConfig() {
        val validator = createValidator()
        val aab = createTempAab()
        val playConsoleServiceAccount = createTempPlayConsoleServiceAccount()

        try {
            val config = Config(
                deploy = DeployConfig(
                    android = AndroidDeployConfig(
                        enabled = true,
                        artifactPath = aab.absolutePath,
                        firebase = FirebaseAndroidDestination(enabled = false),
                        playConsole = PlayConsoleAndroidDestination(
                            enabled = true,
                            serviceAccount = playConsoleServiceAccount.absolutePath,
                            packageName = "com.example.app"
                        ),
                        local = LocalAndroidDestination(enabled = false)
                    )
                )
            )

            val errors = validator.validate(config)
            assertEquals(0, errors.size, "Valid Play Console config should have no errors")
        } finally {
            aab.delete()
            playConsoleServiceAccount.delete()
        }
    }

    /**
     * Test valid Local Android config
     */
    @Test
    fun testValidLocalAndroidConfig() {
        val validator = createValidator()
        val apk = createTempApk()

        try {
            val config = Config(
                deploy = DeployConfig(
                    android = AndroidDeployConfig(
                        enabled = true,
                        artifactPath = apk.absolutePath,
                        firebase = FirebaseAndroidDestination(enabled = false),
                        playConsole = PlayConsoleAndroidDestination(enabled = false),
                        local = LocalAndroidDestination(
                            enabled = true,
                            outputDir = "build/deploy"
                        )
                    )
                )
            )

            val errors = validator.validate(config)
            assertEquals(0, errors.size, "Valid Local config should have no errors")
        } finally {
            apk.delete()
        }
    }

    /**
     * Test valid TestFlight iOS config
     */
    @Test
    fun testValidTestFlightIosConfig() {
        val validator = createValidator()
        val ipa = createTempIpa()
        val apiKey = createTempAppStoreApiKey()

        try {
            val config = Config(
                deploy = DeployConfig(
                    ios = IosDeployConfig(
                        enabled = true,
                        artifactPath = ipa.absolutePath,
                        testflight = TestFlightDestination(
                            enabled = true,
                            apiKeyPath = apiKey.absolutePath,
                            bundleId = "com.example.app",
                            teamId = "ABC123XYZ"
                        ),
                        appStore = AppStoreDestination(enabled = false)
                    )
                )
            )

            val errors = validator.validate(config)
            assertEquals(0, errors.size, "Valid TestFlight config should have no errors")
        } finally {
            ipa.delete()
            apiKey.delete()
        }
    }

    /**
     * Test valid App Store iOS config
     */
    @Test
    fun testValidAppStoreIosConfig() {
        val validator = createValidator()
        val ipa = createTempIpa()
        val apiKey = createTempAppStoreApiKey()

        try {
            val config = Config(
                deploy = DeployConfig(
                    ios = IosDeployConfig(
                        enabled = true,
                        artifactPath = ipa.absolutePath,
                        testflight = TestFlightDestination(enabled = false),
                        appStore = AppStoreDestination(
                            enabled = true,
                            apiKeyPath = apiKey.absolutePath,
                            bundleId = "com.example.app",
                            teamId = "ABC123XYZ"
                        )
                    )
                )
            )

            val errors = validator.validate(config)
            assertEquals(0, errors.size, "Valid App Store config should have no errors")
        } finally {
            ipa.delete()
            apiKey.delete()
        }
    }

    /**
     * Test Firebase missing service account
     */
    @Test
    fun testFirebaseAndroidMissingServiceAccount() {
        val validator = createValidator()
        val apk = createTempApk()

        try {
            val config = Config(
                deploy = DeployConfig(
                    android = AndroidDeployConfig(
                        enabled = true,
                        artifactPath = apk.absolutePath,
                        firebase = FirebaseAndroidDestination(
                            enabled = true,
                            serviceAccount = ""  // Empty!
                        ),
                        playConsole = PlayConsoleAndroidDestination(enabled = false),
                        local = LocalAndroidDestination(enabled = false)
                    )
                )
            )

            val errors = validator.validate(config)
            assertTrue(
                errors.any { it.field == "deploy.android.firebase.serviceAccount" && it.severity == ValidationSeverity.ERROR },
                "Should error on missing service account. Errors: ${errors.map { "${it.field}: ${it.message}" }}"
            )
        } finally {
            apk.delete()
        }
    }

    /**
     * Test both platforms enabled
     */
    @Test
    fun testBothPlatformsEnabled() {
        val validator = createValidator()
        val apk = createTempApk()
        val ipa = createTempIpa()
        val firebaseServiceAccount = createTempFirebaseServiceAccount()
        val apiKey = createTempAppStoreApiKey()

        try {
            val config = Config(
                deploy = DeployConfig(
                    android = AndroidDeployConfig(
                        enabled = true,
                        artifactPath = apk.absolutePath,
                        firebase = FirebaseAndroidDestination(
                            enabled = true,
                            serviceAccount = firebaseServiceAccount.absolutePath
                        ),
                        playConsole = PlayConsoleAndroidDestination(enabled = false),
                        local = LocalAndroidDestination(enabled = false)
                    ),
                    ios = IosDeployConfig(
                        enabled = true,
                        artifactPath = ipa.absolutePath,
                        testflight = TestFlightDestination(
                            enabled = true,
                            apiKeyPath = apiKey.absolutePath,
                            bundleId = "com.example.app",
                            teamId = "ABC123"
                        ),
                        appStore = AppStoreDestination(enabled = false)
                    )
                )
            )

            val errors = validator.validate(config)
            assertEquals(0, errors.size, "Both platforms valid should have no errors. Errors: ${errors.map { "${it.field}: ${it.message}" }}")
        } finally {
            apk.delete()
            ipa.delete()
            firebaseServiceAccount.delete()
            apiKey.delete()
        }
    }

    /**
     * Test error has suggestion
     */
    @Test
    fun testErrorHasSuggestion() {
        val validator = createValidator()
        val apk = createTempApk()

        try {
            val config = Config(
                deploy = DeployConfig(
                    android = AndroidDeployConfig(
                        enabled = true,
                        artifactPath = apk.absolutePath,
                        firebase = FirebaseAndroidDestination(
                            enabled = true,
                            serviceAccount = ""  // Missing
                        ),
                        playConsole = PlayConsoleAndroidDestination(enabled = false),
                        local = LocalAndroidDestination(enabled = false)
                    )
                )
            )

            val errors = validator.validate(config)
            val error = errors.firstOrNull { it.field == "deploy.android.firebase.serviceAccount" }

            assertTrue(error?.suggestion != null, "Error should have suggestion")
            assertTrue(error?.suggestion!!.contains("Firebase"), "Suggestion should mention Firebase")
        } finally {
            apk.delete()
        }
    }

    /**
     * Test multiple destinations with errors
     */
    @Test
    fun testMultipleDestinationsWithErrors() {
        val validator = createValidator()
        val apk = createTempApk()

        try {
            val config = Config(
                deploy = DeployConfig(
                    android = AndroidDeployConfig(
                        enabled = true,
                        artifactPath = apk.absolutePath,
                        firebase = FirebaseAndroidDestination(
                            enabled = true,
                            serviceAccount = ""  // Error
                        ),
                        playConsole = PlayConsoleAndroidDestination(
                            enabled = true,
                            serviceAccount = "creds.json",
                            packageName = ""  // Error
                        ),
                        local = LocalAndroidDestination(enabled = false)
                    )
                )
            )

            val errors = validator.validate(config)
            assertTrue(errors.any { it.field == "deploy.android.firebase.serviceAccount" })
            assertTrue(errors.any { it.field == "deploy.android.playConsole.packageName" })
        } finally {
            apk.delete()
        }
    }

    /**
     * Test valid multiple test groups
     */
    @Test
    fun testValidMultipleTestGroups() {
        val validator = createValidator()
        val apk = createTempApk()
        val firebaseServiceAccount = createTempFirebaseServiceAccount()

        try {
            val config = Config(
                deploy = DeployConfig(
                    android = AndroidDeployConfig(
                        enabled = true,
                        artifactPath = apk.absolutePath,
                        firebase = FirebaseAndroidDestination(
                            enabled = true,
                            serviceAccount = firebaseServiceAccount.absolutePath,  // ✅ Add this!
                            testGroups = listOf("qa-team", "beta-testers", "internal")
                        ),
                        playConsole = PlayConsoleAndroidDestination(enabled = false),
                        local = LocalAndroidDestination(enabled = false)
                    )
                )
            )

            val errors = validator.validate(config)
            assertEquals(0, errors.size, "Multiple test groups should be valid. Errors: ${errors.map { "${it.field}: ${it.message}" }}")
        } finally {
            apk.delete()
            firebaseServiceAccount.delete()
        }
    }

}
