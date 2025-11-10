package com.mobilectl.config

import com.mobilectl.model.deploy.DeployConfig
import com.mobilectl.model.deploy.FlavorGroup
import kotlin.test.*

/**
 * Comprehensive tests for flavor groups config parsing
 *
 * Tests real-world YAML configurations including:
 * - Various syntax styles (snake_case, camelCase, mixed)
 * - Complex nested structures
 * - Edge cases and error scenarios
 * - Real config file examples
 */
class FlavorGroupsConfigParsingTest {

    private val parser = createConfigParser()

    // ═════════════════════════════════════════════════════════════
    // BASIC PARSING TESTS
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testParseBasicFlavorGroups() {
        val yaml = """
            deploy:
              flavor_groups:
                production:
                  name: Production
                  description: Production builds
                  flavors:
                    - free
                    - paid
                    - premium
        """.trimIndent()

        val config = parser.parse(yaml)

        assertNotNull(config.deploy)
        assertEquals(1, config.deploy.flavorGroups.size)

        val group = config.deploy.flavorGroups["production"]
        assertNotNull(group)
        assertEquals("Production", group.name)
        assertEquals("Production builds", group.description)
        assertEquals(3, group.flavors.size)
        assertEquals(listOf("free", "paid", "premium"), group.flavors)
    }

    @Test
    fun testParseDefaultGroup() {
        val yaml = """
            deploy:
              default_group: production

              flavor_groups:
                production:
                  flavors:
                    - free
                    - paid
        """.trimIndent()

        val config = parser.parse(yaml)

        assertEquals("production", config.deploy.defaultGroup)
    }

    @Test
    fun testParseMultipleGroups() {
        val yaml = """
            deploy:
              flavor_groups:
                production:
                  name: Production
                  flavors:
                    - free
                    - paid

                testing:
                  name: Testing
                  flavors:
                    - qa
                    - staging

                premium:
                  name: Premium
                  flavors:
                    - enterprise
        """.trimIndent()

        val config = parser.parse(yaml)

        assertEquals(3, config.deploy.flavorGroups.size)
        assertTrue(config.deploy.flavorGroups.containsKey("production"))
        assertTrue(config.deploy.flavorGroups.containsKey("testing"))
        assertTrue(config.deploy.flavorGroups.containsKey("premium"))
    }

    // ═════════════════════════════════════════════════════════════
    // SYNTAX VARIATIONS
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testParseCamelCaseSyntax() {
        val yaml = """
            deploy:
              defaultGroup: production

              flavorGroups:
                production:
                  name: Production
                  flavors:
                    - free
                    - paid
        """.trimIndent()

        val config = parser.parse(yaml)

        assertEquals("production", config.deploy.defaultGroup)
        assertEquals(1, config.deploy.flavorGroups.size)
    }

    @Test
    fun testParseMixedCaseSyntax() {
        val yaml = """
            deploy:
              default_group: production

              flavorGroups:
                production:
                  name: Production
                  flavors:
                    - free
        """.trimIndent()

        val config = parser.parse(yaml)

        assertEquals("production", config.deploy.defaultGroup)
        assertEquals(1, config.deploy.flavorGroups.size)
    }

    @Test
    fun testParseMinimalGroupDefinition() {
        val yaml = """
            deploy:
              flavor_groups:
                production:
                  flavors:
                    - free
                    - paid
        """.trimIndent()

        val config = parser.parse(yaml)

        val group = config.deploy.flavorGroups["production"]
        assertNotNull(group)
        assertEquals(listOf("free", "paid"), group.flavors)
        // Name and description should have defaults
        assertTrue(group.name.isNotEmpty())
    }

    // ═════════════════════════════════════════════════════════════
    // REAL-WORLD CONFIGURATIONS
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testParseEcommerceConfig() {
        val yaml = """
            app:
              name: EcommerceApp
              identifier: com.example.shop

            build:
              android:
                enabled: true
                flavors:
                  - free
                  - paid
                  - premium
                  - enterprise

            deploy:
              enabled: true
              default_group: production

              flavor_groups:
                production:
                  name: Production
                  description: App store releases
                  flavors:
                    - free
                    - paid
                    - premium

                freemium:
                  name: Freemium Tiers
                  description: Free and paid versions
                  flavors:
                    - free
                    - paid

                enterprise:
                  name: Enterprise
                  description: Enterprise clients
                  flavors:
                    - enterprise

              android:
                enabled: true
                firebase:
                  enabled: true
                  service_account: credentials/firebase.json
                  test_groups:
                    - qa-team
                    - beta-testers
        """.trimIndent()

        val config = parser.parse(yaml)

        // Verify app config
        assertEquals("EcommerceApp", config.app.name)

        // Verify deploy config
        assertEquals("production", config.deploy.defaultGroup)
        assertEquals(3, config.deploy.flavorGroups.size)

        // Verify production group
        val prodGroup = config.deploy.flavorGroups["production"]
        assertNotNull(prodGroup)
        assertEquals("Production", prodGroup.name)
        assertEquals(3, prodGroup.flavors.size)

        // Verify android deploy config
        assertTrue(config.deploy.android?.firebase?.enabled == true)
    }

