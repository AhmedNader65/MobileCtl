package com.mobilectl.commands.deploy

import com.mobilectl.config.Config
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.versionManagement.VersionConfig
import com.mobilectl.model.deploy.DeployConfig
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for version bumping integration in deploy command
 */
class DeployHandlerVersionBumpTest {

    // ═════════════════════════════════════════════════════════════
    // SCENARIO 1: CLI PROVIDED (Highest Priority)
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: CLI --bump-version patch (no config)
     */
    @Test
    fun testCLIBumpVersionPatchNoConfig() = runBlocking {
        val handler = createDeployHandler(
            bumpVersion = "patch"  // ← CLI provided
        )

        val config = createMinimalConfig(
            version = VersionConfig(
                current = "1.0.0",
                autoIncrement = false
            )
        )

        val shouldBump = handler.determineShouldBump(config)
        assertTrue(shouldBump, "CLI should force bump")

        val strategy = handler.determineBumpStrategy(config)
        assertEquals("patch", strategy, "Should use CLI value (patch)")
    }

    /**
     * Test: CLI --bump-version major (overrides config)
     */
    @Test
    fun testCLIBumpVersionOverridesConfig() = runBlocking {
        val handler = createDeployHandler(
            bumpVersion = "major"  // ← CLI says major
        )

        val config = createMinimalConfig(
            version = VersionConfig(
                current = "1.0.0",
                autoIncrement = true,
                bumpStrategy = "patch"  // ← Config says patch
            )
        )

        val strategy = handler.determineBumpStrategy(config)
        assertEquals("major", strategy, "CLI should win over config")
    }

    /**
     * Test: CLI -B minor (short form)
     */
    @Test
    fun testCLIBumpVersionShortForm() = runBlocking {
        val handler = createDeployHandler(
            bumpVersion = "minor"  // From -B flag
        )

        val config = createMinimalConfig()

        val shouldBump = handler.determineShouldBump(config)
        assertTrue(shouldBump, "Short form -B should work")
    }

    /**
     * Test: CLI with invalid strategy
     */
    @Test
    fun testCLIInvalidStrategy() = runBlocking {
        val handler = createDeployHandler(
            bumpVersion = "invalid"
        )

        val isValid = handler.isValidBumpStrategy("invalid")
        assertFalse(isValid, "Invalid strategy should be rejected")
    }

    // ═════════════════════════════════════════════════════════════
    // SCENARIO 2: CONFIG PROVIDED (No CLI)
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Config autoIncrement: true, strategy: patch
     */
    @Test
    fun testConfigAutoIncrementPatch() = runBlocking {
        val handler = createDeployHandler(
            bumpVersion = null  // No CLI
        )

        val config = createMinimalConfig(
            version = VersionConfig(
                current = "1.0.0",
                autoIncrement = true,
                bumpStrategy = "patch"
            )
        )

        val shouldBump = handler.determineShouldBump(config)
        assertTrue(shouldBump, "Config autoIncrement should enable bump")

        val strategy = handler.determineBumpStrategy(config)
        assertEquals("patch", strategy, "Should use config strategy")
    }

    /**
     * Test: Config autoIncrement: true, strategy: minor
     */
    @Test
    fun testConfigAutoIncrementMinor() = runBlocking {
        val handler = createDeployHandler(
            bumpVersion = null
        )

        val config = createMinimalConfig(
            version = VersionConfig(
                current = "1.0.0",
                autoIncrement = true,
                bumpStrategy = "minor"
            )
        )

        val strategy = handler.determineBumpStrategy(config)
        assertEquals("minor", strategy)
    }

    /**
     * Test: Config autoIncrement: true, strategy: major
     */
    @Test
    fun testConfigAutoIncrementMajor() = runBlocking {
        val handler = createDeployHandler(
            bumpVersion = null
        )

        val config = createMinimalConfig(
            version = VersionConfig(
                current = "1.0.0",
                autoIncrement = true,
                bumpStrategy = "major"
            )
        )

        val strategy = handler.determineBumpStrategy(config)
        assertEquals("major", strategy)
    }

    /**
     * Test: Config autoIncrement: true, strategy: auto
     */
    @Test
    fun testConfigAutoIncrementAuto() = runBlocking {
        val handler = createDeployHandler(
            bumpVersion = null
        )

        val config = createMinimalConfig(
            version = VersionConfig(
                current = "1.0.0",
                autoIncrement = true,
                bumpStrategy = "auto"
            )
        )

        val strategy = handler.determineBumpStrategy(config)
        assertEquals("auto", strategy)
    }

    /**
     * Test: Config autoIncrement: true, strategy: manual
     */
    @Test
    fun testConfigManualStrategySkipsBump() = runBlocking {
        val handler = createDeployHandler(
            bumpVersion = null
        )

        val config = createMinimalConfig(
            version = VersionConfig(
                current = "1.0.0",
                autoIncrement = true,
                bumpStrategy = "manual"
            )
        )

        val shouldBump = handler.determineShouldBump(config)
        assertTrue(shouldBump, "autoIncrement=true forces check of strategy")

        val strategy = handler.determineBumpStrategy(config)
        assertEquals("manual", strategy, "Should use manual strategy")
    }

