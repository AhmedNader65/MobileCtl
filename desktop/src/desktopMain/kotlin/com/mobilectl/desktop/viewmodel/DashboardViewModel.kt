package com.mobilectl.desktop.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilectl.config.ConfigLoader
import com.mobilectl.config.createConfigParser
import com.mobilectl.detector.ProjectDetector
import com.mobilectl.deploy.createDeployOrchestrator
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.model.Platform
import com.mobilectl.model.deploy.DeployResult
import com.mobilectl.util.FileUtil
import com.mobilectl.util.JvmFileUtil
import kotlinx.coroutines.launch
import java.io.File

data class DeployFormState(
    val platform: Platform = Platform.ANDROID,
    val flavor: String = "",
    val track: String = "internal",
    val availableFlavors: List<String> = emptyList(),
    val availableTracks: List<String> = listOf("internal", "alpha", "beta", "production"),
    val isDeploying: Boolean = false,
    val error: String? = null
)

data class StatsState(
    val totalDeploys: Int = 0,
    val successRate: Double = 0.0,
    val avgTime: Long = 0,
    val recentDeploys: List<DeployHistoryItem> = emptyList()
)

data class DeployHistoryItem(
    val platform: String,
    val flavor: String,
    val track: String,
    val success: Boolean,
    val timestamp: Long,
    val duration: Long
)

class DashboardViewModel : ViewModel() {
    var formState by mutableStateOf(DeployFormState())
        private set

    var statsState by mutableStateOf(StatsState())
        private set

    private val configLoader = ConfigLoader(
        fileUtil = JvmFileUtil(),
        projectDetector = createProjectDetector()
    )

    private val deployOrchestrator = createDeployOrchestrator()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // Load config to get available flavors
                val configResult = configLoader.loadConfig(null)
                configResult.onSuccess { config ->
                    val flavors = config.deploy?.flavorGroups?.values
                        ?.flatMap { it.flavors }
                        ?.distinct() ?: emptyList()

                    formState = formState.copy(
                        availableFlavors = flavors,
                        flavor = flavors.firstOrNull() ?: ""
                    )
                }

                // Load deployment history (placeholder for now)
                loadDeploymentHistory()
            } catch (e: Exception) {
                formState = formState.copy(error = e.message)
            }
        }
    }

    private fun loadDeploymentHistory() {
        // Placeholder - in real implementation, this would read from a persistence layer
        val mockHistory = listOf(
            DeployHistoryItem(
                platform = "Android",
                flavor = "production",
                track = "internal",
                success = true,
                timestamp = System.currentTimeMillis() - 3600000,
                duration = 120000
            ),
            DeployHistoryItem(
                platform = "Android",
                flavor = "staging",
                track = "alpha",
                success = true,
                timestamp = System.currentTimeMillis() - 7200000,
                duration = 135000
            ),
            DeployHistoryItem(
                platform = "iOS",
                flavor = "production",
                track = "testflight",
                success = false,
                timestamp = System.currentTimeMillis() - 10800000,
                duration = 45000
            )
        )

        val successCount = mockHistory.count { it.success }
        val successRate = if (mockHistory.isNotEmpty()) {
            (successCount.toDouble() / mockHistory.size) * 100
        } else 0.0

        val avgDuration = if (mockHistory.isNotEmpty()) {
            mockHistory.map { it.duration }.average().toLong()
        } else 0L

        statsState = StatsState(
            totalDeploys = mockHistory.size,
            successRate = successRate,
            avgTime = avgDuration,
            recentDeploys = mockHistory
        )
    }

    fun updatePlatform(platform: Platform) {
        formState = formState.copy(platform = platform)
    }

    fun updateFlavor(flavor: String) {
        formState = formState.copy(flavor = flavor)
    }

    fun updateTrack(track: String) {
        formState = formState.copy(track = track)
    }

    fun startDeploy(onNavigateToProgress: () -> Unit) {
        formState = formState.copy(isDeploying = true, error = null)
        onNavigateToProgress()

        viewModelScope.launch {
            try {
                // This will be implemented with the progress screen
                // For now, just simulate a deployment
                kotlinx.coroutines.delay(100)
                formState = formState.copy(isDeploying = false)
            } catch (e: Exception) {
                formState = formState.copy(
                    isDeploying = false,
                    error = e.message
                )
            }
        }
    }

    fun clearError() {
        formState = formState.copy(error = null)
    }
}
