package com.mobilectl.model.deploy

import kotlinx.serialization.Serializable

@Serializable
data class DeployConfig(
    val enabled: Boolean = false,
    val android: AndroidDeployConfig? = null,
    val ios: IosDeployConfig? = null,
    val flavorGroups: Map<String, FlavorGroup> = emptyMap(),
    val defaultGroup: String? = null
)

@Serializable
data class FlavorGroup(
    val name: String = "",
    val description: String = "",
    val flavors: List<String> = emptyList()
)

@Serializable
data class AndroidDeployConfig(
    val enabled: Boolean = true,

    val artifactPath: String = "build/outputs/apk/release/app-release.apk",

    val firebase: FirebaseAndroidDestination = FirebaseAndroidDestination(),
    val playConsole: PlayConsoleAndroidDestination = PlayConsoleAndroidDestination(),
    val local: LocalAndroidDestination = LocalAndroidDestination()
)
/**
* Firebase App Distribution
*/
@Serializable
data class FirebaseAndroidDestination(
    val enabled: Boolean = true,
    val serviceAccount: String = "credentials/firebase-service-account.json",
    val googleServices: String? = null,
    var releaseNotes: String = "Automated upload",
    var testGroups: List<String> = listOf("qa-team")
)

/**
 * Google Play Console
 */
@Serializable
data class PlayConsoleAndroidDestination(
    val enabled: Boolean = false,
    val serviceAccount: String = "credentials/play-console-service-account.json",
    val packageName: String = "",
    val track: String = "internal",
    val status: String = "draft"
)

/**
 * Local Filesystem
 */
@Serializable
data class LocalAndroidDestination(
    val enabled: Boolean = false,
    val outputDir: String = "build/deploy"
)

@Serializable
data class IosDeployConfig(
    val enabled: Boolean = true,

    // IPA path
    val artifactPath: String = "build/outputs/ipa/release/app.ipa",

    // Multiple destination configs
    val testflight: TestFlightDestination = TestFlightDestination(),
    val appStore: AppStoreDestination = AppStoreDestination()
)

/**
 * TestFlight
 */
@Serializable
data class TestFlightDestination(
    val enabled: Boolean = true,
    val apiKeyPath: String = "credentials/app-store-connect-api-key.json",
    val bundleId: String = "",
    val teamId: String = ""
)

/**
 * App Store
 */
@Serializable
data class AppStoreDestination(
    val enabled: Boolean = false,
    val apiKeyPath: String = "credentials/app-store-connect-api-key.json",
    val bundleId: String = "",
    val teamId: String = ""
)

data class DeployResult(
    val success: Boolean,
    val platform: String,  // "android" or "ios"
    val destination: String,
    val message: String,
    val error: Exception? = null,
    val buildUrl: String? = null,
    val buildId: String? = null,
    val duration: Long = 0  // milliseconds
)
