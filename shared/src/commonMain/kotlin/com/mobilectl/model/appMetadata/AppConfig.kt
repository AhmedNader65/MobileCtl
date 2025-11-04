package com.mobilectl.model.appMetadata

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val name: String? = null,
    val identifier: String? = null,
    val version: String? = null
)