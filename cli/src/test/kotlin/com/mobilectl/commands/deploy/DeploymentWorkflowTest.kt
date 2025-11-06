package com.mobilectl.commands.deploy

import com.mobilectl.config.Config
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.model.Platform
import com.mobilectl.model.buildConfig.AndroidBuildConfig
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.deploy.AndroidDeployConfig
import com.mobilectl.model.deploy.DeployConfig
import com.mobilectl.model.versionManagement.VersionConfig
import kotlin.test.*

/**
 * Unit tests for DeploymentWorkflow
 *
 * Tests:
 * - Platform parsing
 * - Build necessity checks
 * - Strategy validation
 * - Flavor selection (already covered in DeployHandlerMultiFlavorTest)
 */
class DeploymentWorkflowTest {

    private lateinit var workflow: DeploymentWorkflow
    private val workingPath = System.getProperty("user.dir")

    @BeforeTest
    fun setup() {
        val detector = createProjectDetector()
        workflow = DeploymentWorkflow(
            workingPath = workingPath,
            detector = detector,
            verbose = false,
            allFlavors = false,
            group = null,
            flavors = null,
            exclude = null
        )
    }

    // ═════════════════════════════════════════════════════════════
    // PLATFORM PARSING
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testParsePlatforms_Android() {
        val config = createConfig(androidEnabled = true, iosEnabled = false)

        val result = workflow.parsePlatforms("android", config)

        assertNotNull(result)
        assertEquals(setOf(Platform.ANDROID), result)
    }

    @Test
    fun testParsePlatforms_iOS() {
        val config = createConfig(androidEnabled = false, iosEnabled = true)

        val result = workflow.parsePlatforms("ios", config)

        assertNotNull(result)
        assertEquals(setOf(Platform.IOS), result)
    }

    @Test
    fun testParsePlatforms_All() {
        val config = createConfig(androidEnabled = true, iosEnabled = true)

        val result = workflow.parsePlatforms("all", config)

        assertNotNull(result)
        assertEquals(setOf(Platform.ANDROID, Platform.IOS), result)
        assertEquals(2, result.size)
    }

    @Test
    fun testParsePlatforms_Invalid() {
        val config = createConfig(androidEnabled = true, iosEnabled = false)

        val result = workflow.parsePlatforms("invalid-platform", config)

        assertNull(result, "Invalid platform should return null")
    }

    @Test
    fun testParsePlatforms_NullUsesConfig() {
        val config = createConfig(androidEnabled = true, iosEnabled = false)

        val result = workflow.parsePlatforms(null, config)

        assertNotNull(result)
        assertTrue(Platform.ANDROID in result)
        assertFalse(Platform.IOS in result)
    }

    @Test
    fun testParsePlatforms_ConfigBothEnabled() {
        val config = createConfig(androidEnabled = true, iosEnabled = true)

        val result = workflow.parsePlatforms(null, config)

        assertNotNull(result)
        assertEquals(2, result.size)
        assertTrue(Platform.ANDROID in result)
        assertTrue(Platform.IOS in result)
    }

    @Test
    fun testParsePlatforms_ConfigNoneEnabled() {
        val config = createConfig(androidEnabled = false, iosEnabled = false)

        val result = workflow.parsePlatforms(null, config)

        // When no platforms enabled in config, should return null (will fall back to detector)
        assertNull(result)
    }

    // ═════════════════════════════════════════════════════════════
    // STRATEGY VALIDATION
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testIsValidStrategy_Patch() {
        assertTrue(workflow.isValidStrategy("patch"))
    }

    @Test
    fun testIsValidStrategy_Minor() {
        assertTrue(workflow.isValidStrategy("minor"))
    }

    @Test
    fun testIsValidStrategy_Major() {
        assertTrue(workflow.isValidStrategy("major"))
    }

    @Test
    fun testIsValidStrategy_Auto() {
        assertTrue(workflow.isValidStrategy("auto"))
    }

    @Test
    fun testIsValidStrategy_Manual() {
        assertTrue(workflow.isValidStrategy("manual"))
    }

    @Test
    fun testIsValidStrategy_Invalid() {
        assertFalse(workflow.isValidStrategy("invalid"))
        assertFalse(workflow.isValidStrategy(""))
        assertFalse(workflow.isValidStrategy("PATCH"))  // Case-sensitive
        assertFalse(workflow.isValidStrategy("1.0.0"))
    }

    @Test
    fun testIsValidStrategy_CaseSensitive() {
        assertTrue(workflow.isValidStrategy("patch"))
        assertFalse(workflow.isValidStrategy("PATCH"))
        assertFalse(workflow.isValidStrategy("Patch"))
    }

