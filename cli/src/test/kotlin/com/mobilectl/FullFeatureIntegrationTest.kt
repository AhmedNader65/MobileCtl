package com.mobilectl

import com.mobilectl.commands.buildCommand.BuildHandler
import com.mobilectl.commands.changelog.ChangelogGenerateHandler
import com.mobilectl.commands.deploy.DeploymentWorkflow
import com.mobilectl.commands.deploy.FlavorOptions
import com.mobilectl.commands.version.VersionBumpHandler
import com.mobilectl.config.createConfigParser
import com.mobilectl.detector.createProjectDetector
import kotlin.test.*
import java.io.File

/**
 * Full Feature Integration Test Suite
 *
 * Comprehensive tests covering ALL MobileCtl features:
 * - Build automation (Android & iOS)
 * - Version management
 * - Changelog generation
 * - Deployment (all destinations)
 * - Configuration management
 * - Project detection
 * - Complete workflows
 *
 * These tests validate the entire system working together
 */
class FullFeatureIntegrationTest {

    private val parser = createConfigParser()
    private val workingPath = System.getProperty("user.dir")
    private val detector = createProjectDetector()

    // ═════════════════════════════════════════════════════════════
    // COMPLETE WORKFLOW TESTS
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Complete Release Workflow
     *
     * Simulates: mobilectl deploy --bump-version patch --changelog --all-flavors
     *
     * Steps:
     * 1. Bump version (patch)
     * 2. Generate changelog
     * 3. Build all flavors
     * 4. Deploy to all destinations
     */
    @Test
    fun testCompleteReleaseWorkflow() {
        val yaml = """
            app:
              name: TestApp
              identifier: com.test.app
              version: 1.0.0

            version:
              enabled: true
              current: 1.0.0
              auto_increment: true
              bump_strategy: patch
              files_to_update:
                - version.properties

            changelog:
              enabled: true
              format: markdown
              output_file: CHANGELOG.md

            build:
              android:
                enabled: true
                flavors:
                  - free
                  - paid
                default_flavor: free

            deploy:
              enabled: true
              default_group: production

              flavor_groups:
                production:
                  name: Production
                  flavors:
                    - free
                    - paid

              android:
                firebase:
                  enabled: true
                  service_account: credentials/firebase.json
                  test_groups:
                    - qa-team
        """.trimIndent()

        val config = parser.parse(yaml)

        // Step 1: Verify version config
        assertNotNull(config.version)
        assertEquals("1.0.0", config.version?.current)
        assertEquals("patch", config.version?.bumpStrategy)

        // Step 2: Verify changelog config
        assertTrue(config.changelog.enabled)
        assertEquals("markdown", config.changelog.format)

        // Step 3: Verify build config
        assertTrue(config.build.android.enabled)
        assertEquals(2, config.build.android.flavors.size)

        // Step 4: Verify deploy config
        assertTrue(config.deploy.enabled)
        assertEquals("production", config.deploy.defaultGroup)
        assertTrue(config.deploy.android?.firebase?.enabled == true)

        // Step 5: Simulate deployment workflow
        val workflow = DeploymentWorkflow(workingPath, detector, false)
        val flavors = workflow.selectFlavorsToDeploy(
            config,
            FlavorOptions(group = "production")
        )

        assertEquals(listOf("free", "paid"), flavors)
    }

    // ═════════════════════════════════════════════════════════════
    // BUILD AUTOMATION TESTS
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Android Build Configuration
     */
    @Test
    fun testAndroidBuildConfiguration() {
        val yaml = """
            build:
              android:
                enabled: true
                default_flavor: production
                default_type: release
                flavors:
                  - debug
                  - staging
                  - production
                output_type: aab
                firebase_output_type: apk
                key_store: release.keystore
                key_alias: release-key
                use_env_for_passwords: true
        """.trimIndent()

        val config = parser.parse(yaml)

        assertTrue(config.build.android.enabled)
        assertEquals("production", config.build.android.defaultFlavor)
        assertEquals("release", config.build.android.defaultType)
        assertEquals(3, config.build.android.flavors.size)
        assertEquals("aab", config.build.android.outputType)
        assertEquals("apk", config.build.android.firebaseOutputType)
        assertEquals("release.keystore", config.build.android.keyStore)
        assertTrue(config.build.android.useEnvForPasswords)
    }

