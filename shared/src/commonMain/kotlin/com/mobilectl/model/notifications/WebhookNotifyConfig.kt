package com.mobilectl.model.notifications

import kotlinx.serialization.Serializable

@Serializable
data class WebhookNotifyConfig(
    val enabled: Boolean = false,
    val url: String = "",
    val events: List<String> = emptyList()
)