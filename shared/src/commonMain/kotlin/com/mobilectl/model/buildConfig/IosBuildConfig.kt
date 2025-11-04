package com.mobilectl.model.buildConfig

import com.mobilectl.model.buildConfig.OutputConfig
import kotlinx.serialization.Serializable

@Serializable
data class IosBuildConfig(
    val enabled: Boolean = true,
    val projectPath: String = ".",
    val scheme: String = "",
    val configuration: String = "Release",
    val destination: String = "generic/platform=iOS",
    val codeSignIdentity: String? = null,
    val provisioningProfile: String? = null,
    val output: OutputConfig = OutputConfig()
)