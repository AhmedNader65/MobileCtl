package com.mobilectl.deploy

import com.mobilectl.deploy.firebase.FirebaseHttpClient
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class FirebaseAndroidUploaderManualTest {

    @Test
    fun testRealFirebaseUploadWithServiceAccount() = runBlocking {
        val projectId = System.getenv("FIREBASE_PROJECT_ID")
        val serviceAccountPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH")
        val artifactPath = System.getenv("ARTIFACT_PATH") ?: "build/outputs/apk/release/app-release.apk"

        if (projectId.isNullOrBlank() || serviceAccountPath.isNullOrBlank()) {
            println("⏭️  Skipping manual Firebase test")
            println("   Set: FIREBASE_PROJECT_ID, FIREBASE_SERVICE_ACCOUNT_PATH")
            return@runBlocking
        }

        println("🔥 Testing Firebase Upload with Service Account")
        println("Project: $projectId")
        println("Service Account: $serviceAccountPath")
        println("Artifact: $artifactPath")

        val serviceAccountFile = File(serviceAccountPath)
        if (!serviceAccountFile.exists()) {
            println("❌ Service account file not found: $serviceAccountPath")
            return@runBlocking
        }

        val artifactFile = File(artifactPath)
        if (!artifactFile.exists()) {
            println("❌ Artifact not found: $artifactPath")
            return@runBlocking
        }

        try {
            // Create client from service account
            val firebaseClient = FirebaseHttpClient.create(
                serviceAccountFile = serviceAccountFile
            )

            // Upload
            val result = firebaseClient.uploadBuild(
                file = artifactFile,
                releaseNotes = "Manual test ${System.currentTimeMillis()}",
                testGroups = listOf("qa-team")
            )

            println()
            println("✅ Result:")
            println("   Success: ${result.success}")
            println("   Message: ${result.message}")
            println("   Build ID: ${result.buildId}")
            println("   URL: ${result.buildUrl}")

            assertTrue(result.success, "Upload should succeed")

        } catch (e: Exception) {
            println("❌ Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
