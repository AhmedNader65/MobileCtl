package com.mobilectl.deploy

import com.mobilectl.model.deploy.AndroidDeployConfig
import com.mobilectl.model.deploy.DeployResult
import com.mobilectl.model.deploy.IosDeployConfig
import java.io.File

class DeployOrchestrator(
    private val androidStrategies: Map<String, UploadStrategy> = createDefaultAndroidStrategies(),
    private val iosStrategies: Map<String, UploadStrategy> = createDefaultIosStrategies()
) {

    suspend fun deployAndroid(
        config: AndroidDeployConfig,
        artifactPath: String
    ): DeployResult {
        return try {
            if (!config.enabled) {
                return DeployResult(
                    success = false,
                    platform = "android",
                    destination = config.destination,
                    message = "Android deploy is disabled in config"
                )
            }

            val strategy = androidStrategies[config.destination]
                ?: return DeployResult(
                    success = false,
                    platform = "android",
                    destination = config.destination,
                    message = "Unknown destination: ${config.destination}"
                )

            val uploadConfig = mapOf(
                "appId" to config.appId,
                "token" to config.token
            )

            val errors = strategy.validateConfig(uploadConfig)
            if (errors.isNotEmpty()) {
                return DeployResult(
                    success = false,
                    platform = "android",
                    destination = config.destination,
                    message = "Configuration error: ${errors.joinToString(", ")}"
                )
            }

            val artifactFile = File(artifactPath)
            strategy.upload(artifactFile, uploadConfig)

        } catch (e: Exception) {
            DeployResult(
                success = false,
                platform = "android",
                destination = config.destination,
                message = "Deployment failed: ${e.message}"
            )
        }
    }

    suspend fun deployIos(
        config: IosDeployConfig,
        artifactPath: String
    ): DeployResult {
        return try {
            if (!config.enabled) {
                return DeployResult(
                    success = false,
                    platform = "ios",
                    destination = config.destination,
                    message = "iOS deploy is disabled in config"
                )
            }

            val strategy = iosStrategies[config.destination]
                ?: return DeployResult(
                    success = false,
                    platform = "ios",
                    destination = config.destination,
                    message = "Unknown destination: ${config.destination}"
                )

            val uploadConfig = mapOf(
                "bundleId" to config.appId,
                "teamId" to config.teamId,
                "apiKey" to config.apiKey
            )

            val errors = strategy.validateConfig(uploadConfig)
            if (errors.isNotEmpty()) {
                return DeployResult(
                    success = false,
                    platform = "ios",
                    destination = config.destination,
                    message = "Configuration error: ${errors.joinToString(", ")}"
                )
            }

            val artifactFile = File(artifactPath)
            strategy.upload(artifactFile, uploadConfig)

        } catch (e: Exception) {
            DeployResult(
                success = false,
                platform = "ios",
                destination = config.destination,
                message = "Deployment failed: ${e.message}"
            )
        }
    }
}

expect fun createDefaultAndroidStrategies(): Map<String, UploadStrategy>
expect fun createDefaultIosStrategies(): Map<String, UploadStrategy>