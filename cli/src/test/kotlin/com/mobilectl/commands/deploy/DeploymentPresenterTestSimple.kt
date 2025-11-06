package com.mobilectl.commands.deploy

import com.mobilectl.config.Config
import com.mobilectl.model.Platform
import com.mobilectl.model.buildConfig.AndroidBuildConfig
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.deploy.AndroidDeployConfig
import com.mobilectl.model.deploy.DeployConfig
import com.mobilectl.model.deploy.FirebaseAndroidDestination
import com.mobilectl.model.versionManagement.VersionConfig
import kotlin.test.*

/**
 * Simplified unit tests for DeploymentPresenter
 *
 * Tests that methods execute without errors rather than
 * trying to capture output (which goes through PremiumLogger)
 */
class DeploymentPresenterTestSimple {

    private lateinit var presenter: DeploymentPresenter

    @BeforeTest
    fun setup() {
        presenter = DeploymentPresenter()
    }

    // ═══════════════════════════════════════════════════════════
    // BASIC EXECUTION TESTS - Verify no crashes
    // ═══════════════════════════════════════════════════════════

    @Test
    fun testPrintSummary_DoesNotCrash() {
        val config = createBasicConfig()

        // Should execute without throwing
        presenter.printSummary(config, setOf(Platform.ANDROID), "dev")
    }

    @Test
    fun testPrintSummary_MultiplePlatforms_DoesNotCrash() {
        val config = createBasicConfig()

        presenter.printSummary(config, setOf(Platform.ANDROID, Platform.IOS), "production")
    }

    @Test
    fun testPrintSummary_EmptyPlatforms_DoesNotCrash() {
        val config = createBasicConfig()

        presenter.printSummary(config, emptySet(), "dev")
    }

    @Test
    fun testShowDryRunDetails_DoesNotCrash() {
        val config = createBasicConfig()

        presenter.showDryRunDetails(config, setOf(Platform.ANDROID), "dev")
    }

    @Test
    fun testShowDryRunDetails_MultiplePlatforms_DoesNotCrash() {
        val config = createBasicConfig()

        presenter.showDryRunDetails(config, setOf(Platform.ANDROID, Platform.IOS), "staging")
    }

    @Test
    fun testShowDeploymentHeader_DoesNotCrash() {
        presenter.showDeploymentHeader("production", setOf(Platform.ANDROID))
    }

    // ═══════════════════════════════════════════════════════════
    // CONSTRUCTOR VARIATIONS
    // ═══════════════════════════════════════════════════════════

    @Test
    fun testPresenter_WithAllFlavors() {
        val presenterWithFlavors = DeploymentPresenter(allFlavors = true)

        presenterWithFlavors.printSummary(createBasicConfig(), setOf(Platform.ANDROID), "dev")
    }

    @Test
    fun testPresenter_WithGroup() {
        val presenterWithGroup = DeploymentPresenter(group = "production")

        presenterWithGroup.printSummary(createBasicConfig(), setOf(Platform.ANDROID), "dev")
    }

    @Test
    fun testPresenter_WithFlavors() {
        val presenterWithFlavors = DeploymentPresenter(flavors = "free,paid")

        presenterWithFlavors.printSummary(createBasicConfig(), setOf(Platform.ANDROID), "dev")
    }

    @Test
    fun testPresenter_WithExclude() {
        val presenterWithExclude = DeploymentPresenter(exclude = "qa,staging")

        presenterWithExclude.printSummary(createBasicConfig(), setOf(Platform.ANDROID), "dev")
    }

    @Test
    fun testPresenter_WithVersionBump() {
        val presenterWithBump = DeploymentPresenter(bumpVersion = "minor")

        presenterWithBump.printSummary(createBasicConfig(), setOf(Platform.ANDROID), "dev")
    }

    @Test
    fun testPresenter_WithChangelog() {
        val presenterWithChangelog = DeploymentPresenter(changelog = true)

        presenterWithChangelog.printSummary(createBasicConfig(), setOf(Platform.ANDROID), "dev")
    }

    @Test
    fun testPresenter_WithReleaseNotes() {
        val presenterWithNotes = DeploymentPresenter(releaseNotes = "Test release")

        presenterWithNotes.printSummary(createBasicConfig(), setOf(Platform.ANDROID), "dev")
    }

    @Test
    fun testPresenter_WithTestGroups() {
        val presenterWithGroups = DeploymentPresenter(testGroups = "qa,beta")

        presenterWithGroups.printSummary(createBasicConfig(), setOf(Platform.ANDROID), "dev")
    }

    @Test
    fun testPresenter_WithSkipBuild() {
        val presenterSkipBuild = DeploymentPresenter(skipBuild = true)

        presenterSkipBuild.printSummary(createBasicConfig(), setOf(Platform.ANDROID), "dev")
    }

    // ═══════════════════════════════════════════════════════════
    // EDGE CASES
    // ═══════════════════════════════════════════════════════════

    @Test
    fun testPrintSummary_NullDeployConfig() {
        val config = Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(android = null),
            changelog = ChangelogConfig()
        )

        // Should handle null gracefully
        presenter.printSummary(config, setOf(Platform.ANDROID), "dev")
    }

    @Test
    fun testShowDryRunDetails_NullDeployConfig() {
        val config = Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(android = null),
            changelog = ChangelogConfig()
        )

        presenter.showDryRunDetails(config, setOf(Platform.ANDROID), "dev")
    }

    @Test
    fun testPrintSummary_EmptyEnvironment() {
        val config = createBasicConfig()

        presenter.printSummary(config, setOf(Platform.ANDROID), "")
    }

    @Test
    fun testShowDeploymentHeader_EmptyEnvironment() {
        presenter.showDeploymentHeader("", setOf(Platform.ANDROID))
    }

    @Test
    fun testShowDeploymentHeader_EmptyPlatforms() {
        presenter.showDeploymentHeader("production", emptySet())
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════════════

    private fun createBasicConfig(): Config {
        return Config(
            version = VersionConfig(),
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    defaultFlavor = "free"
                )
            ),
            deploy = DeployConfig(
                android = AndroidDeployConfig(
                    enabled = true,
                    firebase = FirebaseAndroidDestination(enabled = true)
                )
            ),
            changelog = ChangelogConfig()
        )
    }
}
