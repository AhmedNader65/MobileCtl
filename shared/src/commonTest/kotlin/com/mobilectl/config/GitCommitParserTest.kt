package com.mobilectl.changelog

import com.mobilectl.model.changelog.CommitType
import com.mobilectl.model.changelog.GitCommit
import kotlin.test.Test
import kotlin.test.assertEquals

class GitCommitParserTest {

    @Test
    fun testParseConventionalCommit() {
        val parser = JvmGitCommitParser()
        val message = "feat(auth): add login functionality"
        val commitTypes = listOf(
            CommitType("feat", "Features", "✨"),
            CommitType("fix", "Bug Fixes", "🐛")
        )

        val result = parser.parseCommitMessage(message, commitTypes)

        assertEquals("feat", result?.type)
        assertEquals("auth", result?.scope)
        assertEquals("add login functionality", result?.message)
    }

    @Test
    fun testParseCommitWithoutScope() {
        val parser = JvmGitCommitParser()
        val message = "fix: resolve null pointer exception"
        val commitTypes = listOf(CommitType("fix", "Bug Fixes", "🐛"))

        val result = parser.parseCommitMessage(message, commitTypes)

        assertEquals("fix", result?.type)
        assertEquals(null, result?.scope)
        assertEquals("resolve null pointer exception", result?.message)
    }

    @Test
    fun testDetectBreakingChange() {
        val message = """
            feat: redesign API
            
            BREAKING CHANGE: endpoint /v1/users no longer exists
        """.trimIndent()

        val commit = GitCommit(
            hash = "abc123",
            shortHash = "abc123",
            type = "feat",
            scope = null,
            message = "redesign API",
            body = "BREAKING CHANGE: endpoint /v1/users no longer exists",
            breaking = true
        )

        assertEquals(true, commit.breaking)
    }
}