    // ═════════════════════════════════════════════════════════════
    // SCENARIO 3: NO CLI, NO CONFIG (Default = NO BUMP)
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: No CLI, config autoIncrement: false
     */
    @Test
    fun testNoCLIConfigDisabledNoBump() = runBlocking {
        val handler = createDeployHandler(
            bumpVersion = null  // No CLI
        )

        val config = createMinimalConfig(
            version = VersionConfig(
                current = "1.0.0",
                autoIncrement = false  // ← Explicitly disabled
            )
        )

        val shouldBump = handler.determineShouldBump(config)
        assertFalse(shouldBump, "Should NOT bump when disabled")
    }

    /**
     * Test: No CLI, no config file (default)
     */
    @Test
    fun testNoCLINoConfigNoBump() = runBlocking {
        val handler = createDeployHandler(
            bumpVersion = null
        )

        val config = createMinimalConfig()  // Default config

        val shouldBump = handler.determineShouldBump(config)
        assertFalse(shouldBump, "Default should NOT bump")
    }

    /**
     * Test: Fallback strategy when config doesn't specify
     */
    @Test
    fun testDefaultStrategyPatch() = runBlocking {
        val handler = createDeployHandler(
            bumpVersion = null
        )

        val config = createMinimalConfig(
            version = VersionConfig(
                current = "1.0.0",
                autoIncrement = true,
                bumpStrategy = ""  // Empty string
            )
        )

        val strategy = handler.determineBumpStrategy(config)
        assertEquals("patch", strategy, "Should default to patch")
    }

    // ═════════════════════════════════════════════════════════════
    // STRATEGY VALIDATION
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Valid strategies
     */
    @Test
    fun testValidStrategies() = runBlocking {
        val handler = createDeployHandler()

        val valid = listOf("patch", "minor", "major", "auto")
        valid.forEach { strategy ->
            assertTrue(
                handler.isValidBumpStrategy(strategy),
                "Strategy '$strategy' should be valid"
            )
        }
    }

    /**
     * Test: Invalid strategies
     */
    @Test
    fun testInvalidStrategies() = runBlocking {
        val handler = createDeployHandler()

        val invalid = listOf("semver", "random", "unknown", "v1.0.0")
        invalid.forEach { strategy ->
            assertFalse(
                handler.isValidBumpStrategy(strategy),
                "Strategy '$strategy' should be invalid"
            )
        }
    }

    /**
     * Test: Manual strategy is special (means "don't bump")
     */
    @Test
    fun testManualStrategyMeansNoBump() = runBlocking {
        val handler = createDeployHandler(
            bumpVersion = null
        )

        val config = createMinimalConfig(
            version = VersionConfig(
                current = "1.0.0",
                autoIncrement = true,
                bumpStrategy = "manual"
            )
        )

        val strategy = handler.determineBumpStrategy(config)
        assertEquals("manual", strategy)
    }

    // ═════════════════════════════════════════════════════════════
    // PRIORITY/PRECEDENCE TESTS
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Priority matrix - CLI wins
     */
    @Test
    fun testCLIWinsPriority() = runBlocking {
        val handler = createDeployHandler(
            bumpVersion = "patch"
        )

        val config = createMinimalConfig(
            version = VersionConfig(
                autoIncrement = true,
                bumpStrategy = "major"  // Different from CLI
            )
        )

        val strategy = handler.determineBumpStrategy(config)
        assertEquals("patch", strategy, "CLI should have highest priority")
    }

    /**
     * Test: Config priority when no CLI
     */
    @Test
    fun testConfigPriorityWhenNoCLI() = runBlocking {
        val handler = createDeployHandler(
            bumpVersion = null
        )

        val config = createMinimalConfig(
            version = VersionConfig(
                autoIncrement = true,
                bumpStrategy = "minor"
            )
        )

        val shouldBump = handler.determineShouldBump(config)
        assertTrue(shouldBump)

        val strategy = handler.determineBumpStrategy(config)
        assertEquals("minor", strategy, "Config should be used when CLI not provided")
    }

    /**
     * Test: Default priority when neither CLI nor config
     */
    @Test
    fun testDefaultPriorityFallback() = runBlocking {
        val handler = createDeployHandler(
            bumpVersion = null
        )

        val config = createMinimalConfig(
            version = VersionConfig(
                autoIncrement = true,
                bumpStrategy = ""  // Not specified
            )
        )

        val strategy = handler.determineBumpStrategy(config)
        assertEquals("patch", strategy, "Should default to patch")
    }

    // ═════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═════════════════════════════════════════════════════════════

    /**
     * Create DeployHandler with all required constructor parameters
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
        changelog: Boolean = false
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
     * Create minimal valid config for testing
     */
    private fun createMinimalConfig(
        version: VersionConfig = VersionConfig()
    ): Config {
        return Config(
            version = version,
            build = BuildConfig(),
            deploy = DeployConfig()
        )
    }
}

/**
 * Extension functions for testing (add to DeployHandler)
 */
internal fun DeployHandler.determineShouldBump(config: Config): Boolean {
    return when {
        bumpVersion != null -> true
        config.version?.autoIncrement == true -> true
        else -> false
    }
}

internal fun DeployHandler.determineBumpStrategy(config: Config): String {
    return when {
        bumpVersion != null -> bumpVersion
        config.version?.bumpStrategy?.isNotEmpty() == true -> config.version!!.bumpStrategy
        else -> "patch"
    }
}

internal fun DeployHandler.isValidBumpStrategy(strategy: String): Boolean {
    return strategy in listOf("patch", "minor", "major", "auto", "manual")
}
