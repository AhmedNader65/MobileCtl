package com.mobilectl.deploy.firebase

import java.io.File

/**
 * Mock Firebase client for testing
 * Simulates Firebase API without making real requests
 */
class MockFirebaseClient(
    private val shouldFail: Boolean = false,
    private val failureMessage: String = "Mock Firebase error"
) : FirebaseClient {

    var lastUploadedFile: File? = null
    var lastReleaseNotes: String? = null
    var lastTestGroups: List<String> = emptyList()
    var uploadCallCount: Int = 0

    override suspend fun uploadBuild(
        file: File,
        releaseNotes: String?,
        testGroups: List<String>
    ): FirebaseUploadResponse {
        uploadCallCount++
        lastUploadedFile = file
        lastReleaseNotes = releaseNotes
        lastTestGroups = testGroups

        return if (shouldFail) {
            FirebaseUploadResponse(
                success = false,
                buildId = null,
                message = failureMessage,
                error = Exception(failureMessage)
            )
        } else {
            val releaseId = "releases/mock-${System.currentTimeMillis()}"

            FirebaseUploadResponse(
                success = true,
                buildId = releaseId,
                message = "Successfully created release: $releaseId",
                buildUrl = "https://console.firebase.google.com/project/test-project/appdistribution"
            )
        }
    }
}
