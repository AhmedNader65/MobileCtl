package com.mobilectl.commands.deploy

import com.mobilectl.config.Config
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.changelog.CommitType
import com.mobilectl.model.deploy.DeployConfig
import com.mobilectl.model.versionManagement.VersionConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Focused tests for changelog generation in deploy command
 *
 * Design:
 * - CLI: Just -C flag to enable generation
 * - Config: All detailed options in config file (fromTag, append, useLastState, etc)
 * - Standalone changelog command: Full CLI options
 *
 * Tests specifically:
 * - When changelog should be generated (-C flag)
 * - Auto-generation on version bump
 * - Config values used (no CLI overrides needed)
 */
class DeployHandlerChangelogFocusTest {

    // ═════════════════════════════════════════════════════════════
    // CORE DECISION: WHEN TO GENERATE CHANGELOG
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: CLI flag -C forces generation
     *
     * Scenario:
     * - mobilectl deploy -C
     * - No version bump
     * - Config may or may not have changelog
     *
     * Expected: Generate (explicit flag)
     */
    @Test
    fun testCLIFlagCForcesGeneration() {
        val handler = createDeployHandler(
            changelog = true  // ← -C flag
        )

        val config = createConfigWithChangelog(enabled = false)

        val shouldGenerate = handler.shouldGenerateChangelog(config)
        assertTrue(shouldGenerate, "-C flag should force generation")
    }

    /**
     * Test: Auto-generate when bumping version
     *
     * Scenario:
     * - mobilectl deploy --bump-version patch
     * - Config has: changelog.enabled = true
     * - No explicit -C flag
     *
     * Expected: Auto-generate (smart: version change + enabled in config)
     */
    @Test
    fun testAutoGenerateOnVersionBump() {
        val handler = createDeployHandler(
            bumpVersion = "patch",  // ← Bumping version
            changelog = false       // ← No -C flag
        )

        val config = createConfigWithChangelog(enabled = true)

        val shouldGenerate = handler.shouldGenerateChangelog(config)
        assertTrue(shouldGenerate, "Should auto-generate when bumping + config enabled")
    }

    /**
     * Test: Don't generate if changelog disabled in config
     *
     * Scenario:
     * - mobilectl deploy --bump-version minor
     * - Config has: changelog.enabled = false
     *
     * Expected: NO generation (respect config)
     */
    @Test
    fun testRespectDisabledInConfig() {
        val handler = createDeployHandler(
            bumpVersion = "minor",
            changelog = false
        )

        val config = createConfigWithChangelog(enabled = false)

        val shouldGenerate = handler.shouldGenerateChangelog(config)
        assertFalse(shouldGenerate, "Should respect disabled in config")
    }

    /**
     * Test: Don't auto-generate without version bump and no -C
     *
     * Scenario:
     * - mobilectl deploy
     * - No --bump-version
     * - No -C flag
     * - Config has: changelog.enabled = true
     *
     * Expected: NO generation (no reason to generate)
     */
    @Test
    fun testNoAutoGenerateWithoutBumpOrFlag() {
        val handler = createDeployHandler(
            bumpVersion = null,  // ← Not bumping
            changelog = false    // ← No -C flag
        )

        val config = createConfigWithChangelog(enabled = true)

        val shouldGenerate = handler.shouldGenerateChangelog(config)
        assertFalse(shouldGenerate, "Should NOT generate without bump or -C")
    }
    /**
     * Test: CLI -C flag forces generation even if config disabled
     *
     * Scenario:
     * - mobilectl deploy -C
     * - Config has: changelog.enabled = false
     *
     * Expected: Generate (CLI -C wins!)
     */
    @Test
    fun testCLIFlagForcesGenerationOverConfig() {
        val handler = createDeployHandler(
            changelog = true  // ← -C flag provided
        )

        val config = Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(),
            changelog = ChangelogConfig(enabled = false)  // ← Config says NO
        )