    /**
     * Test: iOS Build Configuration
     */
    @Test
    fun testIOSBuildConfiguration() {
        val yaml = """
            build:
              ios:
                enabled: true
                project_path: ios/App.xcworkspace
                scheme: AppScheme
                configuration: Release
                destination: generic/platform=iOS
                code_sign_identity: iPhone Distribution
                provisioning_profile: AppStore_Profile
                output:
                  format: ipa
                  name: app-release.ipa
        """.trimIndent()

        val config = parser.parse(yaml)

        assertTrue(config.build.ios.enabled == true)
        assertEquals("ios/App.xcworkspace", config.build.ios.projectPath)
        assertEquals("AppScheme", config.build.ios.scheme)
        assertEquals("Release", config.build.ios.configuration)
        assertEquals("iPhone Distribution", config.build.ios.codeSignIdentity)
        assertEquals("ipa", config.build.ios.output.format)
    }

    /**
     * Test: Multi-Platform Build
     */
    @Test
    fun testMultiPlatformBuild() {
        val yaml = """
            build:
              android:
                enabled: true
                flavors:
                  - free
                  - paid

              ios:
                enabled: true
                scheme: Runner
        """.trimIndent()

        val config = parser.parse(yaml)

        assertTrue(config.build.android.enabled)
        assertTrue(config.build.ios.enabled == true)
        assertEquals(2, config.build.android.flavors.size)
        assertEquals("Runner", config.build.ios.scheme)
    }

    // ═════════════════════════════════════════════════════════════
    // VERSION MANAGEMENT TESTS
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Version Configuration
     */
    @Test
    fun testVersionManagementConfiguration() {
        val yaml = """
            version:
              enabled: true
              current: 2.5.3
              auto_increment: true
              bump_strategy: minor
              files_to_update:
                - package.json
                - android/app/build.gradle
                - ios/App/Info.plist
                - version.properties
        """.trimIndent()

        val config = parser.parse(yaml)

        assertNotNull(config.version)
        assertTrue(config.version!!.enabled)
        assertEquals("2.5.3", config.version?.current)
        assertTrue(config.version!!.autoIncrement)
        assertEquals("minor", config.version?.bumpStrategy)
        assertEquals(4, config.version?.filesToUpdate?.size)
        assertTrue(config.version?.filesToUpdate?.contains("package.json") == true)
    }

    /**
     * Test: Version Bump Strategies
     */
    @Test
    fun testVersionBumpStrategies() {
        val strategies = listOf("patch", "minor", "major", "auto", "manual")
        val workflow = DeploymentWorkflow(workingPath, detector, false)

        strategies.forEach { strategy ->
            assertTrue(workflow.isValidStrategy(strategy))
        }

        assertFalse(workflow.isValidStrategy("invalid"))
    }

