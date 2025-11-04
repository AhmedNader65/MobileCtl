package com.mobilectl.model.buildConfig

import kotlinx.serialization.Serializable

@Serializable
data class KeystoreConfig(
    val path: String = "",
    val alias: String = "",
    val storePassword: String = "",
    val keyPassword: String = ""
)