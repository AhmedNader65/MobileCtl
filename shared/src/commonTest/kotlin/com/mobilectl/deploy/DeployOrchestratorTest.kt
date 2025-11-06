package com.mobilectl.deploy

import com.mobilectl.model.Platform
import com.mobilectl.model.deploy.*
import com.mobilectl.deploy.DeployTestHelpers.createTempApk
import com.mobilectl.deploy.DeployTestHelpers.createTempIpa
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class DeployOrchestratorTest {

    /**
     * Test Firebase destination validation
     */
    @Test
    fun testAndroidFirebaseValidation() = runBlocking {
        val orchestrator = DeployOrchestrator()
        val apk = createTempApk()

        try {
            val config = AndroidDeployConfig(
                enabled = true,
                artifactPath = apk.absolutePath,
                firebase = FirebaseAndroidDestination(
                    enabled = true,
                    serviceAccount = ""  // Empty - invalid
                ),
                playConsole = PlayConsoleAndroidDestination(enabled = false),
                local = LocalAndroidDestination(enabled = false)
            )

            val result = orchestrator.deployAndroid(config, apk.absolutePath)

            // Should have validation error
            val firebaseResult = result.individual.find { it.destination == "firebase" }
            assertFalse(firebaseResult?.success ?: false)
            assertTrue(firebaseResult?.message?.contains("serviceAccount") ?: false)
        } finally {
            apk.delete()
        }
    }

    /**
     * Test Play Console package name validation
     */
    @Test
    fun testAndroidPlayConsoleValidation() = runBlocking {
        val orchestrator = DeployOrchestrator()
        val apk = createTempApk()

        try {
            val config = AndroidDeployConfig(
                enabled = true,
                artifactPath = apk.absolutePath,
                firebase = FirebaseAndroidDestination(enabled = false),
                playConsole = PlayConsoleAndroidDestination(
                    enabled = true,
                    serviceAccount = "creds.json",
                    packageName = ""  // Empty - invalid
                ),
                local = LocalAndroidDestination(enabled = false)
            )

            val result = orchestrator.deployAndroid(config, apk.absolutePath)

            val playConsoleResult = result.individual.find { it.destination == "play-console" }
            assertFalse(playConsoleResult?.success ?: false)
            assertTrue(playConsoleResult?.message?.contains("packageName") ?: false)
        } finally {
            apk.delete()
        }
    }

    /**
     * Test iOS TestFlight API key validation
     */
    @Test
    fun testIosTestFlightValidation() = runBlocking {
        val orchestrator = DeployOrchestrator()
        val ipa = createTempIpa()

        try {
            val config = IosDeployConfig(
                enabled = true,
                artifactPath = ipa.absolutePath,
                testflight = TestFlightDestination(
                    enabled = true,
                    apiKeyPath = "",  // Empty - invalid
                    bundleId = "com.example.app",
                    teamId = "ABC123"
                ),
                appStore = AppStoreDestination(enabled = false)
            )

            val result = orchestrator.deployIos(config, ipa.absolutePath)

            val testFlightResult = result.individual.find { it.destination == "testflight" }
            assertFalse(testFlightResult?.success ?: false)
            assertTrue(testFlightResult?.message?.contains("apiKeyPath") ?: false)
        } finally {
            ipa.delete()
        }
    }

    /**
     * Test iOS App Store bundle ID validation
     */
    @Test
    fun testIosAppStoreValidation() = runBlocking {
        val orchestrator = DeployOrchestrator()
        val ipa = createTempIpa()

        try {
            val config = IosDeployConfig(
                enabled = true,
                artifactPath = ipa.absolutePath,
                testflight = TestFlightDestination(enabled = false),
                appStore = AppStoreDestination(
                    enabled = true,
                    apiKeyPath = "api-key.json",
                    bundleId = "",  // Empty - invalid
                    teamId = "ABC123"
                )
            )

            val result = orchestrator.deployIos(config, ipa.absolutePath)

            val appStoreResult = result.individual.find { it.destination == "app-store" }
            assertFalse(appStoreResult?.success ?: false)
            assertTrue(appStoreResult?.message?.contains("bundleId") ?: false)
        } finally {
            ipa.delete()
        }
    }

    /**
     * Test deployment result structure with real artifact
     */
    @Test
    fun testDeploymentResultStructure() = runBlocking {
        val orchestrator = DeployOrchestrator()
        val apk = createTempApk()

        try {
            val config = AndroidDeployConfig(
                enabled = true,
                artifactPath = apk.absolutePath,
                firebase = FirebaseAndroidDestination(enabled = true),
                playConsole = PlayConsoleAndroidDestination(enabled = true),
                local = LocalAndroidDestination(enabled = false)
            )

            val result = orchestrator.deployAndroid(config, apk.absolutePath)

            // Check result structure
            assertEquals("android", result.platform)
            assertTrue(result.individual.isNotEmpty())

            // Should have Firebase and Play Console results
            val hasFirebase = result.individual.any { it.destination == "firebase" }
            val hasPlayConsole = result.individual.any { it.destination == "play-console" }

            assertTrue(hasFirebase)
            assertTrue(hasPlayConsole)
        } finally {
            apk.delete()
        }
    }

    /**
     * Test Local destination with real artifact
     */
    @Test
    fun testAndroidLocalDeployment() = runBlocking {
        val orchestrator = DeployOrchestrator()
        val apk = createTempApk()

        try {
            val config = AndroidDeployConfig(
                enabled = true,
                artifactPath = apk.absolutePath,
                firebase = FirebaseAndroidDestination(enabled = false),
                playConsole = PlayConsoleAndroidDestination(enabled = false),
                local = LocalAndroidDestination(
                    enabled = true,
                    outputDir = "/tmp/deploy-test"
                )
            )

            val result = orchestrator.deployAndroid(config, apk.absolutePath)

            // Local deployment should be attempted
            val localResult = result.individual.find { it.destination == "local" }
            assertNotNull(localResult)
            assertEquals("local", localResult?.destination)
        } finally {
            apk.delete()
        }
    }

    /**
     * Test Android disabled (no validation)
     */
    @Test
    fun testAndroidDisabledSkipsValidation() = runBlocking {
        val orchestrator = DeployOrchestrator()
        val apk = createTempApk()

        try {
            val config = AndroidDeployConfig(
                enabled = false,  // Disabled
                artifactPath = apk.absolutePath,
                firebase = FirebaseAndroidDestination(enabled = true, serviceAccount = ""),
                playConsole = PlayConsoleAndroidDestination(enabled = false),
                local = LocalAndroidDestination(enabled = false)
            )

            val result = orchestrator.deployAndroid(config, apk.absolutePath)

            assertFalse(result.success)
            assertTrue(result.individual[0].message.contains("disabled"))
        } finally {
            apk.delete()
        }
    }

    /**
     * Test iOS disabled (no validation)
     */
    @Test
    fun testIosDisabledSkipsValidation() = runBlocking {
        val orchestrator = DeployOrchestrator()
        val ipa = createTempIpa()

        try {
            val config = IosDeployConfig(
                enabled = false,  // Disabled
                artifactPath = ipa.absolutePath,
                testflight = TestFlightDestination(enabled = true, apiKeyPath = ""),
                appStore = AppStoreDestination(enabled = false)
            )

            val result = orchestrator.deployIos(config, ipa.absolutePath)

            assertFalse(result.success)
            assertTrue(result.individual[0].message.contains("disabled"))
        } finally {
            ipa.delete()
        }
    }

    /**
     * Test multiple destinations with mixed validity
     */
    @Test
    fun testMultipleDestinationsPartialFailure() = runBlocking {
        val orchestrator = DeployOrchestrator()
        val apk = createTempApk()

        try {
            val config = AndroidDeployConfig(
                enabled = true,
                artifactPath = apk.absolutePath,
                firebase = FirebaseAndroidDestination(
                    enabled = true,
                    serviceAccount = "creds.json"  // Valid
                ),
                playConsole = PlayConsoleAndroidDestination(
                    enabled = true,
                    serviceAccount = "creds.json",
                    packageName = ""  // Invalid
                ),
                local = LocalAndroidDestination(enabled = false)
            )

            val result = orchestrator.deployAndroid(config, apk.absolutePath)

            // Firebase should pass validation, Play Console should fail
            val firebaseResult = result.individual.find { it.destination == "firebase" }
            val playConsoleResult = result.individual.find { it.destination == "play-console" }

            // Both should be attempted despite validation errors
            assertTrue(result.individual.size >= 2)
        } finally {
            apk.delete()
        }
    }
}

// Helper: assertNotNull for Kotlin/Native compatibility
private fun assertNotNull(value: Any?) {
    assertTrue(value != null)
}
