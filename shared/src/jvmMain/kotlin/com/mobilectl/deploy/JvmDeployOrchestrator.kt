package com.mobilectl.deploy

import com.mobilectl.model.deploy.*
import java.io.File

/**
 * JVM implementation of DeployOrchestrator
 *
 * Orchestrates multi-destination deployments using factory pattern
 */
class JvmDeployOrchestrator(
    private val androidDestinationFactories: Map<String, suspend (File, Map<String, String>) -> DeployResult> = createDefaultAndroidDestinations(),
    private val iosDestinationFactories: Map<String, suspend (File, Map<String, String>) -> DeployResult> = createDefaultIosDestinations()
) : DeployOrchestrator {

    /**
     * Deploy to all enabled Android destinations
     */
    override suspend fun deployAndroid(
        config: AndroidDeployConfig,
        artifacts: List<File>,
        containsAPK: Boolean
    ): DeploymentResults {
        return try {
            if (!config.enabled) {
                return DeploymentResults(
                    platform = "android",
                    individual = listOf(
                        DeployResult(
                            success = false,
                            platform = "android",
                            destination = "all",
                            message = "Android deployment is disabled"
                        )
                    )
                )
            }

            val results = mutableListOf<DeployResult>()
            val apkFile = artifacts.firstOrNull { it.extension == "apk" }
            val aabFile = artifacts.firstOrNull { it.extension == "aab" }

            // Deploy to Firebase if enabled
            if (config.firebase.enabled) {
                val fileToDeployToFirebase = if (containsAPK) {
                    apkFile
                } else {
                    aabFile
                }
                if (fileToDeployToFirebase == null || !fileToDeployToFirebase.exists()) {
                    results.add(
                        DeployResult(
                            success = false,
                            platform = "android",
                            destination = "firebase",
                            message = "No APK or AAB artifact found for Firebase deployment"
                        )
                    )
                } else {
                    val result = deployToDestination("firebase", fileToDeployToFirebase, config.firebase)
                    results.add(result)
                }
            }

            // Deploy to Play Console if enabled
            if (config.playConsole.enabled) {
                if (aabFile == null || !aabFile.exists()) {
                    results.add(
                        DeployResult(
                            success = false,
                            platform = "android",
                            destination = "play-console",
                            message = "No AAB artifact found for Play Console deployment"
                        )
                    )
                } else {
                    val result = deployToDestination("play-console", aabFile, config.playConsole)
                    results.add(result)
                }
            }

            // Deploy locally if enabled
            if (config.local.enabled) {
                if (apkFile == null || !apkFile.exists()) {
                    results.add(
                        DeployResult(
                            success = false,
                            platform = "android",
                            destination = "local",
                            message = "No APK artifact found for local deployment"
                        )
                    )
                } else {
                    val result = deployToDestination("local", apkFile, config.local)
                    results.add(result)
                }
            }

            // Return all results individually (NOT aggregated)
            DeploymentResults(
                platform = "android",
                individual = results
            )

        } catch (e: Exception) {
            DeploymentResults(
                platform = "android",
                individual = listOf(
                    DeployResult(
                        success = false,
                        platform = "android",
                        destination = "all",
                        message = "Deployment failed: ${e.message}",
                        error = e
                    )
                )
            )
        }
    }

    /**
     * Deploy to all enabled iOS destinations
     */
    override suspend fun deployIos(
        config: IosDeployConfig,
        artifactPath: String
    ): DeploymentResults {
        return try {
            if (!config.enabled) {
                return DeploymentResults(
                    platform = "ios",
                    individual = listOf(
                        DeployResult(
                            success = false,
                            platform = "ios",
                            destination = "all",
                            message = "iOS deployment is disabled"
                        )
                    )
                )
            }

            val artifactFile = File(artifactPath)
            if (!artifactFile.exists()) {
                return DeploymentResults(
                    platform = "ios",
                    individual = listOf(
                        DeployResult(
                            success = false,
                            platform = "ios",
                            destination = "all",
                            message = "Artifact not found: $artifactPath"
                        )
                    )
                )
            }

            val results = mutableListOf<DeployResult>()

            // Deploy to TestFlight if enabled
            if (config.testflight.enabled) {
                val result = deployToDestination("testflight", artifactFile, config.testflight)
                results.add(result)
            }

            // Deploy to App Store if enabled
            if (config.appStore.enabled) {
                val result = deployToDestination("app-store", artifactFile, config.appStore)
                results.add(result)
            }

            DeploymentResults(
                platform = "ios",
                individual = results
            )

        } catch (e: Exception) {
            DeploymentResults(
                platform = "ios",
                individual = listOf(
                    DeployResult(
                        success = false,
                        platform = "ios",
                        destination = "all",
                        message = "Deployment failed: ${e.message}",
                        error = e
                    )
                )
            )
        }
    }

    /**
     * Deploy to a specific destination using the factory
     */
    private suspend fun deployToDestination(
        destinationName: String,
        artifactFile: File,
        config: Any
    ): DeployResult {
        return try {
            // Get the factory for this destination
            val factory = androidDestinationFactories[destinationName]
                ?: iosDestinationFactories[destinationName]
                ?: return DeployResult(
                    success = false,
                    platform = "unknown",
                    destination = destinationName,
                    message = "Unknown destination: $destinationName"
                )

            // Build config for the destination
            val deployConfig = buildDestinationConfig(destinationName, config)

            // Validate config
            val errors = validateDestinationConfig(destinationName, deployConfig)
            if (errors.isNotEmpty()) {
                return DeployResult(
                    success = false,
                    platform = "unknown",
                    destination = destinationName,
                    message = "Configuration error: ${errors.joinToString(", ")}"
                )
            }

            // Execute deployment using the factory
            factory(artifactFile, deployConfig)

        } catch (e: Exception) {
            DeployResult(
                success = false,
                platform = "unknown",
                error = e,
                destination = destinationName,
                message = "Deployment failed: ${e.message}"
            )
        }
    }

    /**
     * Build deployment config for the destination
     */
    private fun buildDestinationConfig(
        destinationName: String,
        config: Any
    ): Map<String, String> {
        return when {
            // Android destinations
            destinationName == "firebase" && config is FirebaseAndroidDestination -> mapOf(
                "serviceAccount" to config.serviceAccount,
                "testGroups" to config.testGroups.joinToString(","),
                "releaseNotes" to config.releaseNotes
            )

            destinationName == "play-console" && config is PlayConsoleAndroidDestination -> mapOf(
                "serviceAccount" to config.serviceAccount,
                "packageName" to config.packageName
            )

            destinationName == "local" && config is LocalAndroidDestination -> mapOf(
                "outputDir" to config.outputDir
            )

            // iOS destinations
            destinationName == "testflight" && config is TestFlightDestination -> mapOf(
                "apiKeyPath" to config.apiKeyPath,
                "bundleId" to config.bundleId,
                "teamId" to config.teamId
            )

            destinationName == "app-store" && config is AppStoreDestination -> mapOf(
                "apiKeyPath" to config.apiKeyPath,
                "bundleId" to config.bundleId,
                "teamId" to config.teamId
            )

            else -> emptyMap()
        }
    }

    /**
     * Validate destination configuration
     */
    private fun validateDestinationConfig(
        destinationName: String,
        config: Map<String, String>
    ): List<String> {
        val errors = mutableListOf<String>()

        when (destinationName) {
            "firebase" -> {
                if (config["serviceAccount"].isNullOrBlank()) {
                    errors.add("Firebase: serviceAccount is required")
                }
            }

            "play-console" -> {
                if (config["serviceAccount"].isNullOrBlank()) {
                    errors.add("Play Console: serviceAccount is required")
                }
                if (config["packageName"].isNullOrBlank()) {
                    errors.add("Play Console: packageName is required")
                }
            }

            "testflight" -> {
                if (config["apiKeyPath"].isNullOrBlank()) {
                    errors.add("TestFlight: apiKeyPath is required")
                }
                if (config["bundleId"].isNullOrBlank()) {
                    errors.add("TestFlight: bundleId is required")
                }
            }

            "app-store" -> {
                if (config["apiKeyPath"].isNullOrBlank()) {
                    errors.add("App Store: apiKeyPath is required")
                }
                if (config["bundleId"].isNullOrBlank()) {
                    errors.add("App Store: bundleId is required")
                }
            }
        }

        return errors
    }
}

/**
 * Factory implementation for JVM
 */
actual fun createDeployOrchestrator(): DeployOrchestrator {
    return JvmDeployOrchestrator()
}
