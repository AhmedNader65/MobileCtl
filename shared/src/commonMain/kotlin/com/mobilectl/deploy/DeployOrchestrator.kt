package com.mobilectl.deploy

import com.mobilectl.model.deploy.AndroidDeployConfig
import com.mobilectl.model.deploy.DeployResult
import com.mobilectl.model.deploy.DeploymentResults
import com.mobilectl.model.deploy.IosDeployConfig
import java.io.File

/**
 * Orchestrates multi-destination deployments
 * Handles Android and iOS platforms with multiple deployment targets
 */
interface DeployOrchestrator {

    /**
     * Deploy to all enabled Android destinations
     */
    suspend fun deployAndroid(
        config: AndroidDeployConfig,
        artifacts: List<File>,
        containsAPK: Boolean = false
    ): DeploymentResults

    /**
     * Deploy to all enabled iOS destinations
     */
    suspend fun deployIos(
        config: IosDeployConfig,
        artifactPath: String
    ): DeploymentResults
}

/**
 * Factory function to create platform-specific implementation
 */
expect fun createDeployOrchestrator(): DeployOrchestrator

expect fun createDefaultAndroidDestinations(): Map<String, suspend (File, Map<String, String>) -> DeployResult>
expect fun createDefaultIosDestinations(): Map<String, suspend (File, Map<String, String>) -> DeployResult>
