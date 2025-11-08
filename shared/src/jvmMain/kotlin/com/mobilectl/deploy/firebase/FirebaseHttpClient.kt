package com.mobilectl.deploy.firebase

import com.mobilectl.util.ApkAnalyzer
import com.mobilectl.util.PremiumLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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
            googleServicesJson: File? = null,
            apkFile: File? = null
        ): FirebaseHttpClient {
            // Get access token from service account
            val accessToken =
                ServiceAccountAuth.getAccessTokenFromServiceAccount(serviceAccountFile)

            // Extract package ID from APK if provided
            val packageId = apkFile?.let { ApkAnalyzer.getPackageId(it) }
                ?: throw Exception("Could not extract package ID from APK. APK file is required.")

            // Get Firebase config from google-services.json
            val config = if (googleServicesJson != null) {
                GoogleServicesParser.parse(googleServicesJson, packageId)
            } else {
                GoogleServicesParser.findAndParse(packageId = packageId)
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
                val releaseId = uploadApk(file, releaseNotes, testGroups)

                PremiumLogger.success("Release created with id: $releaseId")

                FirebaseUploadResponse(
                    success = true,
                    buildId = releaseId,
                    message = "Successfully created release: $releaseId",
                    buildUrl = "https://console.firebase.google.com/project/$projectId/appdistribution"
                )

            } catch (e: Exception) {
                FirebaseUploadResponse(
                    success = false,
                    buildId = null,
                    message = e.message ?: "Upload failed",
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
                val startUpload = System.currentTimeMillis()

                PremiumLogger.progress("Uploading to Firebase (${file.length() / (1024 * 1024)} MB)")

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
                    val uploadDuration = (System.currentTimeMillis() - startUpload) / 1000

                    if (!response.isSuccessful) {
                        val cleanError = cleanErrorMessage(responseBody, response.code)
                        throw Exception("Upload failed: $cleanError")
                    }

                    PremiumLogger.success("Uploaded in ${uploadDuration}s")

                    val operationName = parseOperationName(responseBody)
                    PremiumLogger.progress("Processing release...")

                    val releaseName = pollOperation(operationName)
                    PremiumLogger.info("testGroups: $testGroups")
                    if(testGroups.isEmpty()){
                        PremiumLogger.info("No test groups specified, skipping distribution step.")
                        return@withContext releaseName.substringAfterLast("/")
                    }
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

        val requestBody = requestBodyJson.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val cleanError = cleanErrorMessage(responseBody, response.code)
                throw Exception("Distribution failed: $cleanError")
            }

            PremiumLogger.success("Distributed to: ${groupsToDistribute.joinToString(", ")}")
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
                    val cleanError = cleanErrorMessage(responseBody, response.code)
                    throw Exception("Operation poll failed: $cleanError")
                }

                // Check if operation is done
                if (responseBody.contains("\"done\":true") || responseBody.contains("\"done\": true")) {
                    // Check if there's an error in the response first
                    if (responseBody.contains("\"error\"")) {
                        val errorMessage = extractErrorMessage(responseBody)
                        throw Exception(errorMessage)
                    }

                    // Extract release name from the "release" object in the response
                    // Response structure: { "name": "operations/...", "done": true, "response": { "@type": "...", "result": { "release": { "name": "projects/.../releases/..." } } } }
                    // OR: { "name": "operations/...", "done": true, "response": { "@type": "...", "name": "projects/.../releases/..." } }

                    // Try to extract from response.result.release.name first
                    val resultMatch = Regex("\"result\"\\s*:\\s*\\{[^}]*\"release\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"").find(responseBody)
                    if (resultMatch != null) {
                        val releaseName = resultMatch.groupValues[1]
                        PremiumLogger.detail("Release", releaseName, dim = true)
                        return@withContext releaseName
                    }

                    // Try to extract from response.name (if release is at top level of response)
                    val responseMatch = Regex("\"response\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"(projects/[^\"]+/releases/[^\"]+)\"").find(responseBody)
                    if (responseMatch != null) {
                        val releaseName = responseMatch.groupValues[1]
                        PremiumLogger.detail("Release", releaseName, dim = true)
                        return@withContext releaseName
                    }

                    // If we get here, the operation completed but we couldn't find the release name
                    throw Exception("Release name not found in completed operation.")
                }
            }

            // Wait before next poll
            kotlinx.coroutines.delay(2000)
        }

        throw Exception("Operation timed out after 120 seconds")
    }

    /**
     * Extract error message from Firebase operation response
     */
    private fun extractErrorMessage(responseBody: String): String {
        // Try to extract error.message from response
        val messageMatch = Regex("\"error\"\\s*:\\s*\\{[^}]*\"message\"\\s*:\\s*\"([^\"]+)\"").find(responseBody)
        if (messageMatch != null) {
            return messageMatch.groupValues[1].trim()
        }

        // Fallback to generic message
        return "Operation completed with error. Check Firebase Console for details."
    }

    /**
     * Clean up error messages to remove HTML and keep only useful info
     */
    private fun cleanErrorMessage(responseBody: String, code: Int): String {
        // If it's HTML, extract the meaningful error
        if (responseBody.contains("<!DOCTYPE html>") || responseBody.contains("<html")) {
            // Try to extract error message from HTML title or body
            val titleMatch = Regex("<title>(.*?)</title>").find(responseBody)
            if (titleMatch != null) {
                return "HTTP $code - ${titleMatch.groupValues[1]}"
            }
            return "HTTP $code - Server returned HTML error page"
        }

        // If it's JSON, try to extract error message
        if (responseBody.trim().startsWith("{")) {
            try {
                val errorMatch = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(responseBody)
                if (errorMatch != null) {
                    return "HTTP $code - ${errorMatch.groupValues[1]}"
                }

                val messageMatch = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(responseBody)
                if (messageMatch != null) {
                    return "HTTP $code - ${messageMatch.groupValues[1]}"
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }

        // If response is short, return it
        if (responseBody.length < 200) {
            return "HTTP $code - $responseBody"
        }

        // Otherwise return truncated
        return "HTTP $code - ${responseBody.take(200)}..."
    }
}