    // ═════════════════════════════════════════════════════════════
    // BUILD NECESSITY CHECKS
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testCheckIfBuildNeeded_ArtifactMissing() {
        val config = createConfigWithArtifactPath("/non/existent/path/app.apk")

        val needsBuild = workflow.checkIfBuildNeeded(config, setOf(Platform.ANDROID))

        assertTrue(needsBuild, "Should need build when artifact is missing")
    }

    @Test
    fun testCheckIfBuildNeeded_EmptyPlatforms() {
        val config = createConfig(androidEnabled = true, iosEnabled = false)

        val needsBuild = workflow.checkIfBuildNeeded(config, emptySet())

        assertFalse(needsBuild, "Should not need build for empty platforms")
    }

    @Test
    fun testCheckIfBuildNeeded_NullArtifactPath() {
        val config = Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(android = null), // No android config
            changelog = ChangelogConfig()
        )

        val needsBuild = workflow.checkIfBuildNeeded(config, setOf(Platform.ANDROID))

        assertTrue(needsBuild, "Should need build when artifact path is null")
    }

    // ═════════════════════════════════════════════════════════════
    // FLAVOR SELECTION EDGE CASES
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testSelectFlavorsToDeploy_NoFlavorsConfigured() {
        val config = Config(
            version = VersionConfig(),
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    defaultFlavor = "default",
                    flavors = emptyList()
                )
            ),
            deploy = DeployConfig(),
            changelog = ChangelogConfig()
        )

        val result = workflow.selectFlavorsToDeploy(config)

        assertEquals(listOf("default"), result, "Should fall back to defaultFlavor")
    }

    @Test
    fun testSelectFlavorsToDeploy_EmptyDefaultFlavor() {
        val config = Config(
            version = VersionConfig(),
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    defaultFlavor = "",
                    flavors = emptyList()
                )
            ),
            deploy = DeployConfig(),
            changelog = ChangelogConfig()
        )

        val result = workflow.selectFlavorsToDeploy(config)

        assertEquals(listOf(""), result)
    }

    // ═════════════════════════════════════════════════════════════
    // PLATFORM COMBINATIONS
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testParsePlatforms_AndroidOnly() {
        val config = createConfig(androidEnabled = true, iosEnabled = false)

        val android = workflow.parsePlatforms("android", config)
        val all = workflow.parsePlatforms("all", config)

        assertNotNull(android)
        assertTrue(Platform.ANDROID in android)
        assertFalse(Platform.IOS in android)

        assertNotNull(all)
        assertTrue(Platform.ANDROID in all)
        assertTrue(Platform.IOS in all)  // "all" includes iOS even if not enabled
    }

    @Test
    fun testParsePlatforms_iOSOnly() {
        val config = createConfig(androidEnabled = false, iosEnabled = true)

        val ios = workflow.parsePlatforms("ios", config)

        assertNotNull(ios)
        assertFalse(Platform.ANDROID in ios)
        assertTrue(Platform.IOS in ios)
    }

    // ═════════════════════════════════════════════════════════════
    // CONFIGURATION VALIDATION
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testParsePlatforms_ConfigWithNullDeploy() {
        val config = Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(android = null, ios = null),
            changelog = ChangelogConfig()
        )

        val result = workflow.parsePlatforms(null, config)

        // With no enabled platforms, should return null
        assertNull(result)
    }

    @Test
    fun testCheckIfBuildNeeded_MultiplePlatforms() {
        val config = createConfigWithMultiplePlatforms()

        val needsBuild = workflow.checkIfBuildNeeded(
            config,
            setOf(Platform.ANDROID, Platform.IOS)
        )

        // If any platform's artifact is missing, need to build
        assertTrue(needsBuild)
    }

    // ═════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═════════════════════════════════════════════════════════════

    private fun createConfig(androidEnabled: Boolean, iosEnabled: Boolean): Config {
        return Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(
                android = if (androidEnabled) {
                    AndroidDeployConfig(enabled = true)
                } else null,
                ios = if (iosEnabled) {
                    com.mobilectl.model.deploy.IosDeployConfig(enabled = true)
                } else null
            ),
            changelog = ChangelogConfig()
        )
    }

    private fun createConfigWithArtifactPath(path: String): Config {
        return Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(
                android = AndroidDeployConfig(
                    enabled = true,
                    artifactPath = path
                )
            ),
            changelog = ChangelogConfig()
        )
    }

    private fun createConfigWithMultiplePlatforms(): Config {
        return Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(
                android = AndroidDeployConfig(
                    enabled = true,
                    artifactPath = "/non/existent/android.apk"
                ),
                ios = com.mobilectl.model.deploy.IosDeployConfig(
                    enabled = true,
                    artifactPath = "/non/existent/ios.ipa"
                )
            ),
            changelog = ChangelogConfig()
        )
    }
}
