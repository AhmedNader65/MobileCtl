package com.mobilectl.model.buildConfig

import kotlinx.serialization.Serializable

@Serializable
data class OutputConfig(
    val format: String = "apk",
    val name: String = "app-release.apk"
)