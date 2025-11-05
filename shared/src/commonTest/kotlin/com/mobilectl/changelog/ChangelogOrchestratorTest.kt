package com.mobilectl.changelog

import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.changelog.CommitType
import com.mobilectl.model.changelog.GitCommit
import com.mobilectl.model.changelog.ReleaseNotes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChangelogOrchestratorTest {

    private val mockParser = MockGitParser()
    private val mockWriter = MockChangelogWriter()
    private val mockStateManager = MockStateManager()

    private val config = ChangelogConfig(
        enabled = true,
        format = "markdown",
        outputFile = "CHANGELOG.md",
        commitTypes = listOf(
            CommitType("feat", "Features", "✨"),
            CommitType("fix", "Bug Fixes", "🐛")
        )
    )

    private val orchestrator = ChangelogOrchestrator(
        mockParser,
        mockWriter,
        mockStateManager,
        config
    )

    @Test
    fun testGenerateSuccessfully() {
        val result = orchestrator.generate(dryRun = false)

        assertTrue(result.success)
        assertEquals(3, result.commitCount)
        assertTrue(result.content?.contains("Features") ?: false)
    }

    @Test
    fun testGenerateDryRun() {
        val result = orchestrator.generate(dryRun = true)

        assertTrue(result.success)
        assertEquals(3, result.commitCount)
        // Content should be populated in dry run
        assertTrue(result.content?.isNotEmpty() ?: false)
    }

    @Test
    fun testGenerateWithFromTag() {
        val result = orchestrator.generate(fromTag = "v1.0.0", dryRun = true)

        assertTrue(result.success)
    }

    @Test
    fun testGenerateNoCommits() {
        val emptyParser = MockGitParser(commits = emptyList())
        val orchestrator = ChangelogOrchestrator(
            emptyParser,
            mockWriter,
            mockStateManager,
            config
        )

        val result = orchestrator.generate(dryRun = true)

        assertTrue(!result.success)
        assertTrue(result.error?.contains("No commits") ?: false)
    }

    @Test
    fun testGenerateMarkdownFormat() {
        val result = orchestrator.generate(dryRun = true)

        assertTrue(result.success)
        val markdown = result.content ?: ""

        // Check markdown structure
        assertTrue(markdown.contains("# Changelog"))
        assertTrue(markdown.contains("## ["))
        assertTrue(markdown.contains("### ✨ Features"))
        assertTrue(markdown.contains("### 🐛 Bug Fixes"))
    }

    @Test
    fun testGenerateWithReleaseNotes() {
        val configWithNotes = config.copy(
            releases = mapOf(
                "1.5.0" to ReleaseNotes(
                    highlights = "Major update",
                    breaking_changes = listOf("API v1 removed")
                )
            )
        )

        val orchestrator = ChangelogOrchestrator(
            mockParser,
            mockWriter,
            mockStateManager,
            configWithNotes
        )

        val result = orchestrator.generate(dryRun = true)

        assertTrue(result.success)
        assertTrue(result.content?.contains("Major update") ?: false)
    }

    @Test
    fun testGenerateWithBreakingChanges() {
        val breaksParser = MockGitParser(
            commits = listOf(
                GitCommit("1", "1", "feat", null, "New API", breaking = true),
                GitCommit("2", "2", "fix", null, "Bug fix", breaking = false)
            )
        )

        val orchestrator = ChangelogOrchestrator(
            breaksParser,
            mockWriter,
            mockStateManager,
            config
        )

        val result = orchestrator.generate(dryRun = true)

        assertTrue(result.success)
        assertTrue(result.content?.contains("BREAKING") ?: false)
    }

    private class MockGitParser(val commits: List<GitCommit> = defaultCommits) : GitCommitParser {
        override fun parseCommits(fromTag: String?, toTag: String?): List<GitCommit> = commits
        override fun parseCommitsSinceHash(sinceHash: String): List<GitCommit> = commits
        override fun getLatestTag(): String? = "v1.5.0"
        override fun getAllTags(): List<String> = listOf("v1.5.0", "v1.4.9")
        override fun getTagDate(tag: String) = java.time.LocalDate.now()
        override fun parseCommitMessage(message: String, commitTypes: List<CommitType>) = null
        override fun getCompareUrl(fromTag: String, toTag: String) = "https://example.com/compare"

        companion object {
            val defaultCommits = listOf(
                GitCommit("1", "1", "feat", "auth", "Add login", author = "Ahmed", breaking = false),
                GitCommit("2", "2", "feat", "ui", "Add dark mode", author = "Jane", breaking = false),
                GitCommit("3", "3", "fix", "cache", "Fix leak", author = "John", breaking = false)
            )
        }
    }

    private class MockChangelogWriter : ChangelogWriter {
        override fun write(content: String, filePath: String) = true
        override fun read(filePath: String) = null
    }

    private class MockStateManager : ChangelogStateManager {
        override fun getState() = ChangelogState()
        override fun saveState(state: ChangelogState) = true
    }

    private class MockBackupManager : ChangelogBackupManager {
        override fun createBackup(filePath: String) = Result.success(
            BackupInfo("id", java.time.LocalDateTime.now(), filePath, 100)
        )
        override fun listBackups(filePath: String): List<BackupInfo> = emptyList()
        override fun restoreBackup(backupId: String, filePath: String) = Result.success(true)
        override fun deleteBackup(backupId: String) = Result.success(true)
        override fun deleteOldBackups(keepCount: Int) = Result.success(0)
    }
}