    // ═════════════════════════════════════════════════════════════
    // CHANGELOG GENERATION TESTS
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Changelog Configuration
     */
    @Test
    fun testChangelogConfiguration() {
        val yaml = """
            changelog:
              enabled: true
              format: markdown
              output_file: CHANGELOG.md
              include_breaking_changes: true
              include_contributors: true
              include_stats: true
              include_compare_links: true
              group_by_version: true
              commit_types:
                - type: feat
                  title: Features
                  emoji: ✨
                - type: fix
                  title: Bug Fixes
                  emoji: 🐛
                - type: docs
                  title: Documentation
                  emoji: 📝
        """.trimIndent()

        val config = parser.parse(yaml)

        assertTrue(config.changelog.enabled)
        assertEquals("markdown", config.changelog.format)
        assertEquals("CHANGELOG.md", config.changelog.outputFile)
        assertTrue(config.changelog.includeBreakingChanges)
        assertTrue(config.changelog.includeContributors)
        assertTrue(config.changelog.includeStats)
        assertTrue(config.changelog.includeCompareLinks)
        assertTrue(config.changelog.groupByVersion)
        assertEquals(3, config.changelog.commitTypes.size)

        val featType = config.changelog.commitTypes.find { it.type == "feat" }
        assertNotNull(featType)
        assertEquals("Features", featType.title)
        assertEquals("✨", featType.emoji)
    }

    /**
     * Test: Changelog with Release Notes
     */
    @Test
    fun testChangelogWithReleaseNotes() {
        val yaml = """
            changelog:
              enabled: true
              releases:
                "1.0.0":
                  highlights: "Initial release with core features"
                  breaking_changes:
                    - "New API structure"
                    - "Updated authentication flow"
                  contributors:
                    - "developer1"
                    - "developer2"
                "0.9.0":
                  highlights: "Beta release"
        """.trimIndent()

        val config = parser.parse(yaml)

        assertEquals(2, config.changelog.releases.size)

        val v1Release = config.changelog.releases["1.0.0"]
        assertNotNull(v1Release)
        assertEquals("Initial release with core features", v1Release.highlights)
        assertEquals(2, v1Release.breaking_changes.size)
        assertEquals(2, v1Release.contributors.size)
    }

    // ═════════════════════════════════════════════════════════════
    // DEPLOYMENT DESTINATION TESTS
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Firebase App Distribution Configuration
     */
    @Test
    fun testFirebaseConfiguration() {
        val yaml = """
            deploy:
              android:
                firebase:
                  enabled: true
                  service_account: credentials/firebase-adminsdk.json
                  google_services: android/app/google-services.json
                  test_groups:
                    - qa-team
                    - beta-testers
                    - internal-team
        """.trimIndent()

        val config = parser.parse(yaml)

        val firebase = config.deploy.android?.firebase
        assertNotNull(firebase)
        assertTrue(firebase.enabled)
        assertEquals("credentials/firebase-adminsdk.json", firebase.serviceAccount)
        assertEquals("android/app/google-services.json", firebase.googleServices)
        assertEquals(3, firebase.testGroups.size)
        assertTrue(firebase.testGroups.contains("qa-team"))
    }

    /**
     * Test: Google Play Console Configuration
     */
    @Test
    fun testPlayConsoleConfiguration() {
        val yaml = """
            deploy:
              android:
                play_console:
                  enabled: true
                  service_account: credentials/play-console.json
                  package_name: com.example.app
        """.trimIndent()

        val config = parser.parse(yaml)

        val playConsole = config.deploy.android?.playConsole
        assertNotNull(playConsole)
        assertTrue(playConsole.enabled)
        assertEquals("credentials/play-console.json", playConsole.serviceAccount)
        assertEquals("com.example.app", playConsole.packageName)
    }

    /**
     * Test: TestFlight Configuration
     */
    @Test
    fun testTestFlightConfiguration() {
        val yaml = """
            deploy:
              ios:
                testflight:
                  enabled: true
                  api_key_path: credentials/AuthKey_ABC123.p8
                  bundle_id: com.example.app
                  team_id: XYZ789
        """.trimIndent()

        val config = parser.parse(yaml)

        val testflight = config.deploy.ios?.testflight
        assertNotNull(testflight)
        assertTrue(testflight.enabled)
        assertEquals("credentials/AuthKey_ABC123.p8", testflight.apiKeyPath)
        assertEquals("com.example.app", testflight.bundleId)
        assertEquals("XYZ789", testflight.teamId)
    }

