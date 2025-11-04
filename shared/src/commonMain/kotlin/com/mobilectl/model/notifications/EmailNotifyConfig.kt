package com.mobilectl.model.notifications

import kotlinx.serialization.Serializable

@Serializable
data class EmailNotifyConfig(
    val enabled: Boolean = false,
    val recipients: List<String> = emptyList(),
    val notifyOn: List<String> = listOf("failure")
)