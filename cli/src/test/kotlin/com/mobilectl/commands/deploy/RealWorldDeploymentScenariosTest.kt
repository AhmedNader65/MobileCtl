package com.mobilectl.commands.deploy

import com.mobilectl.config.Config
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.model.appMetadata.AppConfig
import com.mobilectl.model.buildConfig.AndroidBuildConfig
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.deploy.*
import com.mobilectl.model.versionManagement.VersionConfig
import kotlin.test.*

/**
 * Real-world deployment scenario tests
 *
 * These tests simulate actual production use cases:
 * - E-commerce app with multiple tiers (free, paid, premium)
 * - SaaS app with regional deployments
 * - Enterprise app with staged rollouts
 * - Mobile game with beta testing
 * - Multi-brand app deployment
 */
class RealWorldDeploymentScenariosTest {

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
    // SCENARIO 1: E-COMMERCE APP WITH TIERED PRICING
    // ═════════════════════════════════════════════════════════════

    /**
     * Real-world scenario: E-commerce app with free, paid, and premium tiers
     *
     * Business context:
     * - Free version: Limited features, ad-supported
     * - Paid version: Full features, no ads
     * - Premium version: Enterprise features, priority support
     *
     * Deployment workflow:
     * 1. Deploy to Firebase for internal testing (all tiers)
     * 2. Deploy production group to Play Store (free + paid + premium)
     * 3. Deploy premium separately for enterprise clients
     */
    @Test
    fun testEcommerceApp_ProductionRelease() {
        val config = createEcommerceConfig()

        // Test 1: Internal testing - deploy all flavors to Firebase
        val internalOptions = FlavorOptions(allFlavors = true)
        val internalFlavors = workflow.selectFlavorsToDeploy(config, internalOptions)

        assertEquals(4, internalFlavors.size)
        assertTrue(internalFlavors.containsAll(listOf("free", "paid", "premium", "enterprise")))

        // Test 2: Production release - deploy production group
        val productionOptions = FlavorOptions(group = "production")
        val productionFlavors = workflow.selectFlavorsToDeploy(config, productionOptions)

        assertEquals(3, productionFlavors.size)
        assertEquals(listOf("free", "paid", "premium"), productionFlavors)
        assertFalse(productionFlavors.contains("enterprise"))

        // Test 3: Enterprise-only deployment
        val enterpriseOptions = FlavorOptions(flavors = "enterprise")
        val enterpriseFlavors = workflow.selectFlavorsToDeploy(config, enterpriseOptions)

        assertEquals(listOf("enterprise"), enterpriseFlavors)
    }

