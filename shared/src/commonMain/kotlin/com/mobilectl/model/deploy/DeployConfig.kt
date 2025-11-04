package com.mobilectl.model.deploy

import kotlinx.serialization.Serializable

@Serializable
data class DeployConfig(
    val destinations: List<DeploymentDestination> = listOf(
        DeploymentDestination(
            type = "local",
            config = mapOf("path" to "./builds")
        )
    )
)