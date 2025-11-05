package com.mobilectl.model.changelog

import kotlinx.serialization.Serializable

@Serializable
data class CommitType(
    val type: String,        // feat, fix, docs, etc
    val title: String,       // Feature, Bug Fix, Documentation
    val emoji: String = ""   // 🎉, 🐛, 📚, etc
)

fun getDefaultCommitTypes(): List<CommitType> = listOf(
    CommitType("feat", "Features", "✨"),
    CommitType("fix", "Bug Fixes", "🐛"),
    CommitType("docs", "Documentation", "📚"),
    CommitType("style", "Style", "🎨"),
    CommitType("refactor", "Refactoring", "♻️"),
    CommitType("perf", "Performance", "⚡"),
    CommitType("test", "Tests", "✅"),
    CommitType("chore", "Chores", "🔧"),
    CommitType("ci", "CI/CD", "👷")
)