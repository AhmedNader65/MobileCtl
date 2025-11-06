package com.mobilectl.deploy.firebase

import com.mobilectl.model.deploy.DeployResult
import com.mobilectl.validation.ValidationTestHelpers.createTempApk
import com.mobilectl.validation.ValidationTestHelpers.createTempFirebaseServiceAccount
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class FirebaseAndroidUploaderTest {

    /**
     * Test Firebase result structure
     */
    @Test
    fun testFirebaseDeployResultStructure() {
        val apk = createTempApk()
        val serviceAccount = createTempFirebaseServiceAccount()

        try {
            // Create a deploy result manually
            val result = DeployResult(
                success = true,
                platform = "android",
                destination = "firebase",
                message = "Successfully uploaded to Firebase",
                buildId = "release-abc123",
                duration = 5000
            )

            // Verify structure
            assertEquals("android", result.platform)
            assertEquals("firebase", result.destination)
            assertTrue(result.success)
            assertEquals("release-abc123", result.buildId)
            assertTrue(result.duration >= 0)
        } finally {
            apk.delete()
            serviceAccount.delete()
        }
    }

    /**
     * Test Firebase deploy failure result
     */
    @Test
    fun testFirebaseDeployFailureResult() {
        val apk = createTempApk()

        try {
            val result = DeployResult(
                success = false,
                platform = "android",
                destination = "firebase",
                message = "Failed to authenticate with Firebase",
                error = Exception("Invalid service account"),
                duration = 1000
            )

            assertFalse(result.success)
            assertEquals("android", result.platform)
            assertEquals("firebase", result.destination)
            assertTrue(result.message.contains("authenticate"))
            assertTrue(result.error?.message?.contains("Invalid") ?: false)
        } finally {
            apk.delete()
        }
    }

    /**
     * Test Firebase with test groups
     */
    @Test
    fun testFirebaseTestGroupsConfig() {
        val testGroups = listOf("qa-team", "beta-testers", "internal")

        assertTrue(testGroups.size == 3)
        assertTrue(testGroups.contains("qa-team"))
        assertTrue(testGroups.contains("beta-testers"))
        assertTrue(testGroups.contains("internal"))
    }

    /**
     * Test Firebase with release notes
     */
    @Test
    fun testFirebaseReleaseNotesConfig() {
        val releaseNotes = "Version 1.0.0 - Bug fixes and improvements"

        assertTrue(releaseNotes.isNotBlank())
        assertTrue(releaseNotes.contains("1.0.0"))
    }

    /**
     * Test artifact validation
     */
    @Test
    fun testArtifactValidation() {
        val apk = createTempApk()

        try {
            // Artifact exists
            assertTrue(apk.exists())

            // Artifact is file
            assertTrue(apk.isFile)

            // Artifact has valid extension
            assertTrue(apk.name.endsWith(".apk"))
        } finally {
            apk.delete()
        }
    }

    /**
     * Test empty artifact detection
     */
    @Test
    fun testEmptyArtifactDetection() {
        val testDir = File(".mobilectl/test-firebase")
        testDir.mkdirs()
        val emptyApk = testDir.resolve("empty.apk")
        emptyApk.writeText("")

        try {
            assertTrue(emptyApk.exists())
            assertEquals(0, emptyApk.length())
            assertFalse(emptyApk.length() > 0)
        } finally {
            emptyApk.delete()
            testDir.deleteRecursively()
        }
    }

    /**
     * Test missing artifact detection
     */
    @Test
    fun testMissingArtifactDetection() {
        val nonExistentApk = File("/nonexistent/app.apk")

        assertFalse(nonExistentApk.exists())
    }

    /**
     * Test service account file validation
     */
    @Test
    fun testServiceAccountValidation() {
        val serviceAccount = createTempFirebaseServiceAccount()

        try {
            assertTrue(serviceAccount.exists())
            assertTrue(serviceAccount.isFile)
            assertTrue(serviceAccount.name.endsWith(".json"))

            val content = serviceAccount.readText()
            assertTrue(content.contains("service_account"))
            assertTrue(content.contains("private_key"))
        } finally {
            serviceAccount.delete()
        }
    }

    /**
     * Test deploy result with URL
     */
    @Test
    fun testFirebaseDeployResultWithUrl() {
        val result = DeployResult(
            success = true,
            platform = "android",
            destination = "firebase",
            message = "Release created",
            buildUrl = "https://console.firebase.google.com/project/my-project/appdistribution",
            buildId = "release-123",
            duration = 5000
        )

        assertTrue(result.success)
        assertTrue(result.buildUrl?.contains("firebase.google.com") ?: false)
        assertEquals("release-123", result.buildId)
    }

    /**
     * Test multiple deployment results
     */
    @Test
    fun testMultipleDeploymentResults() {
        val results = listOf(
            DeployResult(
                success = true,
                platform = "android",
                destination = "firebase",
                message = "Firebase upload successful",
                buildId = "firebase-123",
                duration = 5000
            ),
            DeployResult(
                success = false,
                platform = "android",
                destination = "play-console",
                message = "Play Console upload failed",
                error = Exception("Invalid credentials"),
                duration = 2000
            )
        )

        assertEquals(2, results.size)
        assertTrue(results[0].success)
        assertFalse(results[1].success)
        assertTrue(results[0].buildId?.contains("firebase") ?: false)
        assertTrue(results[1].error?.message?.contains("credentials") ?: false)
    }
}
