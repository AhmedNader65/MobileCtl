package com.mobilectl.changelog

import com.mobilectl.model.changelog.CommitType
import com.mobilectl.model.changelog.GitCommit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GitCommitParserTest {

    private val parser = JGitCommitParser()

    @Test
    fun testParseConventionalCommit() {
        val message = "feat(auth): add two-factor authentication"
        val commitTypes = listOf(CommitType("feat", "Features", "✨"))

        val result = parser.parseCommitMessage(message, commitTypes)

        assertNotNull(result)
        assertEquals("feat", result.type)
        assertEquals("auth", result.scope)
        assertEquals("add two-factor authentication", result.message)
    }

    @Test
    fun testParseCommitWithoutScope() {
        val message = "fix: resolve null pointer exception"
        val commitTypes = listOf(CommitType("fix", "Bug Fixes", "🐛"))

        val result = parser.parseCommitMessage(message, commitTypes)

        assertNotNull(result)
        assertEquals("fix", result.type)
        assertEquals(null, result.scope)
        assertEquals("resolve null pointer exception", result.message)
    }

    @Test
    fun testParseCommitWithMultipleParts() {
        val message = "refactor(core): reorganize module structure"

        val result = parser.parseCommitMessage(message, emptyList())

        assertNotNull(result)
        assertEquals("refactor", result.type)
        assertEquals("core", result.scope)
    }

    @Test
    fun testParseInvalidCommitFormat() {
        val message = "just some commit message"

        val result = parser.parseCommitMessage(message, emptyList())

        assertEquals(null, result)
    }

    @Test
    fun testParseCommitWithSpecialCharacters() {
        val message = "feat(api): add support for 'async/await' pattern"

        val result = parser.parseCommitMessage(message, emptyList())

        assertNotNull(result)
        assertTrue(result.message.contains("'async/await'"))
    }

    @Test
    fun testExtractTypeFromCommit() {
        val commits = listOf(
            GitCommit("hash1", "h1", "feat", null, "Add feature", breaking = false),
            GitCommit("hash2", "h2", "fix", null, "Fix bug", breaking = false),
            GitCommit("hash3", "h3", "docs", null, "Update docs", breaking = false)
        )

        val types = commits.map { it.type }.distinct()

        assertEquals(3, types.size)
        assertTrue(types.contains("feat"))
        assertTrue(types.contains("fix"))
        assertTrue(types.contains("docs"))
    }

    @Test
    fun testDetectBreakingChange() {
        val commit = GitCommit(
            hash = "abc123",
            shortHash = "abc123",
            type = "feat",
            scope = null,
            message = "redesign API",
            body = "BREAKING CHANGE: endpoint /v1/users no longer exists",
            breaking = true
        )

        assertTrue(commit.breaking)
        assertTrue(commit.body?.contains("BREAKING CHANGE:") ?: false)
    }

    @Test
    fun testCommitWithAuthor() {
        val commit = GitCommit(
            hash = "abc123",
            shortHash = "abc123",
            type = "feat",
            scope = null,
            message = "Add feature",
            author = "Ahmed Nader",
            breaking = false
        )

        assertEquals("Ahmed Nader", commit.author)
    }

    @Test
    fun testGroupCommitsByType() {
        val commits = listOf(
            GitCommit("1", "1", "feat", null, "Feature 1", breaking = false),
            GitCommit("2", "2", "feat", null, "Feature 2", breaking = false),
            GitCommit("3", "3", "fix", null, "Fix 1", breaking = false),
            GitCommit("4", "4", "docs", null, "Doc 1", breaking = false)
        )

        val grouped = commits.groupBy { it.type }

        assertEquals(2, grouped["feat"]?.size)
        assertEquals(1, grouped["fix"]?.size)
        assertEquals(1, grouped["docs"]?.size)
    }
}
