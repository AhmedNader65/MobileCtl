package com.mobilectl.changelog

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class ChangelogState(
    val lastGeneratedCommit: String? = null,      // Last commit hash we processed
    val lastGeneratedDate: String? = null,        // When we generated it
    val lastGeneratedVersion: String? = null,     // Version it was for
    val lastGeneratedRange: String? = null        // Tag range used (v1.0.0..v1.1.0)
)

interface ChangelogStateManager {
    fun getState(): ChangelogState
    fun saveState(state: ChangelogState): Boolean
}

expect fun createChangelogStateManager(): ChangelogStateManager