    @Test
    fun testParseMultiRegionalConfig() {
        val yaml = """
            deploy:
              default_group: north-america

              flavor_groups:
                north-america:
                  name: North America
                  description: US and Canada
                  flavors:
                    - us
                    - canada

                europe:
                  name: Europe
                  description: European markets
                  flavors:
                    - uk
                    - germany
                    - france

                asia-pacific:
                  name: Asia Pacific
                  flavors:
                    - japan
                    - australia
                    - singapore

                global:
                  name: Global Rollout
                  description: All regions
                  flavors:
                    - us
                    - canada
                    - uk
                    - germany
                    - france
                    - japan
                    - australia
                    - singapore
        """.trimIndent()

        val config = parser.parse(yaml)

        assertEquals(4, config.deploy.flavorGroups.size)

        val northAmerica = config.deploy.flavorGroups["north-america"]
        assertEquals(2, northAmerica?.flavors?.size)

        val europe = config.deploy.flavorGroups["europe"]
        assertEquals(3, europe?.flavors?.size)

        val global = config.deploy.flavorGroups["global"]
        assertEquals(8, global?.flavors?.size)
    }

    @Test
    fun testParseEnterpriseDeploymentPipeline() {
        val yaml = """
            deploy:
              enabled: true
              default_group: production

              flavor_groups:
                development:
                  name: Development
                  description: Dev environment
                  flavors:
                    - developmentDebug

                staging:
                  name: Staging
                  description: QA testing
                  flavors:
                    - stagingDebug
                    - stagingRelease

                production:
                  name: Production
                  description: Live releases
                  flavors:
                    - productionRelease

                all-releases:
                  name: All Releases
                  description: Release builds only
                  flavors:
                    - stagingRelease
                    - productionRelease

                canary:
                  name: Canary
                  description: Gradual rollout
                  flavors:
                    - productionCanary
        """.trimIndent()

        val config = parser.parse(yaml)

        assertEquals(5, config.deploy.flavorGroups.size)

        // Verify each stage
        val dev = config.deploy.flavorGroups["development"]
        assertEquals(1, dev?.flavors?.size)

        val staging = config.deploy.flavorGroups["staging"]
        assertEquals(2, staging?.flavors?.size)

        val prod = config.deploy.flavorGroups["production"]
        assertEquals(1, prod?.flavors?.size)

        val allReleases = config.deploy.flavorGroups["all-releases"]
        assertEquals(2, allReleases?.flavors?.size)
    }

    // ═════════════════════════════════════════════════════════════
    // EDGE CASES
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testParseEmptyFlavorGroups() {
        val yaml = """
            deploy:
              flavor_groups: {}
        """.trimIndent()

        val config = parser.parse(yaml)

        assertTrue(config.deploy.flavorGroups.isEmpty())
    }

    @Test
    fun testParseGroupWithNoFlavors() {
        val yaml = """
            deploy:
              flavor_groups:
                empty-group:
                  name: Empty Group
                  flavors: []
        """.trimIndent()

        val config = parser.parse(yaml)

        val group = config.deploy.flavorGroups["empty-group"]
        assertNotNull(group)
        assertTrue(group.flavors.isEmpty())
    }

    @Test
    fun testParseGroupWithOnlyName() {
        val yaml = """
            deploy:
              flavor_groups:
                production:
                  name: Production
                  flavors:
                    - free
        """.trimIndent()

        val config = parser.parse(yaml)

        val group = config.deploy.flavorGroups["production"]
        assertNotNull(group)
        assertEquals("Production", group.name)
        assertEquals("", group.description)
    }

    @Test
    fun testParseGroupWithSpecialCharacters() {
        val yaml = """
            deploy:
              flavor_groups:
                production-v2:
                  name: Production V2
                  description: Production (version 2.0)
                  flavors:
                    - free
                    - paid

                testing_internal:
                  name: Internal Testing
                  flavors:
                    - qa
        """.trimIndent()

        val config = parser.parse(yaml)

        assertEquals(2, config.deploy.flavorGroups.size)
        assertTrue(config.deploy.flavorGroups.containsKey("production-v2"))
        assertTrue(config.deploy.flavorGroups.containsKey("testing_internal"))
    }

    @Test
    fun testParseNullDefaultGroup() {
        val yaml = """
            deploy:
              default_group: null

              flavor_groups:
                production:
                  flavors:
                    - free
        """.trimIndent()

        val config = parser.parse(yaml)

        assertNull(config.deploy.defaultGroup)
        assertEquals(1, config.deploy.flavorGroups.size)
    }

