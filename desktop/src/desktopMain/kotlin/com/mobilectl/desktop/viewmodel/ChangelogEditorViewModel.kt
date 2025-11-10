package com.mobilectl.desktop.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilectl.changelog.ChangelogOrchestrator
import com.mobilectl.changelog.GitCommitParser
import com.mobilectl.changelog.JGitCommitParser
import com.mobilectl.changelog.JvmChangelogWriter
import com.mobilectl.changelog.createChangelogStateManager
import com.mobilectl.config.ConfigLoader
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.util.JvmFileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ChangelogEditorState(
    val content: String = "",
    val savedContent: String = "",
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val hasUnsavedChanges: Boolean = false,
    val changelogPath: String = "CHANGELOG.md",
    val fromTag: String? = null,
    val toTag: String? = null,
    val previewMode: Boolean = false,
    val commitCount: Int = 0
)

class ChangelogEditorViewModel : ViewModel() {
    var state by mutableStateOf(ChangelogEditorState())
        private set

    private val configLoader = ConfigLoader(
        fileUtil = JvmFileUtil(),
        projectDetector = createProjectDetector()
    )

    init {
        loadExistingChangelog()
    }

    /**
     * Load existing changelog file
     */
    private fun loadExistingChangelog() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val changelogFile = File(System.getProperty("user.dir"), state.changelogPath)
                    if (changelogFile.exists()) {
                        val content = changelogFile.readText()
                        state = state.copy(
                            content = content,
                            savedContent = content
                        )
                    }
                } catch (e: Exception) {
                    state = state.copy(error = "Failed to load changelog: ${e.message}")
                }
            }
        }
    }

    /**
     * Generate changelog from git commits
     */
    fun generateChangelog(append: Boolean = false) {
        state = state.copy(isGenerating = true, error = null, successMessage = null)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val configResult = configLoader.loadConfig(null)
                    if (configResult.isFailure) {
                        state = state.copy(
                            isGenerating = false,
                            error = "Failed to load config: ${configResult.exceptionOrNull()?.message}"
                        )
                        return@withContext
                    }

                    val config = configResult.getOrNull()
                    val changelogConfig = config?.changelog ?: run {
                        state = state.copy(
                            isGenerating = false,
                            error = "Changelog configuration not found in mobilectl.yaml"
                        )
                        return@withContext
                    }

                    val orchestrator = ChangelogOrchestrator(
                        parser = JGitCommitParser(),
                        writer = JvmChangelogWriter(),
                        stateManager = createChangelogStateManager(),
                        config = changelogConfig
                    )

                    val result = orchestrator.generate(
                        fromTag = state.fromTag,
                        toTag = state.toTag,
                        dryRun = false,
                        append = append,
                        useLastState = state.fromTag == null
                    )

                    if (result.success) {
                        // Reload the changelog file
                        val changelogFile = File(System.getProperty("user.dir"), state.changelogPath)
                        val newContent = changelogFile.readText()

                        state = state.copy(
                            isGenerating = false,
                            content = newContent,
                            savedContent = newContent,
                            successMessage = "Changelog generated successfully (${result.commitCount} commits)",
                            commitCount = result.commitCount
                        )
                    } else {
                        state = state.copy(
                            isGenerating = false,
                            error = result.error ?: "Failed to generate changelog"
                        )
                    }

                } catch (e: Exception) {
                    state = state.copy(
                        isGenerating = false,
                        error = "Failed to generate changelog: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Update changelog content (for manual editing)
     */
    fun updateContent(newContent: String) {
        state = state.copy(
            content = newContent,
            hasUnsavedChanges = newContent != state.savedContent
        )
    }

    /**
     * Save changelog to file
     */
    fun saveChangelog() {
        state = state.copy(isSaving = true, error = null, successMessage = null)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val changelogFile = File(System.getProperty("user.dir"), state.changelogPath)
                    changelogFile.writeText(state.content)

                    state = state.copy(
                        isSaving = false,
                        savedContent = state.content,
                        hasUnsavedChanges = false,
                        successMessage = "Changelog saved successfully"
                    )

                } catch (e: Exception) {
                    state = state.copy(
                        isSaving = false,
                        error = "Failed to save changelog: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Toggle preview mode
     */
    fun togglePreview() {
        state = state.copy(previewMode = !state.previewMode)
    }

    /**
     * Update tag filters
     */
    fun updateFromTag(tag: String?) {
        state = state.copy(fromTag = tag?.takeIf { it.isNotBlank() })
    }

    fun updateToTag(tag: String?) {
        state = state.copy(toTag = tag?.takeIf { it.isNotBlank() })
    }

    /**
     * Clear messages
     */
    fun clearMessages() {
        state = state.copy(error = null, successMessage = null)
    }

    /**
     * Discard unsaved changes
     */
    fun discardChanges() {
        state = state.copy(
            content = state.savedContent,
            hasUnsavedChanges = false
        )
    }

    /**
     * Refresh from file
     */
    fun refresh() {
        loadExistingChangelog()
    }
}
