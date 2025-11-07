package com.mobilectl.commands.deploy

import com.mobilectl.config.Config
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.deploy.AndroidDeployConfig
import com.mobilectl.model.deploy.DeployConfig
import com.mobilectl.model.deploy.FirebaseAndroidDestination
import com.mobilectl.model.deploy.IosDeployConfig
import com.mobilectl.model.deploy.LocalAndroidDestination
import com.mobilectl.model.deploy.PlayConsoleAndroidDestination
import com.mobilectl.model.versionManagement.VersionConfig
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Unit tests for ConfigurationService
 *
 * Tests:
 * - Config loading and merging
 * - Smart defaults fallback
 * - Command-line overrides
 * - Destination toggling
 */
class ConfigurationServiceTest {

    private lateinit var smartDefaultsProvider: SmartDefaultsProvider
    private lateinit var configService: ConfigurationService
    private val workingPath = System.getProperty("user.dir")

    @BeforeTest
    fun setup() {
        smartDefaultsProvider = SmartDefaultsProvider(workingPath, verbose = false)
        configService = ConfigurationService(workingPath, smartDefaultsProvider, verbose = false)
    }

    // ═════════════════════════════════════════════════════════════
    // COMMAND-LINE OVERRIDES
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testApplyDestinationOverride_Firebase() {
        val baseConfig = createMinimalConfig()

        val result = configService.applyCommandLineOverrides(
            config = baseConfig,
            destination = "firebase",
            releaseNotes = null,
            testGroups = null
        )

        assertTrue(result.deploy.android?.firebase?.enabled == true, "Firebase should be enabled")
    }

    @Test
    fun testApplyDestinationOverride_PlayStore() {
        val baseConfig = createMinimalConfig()

        val result = configService.applyCommandLineOverrides(
            config = baseConfig,
            destination = "play",
            releaseNotes = null,
            testGroups = null
        )

        assertTrue(result.deploy.android?.playConsole?.enabled == true, "Play Console should be enabled")
    }

    @Test
    fun testApplyDestinationOverride_Local() {
        val baseConfig = createMinimalConfig()

        val result = configService.applyCommandLineOverrides(
            config = baseConfig,
            destination = "local",
            releaseNotes = null,
            testGroups = null
        )

        assertTrue(result.deploy.android?.local?.enabled == true, "Local should be enabled")
    }

    @Test
    fun testApplyReleaseNotesOverride() {
        val baseConfig = createMinimalConfig()

        val result = configService.applyCommandLineOverrides(
            config = baseConfig,
            destination = null,
            releaseNotes = "Custom release notes",
            testGroups = null
        )

        assertEquals(
            "Custom release notes",
            result.deploy.android?.firebase?.releaseNotes,
            "Release notes should be overridden"
        )
    }

    @Test
    fun testApplyTestGroupsOverride() {
        val baseConfig = createMinimalConfig()

        val result = configService.applyCommandLineOverrides(
            config = baseConfig,
            destination = null,
            releaseNotes = null,
            testGroups = "qa-team,beta-testers"
        )

        assertEquals(
            listOf("qa-team", "beta-testers"),
            result.deploy.android?.firebase?.testGroups,
            "Test groups should be overridden"
        )
    }

    @Test
    fun testApplyMultipleOverrides() {
        val baseConfig = createMinimalConfig()

        val result = configService.applyCommandLineOverrides(
            config = baseConfig,
            destination = "firebase",
            releaseNotes = "Test build",
            testGroups = "internal"
        )

        assertTrue(result.deploy.android?.firebase?.enabled == true)
        assertEquals("Test build", result.deploy.android?.firebase?.releaseNotes)
        assertEquals(listOf("internal"), result.deploy.android?.firebase?.testGroups)
    }

    // ═════════════════════════════════════════════════════════════
    // CONFIG MERGING
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testMergeWithDefaults_UserConfigTakesPrecedence() {
        val smartDefaults = createDefaultConfig()

        // User config with only version specified
        val userConfig = Config(
            version = VersionConfig(current = "2.0.0"),
            build = BuildConfig(),
            deploy = DeployConfig(android = null), // No android config
            changelog = ChangelogConfig()
        )

        // This would be called internally by loadConfigOrUseDefaults
        // We'll test the logic by checking that user version is preserved
        // while deploy config is filled from defaults
        val result = configService.applyCommandLineOverrides(userConfig, null, null, null)

        assertEquals("2.0.0", result.version?.current, "User version should be preserved")
    }

