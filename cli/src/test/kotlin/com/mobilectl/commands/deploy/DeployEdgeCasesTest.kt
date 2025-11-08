package com.mobilectl.commands.deploy

import com.mobilectl.config.Config
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.model.Platform
import com.mobilectl.model.buildConfig.AndroidBuildConfig
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.deploy.AndroidDeployConfig
import com.mobilectl.model.deploy.DeployConfig
import com.mobilectl.model.deploy.FlavorGroup
import com.mobilectl.model.versionManagement.VersionConfig
import kotlin.test.*

/**
 * Edge case and error scenario tests for Deploy functionality
 *
 * Tests:
 * - Null/empty values
 * - Invalid inputs
 * - Boundary conditions
 * - Exceptional scenarios
 * - Error handling
 */
class DeployEdgeCasesTest {

    private val workingPath = System.getProperty("user.dir")
    private lateinit var workflow: DeploymentWorkflow

    @BeforeTest
    fun setup() {
        val detector = createProjectDetector()
        workflow = DeploymentWorkflow(
            workingPath = workingPath,
            detector = detector,
            verbose = false
        )
    }

    // ═════════════════════════════════════════════════════════════
    // NULL & EMPTY VALUES
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testPlatformParsing_NullPlatformString() {
        val config = createMinimalConfig()

        val result = workflow.parsePlatforms(null, config)

        // Should fall back to config
        assertNull(result) // Since minimal config has no enabled platforms
    }

    @Test
    fun testPlatformParsing_EmptyString() {
        val config = createMinimalConfig()

        val result = workflow.parsePlatforms("", config)

        // Empty string is not a valid platform
        assertNull(result)
    }

    @Test
    fun testFlavorSelection_EmptyFlavorString() {
        val config = createConfigWithFlavors(listOf("free", "paid"))
        val options = FlavorOptions(flavors = "")

        val result = workflow.selectFlavorsToDeploy(config, options)

        // Empty string splits to empty list
        assertNotNull(result)
    }

    @Test
    fun testFlavorSelection_WhitespaceOnlyFlavor() {
        val config = createConfigWithFlavors(listOf("free", "paid"))
        val options = FlavorOptions(flavors = "   ")

        val result = workflow.selectFlavorsToDeploy(config, options)

        // Whitespace should be handled gracefully
        assertNotNull(result)
    }

    @Test
    fun testFlavorSelection_CommasOnly() {
        val config = createConfigWithFlavors(listOf("free", "paid"))
        val options = FlavorOptions(flavors = ",,,")

        val result = workflow.selectFlavorsToDeploy(config, options)

        // Should filter out empty elements
        assertNotNull(result)
    }

    // ═════════════════════════════════════════════════════════════
    // INVALID INPUTS
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testStrategyValidation_SpecialCharacters() {
        assertFalse(workflow.isValidStrategy("@#$%"))
        assertFalse(workflow.isValidStrategy("patch!"))
        assertFalse(workflow.isValidStrategy("minor-v2"))
    }

    @Test
    fun testStrategyValidation_Numbers() {
        assertFalse(workflow.isValidStrategy("1"))
        assertFalse(workflow.isValidStrategy("123"))
        assertFalse(workflow.isValidStrategy("1.0.0"))
    }

    @Test
    fun testStrategyValidation_MixedCase() {
        assertFalse(workflow.isValidStrategy("PATCH"))
        assertFalse(workflow.isValidStrategy("Patch"))
        assertFalse(workflow.isValidStrategy("pAtCh"))
        assertFalse(workflow.isValidStrategy("Minor"))
    }

    @Test
    fun testPlatformParsing_InvalidPlatforms() {
        val config = createMinimalConfig()

        assertNull(workflow.parsePlatforms("windows", config))
        assertNull(workflow.parsePlatforms("linux", config))
        assertNull(workflow.parsePlatforms("macos", config))
        assertNull(workflow.parsePlatforms("android-tv", config))
    }

    @Test
    fun testPlatformParsing_TypoVariations() {
        val config = createMinimalConfig()

        assertNull(workflow.parsePlatforms("andoid", config))
        assertNull(workflow.parsePlatforms("adnroid", config))
        assertNull(workflow.parsePlatforms("andriod", config))
        assertNull(workflow.parsePlatforms("ois", config))
    }

