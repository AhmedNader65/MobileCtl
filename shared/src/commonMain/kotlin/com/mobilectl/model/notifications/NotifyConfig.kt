package com.mobilectl.model.notifications

import kotlinx.serialization.Serializable

@Serializable
data class NotifyConfig(
    val slack: SlackNotifyConfig = SlackNotifyConfig(),
    val email: EmailNotifyConfig = EmailNotifyConfig(),
    val webhook: WebhookNotifyConfig = WebhookNotifyConfig()
)
