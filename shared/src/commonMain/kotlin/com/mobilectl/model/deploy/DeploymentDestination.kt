package com.mobilectl.model.deploy

import kotlinx.serialization.Serializable

@Serializable
data class DeploymentDestination(
    val type: String = "",
    val config: Map<String, String> = emptyMap(),
    val enabled: Boolean = true
)