package com.mobilectl.changelog

import kotlinx.serialization.json.Json

class JvmChangelogStateManager : ChangelogStateManager {
    private val stateFile = java.io.File(".mobilectl/changelog-state.json")
    private val json = Json { prettyPrint = true }

    override fun getState(): ChangelogState {
        return try {
            if (!stateFile.exists()) return ChangelogState()

            val content = stateFile.readText()
            json.decodeFromString<ChangelogState>(content)
        } catch (e: Exception) {
            ChangelogState()
        }
    }

    override fun saveState(state: ChangelogState): Boolean {
        return try {
            stateFile.parentFile?.mkdirs()
            val content = json.encodeToString(ChangelogState.serializer(), state)
            stateFile.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }
}

actual fun createChangelogStateManager(): ChangelogStateManager = JvmChangelogStateManager()
