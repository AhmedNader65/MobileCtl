package com.mobilectl.model.versionManagement

import kotlinx.serialization.Serializable

@Serializable
data class VersionConfig(
    val autoIncrement: Boolean = false,
    val bumpStrategy: String = "semver",
    val filesToUpdate: List<String> = emptyList()
)