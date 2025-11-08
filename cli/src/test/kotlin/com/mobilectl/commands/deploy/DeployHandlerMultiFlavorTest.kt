package com.mobilectl.commands.deploy

import com.mobilectl.config.Config
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.model.buildConfig.AndroidBuildConfig
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.deploy.DeployConfig
import com.mobilectl.model.deploy.FlavorGroup
import com.mobilectl.model.versionManagement.VersionConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for multi-flavor deployment
 *
 * Focuses on:
 * - Flavor selection (--all-variants, --group, --variants, --exclude)
 * - Config-driven flavor groups
 * - Flavor loop iteration
 * - Default flavor fallback
 *
 * Uses FlavorOptions data class (refactored API)
 */
class DeployHandlerMultiFlavorTest {

    private val workflow by lazy {
        val workingPath = System.getProperty("user.dir")
        val detector = createProjectDetector()
        DeploymentWorkflow(
            workingPath = workingPath,
            detector = detector,
            verbose = false
        )
    }

    // ═════════════════════════════════════════════════════════════
    // FLAVOR SELECTION DECISION TREE
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: --all-variants deploys all configured flavors
     *
     * Scenario:
     * - mobilectl deploy --all-variants
     * - Config has: flavors: [free, paid, premium]
     *
     * Expected: Deploy all 3 flavors
     */
    @Test
    fun testAllVariantsSelectsAllFlavors() {
        val config = createConfigWithFlavors(
            flavors = listOf("free", "paid", "premium")
        )
        val options = FlavorOptions(allFlavors = true)

        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(
            listOf("free", "paid", "premium"),
            selected,
            "Should select all configured flavors"
        )
    }

