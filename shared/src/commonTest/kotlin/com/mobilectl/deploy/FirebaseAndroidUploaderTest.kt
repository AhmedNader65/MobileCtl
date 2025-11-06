package com.mobilectl.deploy

import com.mobilectl.deploy.firebase.MockFirebaseClient
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class FirebaseAndroidUploaderTest {

    private val testDir = File(".mobilectl/test-deploy-firebase")

    @Test
    fun testValidateConfigMissingToken() {
        val uploader = FirebaseAndroidUploader()
        val config = mapOf("projectId" to "my-project")

        val errors = uploader.validateConfig(config)
        assertTrue(errors.any { it.contains("access token") })
    }

    @Test
    fun testValidateConfigMissingProjectId() {
        val uploader = FirebaseAndroidUploader()
        val config = mapOf("accessToken" to "token123")

        val errors = uploader.validateConfig(config)
        assertTrue(errors.any { it.contains("project ID") })
    }

    @Test
    fun testValidateConfigComplete() {
        val uploader = FirebaseAndroidUploader()
        val config = mapOf(
            "accessToken" to "token123",
            "projectId" to "my-project"
        )

        val errors = uploader.validateConfig(config)
        assertEquals(0, errors.size)
    }

    @Test
    fun testUploadMissingFile() = runBlocking {
        val mockClient = MockFirebaseClient()
        val uploader = FirebaseAndroidUploader { _-> mockClient }

        val config = mapOf(
            "accessToken" to "token123",
            "projectId" to "my-project"
        )

        val result = uploader.upload(File("non-existent.apk"), config)

        assertFalse(result.success)
        assertTrue(result.message.contains("not found"))
        assertEquals("android", result.platform)
        assertEquals("firebase", result.destination)
        assertEquals(0, mockClient.uploadCallCount, "Should not call Firebase for invalid file")
    }

    @Test
    fun testUploadEmptyFile() = runBlocking {
        testDir.mkdirs()
        val emptyFile = testDir.resolve("empty.apk")
        emptyFile.writeText("")

        try {
            val mockClient = MockFirebaseClient()
            val uploader = FirebaseAndroidUploader { _-> mockClient }

            val config = mapOf(
                "accessToken" to "token123",
                "projectId" to "my-project"
            )

            val result = uploader.upload(emptyFile, config)

            assertFalse(result.success)
            assertTrue(result.message.contains("empty"))
            assertEquals(0, mockClient.uploadCallCount)
        } finally {
            emptyFile.delete()
        }
    }

    @Test
    fun testUploadSuccessfulWithMock() = runBlocking {
        testDir.mkdirs()
        val testFile = testDir.resolve("test.apk")
        testFile.writeText("fake apk content for testing")

        try {
            val mockClient = MockFirebaseClient(shouldFail = false)
            val uploader = FirebaseAndroidUploader { _-> mockClient }

            val config = mapOf(
                "accessToken" to "token123",
                "projectId" to "my-project",
                "releaseNotes" to "Version 1.0.0 beta"
            )

            val result = uploader.upload(testFile, config)

            assertTrue(result.success, "Upload should succeed")
            assertEquals("android", result.platform)
            assertEquals("firebase", result.destination)
            assertTrue(result.buildId != null, "Should have release ID")
            assertEquals(1, mockClient.uploadCallCount, "Should call Firebase once")
            assertEquals(testFile, mockClient.lastUploadedFile)
            assertEquals("Version 1.0.0 beta", mockClient.lastReleaseNotes)
        } finally {
            testFile.delete()
        }
    }

    @Test
    fun testUploadFailureWithMock() = runBlocking {
        testDir.mkdirs()
        val testFile = testDir.resolve("test.apk")
        testFile.writeText("fake apk content")

        try {
            val mockClient = MockFirebaseClient(
                shouldFail = true,
                failureMessage = "Authentication failed"
            )
            val uploader = FirebaseAndroidUploader { _-> mockClient }

            val config = mapOf(
                "accessToken" to "invalid-token",
                "projectId" to "my-project"
            )

            val result = uploader.upload(testFile, config)

            assertFalse(result.success)
            assertTrue(result.message.contains("Authentication failed"))
            assertEquals(1, mockClient.uploadCallCount)
        } finally {
            testFile.delete()
        }
    }

    @Test
    fun testUploadWithTestGroups() = runBlocking {
        testDir.mkdirs()
        val testFile = testDir.resolve("test.apk")
        testFile.writeText("fake apk")

        try {
            val mockClient = MockFirebaseClient()
            val uploader = FirebaseAndroidUploader { _-> mockClient }

            val config = mapOf(
                "accessToken" to "token123",
                "projectId" to "my-project",
                "testGroups" to "qa-team,beta-testers,internal"
            )

            val result = uploader.upload(testFile, config)

            assertTrue(result.success)
            assertEquals(3, mockClient.lastTestGroups.size)
            assertTrue(mockClient.lastTestGroups.contains("qa-team"))
            assertTrue(mockClient.lastTestGroups.contains("beta-testers"))
            assertTrue(mockClient.lastTestGroups.contains("internal"))
        } finally {
            testFile.delete()
        }
    }

    @Test
    fun testUploadMissingConfig() = runBlocking {
        testDir.mkdirs()
        val testFile = testDir.resolve("test.apk")
        testFile.writeText("fake apk")

        try {
            val mockClient = MockFirebaseClient()
            val uploader = FirebaseAndroidUploader { _-> mockClient }

            val config = mapOf("projectId" to "my-project")  // Missing token!

            val result = uploader.upload(testFile, config)

            assertFalse(result.success)
            assertTrue(result.message.contains("token"))
            assertEquals(0, mockClient.uploadCallCount, "Should not call Firebase")
        } finally {
            testFile.delete()
        }
    }

    @Test
    fun testUploadResultHasDuration() = runBlocking {
        testDir.mkdirs()
        val testFile = testDir.resolve("test.apk")
        testFile.writeText("fake apk")

        try {
            val mockClient = MockFirebaseClient()
            val uploader = FirebaseAndroidUploader { _-> mockClient }

            val config = mapOf(
                "accessToken" to "token123",
                "projectId" to "my-project"
            )

            val result = uploader.upload(testFile, config)

            assertTrue(result.duration >= 0, "Should have duration")
            assertTrue(result.duration < 10000, "Should be quick (mock)")
        } finally {
            testFile.delete()
        }
    }
}
