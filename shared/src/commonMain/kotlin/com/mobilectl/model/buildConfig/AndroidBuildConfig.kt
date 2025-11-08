package com.mobilectl.model.buildConfig

import com.mobilectl.model.buildConfig.KeystoreConfig
import com.mobilectl.model.buildConfig.OutputConfig
import kotlinx.serialization.Serializable


@Serializable
data class AndroidBuildConfig(
    val enabled: Boolean = true,
    var defaultFlavor: String = "",
    var defaultType: String = "release",
    val flavors: List<String> = emptyList(),
    val outputType: String = "aab",              // "apk" or "aab" (App Bundle) - default for all platforms
    val firebaseOutputType: String? = null,      // Optional override for Firebase App Distribution
    val keyStore: String = "keystore.jks",       // Path to keystore
    val keyAlias: String = "",                   // Alias in keystore
    val keyPassword: String = "",                // Key password (from env)
    val storePassword: String = "",              // Keystore password (from env)

    val useEnvForPasswords: Boolean = true
)