    /**
     * Test: --all-variants with empty flavors config
     *
     * Scenario:
     * - mobilectl deploy --all-variants
     * - Config has: flavors: []
     *
     * Expected: Falls back to defaultFlavor
     */
    @Test
    fun testAllVariantsWithNoConfiguredFlavors() {
        val config = createConfigWithFlavors(
            flavors = emptyList(),
            defaultFlavor = ""
        )
        val options = FlavorOptions(allFlavors = true)

        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf(""), selected, "Should return defaultFlavor when no flavors configured")
    }

    /**
     * Test: --variant-group deploys specific group
     *
     * Scenario:
     * - mobilectl deploy --variant-group production
     * - Config has:
     *   variantGroups:
     *     production: [free, paid, premium, enterprise]
     *     testing: [qa, staging]
     *
     * Expected: Deploy production group (4 flavors)
     */
    @Test
    fun testVariantGroupSelectsGroup() {
        val config = createConfigWithGroups(
            groups = mapOf(
                "production" to FlavorGroup(
                    name = "Production",
                    flavors = listOf("free", "paid", "premium", "enterprise")
                ),
                "testing" to FlavorGroup(
                    name = "Testing",
                    flavors = listOf("qa", "staging")
                )
            )
        )
        val options = FlavorOptions(group = "production")

        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(
            listOf("free", "paid", "premium", "enterprise"),
            selected,
            "Should select production group"
        )
    }

    /**
     * Test: --variant-group with non-existent group
     *
     * Scenario:
     * - mobilectl deploy --variant-group unknown
     * - Config has: only "production" group
     *
     * Expected: Empty list (group doesn't exist)
     */
    @Test
    fun testVariantGroupNotFound() {
        val config = createConfigWithGroups(
            groups = mapOf(
                "production" to FlavorGroup(
                    name = "Production",
                    flavors = listOf("free", "paid")
                )
            )
        )
        val options = FlavorOptions(group = "unknown")

        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(emptyList(), selected, "Should return empty when group not found")
    }

    /**
     * Test: --variants specific flavors
     *
     * Scenario:
     * - mobilectl deploy --variants free,premium
     * - Config has: flavors: [free, paid, premium]
     *
     * Expected: Deploy only free and premium
     */
    @Test
    fun testVariantsSelectsSpecific() {
        val config = createConfigWithFlavors(
            flavors = listOf("free", "paid", "premium")
        )
        val options = FlavorOptions(flavors = "free,premium")

        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(
            listOf("free", "premium"),
            selected,
            "Should select only specified flavors"
        )
    }

    /**
     * Test: --exclude filters out flavors
     *
     * Scenario:
     * - mobilectl deploy --all-variants --exclude qa,staging
     * - Config has: flavors: [free, paid, premium, qa, staging]
     *
     * Expected: Deploy free, paid, premium (excluding qa, staging)
     */
    @Test
    fun testExcludeFiltersOut() {
        val config = createConfigWithFlavors(
            flavors = listOf("free", "paid", "premium", "qa", "staging")
        )
        val options = FlavorOptions(
            allFlavors = true,
            exclude = "qa,staging"
        )

        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(
            listOf("free", "paid", "premium"),
            selected,
            "Should exclude qa and staging"
        )
    }

    // ═════════════════════════════════════════════════════════════
    // PRECEDENCE RULES
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: CLI --all-variants wins over config defaultGroup
     *
     * Scenario:
     * - mobilectl deploy --all-variants
     * - Config has: defaultGroup: production (only 4 flavors)
     *
     * Expected: Deploy ALL 6 flavors (CLI wins)
     */
    @Test
    fun testCLIAllVariantsWinsOverConfig() {
        val config = createConfigWithDefaults(defaultGroup = "production")
        val options = FlavorOptions(allFlavors = true)

        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(6, selected.size, "Should deploy all 6 flavors, not just production group")
        assertTrue(selected.contains("free"))
        assertTrue(selected.contains("paid"))
        assertTrue(selected.contains("premium"))
        assertTrue(selected.contains("enterprise"))
        assertTrue(selected.contains("qa"))
        assertTrue(selected.contains("staging"))
    }

    /**
     * Test: CLI --variants wins over config defaultGroup
     *
     * Scenario:
     * - mobilectl deploy --variants free,paid
     * - Config has: defaultGroup: production
     *
     * Expected: Deploy only free and paid (CLI wins)
     */
    @Test
    fun testCLIVariantsWinsOverConfig() {
        val config = createConfigWithDefaults(defaultGroup = "production")
        val options = FlavorOptions(flavors = "free,paid")

        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(
            listOf("free", "paid"),
            selected,
            "CLI variants should win over config defaultGroup"
        )
    }

    /**
     * Test: Config defaultGroup used when no CLI options
     *
     * Scenario:
     * - mobilectl deploy (no CLI flavor options)
     * - Config has: defaultGroup: production
     *
     * Expected: Deploy production group (4 flavors)
     */
    @Test
    fun testConfigDefaultGroupUsedWhenNoCLI() {
        val config = createConfigWithDefaults(defaultGroup = "production")
        val options = FlavorOptions()  // No CLI options

        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(
            listOf("free", "paid", "premium", "enterprise"),
            selected,
            "Should use config defaultGroup when no CLI options"
        )
    }

    /**
     * Test: Default flavor fallback
     *
     * Scenario:
     * - mobilectl deploy (no CLI options, no config defaultGroup)
     * - Config has: defaultFlavor: free
     *
     * Expected: Deploy only defaultFlavor
     */
    @Test
    fun testDefaultFlavorFallback() {
        val config = createConfigWithDefaults(defaultGroup = null)
        val options = FlavorOptions()

        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(
            listOf("free"),
            selected,
            "Should fall back to defaultFlavor when no group or CLI options"
        )
    }

    // ═════════════════════════════════════════════════════════════
    // FLAVOR GROUPS CONFIGURATION
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Flavor groups can group related variants
     *
     * Scenario:
     * - Config has:
     *   variantGroups:
     *     production: [free, paid, premium, enterprise]
     *
     * Expected: Group contains all 4 production flavors
     */
    @Test
    fun testFlavorGroupsConfiguration() {
        val config = createConfigWithGroups(
            groups = mapOf(
                "production" to FlavorGroup(
                    name = "Production",
                    flavors = listOf("free", "paid", "premium", "enterprise")
                )
            )
        )
        val options = FlavorOptions(group = "production")

        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(4, selected.size)
        assertEquals(listOf("free", "paid", "premium", "enterprise"), selected)
    }

    /**
     * Test: Multiple groups available, each with different flavors
     *
     * Scenario:
     * - Config has:
     *   variantGroups:
     *     production: [free, paid, premium, enterprise]
     *     testing: [qa, staging]
     *
     * Expected: Can select either group independently
     */
    @Test
    fun testMultipleGroupsAvailable() {
        val config = createConfigWithGroups(
            groups = mapOf(
                "production" to FlavorGroup(
                    flavors = listOf("free", "paid", "premium", "enterprise")
                ),
                "testing" to FlavorGroup(
                    flavors = listOf("qa", "staging")
                )
            )
        )

        // Select production
        val productionOptions = FlavorOptions(group = "production")
        val production = workflow.selectFlavorsToDeploy(config, productionOptions)
        assertEquals(listOf("free", "paid", "premium", "enterprise"), production)

        // Select testing
        val testingOptions = FlavorOptions(group = "testing")
        val testing = workflow.selectFlavorsToDeploy(config, testingOptions)
        assertEquals(listOf("qa", "staging"), testing)
    }

    // ═════════════════════════════════════════════════════════════
    // REAL-WORLD WORKFLOWS
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Production release workflow
     *
     * Scenario:
     * - mobilectl deploy --variant-group production
     * - Config has production group with 4 flavors
     *
     * Expected: Deploy all production flavors
     */
    @Test
    fun testProductionReleaseWorkflow() {
        val config = createConfigWithDefaults(defaultGroup = "production")
        val options = FlavorOptions(group = "production")

        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(4, selected.size, "Production should have 4 flavors")
        assertTrue(selected.contains("free"))
        assertTrue(selected.contains("paid"))
        assertTrue(selected.contains("premium"))
        assertTrue(selected.contains("enterprise"))
        assertFalse(selected.contains("qa"), "QA not in production group")
        assertFalse(selected.contains("staging"), "Staging not in production group")
    }

    /**
     * Test: QA testing workflow
     *
     * Scenario:
     * - mobilectl deploy --variant-group testing
     * - Config has testing group with qa and staging
     *
     * Expected: Deploy only qa and staging
     */
    @Test
    fun testQATestingWorkflow() {
        val config = createConfigWithDefaults(defaultGroup = "production")
        val options = FlavorOptions(group = "testing")

        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf("qa", "staging"), selected)
    }

    /**
     * Test: Deploy all except QA
     *
     * Scenario:
     * - mobilectl deploy --all-variants --exclude qa,staging
     *
     * Expected: Deploy all production flavors
     */
    @Test
    fun testDeployAllExceptQA() {
        val config = createConfigWithDefaults(defaultGroup = "production")
        val options = FlavorOptions(
            allFlavors = true,
            exclude = "qa,staging"
        )

        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(4, selected.size)
        assertEquals(listOf("free", "paid", "premium", "enterprise"), selected)
    }

    // ═════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═════════════════════════════════════════════════════════════

    private fun createConfigWithFlavors(
        flavors: List<String> = listOf("free", "paid", "premium"),
        defaultFlavor: String = "free"
    ): Config {
        return Config(
            version = VersionConfig(),
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    defaultFlavor = defaultFlavor,
                    flavors = flavors
                )
            ),
            deploy = DeployConfig(),
            changelog = ChangelogConfig()
        )
    }

    private fun createConfigWithGroups(
        groups: Map<String, FlavorGroup>,
        defaultFlavor: String = "free"
    ): Config {
        val allFlavors = groups.values.flatMap { it.flavors }.distinct()

        return Config(
            version = VersionConfig(),
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    defaultFlavor = defaultFlavor,
                    flavors = allFlavors
                )
            ),
            deploy = DeployConfig(
                flavorGroups = groups
            ),
            changelog = ChangelogConfig()
        )
    }

    private fun createConfigWithDefaults(
        defaultGroup: String? = "production"
    ): Config {
        return Config(
            version = VersionConfig(),
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    defaultFlavor = "free",
                    flavors = listOf("free", "paid", "premium", "enterprise", "qa", "staging")
                )
            ),
            deploy = DeployConfig(
                flavorGroups = mapOf(
                    "production" to FlavorGroup(
                        flavors = listOf("free", "paid", "premium", "enterprise")
                    ),
                    "testing" to FlavorGroup(
                        flavors = listOf("qa", "staging")
                    )
                ),
                defaultGroup = defaultGroup
            ),
            changelog = ChangelogConfig()
        )
    }
}
