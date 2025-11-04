package com.mobilectl.model.changelog

import kotlinx.serialization.Serializable

@Serializable
data class CommitType(
    val type: String = "",
    val title: String = "",
    val emoji: String = ""
)

fun getDefaultCommitTypes() = listOf(
    CommitType("feat", "Features", "✨"),
    CommitType("fix", "Bug Fixes", "🐛"),
    CommitType("docs", "Documentation", "📚"),
    CommitType("perf", "Performance", "⚡"),
    CommitType("test", "Tests", "✅"),
    CommitType("chore", "Chores", "🔧")
)
