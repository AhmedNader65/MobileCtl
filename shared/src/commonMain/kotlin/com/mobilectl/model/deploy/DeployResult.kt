package com.mobilectl.model.deploy

/**
 * Result of deploying to a single destination
 */
//data class DeployResult(
//    val success: Boolean,
//    val destination: String,
//    val url: String? = null,
//    val error: String? = null,
//    val durationMs: Long = 0
//)

/**
 * Aggregated result from multiple destinations
 */
data class MultiDeployResult(
    val success: Boolean,
    val individual: List<DeployResult>
)
