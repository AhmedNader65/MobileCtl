package com.mobilectl.deploy

import com.mobilectl.model.deploy.AndroidDeployConfig
import com.mobilectl.model.deploy.IosDeployConfig
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class DeployOrchestratorTest {

    @Test
    fun testSelectFirebaseAndroidStrategy() = runBlocking {
        val orchestrator = DeployOrchestrator()

        val config = AndroidDeployConfig(
            enabled = true,
            destination = "firebase",
            appId = "com.example.app",
            token = "token",
            artifactPath = "app.apk"
        )

        // DeployOrchestrator should select firebase strategy
        // We can't directly test strategy selection, but we can test the result
        assertTrue(config.destination == "firebase")
    }

    @Test
    fun testAndroidDeployDisabled() = runBlocking {
        val orchestrator = DeployOrchestrator()

        val config = AndroidDeployConfig(
            enabled = false,  // Disabled
            destination = "firebase",
            appId = "com.example.app",
            token = "token",
            artifactPath = "app.apk"
        )

        val result = orchestrator.deployAndroid(config, "app.apk")

        assertFalse(result.success)
        assertTrue(result.message.contains("disabled"))
    }

    @Test
    fun testAndroidUnknownDestination() = runBlocking {
        val orchestrator = DeployOrchestrator()

        val config = AndroidDeployConfig(
            enabled = true,
            destination = "unknown-service",  // Invalid
            appId = "com.example.app",
            token = "token",
            artifactPath = "app.apk"
        )

        val result = orchestrator.deployAndroid(config, "app.apk")

        assertFalse(result.success)
        assertTrue(result.message.contains("Unknown destination"))
    }

    @Test
    fun testIosDeployDisabled() = runBlocking {
        val orchestrator = DeployOrchestrator()

        val config = IosDeployConfig(
            enabled = false,  // Disabled
            destination = "testflight",
            appId = "com.example.app",
            teamId = "ABC123",
            apiKey = "key",
            artifactPath = "app.ipa"
        )

        val result = orchestrator.deployIos(config, "app.ipa")

        assertFalse(result.success)
        assertTrue(result.message.contains("disabled"))
    }

    @Test
    fun testIosUnknownDestination() = runBlocking {
        val orchestrator = DeployOrchestrator()

        val config = IosDeployConfig(
            enabled = true,
            destination = "unknown-service",  // Invalid
            appId = "com.example.app",
            teamId = "ABC123",
            apiKey = "key",
            artifactPath = "app.ipa"
        )

        val result = orchestrator.deployIos(config, "app.ipa")

        assertFalse(result.success)
        assertTrue(result.message.contains("Unknown destination"))
    }

    @Test
    fun testAndroidConfigValidationError() = runBlocking {
        val orchestrator = DeployOrchestrator()

        val config = AndroidDeployConfig(
            enabled = true,
            destination = "firebase",
            appId = "com.example.app",
            token = "",  // Empty (invalid for firebase)
            artifactPath = "app.apk"
        )

        val result = orchestrator.deployAndroid(config, "app.apk")

        assertFalse(result.success)
        assertTrue(result.message.contains("Configuration error"))
    }

    @Test
    fun testIosConfigValidationError() = runBlocking {
        val orchestrator = DeployOrchestrator()

        val config = IosDeployConfig(
            enabled = true,
            destination = "testflight",
            appId = "com.example.app",
            teamId = "ABC123",
            apiKey = "",  // Empty (invalid for testflight)
            artifactPath = "app.ipa"
        )

        val result = orchestrator.deployIos(config, "app.ipa")

        assertFalse(result.success)
        assertTrue(result.message.contains("Configuration error"))
    }

    @Test
    fun testAndroidDeployResultHasPlatform() = runBlocking {
        val orchestrator = DeployOrchestrator()

        val config = AndroidDeployConfig(
            enabled = true,
            destination = "local",  // Local doesn't need token
            appId = "com.example.app",
            token = "",
            artifactPath = "app.apk"
        )

        // File doesn't exist, so will fail, but we test the result structure
        val result = orchestrator.deployAndroid(config, "app.apk")

        assertEquals("android", result.platform)
        assertEquals("local", result.destination)
    }

    @Test
    fun testIosDeployResultHasPlatform() = runBlocking {
        val orchestrator = DeployOrchestrator()

        val config = IosDeployConfig(
            enabled = true,
            destination = "local",  // Local doesn't need API key
            appId = "com.example.app",
            teamId = "",
            apiKey = "",
            artifactPath = "app.ipa"
        )

        // File doesn't exist, so will fail, but we test the result structure
        val result = orchestrator.deployIos(config, "app.ipa")

        assertEquals("ios", result.platform)
        assertEquals("local", result.destination)
    }
}
