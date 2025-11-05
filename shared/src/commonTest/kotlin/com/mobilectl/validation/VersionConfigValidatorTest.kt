package com.mobilectl.validation

import com.mobilectl.config.Config
import com.mobilectl.model.ValidationSeverity
import com.mobilectl.model.versionManagement.VersionConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionConfigValidatorTest {

    private val validator = VersionConfigValidator()

    @Test
    fun testValidVersionConfig() {
        val config = Config(
            version = VersionConfig(
                enabled = true,
                current = "1.0.0",
                bumpStrategy = "semver",
                filesToUpdate = listOf("pubspec.yaml", "package.json")
            )
        )

        val errors = validator.validate(config)
        assertEquals(0, errors.size)
    }

    @Test
    fun testVersionDisabled() {
        val config = Config(
            version = VersionConfig(
                enabled = false,
                current = "invalid",
                bumpStrategy = "invalid",
                filesToUpdate = emptyList()
            )
        )

        val errors = validator.validate(config)
        assertEquals(0, errors.size, "Disabled version config should not be validated")
    }

    @Test
    fun testEmptyFilesToUpdate() {
        val config = Config(
            version = VersionConfig(
                enabled = true,
                current = "1.0.0",
                bumpStrategy = "semver",
                filesToUpdate = emptyList()
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.field == "version.filesToUpdate" && it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun testInvalidBumpStrategy() {
        val config = Config(
            version = VersionConfig(
                enabled = true,
                current = "1.0.0",
                bumpStrategy = "custom",  // Invalid
                filesToUpdate = listOf("pubspec.yaml")
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.field == "version.bumpStrategy" })
        assertTrue(errors.any { it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun testValidBumpStrategies() {
        listOf("semver", "manual").forEach { strategy ->
            val config = Config(
                version = VersionConfig(
                    enabled = true,
                    current = "1.0.0",
                    bumpStrategy = strategy,
                    filesToUpdate = listOf("pubspec.yaml")
                )
            )

            val errors = validator.validate(config)
            assertTrue(errors.none { it.field == "version.bumpStrategy" })
        }
    }

    @Test
    fun testInvalidVersionFormat() {
        listOf("1", "1.0", "1.0.0.0", "v1.0.0", "1.0.0.0-beta").forEach { version ->
            val config = Config(
                version = VersionConfig(
                    enabled = true,
                    current = version,
                    bumpStrategy = "semver",
                    filesToUpdate = listOf("pubspec.yaml")
                )
            )

            val errors = validator.validate(config)

            // Special cases that are actually valid
            if (version == "1.0.0-beta" || version == "1.0.0") {
                assertTrue(errors.none { it.field == "version.current" })
            } else {
                assertTrue(errors.any { it.field == "version.current" })
            }
        }
    }

    @Test
    fun testValidVersionFormats() {
        listOf(
            "1.0.0",
            "0.0.1",
            "1.0.0-alpha",
            "1.0.0-beta.1",
            "1.0.0+build.1"
        ).forEach { version ->
            val config = Config(
                version = VersionConfig(
                    enabled = true,
                    current = version,
                    bumpStrategy = "semver",
                    filesToUpdate = listOf("pubspec.yaml")
                )
            )

            val errors = validator.validate(config)
            assertTrue(
                errors.none { it.field == "version.current" },
                "Version $version should be valid"
            )
        }
    }

    @Test
    fun testVersionWarningNotError() {
        val config = Config(
            version = VersionConfig(
                enabled = true,
                current = "invalid-version",
                bumpStrategy = "semver",
                filesToUpdate = listOf("pubspec.yaml")
            )
        )

        val errors = validator.validate(config)
        val versionError = errors.firstOrNull { it.field == "version.current" }

        assertEquals(ValidationSeverity.WARNING, versionError?.severity)
    }

    @Test
    fun testMultipleFilesToUpdate() {
        val config = Config(
            version = VersionConfig(
                enabled = true,
                current = "1.0.0",
                bumpStrategy = "semver",
                filesToUpdate = listOf("pubspec.yaml", "package.json", "build.gradle")
            )
        )

        val errors = validator.validate(config)
        assertEquals(0, errors.size)
    }
}
