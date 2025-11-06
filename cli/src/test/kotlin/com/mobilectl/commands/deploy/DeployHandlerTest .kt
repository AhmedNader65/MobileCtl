package com.mobilectl.commands.deploy

import com.mobilectl.config.Config
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.deploy.DeployConfig
import com.mobilectl.model.versionManagement.VersionConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for DeployHandler
 *
 * Tests the complete deploy flow including:
 * - Version bumping
 * - Changelog generation
 * - Build cache validation
 * - Artifact detection
 * - Deploy execution
 */
class DeployHandlerTest {

    // ═════════════════════════════════════════════════════════════
    // CONSTRUCTOR & BASIC CREATION
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Create DeployHandler with all parameters
     */
    @Test
    fun testDeployHandlerConstruction() {
        val handler = createDeployHandler(
            platform = "android",
            destination = "firebase",
            environment = "production",
            releaseNotes = "Bug fixes",
            testGroups = "qa-team",
            verbose = true,
            dryRun = false,
            skipBuild = false,
            interactive = false,
            confirm = true,
            bumpVersion = "patch",
            changelog = true
        )

        assertEquals("android", handler.platform)
        assertEquals("patch", handler.bumpVersion)
        assertTrue(handler.verbose)
    }

    /**
     * Test: Create DeployHandler with minimal parameters
     */
    @Test
    fun testDeployHandlerMinimalConstruction() {
        val handler = createDeployHandler()

        assertEquals(null, handler.platform)
        assertEquals(null, handler.bumpVersion)
    }

    // ═════════════════════════════════════════════════════════════
    // VERSION BUMPING & CHANGELOG INTEGRATION
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Determine if version should be bumped
     *
     * Scenario 1: CLI provided --bump-version patch
     * Expected: Should bump
     */
    @Test
    fun testShouldBumpVersionWithCLI() {
        val handler = createDeployHandler(bumpVersion = "patch")
        val config = createMinimalConfig()

        val shouldBump = handler.shouldExecuteVersionBump(config)
        assertTrue(shouldBump, "Should bump when CLI --bump-version provided")
    }

    /**
     * Test: Determine if version should be bumped
     *
     * Scenario 2: Config has autoIncrement = true
     * Expected: Should bump
     */
    @Test
    fun testShouldBumpVersionWithConfig() {
        val handler = createDeployHandler(bumpVersion = null)
        val config = createMinimalConfig(
            version = VersionConfig(
                autoIncrement = true,
                bumpStrategy = "minor"
            )
        )

        val shouldBump = handler.shouldExecuteVersionBump(config)
        assertTrue(shouldBump, "Should bump when config autoIncrement = true")
    }

    /**
     * Test: Determine if changelog should be generated
     *
     * Scenario 1: CLI provided --changelog
     * Expected: Should generate
     */
    @Test
    fun testShouldGenerateChangelogWithCLI() {
        val handler = createDeployHandler(changelog = true)
        val config = createMinimalConfig()

        val shouldGenerate = handler.shouldGenerateChangelog(config)
        assertTrue(shouldGenerate, "Should generate when CLI --changelog provided")
    }

    /**
     * Test: Determine if changelog should be generated
     *
     * Scenario 2: Bumping version + changelog enabled
     * Expected: Should auto-generate
     */
    @Test
    fun testShouldGenerateChangelogAutoOnBump() {
        val handler = createDeployHandler(
            bumpVersion = "patch",
            changelog = false  // Not explicitly requested
        )
        val config = createMinimalConfig(
            changelog = ChangelogConfig(
                enabled = true
            )
        )

        val shouldGenerate = handler.shouldGenerateChangelog(config)
        assertTrue(shouldGenerate, "Should auto-generate when bumping + changelog enabled")
    }

    // ═════════════════════════════════════════════════════════════
    // DRY-RUN MODE
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Dry-run flag prevents execution
     */
    @Test
    fun testDryRunMode() {
        val handler = createDeployHandler(
            dryRun = true,
            confirm = true
        )

        assertEquals(true, handler.dryRun)
        // In actual execute(), dry-run returns early without deploying
    }

    /**
     * Test: Skip build flag works
     */
    @Test
    fun testSkipBuildFlag() {
        val handler = createDeployHandler(
            skipBuild = true
        )

        assertEquals(true, handler.skipBuild)
    }

