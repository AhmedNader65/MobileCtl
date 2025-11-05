package com.mobilectl.model.buildConfig

import com.mobilectl.model.buildConfig.KeystoreConfig
import com.mobilectl.model.buildConfig.OutputConfig
import kotlinx.serialization.Serializable


@Serializable
data class AndroidBuildConfig(
    val enabled: Boolean? = null,
    val projectPath: String = ".",
    var defaultFlavor: String = "",
    var defaultType: String = "release",
    val gradleTask: String = "assemble${if (defaultFlavor.isNotEmpty()) defaultFlavor.capitalize() else ""}${
        defaultType.capitalize()
    }",
    val gradleProperties: Map<String, String> = emptyMap(),
    val keystore: KeystoreConfig? = null,
    val output: OutputConfig = OutputConfig()
)
