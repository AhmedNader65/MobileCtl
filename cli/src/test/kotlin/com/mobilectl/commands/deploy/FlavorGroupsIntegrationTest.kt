package com.mobilectl.commands.deploy

import com.mobilectl.config.Config
import com.mobilectl.config.createConfigParser
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.model.buildConfig.AndroidBuildConfig
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.deploy.DeployConfig
import com.mobilectl.model.deploy.FlavorGroup
import com.mobilectl.model.versionManagement.VersionConfig
import kotlin.test.*

/**
 * Integration tests for flavor groups functionality
 *
 * Tests the complete integration of:
 * - YAML config parsing (SnakeYamlParser)
 * - FlavorSelector logic
 * - DeploymentWorkflow orchestration
 * - Config + CLI interaction
 */
class FlavorGroupsIntegrationTest {

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
    // YAML PARSING → DEPLOYMENT INTEGRATION
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Complete flow from YAML parsing to flavor selection
     *
     * Scenario:
     * 1. Parse YAML config with flavor groups
     * 2. Select flavors using parsed config
     * 3. Verify correct flavors selected
     */
    @Test
    fun testYamlParsingToFlavorSelection() {
        val yaml = """
            build:
              android:
                enabled: true
                flavors:
                  - free
                  - paid
                  - premium
                  - qa

            deploy:
              enabled: true
              default_group: production

              flavor_groups:
                production:
                  name: Production
                  description: Production builds
                  flavors:
                    - free
                    - paid
                    - premium

                testing:
                  name: Testing
                  flavors:
                    - qa
        """.trimIndent()

        val parser = createConfigParser()
        val config = parser.parse(yaml)

        // Verify parsing worked
        assertNotNull(config.deploy)
        assertEquals("production", config.deploy.defaultGroup)
        assertEquals(2, config.deploy.flavorGroups.size)

        // Verify deployment uses parsed groups
        val productionOptions = FlavorOptions(group = "production")
        val productionFlavors = workflow.selectFlavorsToDeploy(config, productionOptions)

        assertEquals(listOf("free", "paid", "premium"), productionFlavors)

        val testingOptions = FlavorOptions(group = "testing")
        val testingFlavors = workflow.selectFlavorsToDeploy(config, testingOptions)

        assertEquals(listOf("qa"), testingFlavors)
    }

