package com.mobilectl.model.changelog

import kotlinx.serialization.Serializable

@Serializable
data class ChangelogConfig(
    val enabled: Boolean = true,
    val format: String = "markdown",
    val includeConventionalCommits: Boolean = true,
    val commitTypes: List<CommitType> = getDefaultCommitTypes(),
    val outputFile: String = "CHANGELOG.md"
)