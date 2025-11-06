package com.mobilectl.deploy.firebase

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

@Serializable
data class GoogleServicesConfig(
    val projectNumber: String,
    val appId: String,
    val projectId: String
)

/**
 * Parser for google-services.json
 * Auto-detects Firebase configuration from known paths
 */
object GoogleServicesParser {

    private val KNOWN_PATHS = listOf(
        "app/google-services.json",
        "app/src/main/google-services.json",
        "google-services.json",
        "./google-services.json",
        "../google-services.json"
    )

    /**
     * Find and parse google-services.json
     * Searches from current directory and parent directories
     */
    fun findAndParse(startDir: File = File(".")): GoogleServicesConfig {
        // Try known paths first
        for (path in KNOWN_PATHS) {
            val file = File(startDir, path)
            if (file.exists()) {
                return parse(file)
            }
        }

        // Try searching up directory tree
        var current = startDir.absoluteFile
        repeat(5) {  // Search up to 5 levels
            for (path in KNOWN_PATHS) {
                val file = File(current, path)
                if (file.exists()) {
                    return parse(file)
                }
            }
            current = current.parentFile ?: return throwNotFound()
        }

        return throwNotFound()
    }

    /**
     * Parse google-services.json file
     */
    fun parse(file: File): GoogleServicesConfig {
        if (!file.exists()) {
            throw Exception("google-services.json not found: ${file.absolutePath}")
        }

        try {
            val json = file.readText()
            val root = Json.parseToJsonElement(json).jsonObject
            val services = root["project_info"]?.jsonObject
                ?: throw Exception("Missing 'project_info' in google-services.json")
            // Extract project_number
            val projectNumber = services["project_number"]?.jsonPrimitive?.content
                ?: throw Exception("Missing 'project_number' in google-services.json")

            // Extract project_id
            val projectId = services["project_id"]?.jsonPrimitive?.content
                ?: throw Exception("Missing 'project_id' in google-services.json")

            // Extract app_id from client[0]
            val clients = root["client"]?.jsonArray
                ?: throw Exception("Missing 'client' array in google-services.json")

            if (clients.isEmpty()) {
                throw Exception("'client' array is empty in google-services.json")
            }

            val clientInfo = clients[0].jsonObject
            val appId = clientInfo["client_info"]?.jsonObject
                ?.get("mobilesdk_app_id")?.jsonPrimitive?.content
                ?: throw Exception("Missing 'mobilesdk_app_id' in google-services.json")

            return GoogleServicesConfig(
                projectNumber = projectNumber,
                appId = appId,
                projectId = projectId
            )

        } catch (e: Exception) {
            throw Exception("Failed to parse google-services.json: ${e.message}", e)
        }
    }

    private fun throwNotFound(): GoogleServicesConfig {
        throw Exception(
            """
            google-services.json not found!
            
            Searched in:
            ${KNOWN_PATHS.joinToString("\n") { "  - $it" }}
            
            Download from Firebase Console:
            1. Go to Project Settings ⚙️
            2. Download google-services.json
            3. Place in app/ folder
            """.trimIndent()
        )
    }
}
