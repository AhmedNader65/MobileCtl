package com.mobilectl.model.buildConfig

import com.mobilectl.model.buildConfig.KeystoreConfig
import com.mobilectl.model.buildConfig.OutputConfig
import kotlinx.serialization.Serializable


@Serializable
data class AndroidBuildConfig(
    val enabled: Boolean = true,
    val projectPath: String = ".",
    val gradleTask: String = "assembleRelease",
    val gradleProperties: Map<String, String> = emptyMap(),
    val keystore: KeystoreConfig? = null,
    val output: OutputConfig = OutputConfig()
)