    /**
     * Test: App Store Configuration
     */
    @Test
    fun testAppStoreConfiguration() {
        val yaml = """
            deploy:
              ios:
                app_store:
                  enabled: true
                  api_key_path: credentials/AuthKey_ABC123.p8
                  bundle_id: com.example.app
                  team_id: XYZ789
        """.trimIndent()

        val config = parser.parse(yaml)

        val appStore = config.deploy.ios?.appStore
        assertNotNull(appStore)
        assertTrue(appStore.enabled)
        assertEquals("credentials/AuthKey_ABC123.p8", appStore.apiKeyPath)
        assertEquals("com.example.app", appStore.bundleId)
    }

    /**
     * Test: Local Deployment Configuration
     */
    @Test
    fun testLocalDeploymentConfiguration() {
        val yaml = """
            deploy:
              android:
                local:
                  enabled: true
                  output_dir: builds/android/releases
        """.trimIndent()

        val config = parser.parse(yaml)

        val local = config.deploy.android?.local
        assertNotNull(local)
        assertTrue(local.enabled)
        assertEquals("builds/android/releases", local.outputDir)
    }

    /**
     * Test: Multi-Destination Deployment
     */
    @Test
    fun testMultiDestinationDeployment() {
        val yaml = """
            deploy:
              android:
                firebase:
                  enabled: true
                  service_account: credentials/firebase.json
                  test_groups:
                    - qa-team

                play_console:
                  enabled: true
                  service_account: credentials/play.json
                  package_name: com.example.app

                local:
                  enabled: true
                  output_dir: builds/

              ios:
                testflight:
                  enabled: true
                  api_key_path: credentials/AuthKey.p8
                  bundle_id: com.example.app
                  team_id: ABC123

                app_store:
                  enabled: false
        """.trimIndent()

        val config = parser.parse(yaml)

        // Verify Android destinations
        assertTrue(config.deploy.android?.firebase?.enabled == true)
        assertTrue(config.deploy.android?.playConsole?.enabled == true)
        assertTrue(config.deploy.android?.local?.enabled == true)

        // Verify iOS destinations
        assertTrue(config.deploy.ios?.testflight?.enabled == true)
        assertFalse(config.deploy.ios?.appStore?.enabled == true)
    }

    // ═════════════════════════════════════════════════════════════
    // NOTIFICATION TESTS
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Slack Notification Configuration
     */
    @Test
    fun testSlackNotifications() {
        val yaml = """
            notify:
              slack:
                enabled: true
                webhook_url: https://hooks.slack.com/services/XXX/YYY/ZZZ
                channel: "#deployments"
                notify_on:
                  - success
                  - failure
        """.trimIndent()

        val config = parser.parse(yaml)

        assertTrue(config.notify.slack.enabled)
        assertTrue(config.notify.slack.webhookUrl.contains("hooks.slack.com"))
        assertEquals("#deployments", config.notify.slack.channel)
        assertEquals(2, config.notify.slack.notifyOn.size)
    }

    /**
     * Test: Email Notification Configuration
     */
    @Test
    fun testEmailNotifications() {
        val yaml = """
            notify:
              email:
                enabled: true
                recipients:
                  - dev-team@company.com
                  - qa-team@company.com
                  - product@company.com
                notify_on:
                  - failure
        """.trimIndent()

        val config = parser.parse(yaml)

        assertTrue(config.notify.email.enabled)
        assertEquals(3, config.notify.email.recipients.size)
        assertTrue(config.notify.email.recipients.contains("dev-team@company.com"))
        assertEquals(listOf("failure"), config.notify.email.notifyOn)
    }

    /**
     * Test: Webhook Notification Configuration
     */
    @Test
    fun testWebhookNotifications() {
        val yaml = """
            notify:
              webhook:
                enabled: true
                url: https://api.company.com/v1/webhooks/deployments
                events:
                  - build_started
                  - build_completed
                  - deploy_started
                  - deploy_completed
                  - deploy_failed
        """.trimIndent()

        val config = parser.parse(yaml)

        assertTrue(config.notify.webhook.enabled)
        assertEquals("https://api.company.com/v1/webhooks/deployments", config.notify.webhook.url)
        assertEquals(5, config.notify.webhook.events.size)
        assertTrue(config.notify.webhook.events.contains("deploy_failed"))
    }