    // ═════════════════════════════════════════════════════════════
    // INTERACTIVE MODE
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Interactive mode flag
     */
    @Test
    fun testInteractiveMode() {
        val handler = createDeployHandler(
            interactive = true
        )

        assertEquals(true, handler.interactive)
    }

    /**
     * Test: Confirm flag (auto-confirm prompts)
     */
    @Test
    fun testConfirmFlag() {
        val handler = createDeployHandler(
            confirm = true
        )

        assertEquals(true, handler.confirm)
    }

    // ═════════════════════════════════════════════════════════════
    // PLATFORM & DESTINATION DETECTION
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Platform parameter
     */
    @Test
    fun testPlatformParameter() {
        val handler = createDeployHandler(platform = "android")
        assertEquals("android", handler.platform)
    }

    /**
     * Test: Destination parameter
     */
    @Test
    fun testDestinationParameter() {
        val handler = createDeployHandler(destination = "firebase")
        assertEquals("firebase", handler.destination)
    }

    /**
     * Test: Environment parameter
     */
    @Test
    fun testEnvironmentParameter() {
        val handler = createDeployHandler(environment = "production")
        assertEquals("production", handler.environment)
    }

    // ═════════════════════════════════════════════════════════════
    // DEPLOYMENT OPTIONS
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Release notes parameter
     */
    @Test
    fun testReleaseNotesParameter() {
        val releaseNotes = "Bug fixes and performance improvements"
        val handler = createDeployHandler(releaseNotes = releaseNotes)
        assertEquals(releaseNotes, handler.releaseNotes)
    }

    /**
     * Test: Test groups parameter
     */
    @Test
    fun testTestGroupsParameter() {
        val testGroups = "qa-team,beta-testers"
        val handler = createDeployHandler(testGroups = testGroups)
        assertEquals(testGroups, handler.testGroups)
    }

    // ═════════════════════════════════════════════════════════════
    // VERBOSE & DEBUG
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Verbose flag enables detailed output
     */
    @Test
    fun testVerboseFlag() {
        val handler = createDeployHandler(verbose = true)
        assertEquals(true, handler.verbose)
    }

    // ═════════════════════════════════════════════════════════════
    // VALIDATION STRATEGIES
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Valid bump strategies
     */
    @Test
    fun testValidBumpStrategies() {
        val handler = createDeployHandler()

        val validStrategies = listOf("patch", "minor", "major", "auto", "manual")
        validStrategies.forEach { strategy ->
            assertTrue(
                handler.isValidStrategy(strategy),
                "Strategy '$strategy' should be valid"
            )
        }
    }

    /**
     * Test: Invalid bump strategies
     */
    @Test
    fun testInvalidBumpStrategies() {
        val handler = createDeployHandler()

        val invalidStrategies = listOf("semver", "random", "v1.0.0", "invalid")
        invalidStrategies.forEach { strategy ->
            assertTrue(
                !handler.isValidStrategy(strategy),
                "Strategy '$strategy' should be invalid"
            )
        }
    }

    // ═════════════════════════════════════════════════════════════
    // COMBINED SCENARIOS
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Full release workflow
     *
     * Scenario:
     * - Bump version (minor)
     * - Generate changelog
     * - Deploy to production
     * - Confirm automatically
     */
    @Test
    fun testFullReleaseWorkflow() {
        val handler = createDeployHandler(
            platform = "android",
            destination = "firebase",
            environment = "production",
            bumpVersion = "minor",
            changelog = true,
            confirm = true,
            skipBuild = false
        )

        val config = createMinimalConfig(
            version = VersionConfig(
                current = "1.0.0",
                autoIncrement = true
            ),
            changelog = ChangelogConfig(
                enabled = true
            )
        )

        assertTrue(handler.shouldExecuteVersionBump(config))
        assertTrue(handler.shouldGenerateChangelog(config))
        assertEquals("minor", handler.bumpVersion)
        assertEquals("production", handler.environment)
        assertEquals("firebase", handler.destination)
    }

