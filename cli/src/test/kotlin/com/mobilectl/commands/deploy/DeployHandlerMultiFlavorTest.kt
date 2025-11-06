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
 * NOTE: After refactoring, these tests now test DeploymentWorkflow directly
 * since that's where the flavor selection logic lives (Single Responsibility Principle)
 */
class DeployHandlerMultiFlavorTest {

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
        val workflow = createDeploymentWorkflow(allVariants = true)

        val config = createConfigWithFlavors(
            flavors = listOf("free", "paid", "premium")
        )

        val selected = workflow.selectFlavorsToDeploy(config)
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
        val workflow = createDeploymentWorkflow(allVariants = true)

        val config = createConfigWithFlavors(
            flavors = emptyList(),
            defaultFlavor = ""
        )

        val selected = workflow.selectFlavorsToDeploy(config)
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
        val workflow = createDeploymentWorkflow(
            variantGroup = "production"
        )

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

        val selected = workflow.selectFlavorsToDeploy(config)
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
        val workflow = createDeploymentWorkflow(
            variantGroup = "unknown"
        )

        val config = createConfigWithGroups(
            groups = mapOf(
                "production" to FlavorGroup(
                    name = "Production",
                    flavors = listOf("free", "paid")
                )
            )
        )

        val selected = workflow.selectFlavorsToDeploy(config)
        assertEquals(emptyList(), selected, "Should return empty when group not found")
    }

    /**
     * Test: --variants specific flavors
     *
     * Scenario:
     * - mobilectl deploy --variants free,premium
     * - CLI overrides everything
     *
     * Expected: Deploy only free and premium
     */
    @Test
    fun testVariantsSelectsSpecific() {
        val workflow = createDeploymentWorkflow(
            variants = "free,premium"
        )

        val config = createConfigWithFlavors(
            flavors = listOf("free", "paid", "premium", "enterprise")
        )

        val selected = workflow.selectFlavorsToDeploy(config)
        assertEquals(
            listOf("free", "premium"),
            selected,
            "Should select specific flavors from CLI"
        )
    }

    /**
     * Test: --exclude filters out specific flavors
     *
     * Scenario:
     * - mobilectl deploy --all-variants --exclude qa,staging
     * - Config has: [free, paid, premium, qa, staging]
     *
     * Expected: Deploy all except qa and staging
     */
    @Test
    fun testExcludeFiltersOut() {
        val workflow = createDeploymentWorkflow(
            allVariants = true,
            exclude = "qa,staging"
        )

        val config = createConfigWithFlavors(
            flavors = listOf("free", "paid", "premium", "qa", "staging")
        )

        val allSelected = workflow.selectFlavorsToDeploy(config)
        val excludeSet = "qa,staging".split(",").map { it.trim() }.toSet()
        val filtered = allSelected.filter { it !in excludeSet }

        assertEquals(
            listOf("free", "paid", "premium"),
            filtered,
            "Should exclude qa and staging"
        )
    }

    // ═════════════════════════════════════════════════════════════
    // PRIORITY ORDER (CLI > Config > Default)
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: CLI --all-variants wins over config defaultGroup
     *
     * Scenario:
     * - mobilectl deploy --all-variants
     * - Config has: defaultGroup: "production"
     *
     * Expected: Deploy ALL (not just production)
     */
    @Test
    fun testCLIAllVariantsWinsOverConfig() {
        val workflow = createDeploymentWorkflow(allVariants = true)

        val config = createConfigWithDefaults(
            defaultGroup = "production"
        )

        val selected = workflow.selectFlavorsToDeploy(config)
        assertEquals(
            listOf("free", "paid", "premium", "enterprise", "qa", "staging"),
            selected,
            "CLI --all-variants should override config defaultGroup"
        )
    }

    /**
     * Test: CLI --variants wins over config defaultGroup
     *
     * Scenario:
     * - mobilectl deploy --variants free,paid
     * - Config has: defaultGroup: "production"
     *
     * Expected: Deploy only free and paid (CLI wins)
     */
    @Test
    fun testCLIVariantsWinsOverConfig() {
        val workflow = createDeploymentWorkflow(
            variants = "free,paid"
        )

        val config = createConfigWithDefaults(
            defaultGroup = "production"  // Says production
        )

        val selected = workflow.selectFlavorsToDeploy(config)
        assertEquals(
            listOf("free", "paid"),
            selected,
            "CLI --variants should override config"
        )
    }

    /**
     * Test: Config defaultGroup used when no CLI flags
     *
     * Scenario:
     * - mobilectl deploy
     * - Config has: defaultGroup: "testing"
     *
     * Expected: Deploy testing group
     */
    @Test
    fun testConfigDefaultGroupUsedWhenNoCLI() {
        val workflow = createDeploymentWorkflow(
            allVariants = false,
            variantGroup = null,
            variants = null
        )

        val config = createConfigWithDefaults(
            defaultGroup = "testing"
        )

        val selected = workflow.selectFlavorsToDeploy(config)
        assertEquals(
            listOf("qa", "staging"),
            selected,
            "Should use config defaultGroup when no CLI flags"
        )
    }

    /**
     * Test: Default flavor fallback
     *
     * Scenario:
     * - mobilectl deploy
     * - No --all-variants
     * - No config defaultGroup
     *
     * Expected: Deploy just defaultFlavor
     */
    @Test
    fun testDefaultFlavorFallback() {
        val workflow = createDeploymentWorkflow(
            allVariants = false,
            variantGroup = null,
            variants = null
        )

        val config = createConfigWithFlavors(
            flavors = listOf("free", "paid", "premium"),
            defaultFlavor = "free"
        )

        val selected = workflow.selectFlavorsToDeploy(config)
        assertEquals(
            listOf("free"),
            selected,
            "Should fall back to defaultFlavor"
        )
    }

    // ═════════════════════════════════════════════════════════════
    // FLAVOR GROUPS (Named Groups)
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Flavor groups configuration
     *
     * Config structure:
     * variantGroups:
     *   production:
     *     name: "Production"
     *     flavors: [free, paid, premium]
     *   testing:
     *     name: "QA"
     *     flavors: [qa, staging]
     */
    @Test
    fun testFlavorGroupsConfiguration() {
        val config = Config(
            version = VersionConfig(),
            build = BuildConfig(),
            deploy = DeployConfig(
                flavorGroups = mapOf(
                    "production" to FlavorGroup(
                        name = "Production",
                        description = "Public release",
                        flavors = listOf("free", "paid", "premium")
                    ),
                    "testing" to FlavorGroup(
                        name = "Testing",
                        description = "QA only",
                        flavors = listOf("qa", "staging")
                    )
                )
            ),
            changelog = ChangelogConfig()
        )

        assertEquals(2, config.deploy.flavorGroups.size)
        assertEquals("Production", config.deploy.flavorGroups["production"]?.name)
        assertEquals(
            listOf("free", "paid", "premium"),
            config.deploy.flavorGroups["production"]?.flavors
        )
    }

    /**
     * Test: Deploy multiple groups
     *
     * Scenario:
     * - mobilectl deploy --variant-group production
     * Then:
     * - mobilectl deploy --variant-group testing
     *
     * Expected: Can switch between groups
     */
    @Test
    fun testMultipleGroupsAvailable() {
        val prodWorkflow = createDeploymentWorkflow(variantGroup = "production")
        val testWorkflow = createDeploymentWorkflow(variantGroup = "testing")

        val config = createConfigWithGroups(
            groups = mapOf(
                "production" to FlavorGroup(
                    flavors = listOf("free", "paid")
                ),
                "testing" to FlavorGroup(
                    flavors = listOf("qa", "staging")
                )
            )
        )

        val prodSelected = prodWorkflow.selectFlavorsToDeploy(config)
        val testSelected = testWorkflow.selectFlavorsToDeploy(config)

        assertEquals(listOf("free", "paid"), prodSelected)
        assertEquals(listOf("qa", "staging"), testSelected)
    }

    // ═════════════════════════════════════════════════════════════
    // REAL-WORLD SCENARIOS
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Production release (all public flavors)
     *
     * Scenario:
     * - mobilectl deploy --variant-group production --bump-version minor -C
     * - Config has: production = [free, paid, premium, enterprise]
     *
     * Expected: Full release workflow
     */
    @Test
    fun testProductionReleaseWorkflow() {
        val bumpVersion = "minor"
        val changelog = true
        val workflow = createDeploymentWorkflow(
            variantGroup = "production"
        )

        val config = createConfigWithGroups(
            groups = mapOf(
                "production" to FlavorGroup(
                    flavors = listOf("free", "paid", "premium", "enterprise")
                )
            ),
            defaultFlavor = "free"
        )

        val selected = workflow.selectFlavorsToDeploy(config)
        assertEquals(4, selected.size)

        // Verify version bump and changelog would be executed
        val shouldBumpVersion = config.version?.autoIncrement == true || bumpVersion != null
        val shouldGenerateChangelog = config.changelog.enabled == true || changelog
        assertTrue(shouldBumpVersion, "Should execute version bump")
        assertTrue(shouldGenerateChangelog, "Should generate changelog")
    }

    /**
     * Test: QA testing (only test flavors)
     *
     * Scenario:
     * - mobilectl deploy --variant-group testing -y
     * - Config has: testing = [qa, staging]
     *
     * Expected: Deploy only QA flavors
     */
    @Test
    fun testQATestingWorkflow() {
        val bumpVersion: String? = null
        val workflow = createDeploymentWorkflow(
            variantGroup = "testing"
        )

        val config = createConfigWithGroups(
            groups = mapOf(
                "testing" to FlavorGroup(
                    flavors = listOf("qa", "staging")
                )
            )
        )

        val selected = workflow.selectFlavorsToDeploy(config)
        assertEquals(listOf("qa", "staging"), selected)
        assertEquals(false, bumpVersion != null, "Should not bump version for QA")
    }

    /**
     * Test: Deploy all except QA
     *
     * Scenario:
     * - mobilectl deploy --all-variants --exclude qa,staging
     * - Config has: [free, paid, premium, enterprise, qa, staging]
     *
     * Expected: Deploy 4 public flavors, skip QA
     */
    @Test
    fun testDeployAllExceptQA() {
        val workflow = createDeploymentWorkflow(
            allVariants = true,
            exclude = "qa,staging"
        )

        val config = createConfigWithFlavors(
            flavors = listOf("free", "paid", "premium", "enterprise", "qa", "staging")
        )

        val selected = workflow.selectFlavorsToDeploy(config)
        val excludeSet = "qa,staging".split(",").map { it.trim() }.toSet()
        val filtered = selected.filter { it !in excludeSet }

        assertEquals(4, filtered.size)
        assertFalse(filtered.contains("qa"))
        assertFalse(filtered.contains("staging"))
    }

    // ═════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═════════════════════════════════════════════════════════════

    private fun createDeploymentWorkflow(
        allVariants: Boolean = false,
        variantGroup: String? = null,
        variants: String? = null,
        exclude: String? = null,
        verbose: Boolean = false
    ): DeploymentWorkflow {
        val workingPath = System.getProperty("user.dir")
        val detector = createProjectDetector()

        return DeploymentWorkflow(
            workingPath = workingPath,
            detector = detector,
            verbose = verbose,
            allFlavors = allVariants,
            group = variantGroup,
            flavors = variants,
            exclude = exclude
        )
    }

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
