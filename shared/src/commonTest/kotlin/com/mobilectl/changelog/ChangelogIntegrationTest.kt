package com.mobilectl.changelog

import com.mobilectl.config.Config
import com.mobilectl.model.ValidationSeverity
import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.changelog.CommitType
import com.mobilectl.model.changelog.GitCommit
import com.mobilectl.validation.ChangelogConfigValidator
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChangelogIntegrationTest {

    private val testDir = File(".mobilectl/test-changelog-integration")

    @Test
    fun testCompleteWorkflow() = runBlocking {
        setup()

        try {
            // 1. Create config (as it would be loaded)
            val config = Config(
                changelog = ChangelogConfig(
                    enabled = true,
                    format = "markdown",
                    outputFile = testDir.resolve("CHANGELOG.md").absolutePath,
                    commitTypes = listOf(
                        CommitType("feat", "Features", "✨"),
                        CommitType("fix", "Bug Fixes", "🐛"),
                        CommitType("docs", "Documentation", "📚")
                    )
                )
            )

            // 2. Validate config (this is what ConfigLoader does)
            val validator = ChangelogConfigValidator()
            val errors = validator.validate(config)
            assertEquals(0, errors.count { it.severity == ValidationSeverity.ERROR })

            // 3. Create orchestrator with validated config
            val parser = MockParser()
            val backupManager = FileSystemBackupManager()
            val writer = SafeChangelogWriter(backupManager)
            val stateManager = FileStateManager(testDir)

            val orchestrator = ChangelogOrchestrator(
                parser,
                writer,
                stateManager,
                config.changelog!!  // ← Pass ChangelogConfig directly, no validator
            )

            // 4. Test dry run
            println("=== PHASE 1: Dry Run ===")
            val dryRun = orchestrator.generate(dryRun = true)
            assertTrue(dryRun.success, "Dry run should succeed")
            assertTrue(dryRun.content?.contains("Features") ?: false, "Should contain Features")
            assertEquals(3, dryRun.commitCount, "Should have 3 commits")

            // 5. Test real generation
            println("=== PHASE 2: First Generation ===")
            val result = orchestrator.generate(dryRun = false)
            assertTrue(result.success, "Generation should succeed")
            assertEquals(3, result.commitCount, "Should have 3 commits")

            // 6. Verify file created
            val changelogFile = File(config.changelog!!.outputFile)
            assertTrue(changelogFile.exists(), "Changelog file should exist")

            val content = changelogFile.readText()
            println("Generated (${content.length} chars):\n$content\n")

            assertTrue(content.contains("# Changelog"), "Should have header")
            assertTrue(content.contains("✨ Features"), "Should have Features")
            assertTrue(content.contains("🐛 Bug Fixes"), "Should have Bug Fixes")
            assertTrue(content.contains("Ahmed"), "Should have contributor")

            // 7. Verify state saved
            val stateFile = testDir.resolve(".mobilectl/changelog-state.json")
            assertTrue(stateFile.exists(), "State file should exist")

            // 8. Test append
            println("=== PHASE 3: Second Generation (Append) ===")
            val result2 = orchestrator.generate(dryRun = false, append = true)
            assertTrue(result2.success, "Append should succeed")

            val content2 = changelogFile.readText()
            println("After append (${content2.length} chars)")

            assertTrue(content2.length > content.length, "Should be longer after append")
            val featureCount = content2.split("### ✨ Features").size - 1
            assertEquals(2, featureCount, "Should have 2 feature sections")

            // 9. Test backup/restore
            println("=== PHASE 4: Backup/Restore ===")
            val backups = backupManager.listBackups(config.changelog!!.outputFile)
            assertTrue(backups.isNotEmpty(), "Should have backups")
            println("Backups: ${backups.size}")

            // Corrupt file
            changelogFile.writeText("CORRUPTED")

            // Restore
            val restoreResult = backupManager.restoreBackup(backups.first().id, config.changelog!!.outputFile)
            assertTrue(restoreResult.isSuccess, "Restore should succeed")

            val restoredContent = changelogFile.readText()
            assertTrue(restoredContent.isNotEmpty(), "Restored content should not be empty")
            assertTrue(restoredContent.contains("# Changelog"), "Should have structure")
            assertFalse(restoredContent.contains("CORRUPTED"), "Should not have corrupted data")

            println("\n✅ All integration tests passed!")

        } finally {
            cleanup()
        }
    }

    @Test
    fun testConfigValidationIntegration() = runBlocking {
        setup()

        try {
            // Invalid config
            val invalidConfig = Config(
                changelog = ChangelogConfig(
                    enabled = true,
                    format = "json",  // Invalid!
                    outputFile = "",  // Empty!
                    commitTypes = emptyList()  // Empty!
                )
            )

            val validator = ChangelogConfigValidator()
            val errors = validator.validate(invalidConfig)

            val criticalErrors = errors.filter { it.severity == ValidationSeverity.ERROR }
            assertTrue(criticalErrors.isNotEmpty(), "Should have critical errors")
            assertTrue(criticalErrors.any { it.field == "changelog.format" })
            assertTrue(criticalErrors.any { it.field == "changelog.output_file" })
            assertTrue(criticalErrors.any { it.field == "changelog.commit_types" })

        } finally {
            cleanup()
        }
    }

    @Test
    fun testErrorHandling() = runBlocking {
        setup()

        try {
            val config = Config(
                changelog = ChangelogConfig(
                    enabled = true,
                    format = "markdown",
                    outputFile = testDir.resolve("CHANGELOG.md").absolutePath,
                    commitTypes = listOf(CommitType("feat", "Features", "✨"))
                )
            )

            // Parser with no commits
            val emptyParser = MockParser(commits = emptyList())
            val backupManager = FileSystemBackupManager()
            val writer = SafeChangelogWriter(backupManager)
            val stateManager = FileStateManager(testDir)

            val orchestrator = ChangelogOrchestrator(
                emptyParser,
                writer,
                stateManager,
                config.changelog!!
            )

            // Should fail
            val result = orchestrator.generate(dryRun = false)

            assertFalse(result.success, "Should fail with no commits")
            assertTrue(result.error?.contains("No commits") ?: false)

        } finally {
            cleanup()
        }
    }

    private fun setup() {
        testDir.mkdirs()
        testDir.resolve(".mobilectl").mkdirs()
    }

    private fun cleanup() {
        testDir.deleteRecursively()
    }

    // ===== MOCKS =====

    private class MockParser(val commits: List<GitCommit> = defaultCommits) : GitCommitParser {
        override fun parseCommits(fromTag: String?, toTag: String?): List<GitCommit> = commits
        override fun parseCommitsSinceHash(sinceHash: String): List<GitCommit> = commits
        override fun getLatestTag(): String? = "v1.0.0"
        override fun getAllTags(): List<String> = listOf("v1.0.0")
        override fun getTagDate(tag: String): LocalDate? = LocalDate.now()
        override fun parseCommitMessage(message: String, commitTypes: List<CommitType>) = null
        override fun getCompareUrl(fromTag: String, toTag: String) = "#"

        companion object {
            val defaultCommits = listOf(
                GitCommit(
                    hash = "abc123def456",
                    shortHash = "abc123d",
                    type = "feat",
                    scope = "api",
                    message = "Add endpoint",
                    author = "Ahmed",
                    date = LocalDate.now(),
                    breaking = false
                ),
                GitCommit(
                    hash = "def456ghi789",
                    shortHash = "def456g",
                    type = "fix",
                    scope = "bug",
                    message = "Fix crash",
                    author = "Jane",
                    date = LocalDate.now(),
                    breaking = false
                ),
                GitCommit(
                    hash = "ghi789jkl012",
                    shortHash = "ghi789j",
                    type = "docs",
                    scope = null,
                    message = "Update readme",
                    author = "John",
                    date = LocalDate.now(),
                    breaking = false
                )
            )
        }
    }

    private class FileStateManager(val dir: File) : ChangelogStateManager {
        private val file = dir.resolve(".mobilectl/changelog-state.json")

        override fun getState(): ChangelogState {
            return try {
                if (!file.exists()) return ChangelogState()
                ChangelogState()  // Simplified for test
            } catch (e: Exception) {
                ChangelogState()
            }
        }

        override fun saveState(state: ChangelogState): Boolean {
            return try {
                file.parentFile?.mkdirs()
                file.writeText(state.toString())
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
