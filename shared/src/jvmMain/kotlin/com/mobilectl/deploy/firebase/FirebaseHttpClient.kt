package com.mobilectl.deploy.firebase

import com.sun.org.apache.xml.internal.security.utils.XMLUtils.encodeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.Duration

interface FirebaseClient {
    suspend fun uploadBuild(
        file: File,
        releaseNotes: String? = null,
        testGroups: List<String> = emptyList()
    ): FirebaseUploadResponse
}

data class FirebaseUploadResponse(
    val success: Boolean,
    val buildId: String?,
    val message: String,
    val buildUrl: String? = null,
    val error: Exception? = null
)

/**
 * Firebase App Distribution HTTP client
 * Uses service account for authentication (only method)
 */
class FirebaseHttpClient(
    private val projectNumber: String,
    private val appId: String,
    private val projectId: String,
    private val accessToken: String
) : FirebaseClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(Duration.ofMinutes(5))
        .readTimeout(Duration.ofMinutes(5))
        .writeTimeout(Duration.ofMinutes(5))
        .build()

    companion object {
        private const val API_BASE = "https://firebaseappdistribution.googleapis.com"

        /**
         * Create Firebase client from service account
         * Auto-detects google-services.json if not provided
         */
        suspend fun create(
            serviceAccountFile: File,
            googleServicesJson: File? = null
        ): FirebaseHttpClient {
            // Get access token from service account
            val accessToken =
                ServiceAccountAuth.getAccessTokenFromServiceAccount(serviceAccountFile)

            // Get Firebase config from google-services.json
            val config = if (googleServicesJson != null) {
                GoogleServicesParser.parse(googleServicesJson)
            } else {
                GoogleServicesParser.findAndParse()
            }

            return FirebaseHttpClient(
                projectNumber = config.projectNumber,
                appId = config.appId,
                projectId = config.projectId,
                accessToken = accessToken
            )
        }
    }

    override suspend fun uploadBuild(
        file: File,
        releaseNotes: String?,
        testGroups: List<String>
    ): FirebaseUploadResponse {
        return withContext(Dispatchers.IO) {
            try {
                println("📤 Uploading ${file.name} (${file.length() / (1024 * 1024)} MB)...")

                val releaseId = uploadApk(file, releaseNotes, testGroups)

                println("✅ Release created: $releaseId")

                FirebaseUploadResponse(
                    success = true,
                    buildId = releaseId,
                    message = "Successfully created release: $releaseId",
                    buildUrl = "https://console.firebase.google.com/project/$projectId/appdistribution"
                )

            } catch (e: Exception) {
                e.printStackTrace()
                FirebaseUploadResponse(
                    success = false,
                    buildId = null,
                    message = "Upload failed: ${e.message}",
                    error = e
                )
            }
        }
    }

    /**
     * Upload APK and create release with metadata
     */

    private suspend fun uploadApk(
        file: File,
        releaseNotes: String?,
        testGroups: List<String>
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildEndpointUrl()
                println("📍 Endpoint: $url")

                val requestBody = file.asRequestBody("application/octet-stream".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("X-Goog-Upload-Protocol", "raw")
                    .addHeader("X-Goog-Upload-File-Name", file.name)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""

                    println("📝 Response: HTTP ${response.code}")

                    if (!response.isSuccessful) {
                        throw Exception("HTTP ${response.code}: $responseBody")
                    }

                    val operationName = parseOperationName(responseBody)
                    println("⏳ Waiting for operation to complete: $operationName")

                    val releaseName = pollOperation(operationName)
                    println("✅ Release ready: $releaseName")

                    // Distribute the release after upload completes
                    distributeRelease(releaseName, releaseNotes, testGroups)

                    releaseName.substringAfterLast("/")
                }

            } catch (e: Exception) {
                throw Exception("APK upload failed: ${e.message}", e)
            }
        }
    }

    /**
     * Build Firebase API endpoint
     */
    private fun buildEndpointUrl(): String {
        return "$API_BASE/upload/v1/projects/$projectNumber/apps/$appId/releases:upload"
    }

    private suspend fun distributeRelease(
        releaseName: String,
        releaseNotes: String?,
        testGroups: List<String>
    ): Unit = withContext(Dispatchers.IO) {
        val url = "$API_BASE/v1/$releaseName:distribute"

        val groupsToDistribute = testGroups.ifEmpty { listOf("qa-team") }

        val requestBodyJson = buildString {
            append("{")
            if (releaseNotes != null) {
                append("\"releaseNotes\": {")
                append("\"text\": \"$releaseNotes\"")
                append("},")
            }
            append("\"testerEmails\": [],")
            append("\"groupAliases\": [")
            append(groupsToDistribute.joinToString(",") { "\"$it\"" })
            append("]}")
        }

        println("📍 Distribution URL: $url")
        println("📝 Request body: $requestBodyJson")

        val request = Request.Builder()
            .url(url)
            .post(okhttp3.RequestBody.create("application/json".toMediaType(), requestBodyJson))
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""

            println("📝 Distribution response: HTTP ${response.code}")
            if (responseBody.isNotEmpty()) {
                println("📝 Response body: $responseBody")
            }

            if (!response.isSuccessful) {
                throw Exception("Distribution failed: HTTP ${response.code}\n$responseBody")
            }

            println("✅ Distributed to groups: ${groupsToDistribute.joinToString()}")
        }
    }

    private fun parseOperationName(response: String): String {
        val nameStart = response.indexOf("\"name\"")
        if (nameStart == -1) throw Exception("No 'name' field in response")

        val valueStart = response.indexOf("\"", nameStart + 7) + 1
        val valueEnd = response.indexOf("\"", valueStart)

        return response.substring(valueStart, valueEnd)
    }

    private suspend fun pollOperation(operationName: String): String = withContext(Dispatchers.IO) {
        val url = "$API_BASE/v1/$operationName"

        repeat(60) { attempt ->
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    throw Exception("Operation poll failed: HTTP ${response.code}\n$responseBody")
                }

                // Check if operation is done
                if (responseBody.contains("\"done\":true") || responseBody.contains("\"done\": true")) {
                    // Extract release name from response
                    val releaseNameStart = responseBody.indexOf("\"name\"", responseBody.indexOf("\"release\""))
                    if (releaseNameStart != -1) {
                        val valueStart = responseBody.indexOf("\"", releaseNameStart + 7) + 1
                        val valueEnd = responseBody.indexOf("\"", valueStart)
                        return@withContext responseBody.substring(valueStart, valueEnd)
                    }
                    throw Exception("Release name not found in completed operation")
                }
            }

            // Wait before next poll
            kotlinx.coroutines.delay(2000)
        }

        throw Exception("Operation timed out after 120 seconds")
    }
}