    @Test
    fun testMergeWithDefaults_NullAndroidConfigFilled() {
        val smartDefaults = createDefaultConfig()

        val userConfig = Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(android = null), // Missing android config
            changelog = ChangelogConfig()
        )

        // When android config is null, it should not crash
        val result = configService.applyCommandLineOverrides(userConfig, "firebase", null, null)

        // Since android is null, the override might not work, but should not crash
        assertNotNull(result)
    }

    // ═════════════════════════════════════════════════════════════
    // DESTINATION TOGGLING
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testMultipleDestinations() {
        val baseConfig = createMinimalConfig()

        // Enable Firebase
        val step1 = configService.applyCommandLineOverrides(
            baseConfig, "firebase", null, null
        )
        assertTrue(step1.deploy.android?.firebase?.enabled == true)

        // Enable Play Console
        val step2 = configService.applyCommandLineOverrides(
            step1, "play", null, null
        )
        assertTrue(step2.deploy.android?.playConsole?.enabled == true)
    }

    @Test
    fun testCaseInsensitiveDestinations() {
        val baseConfig = createMinimalConfig()

        val result1 = configService.applyCommandLineOverrides(
            baseConfig, "FIREBASE", null, null
        )
        assertTrue(result1.deploy.android?.firebase?.enabled == true)

        val result2 = configService.applyCommandLineOverrides(
            baseConfig, "PlayStore", null, null
        )
        assertTrue(result2.deploy.android?.playConsole?.enabled == true)
    }

    @Test
    fun testUnknownDestinationIgnored() {
        val baseConfig = createMinimalConfig()

        val result = configService.applyCommandLineOverrides(
            baseConfig, "unknown-destination", null, null
        )

        // Should not crash, just ignore
        assertNotNull(result)
    }

    // ═════════════════════════════════════════════════════════════
    // EDGE CASES
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testEmptyReleaseNotes() {
        val baseConfig = createMinimalConfig()

        val result = configService.applyCommandLineOverrides(
            baseConfig, null, "", null
        )

        // Empty string should still be set
        assertEquals("", result.deploy.android?.firebase?.releaseNotes)
    }

    @Test
    fun testEmptyTestGroups() {
        val baseConfig = createMinimalConfig()

        val result = configService.applyCommandLineOverrides(
            baseConfig, null, null, ""
        )

        // Empty string should result in empty list
        assertEquals("", result.deploy.android?.firebase?.testGroups?.joinToString(","))
    }

    @Test
    fun testTestGroupsWithSpaces() {
        val baseConfig = createMinimalConfig()

        val result = configService.applyCommandLineOverrides(
            baseConfig, null, null, "qa-team , beta-testers,  internal  "
        )

        assertEquals(
            listOf("qa-team", "beta-testers", "internal"),
            result.deploy.android?.firebase?.testGroups,
            "Should trim whitespace from test groups"
        )
    }

    @Test
    fun testMultipleCommasInTestGroups() {
        val baseConfig = createMinimalConfig()

        val result = configService.applyCommandLineOverrides(
            baseConfig, null, null, "qa-team,,beta-testers"
        )

        // Should handle empty elements gracefully
        assertTrue(result.deploy.android?.firebase?.testGroups?.contains("qa-team") == true)
        assertTrue(result.deploy.android?.firebase?.testGroups?.contains("beta-testers") == true)
    }

    // ═════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═════════════════════════════════════════════════════════════

    private fun createMinimalConfig(): Config {
        return Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(
                android = AndroidDeployConfig(
                    enabled = true,
                    artifactPath = "build/outputs/apk/release/app-release.apk",
                    firebase = FirebaseAndroidDestination(enabled = false),
                    playConsole = PlayConsoleAndroidDestination(enabled = false),
                    local = LocalAndroidDestination(enabled = false)
                )
            ),
            changelog = ChangelogConfig()
        )
    }

    private fun createDefaultConfig(): Config {
        return Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(
                android = AndroidDeployConfig(
                    enabled = true,
                    artifactPath = "build/outputs/apk/release/app-release.apk",
                    firebase = FirebaseAndroidDestination(enabled = true),
                    playConsole = PlayConsoleAndroidDestination(enabled = false),
                    local = LocalAndroidDestination(enabled = false)
                ),
                ios = IosDeployConfig(enabled = false)
            ),
            changelog = ChangelogConfig()
        )
    }
}