    /**
     * Real-world scenario: Hotfix deployment for specific tier
     *
     * Business context:
     * - Critical bug found only in paid version
     * - Need to deploy fix ASAP to paid tier only
     * - Other tiers don't need update
     */
    @Test
    fun testEcommerceApp_HotfixDeployment() {
        val config = createEcommerceConfig()

        val options = FlavorOptions(flavors = "paid")
        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf("paid"), selected)
    }

    /**
     * Real-world scenario: Beta testing with freemium tiers
     *
     * Business context:
     * - New feature available for free and paid users
     * - Premium and enterprise get feature next release
     * - Deploy to beta testers first
     */
    @Test
    fun testEcommerceApp_BetaTesting() {
        val config = createEcommerceConfig()

        val options = FlavorOptions(group = "freemium")
        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf("free", "paid"), selected)
        assertFalse(selected.contains("premium"))
        assertFalse(selected.contains("enterprise"))
    }

    // ═════════════════════════════════════════════════════════════
    // SCENARIO 2: REGIONAL/GLOBAL DEPLOYMENT (SaaS)
    // ═════════════════════════════════════════════════════════════

    /**
     * Real-world scenario: SaaS app with regional deployments
     *
     * Business context:
     * - Global SaaS platform
     * - Different regions: North America, Europe, Asia, LATAM
     * - Staged rollout: NA → EU → Asia → LATAM
     * - Each region has specific compliance requirements
     */
    @Test
    fun testSaasApp_StagedRegionalRollout() {
        val config = createSaasRegionalConfig()

        // Stage 1: Deploy to North America first
        val naOptions = FlavorOptions(group = "north-america")
        val naFlavors = workflow.selectFlavorsToDeploy(config, naOptions)

        assertEquals(listOf("us", "canada"), naFlavors)

        // Stage 2: Deploy to Europe
        val euOptions = FlavorOptions(group = "europe")
        val euFlavors = workflow.selectFlavorsToDeploy(config, euOptions)

        assertEquals(listOf("uk", "germany", "france"), euFlavors)

        // Stage 3: Deploy to Asia-Pacific
        val asiaOptions = FlavorOptions(group = "asia-pacific")
        val asiaFlavors = workflow.selectFlavorsToDeploy(config, asiaOptions)

        assertEquals(listOf("japan", "australia", "singapore"), asiaFlavors)

        // Stage 4: Deploy globally (all regions)
        val globalOptions = FlavorOptions(allFlavors = true)
        val globalFlavors = workflow.selectFlavorsToDeploy(config, globalOptions)

        assertEquals(8, globalFlavors.size)
    }

    /**
     * Real-world scenario: Emergency rollback for specific region
     *
     * Business context:
     * - Critical issue found in Europe deployment
     * - Need to rollback only EU regions
     * - Other regions unaffected
     */
    @Test
    fun testSaasApp_RegionalRollback() {
        val config = createSaasRegionalConfig()

        // Deploy all except Europe
        val options = FlavorOptions(
            allFlavors = true,
            exclude = "uk,germany,france"
        )
        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(5, selected.size)
        assertFalse(selected.contains("uk"))
        assertFalse(selected.contains("germany"))
        assertFalse(selected.contains("france"))
        assertTrue(selected.contains("us"))
        assertTrue(selected.contains("japan"))
    }

    // ═════════════════════════════════════════════════════════════
    // SCENARIO 3: ENTERPRISE APP WITH STAGED ROLLOUT
    // ═════════════════════════════════════════════════════════════

    /**
     * Real-world scenario: Enterprise app deployment pipeline
     *
     * Business context:
     * - Large enterprise with dev → staging → production pipeline
     * - Internal QA team tests in staging
     * - Production deploys only after stakeholder approval
     * - Debug builds for developers, release builds for production
     */
    @Test
    fun testEnterpriseApp_DeploymentPipeline() {
        val config = createEnterpriseConfig()

        // Phase 1: Development - deploy dev builds to Firebase
        val devOptions = FlavorOptions(group = "development")
        val devFlavors = workflow.selectFlavorsToDeploy(config, devOptions)

        assertEquals(listOf("developmentDebug"), devFlavors)

        // Phase 2: Staging - deploy to QA team
        val stagingOptions = FlavorOptions(group = "staging")
        val stagingFlavors = workflow.selectFlavorsToDeploy(config, stagingOptions)

        assertEquals(listOf("stagingRelease", "stagingDebug"), stagingFlavors)

        // Phase 3: Production - deploy release builds only
        val productionOptions = FlavorOptions(group = "production")
        val productionFlavors = workflow.selectFlavorsToDeploy(config, productionOptions)

        assertEquals(listOf("productionRelease"), productionFlavors)

        // Phase 4: All release builds (for reporting)
        val allReleasesOptions = FlavorOptions(group = "all-releases")
        val allReleases = workflow.selectFlavorsToDeploy(config, allReleasesOptions)

        assertEquals(2, allReleases.size)
        assertTrue(allReleases.all { it.contains("Release") })
        assertEquals(listOf("stagingRelease", "productionRelease"), allReleases)
    }

    /**
     * Real-world scenario: Canary deployment
     *
     * Business context:
     * - Deploy to 5% of production users first
     * - Monitor for errors before full rollout
     * - Use canary flavor for gradual rollout
     */
    @Test
    fun testEnterpriseApp_CanaryDeployment() {
        val config = createEnterpriseConfig()

        val options = FlavorOptions(flavors = "productionCanary")
        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf("productionCanary"), selected)
    }

    // ═════════════════════════════════════════════════════════════
    // SCENARIO 4: MOBILE GAME WITH BETA TESTING
    // ═════════════════════════════════════════════════════════════

    /**
     * Real-world scenario: Mobile game beta program
     *
     * Business context:
     * - Alpha: Internal developers only
     * - Beta: Closed beta testers
     * - Open Beta: Public beta on TestFlight/Firebase
     * - Production: Live on app stores
     */
    @Test
    fun testMobileGame_BetaProgram() {
        val config = createMobileGameConfig()

        // Test 1: Alpha deployment (internal only)
        val alphaOptions = FlavorOptions(group = "alpha")
        val alphaFlavors = workflow.selectFlavorsToDeploy(config, alphaOptions)

        assertEquals(listOf("alphaDebug"), alphaFlavors)

        // Test 2: Closed beta (selected testers)
        val betaOptions = FlavorOptions(group = "beta")
        val betaFlavors = workflow.selectFlavorsToDeploy(config, betaOptions)

        assertEquals(listOf("betaRelease"), betaFlavors)

        // Test 3: Open beta (public testing)
        val openBetaOptions = FlavorOptions(group = "open-beta")
        val openBetaFlavors = workflow.selectFlavorsToDeploy(config, openBetaOptions)

        assertEquals(listOf("openBetaRelease"), openBetaFlavors)

        // Test 4: Production release
        val productionOptions = FlavorOptions(group = "production")
        val productionFlavors = workflow.selectFlavorsToDeploy(config, productionOptions)

        assertEquals(listOf("productionRelease"), productionFlavors)
    }

    // ═════════════════════════════════════════════════════════════
    // SCENARIO 5: MULTI-BRAND APP
    // ═════════════════════════════════════════════════════════════

    /**
     * Real-world scenario: White-label app for multiple brands
     *
     * Business context:
     * - Same codebase, different branding
     * - BrandA: Main brand
     * - BrandB: Partner brand
     * - BrandC: Enterprise white-label
     * - Deploy all brands or specific brand
     */
    @Test
    fun testMultiBrandApp_BrandSpecificDeployment() {
        val config = createMultiBrandConfig()

        // Test 1: Deploy all brands
        val allBrandsOptions = FlavorOptions(allFlavors = true)
        val allBrands = workflow.selectFlavorsToDeploy(config, allBrandsOptions)

        assertEquals(3, allBrands.size)
        assertTrue(allBrands.containsAll(listOf("brandA", "brandB", "brandC")))

        // Test 2: Deploy specific brand
        val brandAOptions = FlavorOptions(flavors = "brandA")
        val brandA = workflow.selectFlavorsToDeploy(config, brandAOptions)

        assertEquals(listOf("brandA"), brandA)

        // Test 3: Deploy partner brands only (exclude main brand)
        val partnerOptions = FlavorOptions(group = "partners")
        val partners = workflow.selectFlavorsToDeploy(config, partnerOptions)

        assertEquals(listOf("brandB", "brandC"), partners)
        assertFalse(partners.contains("brandA"))
    }

    // ═════════════════════════════════════════════════════════════
    // SCENARIO 6: DEFAULT GROUP BEHAVIOR
    // ═════════════════════════════════════════════════════════════

    /**
     * Real-world scenario: Default group simplifies deployments
     *
     * Business context:
     * - Most common deployment is production
     * - Set defaultGroup: production
     * - Simple `mobilectl deploy` deploys production
     * - Saves typing --flavor-group production every time
     */
    @Test
    fun testDefaultGroup_SimplifiesCommonWorkflow() {
        val config = createConfigWithDefaultGroup("production")

        // No CLI options - should use default group
        val options = FlavorOptions()
        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf("free", "paid", "premium"), selected)
    }

    /**
     * Real-world scenario: Override default group with CLI
     *
     * Business context:
     * - Default is production
     * - But occasionally need to deploy testing
     * - CLI flag overrides default
     */
    @Test
    fun testDefaultGroup_OverrideWithCLI() {
        val config = createConfigWithDefaultGroup("production")

        // Override with testing group
        val options = FlavorOptions(group = "testing")
        val selected = workflow.selectFlavorsToDeploy(config, options)

        assertEquals(listOf("qa", "staging"), selected)
    }

    // ═════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS - CONFIG BUILDERS
    // ═════════════════════════════════════════════════════════════

    private fun createEcommerceConfig(): Config {
        return Config(
            app = AppConfig(
                name = "EcommerceApp",
                identifier = "com.example.ecommerce"
            ),
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    flavors = listOf("free", "paid", "premium", "enterprise")
                )
            ),
            deploy = DeployConfig(
                enabled = true,
                defaultGroup = "production",
                flavorGroups = mapOf(
                    "production" to FlavorGroup(
                        name = "Production",
                        description = "App store releases",
                        flavors = listOf("free", "paid", "premium")
                    ),
                    "freemium" to FlavorGroup(
                        name = "Freemium",
                        description = "Free and paid tiers",
                        flavors = listOf("free", "paid")
                    ),
                    "enterprise" to FlavorGroup(
                        name = "Enterprise",
                        description = "Enterprise clients",
                        flavors = listOf("enterprise")
                    )
                )
            ),
            version = VersionConfig(),
            changelog = ChangelogConfig()
        )
    }

    private fun createSaasRegionalConfig(): Config {
        return Config(
            app = AppConfig(
                name = "SaaSApp",
                identifier = "com.example.saas"
            ),
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    flavors = listOf("us", "canada", "uk", "germany", "france", "japan", "australia", "singapore")
                )
            ),
            deploy = DeployConfig(
                enabled = true,
                defaultGroup = "north-america",
                flavorGroups = mapOf(
                    "north-america" to FlavorGroup(
                        name = "North America",
                        flavors = listOf("us", "canada")
                    ),
                    "europe" to FlavorGroup(
                        name = "Europe",
                        flavors = listOf("uk", "germany", "france")
                    ),
                    "asia-pacific" to FlavorGroup(
                        name = "Asia Pacific",
                        flavors = listOf("japan", "australia", "singapore")
                    )
                )
            ),
            version = VersionConfig(),
            changelog = ChangelogConfig()
        )
    }

    private fun createEnterpriseConfig(): Config {
        return Config(
            app = AppConfig(
                name = "EnterpriseApp",
                identifier = "com.enterprise.app"
            ),
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    flavors = listOf(
                        "developmentDebug",
                        "stagingDebug", "stagingRelease",
                        "productionDebug", "productionRelease", "productionCanary"
                    )
                )
            ),
            deploy = DeployConfig(
                enabled = true,
                defaultGroup = "production",
                flavorGroups = mapOf(
                    "development" to FlavorGroup(
                        name = "Development",
                        flavors = listOf("developmentDebug")
                    ),
                    "staging" to FlavorGroup(
                        name = "Staging",
                        flavors = listOf("stagingRelease", "stagingDebug")
                    ),
                    "production" to FlavorGroup(
                        name = "Production",
                        flavors = listOf("productionRelease")
                    ),
                    "all-releases" to FlavorGroup(
                        name = "All Releases",
                        flavors = listOf("stagingRelease", "productionRelease")
                    ),
                    "canary" to FlavorGroup(
                        name = "Canary",
                        flavors = listOf("productionCanary")
                    )
                )
            ),
            version = VersionConfig(),
            changelog = ChangelogConfig()
        )
    }

    private fun createMobileGameConfig(): Config {
        return Config(
            app = AppConfig(
                name = "MobileGame",
                identifier = "com.example.game"
            ),
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    flavors = listOf("alphaDebug", "betaRelease", "openBetaRelease", "productionRelease")
                )
            ),
            deploy = DeployConfig(
                enabled = true,
                defaultGroup = "production",
                flavorGroups = mapOf(
                    "alpha" to FlavorGroup(
                        name = "Alpha",
                        description = "Internal testing",
                        flavors = listOf("alphaDebug")
                    ),
                    "beta" to FlavorGroup(
                        name = "Beta",
                        description = "Closed beta testers",
                        flavors = listOf("betaRelease")
                    ),
                    "open-beta" to FlavorGroup(
                        name = "Open Beta",
                        description = "Public beta testing",
                        flavors = listOf("openBetaRelease")
                    ),
                    "production" to FlavorGroup(
                        name = "Production",
                        description = "Live release",
                        flavors = listOf("productionRelease")
                    )
                )
            ),
            version = VersionConfig(),
            changelog = ChangelogConfig()
        )
    }

    private fun createMultiBrandConfig(): Config {
        return Config(
            app = AppConfig(
                name = "MultiBrandApp",
                identifier = "com.example.multibrand"
            ),
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    flavors = listOf("brandA", "brandB", "brandC")
                )
            ),
            deploy = DeployConfig(
                enabled = true,
                defaultGroup = "all-brands",
                flavorGroups = mapOf(
                    "all-brands" to FlavorGroup(
                        name = "All Brands",
                        flavors = listOf("brandA", "brandB", "brandC")
                    ),
                    "partners" to FlavorGroup(
                        name = "Partner Brands",
                        flavors = listOf("brandB", "brandC")
                    )
                )
            ),
            version = VersionConfig(),
            changelog = ChangelogConfig()
        )
    }

    private fun createConfigWithDefaultGroup(defaultGroup: String): Config {
        return Config(
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    flavors = listOf("free", "paid", "premium", "qa", "staging")
                )
            ),
            deploy = DeployConfig(
                enabled = true,
                defaultGroup = defaultGroup,
                flavorGroups = mapOf(
                    "production" to FlavorGroup(
                        flavors = listOf("free", "paid", "premium")
                    ),
                    "testing" to FlavorGroup(
                        flavors = listOf("qa", "staging")
                    )
                )
            ),
            version = VersionConfig(),
            changelog = ChangelogConfig()
        )
    }
}
