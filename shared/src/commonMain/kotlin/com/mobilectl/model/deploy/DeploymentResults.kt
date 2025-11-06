package com.mobilectl.model.deploy


/**
 * Result from deploying to all destinations
 */
data class DeploymentResults(
    val platform: String,
    val individual: List<DeployResult>,
    val successCount: Int = individual.count { it.success },
    val failureCount: Int = individual.count { !it.success }
) {
    val success: Boolean
        get() = failureCount == 0

    val message: String
        get() = "$successCount/${individual.size} successful${
            if (failureCount > 0) ", $failureCount failed" else ""
        }"
}