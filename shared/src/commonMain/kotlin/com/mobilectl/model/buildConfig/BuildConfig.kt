package com.mobilectl.model.buildConfig

import kotlinx.serialization.Serializable


@Serializable
data class BuildConfig(
    val android: AndroidBuildConfig = AndroidBuildConfig(),
    val ios: IosBuildConfig = IosBuildConfig()
)