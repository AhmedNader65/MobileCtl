package com.mobilectl.model.changelog

import kotlinx.serialization.Serializable


@Serializable
data class ReleaseNotes(
    val highlights: String? = null,
    val breaking_changes: List<String> = emptyList(),
    val contributors: List<String> = emptyList()
)