    @Test
    fun testParseMissingDefaultGroup() {
        val yaml = """
            deploy:
              flavor_groups:
                production:
                  flavors:
                    - free
        """.trimIndent()

        val config = parser.parse(yaml)

        assertNull(config.deploy.defaultGroup)
    }

    // ═════════════════════════════════════════════════════════════
    // SERIALIZATION TESTS
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testSerializeFlavorGroups() {
        val config = Config(
            deploy = DeployConfig(
                enabled = true,
                defaultGroup = "production",
                flavorGroups = mapOf(
                    "production" to FlavorGroup(
                        name = "Production",
                        description = "Production builds",
                        flavors = listOf("free", "paid", "premium")
                    ),
                    "testing" to FlavorGroup(
                        name = "Testing",
                        description = "QA testing",
                        flavors = listOf("qa", "staging")
                    )
                )
            )
        )

        val yaml = parser.toYaml(config)

        // Verify YAML contains expected keys
        assertTrue(yaml.contains("deploy:"))
        assertTrue(yaml.contains("default_group:") || yaml.contains("defaultGroup:"))
        assertTrue(yaml.contains("flavor_groups:") || yaml.contains("flavorGroups:"))
        assertTrue(yaml.contains("production"))
        assertTrue(yaml.contains("testing"))
    }

    @Test
    fun testRoundTripSerialization() {
        val originalYaml = """
            deploy:
              enabled: true
              default_group: production

              flavor_groups:
                production:
                  name: Production
                  description: Production releases
                  flavors:
                    - free
                    - paid
                    - premium

                testing:
                  name: Testing
                  description: Internal testing
                  flavors:
                    - qa
                    - staging
        """.trimIndent()

        // Parse
        val config = parser.parse(originalYaml)

        // Serialize
        val serializedYaml = parser.toYaml(config)

        // Parse again
        val reparsedConfig = parser.parse(serializedYaml)

        // Verify
        assertEquals(config.deploy.defaultGroup, reparsedConfig.deploy.defaultGroup)
        assertEquals(config.deploy.flavorGroups.size, reparsedConfig.deploy.flavorGroups.size)

        val origProdGroup = config.deploy.flavorGroups["production"]
        val reparsedProdGroup = reparsedConfig.deploy.flavorGroups["production"]
        assertEquals(origProdGroup?.flavors, reparsedProdGroup?.flavors)
    }

    // ═════════════════════════════════════════════════════════════
    // COMPLETE CONFIG EXAMPLES
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testParseCompleteProductionConfig() {
        val yaml = """
            app:
              name: ProductionApp
              identifier: com.example.app
              version: 2.0.0

            build:
              android:
                enabled: true
                default_flavor: free
                flavors:
                  - free
                  - paid
                  - premium
                  - enterprise
                  - qa
                  - staging

            version:
              enabled: true
              current: 2.0.0
              auto_increment: true
              bump_strategy: patch

            deploy:
              enabled: true
              default_group: production

              flavor_groups:
                production:
                  name: Production
                  description: App store releases
                  flavors:
                    - free
                    - paid
                    - premium
                    - enterprise

                testing:
                  name: Testing
                  description: QA and staging builds
                  flavors:
                    - qa
                    - staging

                premium-only:
                  name: Premium Tiers
                  description: Paid versions only
                  flavors:
                    - paid
                    - premium
                    - enterprise

              android:
                enabled: true
                artifact_path: build/outputs/bundle/release/app-release.aab

                firebase:
                  enabled: true
                  service_account: credentials/firebase-service-account.json
                  test_groups:
                    - qa-team
                    - beta-testers
                    - internal

                play_console:
                  enabled: true
                  service_account: credentials/play-console.json
                  package_name: com.example.app

            changelog:
              enabled: true
              format: markdown
              output_file: CHANGELOG.md
        """.trimIndent()

        val config = parser.parse(yaml)

        // Verify complete structure
        assertNotNull(config.app)
        assertNotNull(config.build)
        assertNotNull(config.version)
        assertNotNull(config.deploy)
        assertNotNull(config.changelog)

        // Verify deploy config
        assertEquals("production", config.deploy.defaultGroup)
        assertEquals(3, config.deploy.flavorGroups.size)

        // Verify groups
        val productionGroup = config.deploy.flavorGroups["production"]
        assertEquals(4, productionGroup?.flavors?.size)

        val testingGroup = config.deploy.flavorGroups["testing"]
        assertEquals(2, testingGroup?.flavors?.size)

        val premiumGroup = config.deploy.flavorGroups["premium-only"]
        assertEquals(3, premiumGroup?.flavors?.size)

        // Verify Android deploy
        val androidDeploy = config.deploy.android
        assertNotNull(androidDeploy)
        assertTrue(androidDeploy.firebase.enabled)
        assertTrue(androidDeploy.playConsole.enabled)
        assertEquals(3, androidDeploy.firebase.testGroups.size)
    }
}
