package com.mobilectl.model.deploy

import kotlinx.serialization.Serializable

@Serializable
data class DeployConfig(
    val enabled: Boolean = false,
    val android: AndroidDeployConfig? = null,
    val ios: IosDeployConfig? = null
)

@Serializable
data class AndroidDeployConfig(
    val enabled: Boolean = true,
    val destination: String = "firebase",  // firebase, google-play
    val appId: String = "",
    val token: String = "",  // Firebase token or API key
    val artifactPath: String = ""  // Path to APK/AAB
)

@Serializable
data class IosDeployConfig(
    val enabled: Boolean = true,
    val destination: String = "testflight",  // testflight, app-store
    val appId: String = "",  // Bundle ID
    val teamId: String = "",  // Apple Team ID
    val apiKey: String = "",  // App Store Connect API key
    val artifactPath: String = ""  // Path to IPA
)

data class DeployResult(
    val success: Boolean,
    val platform: String,  // "android" or "ios"
    val destination: String,
    val message: String,
    val buildUrl: String? = null,
    val buildId: String? = null,
    val duration: Long = 0  // milliseconds
)
