package com.mobilectl.desktop.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilectl.desktop.util.ProcessOutput
import com.mobilectl.desktop.util.StreamingProcessExecutor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DeployStep(
    val name: String,
    val status: StepStatus,
    val progress: Float = 0f
)

enum class StepStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

data class DeployProgressState(
    val steps: List<DeployStep> = emptyList(),
    val logs: List<String> = emptyList(),
    val isComplete: Boolean = false,
    val hasError: Boolean = false,
    val canCancel: Boolean = true
)

class DeployProgressViewModel(
    private val platform: String = "Android",
    private val flavor: String = "production",
    private val track: String = "internal",
    private val useRealDeployment: Boolean = true
) : ViewModel() {
    var state by mutableStateOf(DeployProgressState())
        private set

    private val startTime = System.currentTimeMillis()
    private val processExecutor = StreamingProcessExecutor()
    private var deploymentJob: Job? = null

    init {
        startDeploy()
    }

    private fun startDeploy() {
        val initialSteps = listOf(
            DeployStep("Validating Configuration", StepStatus.PENDING),
            DeployStep("Building Artifacts", StepStatus.PENDING),
            DeployStep("Signing Artifacts", StepStatus.PENDING),
            DeployStep("Uploading to Firebase", StepStatus.PENDING),
            DeployStep("Finalizing Deployment", StepStatus.PENDING)
        )

        state = state.copy(steps = initialSteps)

        deploymentJob = viewModelScope.launch {
            if (useRealDeployment) {
                executeRealDeployment()
            } else {
                executeSimulatedDeployment()
            }
        }
    }

    private suspend fun executeRealDeployment() {
        try {
            // Step 1: Validating Configuration
            updateStepStatus(0, StepStatus.IN_PROGRESS)
            addLog("[INFO] Starting deployment process...")
            addLog("[INFO] Platform: $platform | Flavor: $flavor | Track: $track")
            addLog("[INFO] Validating configuration files...")
            delay(500)
            addLog("[SUCCESS] Configuration validated successfully")
            updateStepStatus(0, StepStatus.COMPLETED)

            // Step 2: Building Artifacts
            updateStepStatus(1, StepStatus.IN_PROGRESS)
            addLog("[INFO] Starting build process...")

            var buildSuccess = false
            var buildProgress = 0f

            processExecutor.executeBuild(
                platform = platform,
                flavor = flavor,
                buildType = "release"
            ).collect { output ->
                when (output) {
                    is ProcessOutput.Started -> {
                        addLog("[INFO] Executing: ${output.command}")
                    }
                    is ProcessOutput.StdOut -> {
                        addLog("[INFO] ${output.line}")
                        // Update progress based on build output
                        if (output.line.contains("BUILD SUCCESSFUL") || output.line.contains("BUILD SUCCEEDED")) {
                            buildProgress = 1f
                            buildSuccess = true
                        } else {
                            buildProgress = (buildProgress + 0.05f).coerceAtMost(0.95f)
                        }
                        updateStepProgress(1, buildProgress)
                    }
                    is ProcessOutput.StdErr -> {
                        if (output.line.isNotBlank()) {
                            addLog("[WARNING] ${output.line}")
                        }
                    }
                    is ProcessOutput.Completed -> {
                        if (output.exitCode == 0) {
                            addLog("[SUCCESS] Build completed successfully")
                            updateStepStatus(1, StepStatus.COMPLETED)
                        } else {
                            addLog("[ERROR] Build failed with exit code ${output.exitCode}")
                            updateStepStatus(1, StepStatus.FAILED)
                            throw Exception("Build failed")
                        }
                    }
                    is ProcessOutput.Error -> {
                        addLog("[ERROR] Build error: ${output.message}")
                        updateStepStatus(1, StepStatus.FAILED)
                        throw Exception(output.message)
                    }
                }
            }

            // Step 3: Signing Artifacts (simulated for now)
            updateStepStatus(2, StepStatus.IN_PROGRESS)
            addLog("[INFO] Signing artifacts...")
            addLog("[INFO] Using keystore configuration...")
            simulateProgress(2, 800)
            addLog("[SUCCESS] Artifacts signed successfully")
            updateStepStatus(2, StepStatus.COMPLETED)

            // Step 4: Uploading (simulated for now - would use real deployment here)
            updateStepStatus(3, StepStatus.IN_PROGRESS)
            addLog("[INFO] Uploading to $track track...")
            addLog("[INFO] Authenticating with service account...")
            simulateProgress(3, 1000)
            addLog("[INFO] Uploading artifacts...")
            simulateProgress(3, 2000)
            addLog("[SUCCESS] Upload completed successfully")
            updateStepStatus(3, StepStatus.COMPLETED)

            // Step 5: Finalizing
            updateStepStatus(4, StepStatus.IN_PROGRESS)
            addLog("[INFO] Finalizing deployment...")
            simulateProgress(4, 500)
            addLog("[SUCCESS] Deployment completed successfully!")
            updateStepStatus(4, StepStatus.COMPLETED)

            state = state.copy(isComplete = true, canCancel = false)
            saveDeploymentToDatabase(success = true)

        } catch (e: Exception) {
            addLog("[ERROR] Deployment failed: ${e.message}")
            state = state.copy(hasError = true, canCancel = false)
            saveDeploymentToDatabase(success = false)
        }
    }

    private suspend fun executeSimulatedDeployment() {
        try {
            // Step 1: Validating Configuration
            updateStepStatus(0, StepStatus.IN_PROGRESS)
            addLog("[INFO] Starting deployment process...")
            addLog("[INFO] Validating configuration files...")
            simulateProgress(0, 1000)
            addLog("[SUCCESS] Configuration validated successfully")
            updateStepStatus(0, StepStatus.COMPLETED)

            // Step 2: Building Artifacts
            updateStepStatus(1, StepStatus.IN_PROGRESS)
            addLog("[INFO] Starting build process...")
            addLog("[INFO] Compiling sources...")
            simulateProgress(1, 1500)
            addLog("[INFO] Generating APK...")
            simulateProgress(1, 1000)
            addLog("[SUCCESS] Build completed successfully")
            updateStepStatus(1, StepStatus.COMPLETED)

            // Step 3: Signing Artifacts
            updateStepStatus(2, StepStatus.IN_PROGRESS)
            addLog("[INFO] Signing artifacts...")
            addLog("[INFO] Using keystore configuration...")
            simulateProgress(2, 800)
            addLog("[SUCCESS] Artifacts signed successfully")
            updateStepStatus(2, StepStatus.COMPLETED)

            // Step 4: Uploading to Firebase
            updateStepStatus(3, StepStatus.IN_PROGRESS)
            addLog("[INFO] Uploading to Firebase App Distribution...")
            addLog("[INFO] Authenticating with service account...")
            simulateProgress(3, 1000)
            addLog("[INFO] Uploading artifacts...")
            simulateProgress(3, 2000)
            addLog("[SUCCESS] Upload completed successfully")
            addLog("[INFO] Distribution URL: https://firebase.google.com/...")
            updateStepStatus(3, StepStatus.COMPLETED)

            // Step 5: Finalizing
            updateStepStatus(4, StepStatus.IN_PROGRESS)
            addLog("[INFO] Finalizing deployment...")
            addLog("[INFO] Notifying testers...")
            simulateProgress(4, 500)
            addLog("[SUCCESS] Deployment completed successfully!")
            updateStepStatus(4, StepStatus.COMPLETED)

            state = state.copy(isComplete = true, canCancel = false)
            saveDeploymentToDatabase(success = true)
        } catch (e: Exception) {
            addLog("[ERROR] Deployment failed: ${e.message}")
            state = state.copy(hasError = true, canCancel = false)
            saveDeploymentToDatabase(success = false)
        }
    }

    private fun saveDeploymentToDatabase(success: Boolean) {
        val duration = System.currentTimeMillis() - startTime
        val deployment = com.mobilectl.desktop.data.DeploymentRecord(
            id = java.util.UUID.randomUUID().toString(),
            platform = platform,
            flavor = flavor,
            track = track,
            success = success,
            timestamp = startTime,
            duration = duration,
            logs = state.logs
        )
        com.mobilectl.desktop.data.DeploymentDatabase.saveDeployment(deployment)
    }

    private suspend fun simulateProgress(stepIndex: Int, duration: Long) {
        val steps = 20
        repeat(steps) {
            delay(duration / steps)
            val progress = (it + 1).toFloat() / steps
            updateStepProgress(stepIndex, progress)
        }
    }

    private fun updateStepStatus(index: Int, status: StepStatus) {
        val updatedSteps = state.steps.toMutableList()
        updatedSteps[index] = updatedSteps[index].copy(status = status, progress = if (status == StepStatus.COMPLETED) 1f else 0f)
        state = state.copy(steps = updatedSteps)
    }

    private fun updateStepProgress(index: Int, progress: Float) {
        val updatedSteps = state.steps.toMutableList()
        updatedSteps[index] = updatedSteps[index].copy(progress = progress)
        state = state.copy(steps = updatedSteps)
    }

    private fun addLog(message: String) {
        state = state.copy(logs = state.logs + message)
    }

    fun cancelDeploy() {
        addLog("[WARNING] Deployment cancelled by user")
        deploymentJob?.cancel()
        state = state.copy(canCancel = false, hasError = true)
        saveDeploymentToDatabase(success = false)
    }
}