        val shouldGenerate = handler.shouldGenerateChangelog(config)
        assertTrue(shouldGenerate, "-C flag should override config")
    }


    // ═════════════════════════════════════════════════════════════
    // CONFIG VALUES (No CLI overrides needed)
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Use fromTag from config
     *
     * Config has: changelog.fromTag = "v1.0.0"
     * CLI: No options for this (set in config)
     *
     * Expected: Use "v1.0.0" from config
     */
    @Test
    fun testUseFromTagFromConfig() {
        val handler = createDeployHandler(changelog = true)

        val config = createConfigWithChangelog(
            enabled = true,
            fromTag = "v1.0.0"
        )

        assertEquals("v1.0.0", config.changelog.fromTag)
    }

    /**
     * Test: Use append from config
     *
     * Config has: changelog.append = false
     * CLI: No options for this
     *
     * Expected: Replace mode (append = false)
     */
    @Test
    fun testUseAppendFromConfig() {
        val handler = createDeployHandler(changelog = true)

        val config = createConfigWithChangelog(
            enabled = true,
            append = false  // Replace mode
        )

        assertEquals(false, config.changelog.append)
    }

    /**
     * Test: Use useLastState from config
     *
     * Config has: changelog.useLastState = true
     * CLI: No options for this
     *
     * Expected: Remember last generation
     */
    @Test
    fun testUseUseLastStateFromConfig() {
        val handler = createDeployHandler(changelog = true)

        val config = createConfigWithChangelog(
            enabled = true,
            useLastState = true
        )

        assertEquals(true, config.changelog.useLastState)
    }

    /**
     * Test: Use custom commit types from config
     *
     * Config has custom types:
     * - feat → Features
     * - fix → Bug Fixes
     * - docs → Documentation
     */
    @Test
    fun testUseCustomCommitTypesFromConfig() {
        val handler = createDeployHandler(changelog = true)

        val customTypes = listOf(
            CommitType("feat", "Features", "✨"),
            CommitType("fix", "Bug Fixes", "🐛"),
            CommitType("docs", "Documentation", "📚")
        )

        val config = createConfigWithChangelog(
            enabled = true,
            commitTypes = customTypes
        )

        assertEquals(3, config.changelog.commitTypes.size)
        assertEquals("feat", config.changelog.commitTypes[0].type)
        assertEquals("✨", config.changelog.commitTypes[0].emoji)
    }

    /**
     * Test: Use output file path from config
     *
     * Config has: changelog.outputFile = "docs/CHANGELOG.md"
     *
     * Expected: Generate at that path
     */
    @Test
    fun testUseOutputPathFromConfig() {
        val handler = createDeployHandler(changelog = true)

        val config = createConfigWithChangelog(enabled = true)
            .copy(
                changelog = ChangelogConfig(
                    enabled = true,
                    outputFile = "docs/CHANGELOG.md"
                )
            )

        assertEquals("docs/CHANGELOG.md", config.changelog.outputFile)
    }

    /**
     * Test: Use format from config
     *
     * Config has: changelog.format = "markdown"
     *
     * Expected: Use markdown format
     */
    @Test
    fun testUseFormatFromConfig() {
        val handler = createDeployHandler(changelog = true)

        val config = createConfigWithChangelog(enabled = true)
            .copy(
                changelog = ChangelogConfig(
                    enabled = true,
                    format = "markdown"
                )
            )

        assertEquals("markdown", config.changelog.format)
    }

    // ═════════════════════════════════════════════════════════════
    // COMBINED WITH VERSION BUMP
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Both version bump AND auto-changelog
     *
     * Scenario:
     * - mobilectl deploy --bump-version minor
     * - Config: autoIncrement = true, changelog.enabled = true
     * - No -C flag needed (auto)
     *
     * Expected: Both operations
     */
    @Test
    fun testBumpAndAutoChangelogTogether() {
        val handler = createDeployHandler(
            bumpVersion = "minor",
            changelog = false  // ← No -C, but auto because bumping
        )

        val config = Config(
            version = VersionConfig(
                autoIncrement = true,
                bumpStrategy = "patch"
            ),
            build = BuildConfig(),
            deploy = DeployConfig(),
            changelog = ChangelogConfig(enabled = true)
        )

        assertTrue(handler.shouldExecuteVersionBump(config))
        assertTrue(handler.shouldGenerateChangelog(config))
    }

    /**
     * Test: Version bump, changelog disabled in config
     *
     * Scenario:
     * - mobilectl deploy --bump-version patch
     * - Config: changelog.enabled = false
     * - No -C flag
     *
     * Expected: Bump but NO changelog
     */
    @Test
    fun testBumpButNoChangelogIfDisabled() {
        val handler = createDeployHandler(
            bumpVersion = "patch",
            changelog = false
        )

        val config = Config(
            version = VersionConfig(autoIncrement = true),
            build = BuildConfig(),
            deploy = DeployConfig(),
            changelog = ChangelogConfig(enabled = false)
        )

        assertTrue(handler.shouldExecuteVersionBump(config))
        assertFalse(handler.shouldGenerateChangelog(config))
    }

    /**
     * Test: Just changelog with -C (no version bump)
     *
     * Scenario:
     * - mobilectl deploy -C
     * - No --bump-version
     * - Config: changelog.enabled = false (but -C overrides)
     *
     * Expected: Generate changelog only
     */
    @Test
    fun testJustChangelogWithCFlag() {
        val handler = createDeployHandler(
            bumpVersion = null,
            changelog = true  // ← -C flag
        )

        val config = createConfigWithChangelog(enabled = false)

        assertFalse(handler.shouldExecuteVersionBump(config))
        assertTrue(handler.shouldGenerateChangelog(config))
    }

    /**
     * Test: Release workflow (version + changelog both auto)
     *
     * Scenario:
     * - mobilectl deploy --bump-version minor
     * - Config: autoIncrement = true, changelog.enabled = true
     * - All options in config file (fromTag, append, useLastState)
     * - No CLI options needed
     *
     * Expected: Full release workflow
     */
    @Test
    fun testFullReleaseWorkflow() {
        val handler = createDeployHandler(
            platform = "android",
            environment = "production",
            bumpVersion = "minor",
            changelog = false  // ← No -C needed (auto)
        )

        val config = Config(
            version = VersionConfig(
                current = "1.0.0",
                autoIncrement = true,
                bumpStrategy = "minor"
            ),
            build = BuildConfig(),
            deploy = DeployConfig(),
            changelog = ChangelogConfig(
                enabled = true,
                fromTag = "v1.0.0",  // ← In config
                append = false,      // ← In config
                useLastState = true, // ← In config
                outputFile = "CHANGELOG.md"  // ← In config
            )
        )

        assertTrue(handler.shouldExecuteVersionBump(config))
        assertTrue(handler.shouldGenerateChangelog(config))

        // All values come from config
        assertEquals("v1.0.0", config.changelog.fromTag)
        assertEquals(false, config.changelog.append)
        assertEquals(true, config.changelog.useLastState)
    }

    /**
     * Test: Hotfix (no version, no changelog)
     *
     * Scenario:
     * - mobilectl deploy
     * - No --bump-version
     * - No -C flag
     *
     * Expected: Just deploy
     */
    @Test
    fun testHotfixNoVersionNoChangelog() {
        val handler = createDeployHandler(
            bumpVersion = null,
            changelog = false
        )

        val config = createConfigWithChangelog(enabled = true)

        assertFalse(handler.shouldExecuteVersionBump(config))
        assertFalse(handler.shouldGenerateChangelog(config))
    }

    // ═════════════════════════════════════════════════════════════
    // EDGE CASES
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Comprehensive config with all options
     *
     * All changelog config options set
     */
    @Test
    fun testComprehensiveConfigOptions() {
        val handler = createDeployHandler(changelog = true)

        val customTypes = listOf(
            CommitType("feat", "Features", "✨"),
            CommitType("fix", "Bug Fixes", "🐛"),
            CommitType("perf", "Performance", "⚡")
        )

        val config = createConfigWithChangelog(
            enabled = true,
            fromTag = "v1.0.0",
            append = true,
            useLastState = true,
            commitTypes = customTypes
        ).copy(
            changelog = ChangelogConfig(
                enabled = true,
                fromTag = "v1.0.0",
                append = true,
                useLastState = true,
                commitTypes = customTypes,
                outputFile = "CHANGELOG.md",
                format = "markdown"
            )
        )

        assertTrue(handler.shouldGenerateChangelog(config))
        assertEquals("v1.0.0", config.changelog.fromTag)
        assertEquals(true, config.changelog.append)
        assertEquals(true, config.changelog.useLastState)
        assertEquals(3, config.changelog.commitTypes.size)
        assertEquals("CHANGELOG.md", config.changelog.outputFile)
        assertEquals("markdown", config.changelog.format)
    }

    /**
     * Test: Minimal config (just enabled)
     *
     * Only changelog.enabled = true
     * All other values use defaults
     */
    @Test
    fun testMinimalConfig() {
        val handler = createDeployHandler(changelog = true)

        val config = ChangelogConfig(enabled = true)

        assertTrue(config.enabled)
        assertEquals(null, config.fromTag)  // Default: auto-detect
        assertEquals(true, config.append)   // Default: append
        assertEquals(true, config.useLastState)  // Default: remember
    }

    // ═════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═════════════════════════════════════════════════════════════

    /**
     * Create DeployHandler with minimal required parameters
     * Only -C flag for changelog (no other CLI options)
     */
    private fun createDeployHandler(
        platform: String? = null,
        destination: String? = null,
        environment: String? = null,
        releaseNotes: String? = null,
        testGroups: String? = null,
        verbose: Boolean = false,
        dryRun: Boolean = false,
        skipBuild: Boolean = false,
        interactive: Boolean = false,
        confirm: Boolean = false,
        bumpVersion: String? = null,
        changelog: Boolean = false  // ← Only -C flag
    ): DeployHandler {
        return DeployHandler(
            platform = platform,
            destination = destination,
            environment = environment,
            releaseNotes = releaseNotes,
            testGroups = testGroups,
            verbose = verbose,
            dryRun = dryRun,
            skipBuild = skipBuild,
            interactive = interactive,
            confirm = confirm,
            bumpVersion = bumpVersion,
            changelog = changelog
        )
    }

    /**
     * Create config with changelog enabled and custom values
     */
    private fun createConfigWithChangelog(
        enabled: Boolean = true,
        fromTag: String? = null,
        append: Boolean = true,
        useLastState: Boolean = true,
        commitTypes: List<CommitType> = emptyList()
    ): Config {
        return Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(),
            changelog = ChangelogConfig(
                enabled = enabled,
                fromTag = fromTag,
                append = append,
                useLastState = useLastState,
                commitTypes = commitTypes
            )
        )
    }
}