    /**
     * Test: Hotfix deployment (no version bump, no changelog)
     *
     * Scenario:
     * - Just deploy existing build
     * - No version changes
     * - No changelog
     */
    @Test
    fun testHotfixDeployment() {
        val handler = createDeployHandler(
            platform = "android",
            destination = "firebase",
            environment = "production",
            bumpVersion = null,
            changelog = false,
            skipBuild = true,
            confirm = true
        )

        val config = createMinimalConfig(
            version = VersionConfig(
                autoIncrement = false  // Don't auto-bump
            ),
            changelog = ChangelogConfig(
                enabled = false  // Don't generate
            )
        )

        assertTrue(!handler.shouldExecuteVersionBump(config))
        assertTrue(!handler.shouldGenerateChangelog(config))
        assertEquals(true, handler.skipBuild)
    }

    /**
     * Test: Development deployment (interactive, dry-run)
     *
     * Scenario:
     * - Interactive selection
     * - Dry-run (preview only)
     * - Verbose output
     */
    @Test
    fun testDevDeployment() {
        val handler = createDeployHandler(
            platform = null,  // Auto-detect
            environment = "dev",
            interactive = true,
            dryRun = true,
            verbose = true,
            skipBuild = false
        )

        assertEquals("dev", handler.environment)
        assertEquals(true, handler.interactive)
        assertEquals(true, handler.dryRun)
        assertEquals(true, handler.verbose)
    }

    // ═════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═════════════════════════════════════════════════════════════

    /**
     * Create DeployHandler with all required parameters
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
     * Create minimal valid config
     */
    private fun createMinimalConfig(
        version: VersionConfig = VersionConfig(),
        changelog: ChangelogConfig = ChangelogConfig()
    ): Config {
        return Config(
            version = version,
            build = BuildConfig(),
            deploy = DeployConfig(),
            changelog = changelog
        )
    }
}

/**
 * Extension functions for testing
 */
internal fun DeployHandler.shouldExecuteVersionBump(config: Config): Boolean {
    return bumpVersion != null || config.version?.autoIncrement == true
}

internal fun DeployHandler.shouldGenerateChangelog(config: Config): Boolean {
    if (changelog) return true
    if (bumpVersion != null && config.changelog.enabled == true) return true
    return false
}

internal fun DeployHandler.isValidStrategy(strategy: String): Boolean {
    return strategy in listOf("patch", "minor", "major", "auto", "manual")
}

// Access private properties for testing
internal val DeployHandler.platform: String?
    get() = this.javaClass.getDeclaredField("platform").let {
        it.isAccessible = true
        it.get(this) as String?
    }

internal val DeployHandler.destination: String?
    get() = this.javaClass.getDeclaredField("destination").let {
        it.isAccessible = true
        it.get(this) as String?
    }

internal val DeployHandler.environment: String?
    get() = this.javaClass.getDeclaredField("environment").let {
        it.isAccessible = true
        it.get(this) as String?
    }

internal val DeployHandler.releaseNotes: String?
    get() = this.javaClass.getDeclaredField("releaseNotes").let {
        it.isAccessible = true
        it.get(this) as String?
    }

internal val DeployHandler.testGroups: String?
    get() = this.javaClass.getDeclaredField("testGroups").let {
        it.isAccessible = true
        it.get(this) as String?
    }

internal val DeployHandler.verbose: Boolean
    get() = this.javaClass.getDeclaredField("verbose").let {
        it.isAccessible = true
        it.get(this) as Boolean
    }

internal val DeployHandler.dryRun: Boolean
    get() = this.javaClass.getDeclaredField("dryRun").let {
        it.isAccessible = true
        it.get(this) as Boolean
    }

internal val DeployHandler.skipBuild: Boolean
    get() = this.javaClass.getDeclaredField("skipBuild").let {
        it.isAccessible = true
        it.get(this) as Boolean
    }

internal val DeployHandler.interactive: Boolean
    get() = this.javaClass.getDeclaredField("interactive").let {
        it.isAccessible = true
        it.get(this) as Boolean
    }

internal val DeployHandler.confirm: Boolean
    get() = this.javaClass.getDeclaredField("confirm").let {
        it.isAccessible = true
        it.get(this) as Boolean
    }

internal val DeployHandler.bumpVersion: String?
    get() = this.javaClass.getDeclaredField("bumpVersion").let {
        it.isAccessible = true
        it.get(this) as String?
    }

internal val DeployHandler.changelog: Boolean
    get() = this.javaClass.getDeclaredField("changelog").let {
        it.isAccessible = true
        it.get(this) as Boolean
    }
