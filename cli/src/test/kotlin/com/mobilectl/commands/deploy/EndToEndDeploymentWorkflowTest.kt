package com.mobilectl.commands.deploy

import com.mobilectl.config.createConfigParser
import com.mobilectl.detector.createProjectDetector
import kotlin.test.*

/**
 * End-to-End Deployment Workflow Tests
 *
 * Tests complete workflows from YAML config → parsing → deployment selection
 * Simulates real user scenarios with actual config files
 */
class EndToEndDeploymentWorkflowTest {

    private val parser = createConfigParser()
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
    // E2E WORKFLOW 1: PRODUCTION RELEASE
    // ═════════════════════════════════════════════════════════════

    /**
     * E2E Test: Complete production release workflow
     *
     * Scenario:
     * 1. Load production config from YAML
     * 2. Deploy production group
     * 3. Verify correct flavors selected
     * 4. Verify destinations configured
     */
    @Test
    fun testE2E_ProductionReleaseWorkflow() {
        val yaml = """
            app:
              name: MyApp
              identifier: com.example.app
              version: 1.0.0

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
                  description: Production releases
                  flavors:
                    - free
                    - paid
                    - premium

                testing:
                  name: Testing
                  flavors:
                    - qa

              android:
                enabled: true
                firebase:
                  enabled: true
                  service_account: credentials/firebase.json
                  test_groups:
                    - beta-testers
                play_console:
                  enabled: true
                  service_account: credentials/play.json
        """.trimIndent()

        // Step 1: Parse config
        val config = parser.parse(yaml)

        // Step 2: Verify config loaded correctly
        assertEquals("production", config.deploy.defaultGroup)
        assertEquals(2, config.deploy.flavorGroups.size)

        // Step 3: Select production flavors
        val productionOptions = FlavorOptions(group = "production")
        val flavors = workflow.selectFlavorsToDeploy(config, productionOptions)

        // Step 4: Verify correct flavors
        assertEquals(listOf("free", "paid", "premium"), flavors)

        // Step 5: Verify destinations configured
        assertTrue(config.deploy.android?.firebase?.enabled == true)
        assertTrue(config.deploy.android?.playConsole?.enabled == true)
    }

    // ═════════════════════════════════════════════════════════════
    // E2E WORKFLOW 2: QA TESTING
    // ═════════════════════════════════════════════════════════════

    /**
     * E2E Test: QA testing deployment
     *
     * Scenario:
     * 1. QA team wants to deploy testing builds
     * 2. Use testing flavor group
     * 3. Deploy to Firebase only (not Play Store)
     */
    @Test
    fun testE2E_QATestingWorkflow() {
        val yaml = """
            build:
              android:
                enabled: true
                flavors:
                  - free
                  - paid
                  - qa
                  - staging

            deploy:
              flavor_groups:
                testing:
                  name: Testing
                  flavors:
                    - qa
                    - staging

              android:
                firebase:
                  enabled: true
                  test_groups:
                    - qa-team
                play_console:
                  enabled: false
        """.trimIndent()

        val config = parser.parse(yaml)

        // Deploy testing group
        val options = FlavorOptions(group = "testing")
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf("qa", "staging"), flavors)

