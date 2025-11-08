package com.mobilectl.deploy.playstore

import com.mobilectl.deploy.firebase.FirebaseUploadResponse
import com.mobilectl.util.ApkAnalyzer
import com.mobilectl.deploy.firebase.ServiceAccountAuth
import com.mobilectl.util.PremiumLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.code
import kotlin.toString

interface PlayClient {
    suspend fun createEdit(): String
    suspend fun uploadBundle(editId: String, aabFile: File): Int
    suspend fun assignToTrack(
        editId: String,
        track: String,
        versionCode: Int,
        releaseNotes: String? = null,
        rolloutPercentage: Double? = null,
        status: String = "draft"
    )
    suspend fun commitEdit(editId: String)
}

class PlayHttpClient private constructor(
    private val accessToken: String,
    private val packageName: String
) : PlayClient{
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://androidpublisher.googleapis.com/androidpublisher/v3"

    companion object {
        suspend fun create(
            serviceAccountFile: File,
            apkFile: File? = null,
            aabFile: File? = null
        ): PlayHttpClient {
            // Get access token from service account
            val accessToken = ServiceAccountAuth.getAccessTokenFromServiceAccount(
                serviceAccountFile,
                ServiceAccountAuth.Scopes.ANDROID_PUBLISHER
            )

            // Extract package ID from AAB/APK
            val artifactFile = aabFile ?: apkFile
            ?: throw Exception("AAB or APK file is required for Play Store upload")

            val packageName = ApkAnalyzer.getPackageId(artifactFile)
                ?: throw Exception("Could not extract package name from ${artifactFile.name}")

            PremiumLogger.detail("Package Name", packageName)

            return PlayHttpClient(accessToken, packageName)
        }
    }

    // === Edit Session Management ===

    /**
     * Creates a new edit session
     * Must be called before any upload operations
     */
    override suspend fun createEdit(): String {
        val url = "$baseUrl/applications/$packageName/edits"

        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create(null, ByteArray(0))) // Empty body
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .build()

        return client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                throw Exception("Failed to create edit session: HTTP ${response.code}\n$body")
            }

            // Parse edit ID from response
            val editId = parseEditId(body)
            PremiumLogger.success("Edit Session Created: $editId")
            editId
        }
    }

    override suspend fun uploadBundle(editId: String, aabFile: File): Int {
        require(aabFile.extension == "aab") {
            "Only AAB files are supported for Play Store upload. Got: ${aabFile.name}"
        }

        // Use the upload base URL, not the regular API base URL
        val url = "https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/$packageName/edits/$editId/bundles"

        val requestBody = RequestBody.create(
            "application/octet-stream".toMediaType(),
            aabFile
        )

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/octet-stream")
            .build()

        return client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                throw Exception("Failed to upload bundle: HTTP ${response.code}\n$body")
            }

            val versionCode = parseVersionCode(body)
            PremiumLogger.success("Bundle Uploaded: Version code: $versionCode")
            versionCode
        }
    }
    /**
     * Assigns uploaded bundle to a track
     */
    override suspend fun assignToTrack(
        editId: String,
        track: String,
        versionCode: Int,
        releaseNotes: String?,
        rolloutPercentage: Double?,
        status: String
    ) {
        val url = "$baseUrl/applications/$packageName/edits/$editId/tracks/$track"

        val releaseBody = buildTrackAssignmentBody(
            versionCode,
            releaseNotes,
            rolloutPercentage,
            status
        )

        val requestBody = RequestBody.create(
            "application/json".toMediaType(),
            releaseBody
        )

        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                throw Exception("Failed to assign to track: HTTP ${response.code}\n$body")
            }

            PremiumLogger.success("Assigned to Track $track")
        }
    }

    /**
     * Commits the edit session, publishing changes
     */
    override suspend fun commitEdit(editId: String) {
        val url = "$baseUrl/applications/$packageName/edits/$editId:commit"

        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create(null, ByteArray(0)))
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                throw Exception("Failed to commit edit: HTTP ${response.code}\n$body")
            }

            PremiumLogger.success("Changes Published: Edit committed successfully")
        }
    }

    // === Helper Methods ===

    private fun parseEditId(json: String): String {
        // Simple JSON parsing for edit ID
        val regex = "\"id\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        return regex.find(json)?.groupValues?.get(1)
            ?: throw Exception("Could not parse edit ID from response")
    }

    private fun parseVersionCode(json: String): Int {
        val regex = "\"versionCode\"\\s*:\\s*(\\d+)".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toInt()
            ?: throw Exception("Could not parse version code from response")
    }

    private fun buildTrackAssignmentBody(
        versionCode: Int,
        releaseNotes: String?,
        rolloutPercentage: Double?,
        status: String
    ): String {
        val releaseNotesJson = releaseNotes?.let {
            """
            "releaseNotes": [{
                "language": "en-US",
                "text": "${it.replace("\"", "\\\"")}"
            }],
            """.trimIndent()
        } ?: ""

        val rolloutJson = rolloutPercentage?.let {
            "\"userFraction\": ${it / 100.0},"
        } ?: ""

        return """
        {
            "releases": [{
                "versionCodes": [$versionCode],
                $releaseNotesJson
                $rolloutJson
                "status": "$status"
            }]
        }
        """.trimIndent()
    }
}