    /**
     * Test: YAML with camelCase keys also works
     *
     * Scenario:
     * - Config uses flavorGroups (camelCase) instead of flavor_groups
     * - Config uses defaultGroup instead of default_group
     * - Should still parse correctly
     */
    @Test
    fun testYamlParsingCamelCase() {
        val yaml = """
            build:
              android:
                enabled: true
                flavors:
                  - free
                  - paid

            deploy:
              enabled: true
              defaultGroup: production

              flavorGroups:
                production:
                  name: Production
                  flavors:
                    - free
                    - paid
        """.trimIndent()

        val parser = createConfigParser()
        val config = parser.parse(yaml)

        assertEquals("production", config.deploy.defaultGroup)
        assertEquals(1, config.deploy.flavorGroups.size)

        val options = FlavorOptions(group = "production")
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf("free", "paid"), flavors)
    }

    /**
     * Test: Default group is used when no CLI options
     *
     * Scenario:
     * - YAML has default_group: production
     * - No CLI flags provided
     * - Should automatically use production group
     */
    @Test
    fun testDefaultGroupFromYaml() {
        val yaml = """
            build:
              android:
                enabled: true
                flavors:
                  - free
                  - paid
                  - qa

            deploy:
              default_group: production

              flavor_groups:
                production:
                  flavors:
                    - free
                    - paid
                testing:
                  flavors:
                    - qa
        """.trimIndent()

        val parser = createConfigParser()
        val config = parser.parse(yaml)

        // No CLI options - should use default group
        val options = FlavorOptions()
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf("free", "paid"), flavors)
    }

    // ═════════════════════════════════════════════════════════════
    // CLI + CONFIG INTERACTION
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: CLI --all-flavors overrides config default group
     *
     * Scenario:
     * - Config has defaultGroup: production (2 flavors)
     * - CLI uses --all-flavors
     * - Should deploy all 5 flavors, not just production
     */
    @Test
    fun testCLIOverridesDefaultGroup() {
        val config = createConfigWithDefaultGroup()

        val options = FlavorOptions(allFlavors = true)
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(5, flavors.size)
        assertTrue(flavors.containsAll(listOf("free", "paid", "premium", "qa", "staging")))
    }

    /**
     * Test: CLI --flavor-group overrides config default group
     *
     * Scenario:
     * - Config has defaultGroup: production
     * - CLI uses --flavor-group testing
     * - Should deploy testing group, not production
     */
    @Test
    fun testCLIGroupOverridesDefaultGroup() {
        val config = createConfigWithDefaultGroup()

        val options = FlavorOptions(group = "testing")
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf("qa", "staging"), flavors)
    }

    /**
     * Test: CLI --flavors overrides config default group
     *
     * Scenario:
     * - Config has defaultGroup: production
     * - CLI uses --flavors free,qa
     * - Should deploy only free and qa
     */
    @Test
    fun testCLIFlavorsOverridesDefaultGroup() {
        val config = createConfigWithDefaultGroup()

        val options = FlavorOptions(flavors = "free,qa")
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf("free", "qa"), flavors)
    }

    /**
     * Test: CLI --exclude works with flavor groups
     *
     * Scenario:
     * - Deploy production group
     * - Exclude premium flavor
     * - Should deploy free and paid only
     */
    @Test
    fun testCLIExcludeWithGroup() {
        val config = createConfigWithDefaultGroup()

        val options = FlavorOptions(
            group = "production",
            exclude = "premium"
        )
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf("free", "paid"), flavors)
        assertFalse(flavors.contains("premium"))
    }

    // ═════════════════════════════════════════════════════════════
    // COMPLEX GROUP CONFIGURATIONS
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Groups can have overlapping flavors
     *
     * Scenario:
     * - premium-tiers: [paid, premium, enterprise]
     * - freemium: [free, paid]
     * - Both groups contain 'paid'
     */
    @Test
    fun testOverlappingGroupFlavors() {
        val config = Config(
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    flavors = listOf("free", "paid", "premium", "enterprise")
                )
            ),
            deploy = DeployConfig(
                flavorGroups = mapOf(
                    "premium-tiers" to FlavorGroup(
                        flavors = listOf("paid", "premium", "enterprise")
                    ),
                    "freemium" to FlavorGroup(
                        flavors = listOf("free", "paid")
                    )
                )
            ),
            version = VersionConfig(),
            changelog = ChangelogConfig()
        )

        // Test premium-tiers group
        val premiumOptions = FlavorOptions(group = "premium-tiers")
        val premiumFlavors = workflow.selectFlavorsToDeploy(config, premiumOptions)
        assertEquals(listOf("paid", "premium", "enterprise"), premiumFlavors)

        // Test freemium group
        val freemiumOptions = FlavorOptions(group = "freemium")
        val freemiumFlavors = workflow.selectFlavorsToDeploy(config, freemiumOptions)
        assertEquals(listOf("free", "paid"), freemiumFlavors)
    }

    /**
     * Test: Group with single flavor
     *
     * Scenario:
     * - hotfix group contains only 'productionRelease'
     * - Should still work correctly
     */
    @Test
    fun testSingleFlavorGroup() {
        val config = Config(
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    flavors = listOf("productionRelease", "productionDebug")
                )
            ),
            deploy = DeployConfig(
                flavorGroups = mapOf(
                    "hotfix" to FlavorGroup(
                        name = "Hotfix",
                        flavors = listOf("productionRelease")
                    )
                )
            ),
            version = VersionConfig(),
            changelog = ChangelogConfig()
        )

        val options = FlavorOptions(group = "hotfix")
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf("productionRelease"), flavors)
    }

    /**
     * Test: Empty group returns empty list
     *
     * Scenario:
     * - Group exists but has no flavors
     * - Should return empty list
     */
    @Test
    fun testEmptyFlavorGroup() {
        val config = Config(
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    flavors = listOf("free", "paid")
                )
            ),
            deploy = DeployConfig(
                flavorGroups = mapOf(
                    "empty-group" to FlavorGroup(
                        name = "Empty",
                        flavors = emptyList()
                    )
                )
            ),
            version = VersionConfig(),
            changelog = ChangelogConfig()
        )

        val options = FlavorOptions(group = "empty-group")
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        assertTrue(flavors.isEmpty())
    }

    // ═════════════════════════════════════════════════════════════
    // ERROR HANDLING
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Non-existent group returns empty list
     *
     * Scenario:
     * - Request group that doesn't exist
     * - Should return empty list (not throw exception)
     */
    @Test
    fun testNonExistentGroup() {
        val config = createConfigWithDefaultGroup()

        val options = FlavorOptions(group = "non-existent")
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        assertTrue(flavors.isEmpty())
    }

    /**
     * Test: No groups defined, request group
     *
     * Scenario:
     * - Config has no flavorGroups defined
     * - Request a group
     * - Should return empty list
     */
    @Test
    fun testNoGroupsDefined() {
        val config = Config(
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    flavors = listOf("free", "paid")
                )
            ),
            deploy = DeployConfig(
                flavorGroups = emptyMap()
            ),
            version = VersionConfig(),
            changelog = ChangelogConfig()
        )

        val options = FlavorOptions(group = "production")
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        assertTrue(flavors.isEmpty())
    }

    /**
     * Test: Default group specified but group doesn't exist
     *
     * Scenario:
     * - Config has defaultGroup: "production"
     * - But flavorGroups doesn't contain "production"
     * - Should return empty list (not crash)
     */
    @Test
    fun testDefaultGroupNotFoundInGroups() {
        val config = Config(
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    flavors = listOf("free", "paid"),
                    defaultFlavor = "free"
                )
            ),
            deploy = DeployConfig(
                defaultGroup = "production",  // This group doesn't exist
                flavorGroups = mapOf(
                    "testing" to FlavorGroup(
                        flavors = listOf("free")
                    )
                )
            ),
            version = VersionConfig(),
            changelog = ChangelogConfig()
        )

        val options = FlavorOptions()  // No CLI options, should use defaultGroup
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        // Should fall back to defaultFlavor when defaultGroup doesn't exist
        assertTrue(flavors.isEmpty() || flavors == listOf("free"))
    }

    // ═════════════════════════════════════════════════════════════
    // CONFIG SERIALIZATION ROUND-TRIP
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Parse → Serialize → Parse produces same result
     *
     * Scenario:
     * - Parse YAML to Config
     * - Serialize Config back to YAML
     * - Parse again
     * - Should get identical flavor groups
     */
    @Test
    fun testRoundTripSerialization() {
        val originalYaml = """
            deploy:
              enabled: true
              default_group: production

              flavor_groups:
                production:
                  name: Production
                  description: Production builds
                  flavors:
                    - free
                    - paid
                testing:
                  name: Testing
                  flavors:
                    - qa
        """.trimIndent()

        val parser = createConfigParser()

        // Parse original
        val config1 = parser.parse(originalYaml)

        // Serialize back to YAML
        val serializedYaml = parser.toYaml(config1)

        // Parse serialized YAML
        val config2 = parser.parse(serializedYaml)

        // Verify they match
        assertEquals(config1.deploy.defaultGroup, config2.deploy.defaultGroup)
        assertEquals(config1.deploy.flavorGroups.size, config2.deploy.flavorGroups.size)

        val group1 = config1.deploy.flavorGroups["production"]
        val group2 = config2.deploy.flavorGroups["production"]

        assertEquals(group1?.flavors, group2?.flavors)
    }

    // ═════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═════════════════════════════════════════════════════════════

    private fun createConfigWithDefaultGroup(): Config {
        return Config(
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    flavors = listOf("free", "paid", "premium", "qa", "staging"),
                    defaultFlavor = "free"
                )
            ),
            deploy = DeployConfig(
                enabled = true,
                defaultGroup = "production",
                flavorGroups = mapOf(
                    "production" to FlavorGroup(
                        name = "Production",
                        flavors = listOf("free", "paid", "premium")
                    ),
                    "testing" to FlavorGroup(
                        name = "Testing",
                        flavors = listOf("qa", "staging")
                    )
                )
            ),
            version = VersionConfig(),
            changelog = ChangelogConfig()
        )
    }
}
