package com.mobilectl.desktop.data

import com.mobilectl.desktop.viewmodel.DeployHistoryItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File

@Serializable
data class DeploymentRecord(
    val id: String,
    val platform: String,
    val flavor: String,
    val track: String,
    val success: Boolean,
    val timestamp: Long,
    val duration: Long,
    val buildUrl: String? = null,
    val buildId: String? = null,
    val logs: List<String> = emptyList()
)

object DeploymentDatabase {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val dataDir = File(System.getProperty("user.home"), ".mobilectl")
    private val dbFile = File(dataDir, "deployment-history.json")

    init {
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }
    }

    fun getAllDeployments(): List<DeploymentRecord> {
        return try {
            if (dbFile.exists()) {
                val content = dbFile.readText()
                json.decodeFromString(content)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("Failed to load deployment history: ${e.message}")
            emptyList()
        }
    }

    fun saveDeployment(deployment: DeploymentRecord) {
        try {
            val currentDeployments = getAllDeployments().toMutableList()
            currentDeployments.add(0, deployment) // Add to beginning

            // Keep only last 100 deployments
            val trimmedDeployments = currentDeployments.take(100)

            val content = json.encodeToString(trimmedDeployments)
            dbFile.writeText(content)
        } catch (e: Exception) {
            println("Failed to save deployment: ${e.message}")
        }
    }

    fun getRecentDeployments(limit: Int = 10): List<DeployHistoryItem> {
        return getAllDeployments()
            .take(limit)
            .map { record ->
                DeployHistoryItem(
                    platform = record.platform,
                    flavor = record.flavor,
                    track = record.track,
                    success = record.success,
                    timestamp = record.timestamp,
                    duration = record.duration
                )
            }
    }

    fun getStats(): DeploymentStats {
        val deployments = getAllDeployments()
        val total = deployments.size
        val successful = deployments.count { it.success }
        val successRate = if (total > 0) (successful.toDouble() / total) * 100 else 0.0
        val avgDuration = if (total > 0) deployments.map { it.duration }.average().toLong() else 0L

        return DeploymentStats(
            totalDeploys = total,
            successRate = successRate,
            avgTime = avgDuration
        )
    }

    fun clearHistory() {
        dbFile.delete()
    }
}

data class DeploymentStats(
    val totalDeploys: Int,
    val successRate: Double,
    val avgTime: Long
)