        // Verify only Firebase enabled
        assertTrue(config.deploy.android?.firebase?.enabled == true)
        assertFalse(config.deploy.android?.playConsole?.enabled == true)
    }

    // ═════════════════════════════════════════════════════════════
    // E2E WORKFLOW 3: HOTFIX DEPLOYMENT
    // ═════════════════════════════════════════════════════════════

    /**
     * E2E Test: Emergency hotfix deployment
     *
     * Scenario:
     * 1. Critical bug in paid version only
     * 2. Deploy single flavor immediately
     * 3. Skip other flavors
     */
    @Test
    fun testE2E_HotfixWorkflow() {
        val yaml = """
            build:
              android:
                flavors:
                  - free
                  - paid
                  - premium

            deploy:
              android:
                firebase:
                  enabled: true
                play_console:
                  enabled: true
        """.trimIndent()

        val config = parser.parse(yaml)

        // Hotfix: deploy paid only
        val options = FlavorOptions(flavors = "paid")
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf("paid"), flavors)
    }

    // ═════════════════════════════════════════════════════════════
    // E2E WORKFLOW 4: STAGED ROLLOUT
    // ═════════════════════════════════════════════════════════════

    /**
     * E2E Test: Staged rollout (internal → beta → production)
     *
     * Scenario:
     * 1. Stage 1: Deploy to internal testers
     * 2. Stage 2: Deploy to beta testers
     * 3. Stage 3: Deploy to production
     */
    @Test
    fun testE2E_StagedRollout() {
        val yaml = """
            build:
              android:
                flavors:
                  - internal
                  - beta
                  - production

            deploy:
              flavor_groups:
                internal:
                  name: Internal
                  flavors:
                    - internal

                beta:
                  name: Beta
                  flavors:
                    - beta

                production:
                  name: Production
                  flavors:
                    - production

              android:
                firebase:
                  enabled: true
        """.trimIndent()

        val config = parser.parse(yaml)

        // Stage 1: Internal
        val internalOptions = FlavorOptions(group = "internal")
        val internalFlavors = workflow.selectFlavorsToDeploy(config, internalOptions)
        assertEquals(listOf("internal"), internalFlavors)

        // Stage 2: Beta
        val betaOptions = FlavorOptions(group = "beta")
        val betaFlavors = workflow.selectFlavorsToDeploy(config, betaOptions)
        assertEquals(listOf("beta"), betaFlavors)

        // Stage 3: Production
        val prodOptions = FlavorOptions(group = "production")
        val prodFlavors = workflow.selectFlavorsToDeploy(config, prodOptions)
        assertEquals(listOf("production"), prodFlavors)
    }

    // ═════════════════════════════════════════════════════════════
    // E2E WORKFLOW 5: REGIONAL DEPLOYMENT
    // ═════════════════════════════════════════════════════════════

    /**
     * E2E Test: Multi-regional deployment
     *
     * Scenario:
     * 1. App has different builds per region
     * 2. Deploy to specific regions
     * 3. Rollout: NA → EU → Asia
     */
    @Test
    fun testE2E_RegionalDeployment() {
        val yaml = """
            build:
              android:
                flavors:
                  - us
                  - canada
                  - uk
                  - germany
                  - japan

            deploy:
              default_group: north-america

              flavor_groups:
                north-america:
                  name: North America
                  flavors:
                    - us
                    - canada

                europe:
                  name: Europe
                  flavors:
                    - uk
                    - germany

                asia:
                  name: Asia
                  flavors:
                    - japan

                global:
                  name: Global
                  flavors:
                    - us
                    - canada
                    - uk
                    - germany
                    - japan
        """.trimIndent()

        val config = parser.parse(yaml)

        // Default should be North America
        val defaultOptions = FlavorOptions()
        val defaultFlavors = workflow.selectFlavorsToDeploy(config, defaultOptions)
        assertEquals(listOf("us", "canada"), defaultFlavors)

        // Europe
        val europeOptions = FlavorOptions(group = "europe")
        val europeFlavors = workflow.selectFlavorsToDeploy(config, europeOptions)
        assertEquals(listOf("uk", "germany"), europeFlavors)

        // Global
        val globalOptions = FlavorOptions(group = "global")
        val globalFlavors = workflow.selectFlavorsToDeploy(config, globalOptions)
        assertEquals(5, globalFlavors.size)
    }

    // ═════════════════════════════════════════════════════════════
    // E2E WORKFLOW 6: COMPLEX EXCLUSION
    // ═════════════════════════════════════════════════════════════

    /**
     * E2E Test: Deploy with exclusions
     *
     * Scenario:
     * 1. Deploy all flavors except problematic ones
     * 2. Exclude specific flavors due to issues
     */
    @Test
    fun testE2E_DeploymentWithExclusions() {
        val yaml = """
            build:
              android:
                flavors:
                  - free
                  - paid
                  - premium
                  - enterprise
                  - qa

            deploy:
              android:
                firebase:
                  enabled: true
        """.trimIndent()

        val config = parser.parse(yaml)

        // Deploy all except QA (has issues)
        val options = FlavorOptions(
            allFlavors = true,
            exclude = "qa"
        )
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(4, flavors.size)
        assertTrue(flavors.containsAll(listOf("free", "paid", "premium", "enterprise")))
        assertFalse(flavors.contains("qa"))
    }

    // ═════════════════════════════════════════════════════════════
    // E2E WORKFLOW 7: DEFAULT GROUP FALLBACK
    // ═════════════════════════════════════════════════════════════

    /**
     * E2E Test: Default group automatic selection
     *
     * Scenario:
     * 1. Developer runs `mobilectl deploy` (no flags)
     * 2. Should use default_group from config
     * 3. Simplifies common workflow
     */
    @Test
    fun testE2E_DefaultGroupAutoSelection() {
        val yaml = """
            build:
              android:
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

        val config = parser.parse(yaml)

        // No options - should use default_group
        val options = FlavorOptions()
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf("free", "paid"), flavors)
    }

    // ═════════════════════════════════════════════════════════════
    // E2E WORKFLOW 8: CLI OVERRIDE
    // ═════════════════════════════════════════════════════════════

    /**
     * E2E Test: CLI overrides config defaults
     *
     * Scenario:
     * 1. Config has default_group: production
     * 2. Developer wants to deploy testing instead
     * 3. CLI --flavor-group testing overrides default
     */
    @Test
    fun testE2E_CLIOverridesConfig() {
        val yaml = """
            build:
              android:
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

        val config = parser.parse(yaml)

        // Override with testing
        val options = FlavorOptions(group = "testing")
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf("qa"), flavors)
    }

    // ═════════════════════════════════════════════════════════════
    // E2E ERROR SCENARIOS
    // ═════════════════════════════════════════════════════════════

    /**
     * E2E Test: Graceful handling of missing group
     *
     * Scenario:
     * 1. Request non-existent group
     * 2. Should return empty list (not crash)
     */
    @Test
    fun testE2E_MissingGroupGracefulFail() {
        val yaml = """
            build:
              android:
                flavors:
                  - free

            deploy:
              flavor_groups:
                production:
                  flavors:
                    - free
        """.trimIndent()

        val config = parser.parse(yaml)

        // Request non-existent group
        val options = FlavorOptions(group = "non-existent")
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        assertTrue(flavors.isEmpty())
    }

    /**
     * E2E Test: Empty config graceful handling
     *
     * Scenario:
     * 1. Minimal config with no flavor groups
     * 2. Request group
     * 3. Should handle gracefully
     */
    @Test
    fun testE2E_EmptyConfigGracefulHandling() {
        val yaml = """
            build:
              android:
                enabled: true
                flavors:
                  - free
                default_flavor: free
        """.trimIndent()

        val config = parser.parse(yaml)

        // Request group when none exist
        val options = FlavorOptions(group = "production")
        val flavors = workflow.selectFlavorsToDeploy(config, options)

        assertTrue(flavors.isEmpty())
    }

    // ═════════════════════════════════════════════════════════════
    // E2E COMPLEX REAL-WORLD SCENARIOS
    // ═════════════════════════════════════════════════════════════

    /**
     * E2E Test: Enterprise multi-environment deployment
     *
     * Scenario:
     * - Large enterprise with dev/staging/production
     * - Multiple flavors per environment
     * - Complex deployment rules
     */
    @Test
    fun testE2E_EnterpriseMultiEnvironment() {
        val yaml = """
            build:
              android:
                flavors:
                  - devDebug
                  - stagingDebug
                  - stagingRelease
                  - prodDebug
                  - prodRelease
                  - prodCanary

            deploy:
              default_group: production

              flavor_groups:
                development:
                  name: Development
                  flavors:
                    - devDebug

                staging:
                  name: Staging
                  flavors:
                    - stagingDebug
                    - stagingRelease

                production:
                  name: Production
                  flavors:
                    - prodRelease

                canary:
                  name: Canary
                  flavors:
                    - prodCanary

                all-releases:
                  name: All Releases
                  flavors:
                    - stagingRelease
                    - prodRelease
                    - prodCanary
        """.trimIndent()

        val config = parser.parse(yaml)

        // Dev deployment
        val devOptions = FlavorOptions(group = "development")
        val devFlavors = workflow.selectFlavorsToDeploy(config, devOptions)
        assertEquals(listOf("devDebug"), devFlavors)

        // Staging deployment
        val stagingOptions = FlavorOptions(group = "staging")
        val stagingFlavors = workflow.selectFlavorsToDeploy(config, stagingOptions)
        assertEquals(2, stagingFlavors.size)

        // Production deployment (default)
        val prodOptions = FlavorOptions()
        val prodFlavors = workflow.selectFlavorsToDeploy(config, prodOptions)
        assertEquals(listOf("prodRelease"), prodFlavors)

        // All releases
        val allReleasesOptions = FlavorOptions(group = "all-releases")
        val allReleases = workflow.selectFlavorsToDeploy(config, allReleasesOptions)
        assertEquals(3, allReleases.size)
    }
}