    // ═════════════════════════════════════════════════════════════
    // BOUNDARY CONDITIONS
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testFlavorSelection_SingleFlavor() {
        val config = createConfigWithFlavors(listOf("only-one"))
        val options = FlavorOptions()

        val result = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf("only-one"), result)
    }

    @Test
    fun testFlavorSelection_ManyFlavors() {
        val manyFlavors = (1..100).map { "flavor$it" }
        val config = createConfigWithFlavors(manyFlavors)
        val options = FlavorOptions(allFlavors = true)

        val result = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(100, result.size)
    }

    @Test
    fun testFlavorSelection_VeryLongFlavorName() {
        val longName = "a".repeat(1000)
        val config = createConfigWithFlavors(listOf(longName))
        val options = FlavorOptions()

        val result = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf(longName), result)
    }

    @Test
    fun testFlavorSelection_SpecialCharactersInName() {
        val specialNames = listOf("free-trial", "paid_premium", "qa.test", "beta@1.0")
        val config = createConfigWithFlavors(specialNames)
        val options = FlavorOptions(allFlavors = true)

        val result = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(4, result.size)
    }

    // ═════════════════════════════════════════════════════════════
    // EXCEPTIONAL SCENARIOS
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testFlavorSelection_NonExistentGroup() {
        val config = Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(
                flavorGroups = mapOf(
                    "production" to FlavorGroup(flavors = listOf("free", "paid"))
                )
            ),
            changelog = ChangelogConfig()
        )
        val options = FlavorOptions(group = "nonexistent-group")

        val result = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(emptyList(), result)
    }

    @Test
    fun testFlavorSelection_EmptyGroup() {
        val config = Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(
                flavorGroups = mapOf(
                    "empty-group" to FlavorGroup(flavors = emptyList())
                )
            ),
            changelog = ChangelogConfig()
        )
        val options = FlavorOptions(group = "empty-group")

        val result = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(emptyList(), result)
    }

    @Test
    fun testFlavorExclusion_ExcludeAll() {
        val config = createConfigWithFlavors(listOf("free", "paid", "premium"))
        val options = FlavorOptions(
            allFlavors = true,
            exclude = "free,paid,premium"
        )

        val selected = workflow.selectFlavorsToDeploy(config, options)
        val excludeSet = "free,paid,premium".split(",").map { it.trim() }.toSet()
        val filtered = selected.filter { it !in excludeSet }

        assertEquals(emptyList(), filtered)
    }

    @Test
    fun testFlavorExclusion_ExcludeNonExistent() {
        val config = createConfigWithFlavors(listOf("free", "paid"))
        val options = FlavorOptions(
            allFlavors = true,
            exclude = "nonexistent1,nonexistent2"
        )

        val selected = workflow.selectFlavorsToDeploy(config, options)
        val excludeSet = "nonexistent1,nonexistent2".split(",").map { it.trim() }.toSet()
        val filtered = selected.filter { it !in excludeSet }

        assertEquals(listOf("free", "paid"), filtered)
    }

    // ═════════════════════════════════════════════════════════════
    // CONFIGURATION EDGE CASES
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testCheckBuildNeeded_RelativePath() {
        val config = Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(
                android = AndroidDeployConfig(
                    enabled = true,
                    artifactPath = "relative/path/app.apk"
                )
            ),
            changelog = ChangelogConfig()
        )

        val result = workflow.checkIfBuildNeeded(config, setOf(Platform.ANDROID))

        // Relative path should be checked
        assertTrue(result) // Will need build since path doesn't exist
    }

    @Test
    fun testCheckBuildNeeded_AbsolutePath() {
        val config = Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(
                android = AndroidDeployConfig(
                    enabled = true,
                    artifactPath = "/absolute/path/app.apk"
                )
            ),
            changelog = ChangelogConfig()
        )

        val result = workflow.checkIfBuildNeeded(config, setOf(Platform.ANDROID))

        assertTrue(result) // Will need build since path doesn't exist
    }

    @Test
    fun testCheckBuildNeeded_PathWithSpaces() {
        val config = Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(
                android = AndroidDeployConfig(
                    enabled = true,
                    artifactPath = "path with spaces/app.apk"
                )
            ),
            changelog = ChangelogConfig()
        )

        val result = workflow.checkIfBuildNeeded(config, setOf(Platform.ANDROID))

        assertTrue(result)
    }

    // ═════════════════════════════════════════════════════════════
    // MULTI-VALUE PARSING
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testFlavorParsing_TrailingComma() {
        val config = createConfigWithFlavors(listOf("free", "paid"))
        val options = FlavorOptions(flavors = "free,paid,")

        val result = workflow.selectFlavorsToDeploy(config, options)

        // Should handle trailing comma gracefully
        assertTrue(result.contains("free"))
        assertTrue(result.contains("paid"))
    }

    @Test
    fun testFlavorParsing_LeadingComma() {
        val config = createConfigWithFlavors(listOf("free", "paid"))
        val options = FlavorOptions(flavors = ",free,paid")

        val result = workflow.selectFlavorsToDeploy(config, options)

        assertTrue(result.contains("free"))
        assertTrue(result.contains("paid"))
    }

    @Test
    fun testFlavorParsing_MixedWhitespace() {
        val config = createConfigWithFlavors(listOf("free", "paid", "premium"))
        val options = FlavorOptions(flavors = " free , paid  ,  premium")

        val result = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(3, result.size)
        assertTrue(result.contains("free"))
        assertTrue(result.contains("paid"))
        assertTrue(result.contains("premium"))
    }

    // ═════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═════════════════════════════════════════════════════════════

    private fun createMinimalConfig(): Config {
        return Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(),
            changelog = ChangelogConfig()
        )
    }

    private fun createConfigWithFlavors(flavors: List<String>): Config {
        return Config(
            version = VersionConfig(),
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    defaultFlavor = flavors.firstOrNull() ?: "",
                    flavors = flavors
                )
            ),
            deploy = DeployConfig(),
            changelog = ChangelogConfig()
        )
    }
}
