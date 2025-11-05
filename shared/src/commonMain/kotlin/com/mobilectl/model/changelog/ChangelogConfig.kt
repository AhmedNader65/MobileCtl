package com.mobilectl.model.changelog

import kotlinx.serialization.Serializable

@Serializable
data class ChangelogConfig(
    val enabled: Boolean = true,
    val format: String = "markdown",  // markdown
    val outputFile: String = "CHANGELOG.md",
    val includeBreakingChanges: Boolean = true,
    val includeContributors: Boolean = true,
    val includeStats: Boolean = true,
    val includeCompareLinks: Boolean = true,
    val groupByVersion: Boolean = true,
    val releases: Map<String, ReleaseNotes> = emptyMap(),  // version -> release notes
    val commitTypes: List<CommitType> = getDefaultCommitTypes()
)