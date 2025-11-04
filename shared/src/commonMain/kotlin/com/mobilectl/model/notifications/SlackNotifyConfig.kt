package com.mobilectl.model.notifications

import kotlinx.serialization.Serializable

@Serializable
data class SlackNotifyConfig(
    val enabled: Boolean = false,
    val webhookUrl: String = "",
    val channel: String? = null,
    val notifyOn: List<String> = listOf("success", "failure")
)