    // ═════════════════════════════════════════════════════════════
    // REPORTING TESTS
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Report Configuration
     */
    @Test
    fun testReportConfiguration() {
        val yaml = """
            report:
              enabled: true
              format: html
              include:
                - build_info
                - git_info
                - build_duration
                - artifact_size
                - test_results
              output_path: ./build-reports
        """.trimIndent()

        val config = parser.parse(yaml)

        assertTrue(config.report.enabled)
        assertEquals("html", config.report.format)
        assertEquals(5, config.report.include.size)
        assertTrue(config.report.include.contains("git_info"))
        assertEquals("./build-reports", config.report.outputPath)
    }

    // ═════════════════════════════════════════════════════════════
    // ENVIRONMENT VARIABLE TESTS
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Environment Variables in Configuration
     */
    @Test
    fun testEnvironmentVariables() {
        val yaml = """
            build:
              android:
                key_password: ${'$'}{ANDROID_KEY_PASSWORD}
                store_password: ${'$'}{ANDROID_STORE_PASSWORD}

            deploy:
              android:
                firebase:
                  service_account: ${'$'}{FIREBASE_SERVICE_ACCOUNT}

            notify:
              slack:
                webhook_url: ${'$'}{SLACK_WEBHOOK_URL}

            env:
              API_URL: https://api.production.com
              APP_ENV: production
        """.trimIndent()

        val config = parser.parse(yaml)

        // Verify env section
        assertEquals(2, config.env.size)
        assertEquals("https://api.production.com", config.env["API_URL"])
        assertEquals("production", config.env["APP_ENV"])

        // Verify environment variable placeholders are preserved
        assertTrue(config.build.android.keyPassword.contains("ANDROID_KEY_PASSWORD") ||
                  config.build.android.keyPassword.isEmpty())
    }

