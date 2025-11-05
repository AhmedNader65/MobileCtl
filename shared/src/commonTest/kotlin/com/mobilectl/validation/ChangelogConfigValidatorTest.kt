package com.mobilectl.validation

import com.mobilectl.config.Config
import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.changelog.CommitType
import com.mobilectl.model.ValidationSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChangelogConfigValidatorTest {

    private val validator = ChangelogConfigValidator()

    @Test
    fun testValidChangelogConfig() {
        val config = Config(
            changelog = ChangelogConfig(
                enabled = true,
                format = "markdown",
                outputFile = "CHANGELOG.md",
                commitTypes = listOf(
                    CommitType("feat", "Features", "✨"),
                    CommitType("fix", "Bug Fixes", "🐛")
                )
            )
        )

        val errors = validator.validate(config)
        assertEquals(0, errors.size)
    }

    @Test
    fun testChangelogDisabled() {
        val config = Config(
            changelog = ChangelogConfig(
                enabled = false,
                format = "invalid",
                outputFile = "",
                commitTypes = emptyList()
            )
        )

        val errors = validator.validate(config)
        assertEquals(0, errors.size, "Disabled changelog should not be validated")
    }

    @Test
    fun testInvalidFormat() {
        val config = Config(
            changelog = ChangelogConfig(
                enabled = true,
                format = "json",  // Invalid
                outputFile = "CHANGELOG.md",
                commitTypes = listOf(CommitType("feat", "Features", "✨"))
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.field == "changelog.format" })
        assertTrue(errors.any { it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun testEmptyOutputFile() {
        val config = Config(
            changelog = ChangelogConfig(
                enabled = true,
                format = "markdown",
                outputFile = "",
                commitTypes = listOf(CommitType("feat", "Features", "✨"))
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.field == "changelog.output_file" && it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun testBlankOutputFile() {
        val config = Config(
            changelog = ChangelogConfig(
                enabled = true,
                format = "markdown",
                outputFile = "   ",
                commitTypes = listOf(CommitType("feat", "Features", "✨"))
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.field == "changelog.output_file" })
    }

    @Test
    fun testWrongOutputFileExtension() {
        val config = Config(
            changelog = ChangelogConfig(
                enabled = true,
                format = "markdown",
                outputFile = "CHANGELOG.txt",
                commitTypes = listOf(CommitType("feat", "Features", "✨"))
            )
        )

        val errors = validator.validate(config)
        val error = errors.firstOrNull { it.field == "changelog.output_file" }
        assertTrue(error != null)
        assertEquals(ValidationSeverity.WARNING, error?.severity)
    }

    @Test
    fun testEmptyCommitTypes() {
        val config = Config(
            changelog = ChangelogConfig(
                enabled = true,
                format = "markdown",
                outputFile = "CHANGELOG.md",
                commitTypes = emptyList()
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.field == "changelog.commit_types" && it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun testMultipleErrors() {
        val config = Config(
            changelog = ChangelogConfig(
                enabled = true,
                format = "xml",  // Invalid
                outputFile = "",  // Empty
                commitTypes = emptyList()  // Empty
            )
        )

        val errors = validator.validate(config)
        assertEquals(3, errors.count { it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun testErrorMessages() {
        val config = Config(
            changelog = ChangelogConfig(
                enabled = true,
                format = "json",
                outputFile = "CHANGELOG.md",
                commitTypes = listOf(CommitType("feat", "Features", "✨"))
            )
        )

        val errors = validator.validate(config)
        val formatError = errors.first { it.field == "changelog.format" }

        assertTrue(formatError.message.contains("json"))
        assertTrue(formatError.suggestion != null)
    }
}