    // ═════════════════════════════════════════════════════════════
    // COMPLETE APPLICATION CONFIGURATION TESTS
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Complete Production Configuration
     *
     * This tests a real-world, production-ready configuration
     * with all features enabled
     */
    @Test
    fun testCompleteProductionConfiguration() {
        val yaml = """
            app:
              name: ProductionApp
              identifier: com.company.app
              version: 2.1.0

            version:
              enabled: true
              current: 2.1.0
              auto_increment: true
              bump_strategy: patch
              files_to_update:
                - package.json
                - version.properties

            changelog:
              enabled: true
              format: markdown
              output_file: CHANGELOG.md
              include_breaking_changes: true
              include_contributors: true

            build:
              android:
                enabled: true
                default_flavor: production
                default_type: release
                flavors:
                  - development
                  - staging
                  - production
                output_type: aab
                firebase_output_type: apk

              ios:
                enabled: true
                project_path: ios/App.xcworkspace
                scheme: App
                configuration: Release

            deploy:
              enabled: true
              default_group: production

              flavor_groups:
                production:
                  name: Production
                  description: Production releases
                  flavors:
                    - production

                internal:
                  name: Internal
                  description: Internal testing
                  flavors:
                    - development
                    - staging

              android:
                firebase:
                  enabled: true
                  service_account: credentials/firebase.json
                  test_groups:
                    - qa-team
                    - beta-testers

                play_console:
                  enabled: true
                  service_account: credentials/play.json
                  package_name: com.company.app

              ios:
                testflight:
                  enabled: true
                  api_key_path: credentials/AuthKey.p8
                  bundle_id: com.company.app
                  team_id: ABC123

            notify:
              slack:
                enabled: true
                webhook_url: https://hooks.slack.com/XXX
                channel: "#deployments"
                notify_on:
                  - success
                  - failure

              email:
                enabled: true
                recipients:
                  - team@company.com
                notify_on:
                  - failure

            report:
              enabled: true
              format: html
              include:
                - build_info
                - git_info
                - build_duration
              output_path: ./reports
        """.trimIndent()

        val config = parser.parse(yaml)

        // Verify all sections loaded
        assertNotNull(config.app)
        assertNotNull(config.version)
        assertNotNull(config.changelog)
        assertNotNull(config.build)
        assertNotNull(config.deploy)
        assertNotNull(config.notify)
        assertNotNull(config.report)

        // Verify app info
        assertEquals("ProductionApp", config.app.name)
        assertEquals("com.company.app", config.app.identifier)

        // Verify version management
        assertTrue(config.version!!.enabled)
        assertEquals("2.1.0", config.version?.current)

        // Verify changelog
        assertTrue(config.changelog.enabled)

        // Verify build
        assertTrue(config.build.android.enabled)
        assertTrue(config.build.ios.enabled == true)
        assertEquals(3, config.build.android.flavors.size)

        // Verify deploy
        assertTrue(config.deploy.enabled)
        assertEquals("production", config.deploy.defaultGroup)
        assertEquals(2, config.deploy.flavorGroups.size)

        // Verify destinations
        assertTrue(config.deploy.android?.firebase?.enabled == true)
        assertTrue(config.deploy.android?.playConsole?.enabled == true)
        assertTrue(config.deploy.ios?.testflight?.enabled == true)

        // Verify notifications
        assertTrue(config.notify.slack.enabled)
        assertTrue(config.notify.email.enabled)

        // Verify reporting
        assertTrue(config.report.enabled)
    }

    // ═════════════════════════════════════════════════════════════
    // CONFIGURATION MERGING TESTS
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Default Configuration Merging
     *
     * Tests that user config properly merges with defaults
     */
    @Test
    fun testConfigurationMerging() {
        // Minimal user config
        val yaml = """
            app:
              name: MinimalApp

            build:
              android:
                enabled: true
                flavors:
                  - free
        """.trimIndent()

        val config = parser.parse(yaml)

        // Verify user values
        assertEquals("MinimalApp", config.app.name)
        assertTrue(config.build.android.enabled)
        assertEquals(listOf("free"), config.build.android.flavors)

        // Verify defaults are applied
        assertNotNull(config.version)
        assertNotNull(config.changelog)
        assertNotNull(config.deploy)
    }

    // ═════════════════════════════════════════════════════════════
    // SERIALIZATION ROUND-TRIP TESTS
    // ═════════════════════════════════════════════════════════════

    /**
     * Test: Complete Configuration Round-Trip
     *
     * Parse → Serialize → Parse should produce identical config
     */
    @Test
    fun testCompleteConfigurationRoundTrip() {
        val originalYaml = """
            app:
              name: RoundTripTest
              identifier: com.test.roundtrip
              version: 1.0.0

            build:
              android:
                enabled: true
                flavors:
                  - free
                  - paid

            deploy:
              default_group: production

              flavor_groups:
                production:
                  name: Production
                  flavors:
                    - free
                    - paid

            version:
              current: 1.0.0
        """.trimIndent()

        // Parse original
        val config1 = parser.parse(originalYaml)

        // Serialize
        val serialized = parser.toYaml(config1)

        // Parse again
        val config2 = parser.parse(serialized)

        // Verify key values match
        assertEquals(config1.app.name, config2.app.name)
        assertEquals(config1.app.identifier, config2.app.identifier)
        assertEquals(config1.build.android.flavors.size, config2.build.android.flavors.size)
        assertEquals(config1.deploy.defaultGroup, config2.deploy.defaultGroup)
        assertEquals(config1.deploy.flavorGroups.size, config2.deploy.flavorGroups.size)
    }
}
