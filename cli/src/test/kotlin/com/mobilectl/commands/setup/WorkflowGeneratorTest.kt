package com.mobilectl.commands.setup

import com.mobilectl.config.Config
import com.mobilectl.model.buildConfig.AndroidBuildConfig
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.buildConfig.IosBuildConfig
import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.versionManagement.VersionConfig
import kotlin.test.*

/**
 * Unit tests for WorkflowGenerator
 *
 * Tests:
 * - GitHub Actions workflow generation
 * - GitLab CI pipeline generation
 * - Different platform configurations
 * - Proper secret handling
 * - Workflow structure validation
 */
class WorkflowGeneratorTest {

    private lateinit var generator: WorkflowGenerator

    @BeforeTest
    fun setup() {
        generator = WorkflowGenerator()
    }

    // ═════════════════════════════════════════════════════════════
    // GITHUB ACTIONS TESTS
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testGenerateGitHubActions_AndroidOnly() {
        val config = createConfigWithAndroid()

        val workflow = generator.generateGitHubActions(config)

        assertNotNull(workflow)
        assertTrue(workflow.contains("deploy-android"), "Should have Android job")
        assertTrue(workflow.contains("openjdk") || workflow.contains("java"), "Should set up Java")
        assertTrue(workflow.contains("mobilectl build"), "Should build with mobilectl")
        assertTrue(workflow.contains("mobilectl deploy"), "Should deploy with mobilectl")
    }

    @Test
    fun testGenerateGitHubActions_IosOnly() {
        val config = createConfigWithIos()

        val workflow = generator.generateGitHubActions(config)

        assertNotNull(workflow)
        assertTrue(workflow.contains("deploy-ios"), "Should have iOS job")
        assertTrue(workflow.contains("macos"), "Should use macOS runner")
        assertTrue(workflow.contains("xcode") || workflow.contains("Xcode"), "Should set up Xcode")
    }

    @Test
    fun testGenerateGitHubActions_BothPlatforms() {
        val config = createConfigWithBothPlatforms()

        val workflow = generator.generateGitHubActions(config)

        assertNotNull(workflow)
        assertTrue(workflow.contains("deploy-android"), "Should have Android job")
        assertTrue(workflow.contains("deploy-ios"), "Should have iOS job")
    }

    @Test
    fun testGenerateGitHubActions_HasTriggers() {
        val config = createConfigWithAndroid()

        val workflow = generator.generateGitHubActions(config)

        // Should have multiple triggers
        assertTrue(workflow.contains("on:"), "Should define triggers")
        assertTrue(workflow.contains("push:") || workflow.contains("tags:"), "Should trigger on tags")
        assertTrue(workflow.contains("workflow_dispatch:"), "Should have manual trigger")
        assertTrue(workflow.contains("pull_request:"), "Should trigger on PRs")
    }

    @Test
    fun testGenerateGitHubActions_HasSecrets() {
        val config = createConfigWithAndroid()

        val workflow = generator.generateGitHubActions(config)

        // Should reference secrets
        assertTrue(workflow.contains("secrets."), "Should use GitHub secrets")
        assertTrue(
            workflow.contains("ANDROID_KEY_PASSWORD") || workflow.contains("FIREBASE_SERVICE_ACCOUNT"),
            "Should reference Android/Firebase secrets"
        )
    }

    @Test
    fun testGenerateGitHubActions_VersionManagement() {
        val config = createConfigWithVersioning()

        val workflow = generator.generateGitHubActions(config)

        assertTrue(workflow.contains("version"), "Should include version management")
        assertTrue(workflow.contains("bump"), "Should bump version")
    }

    @Test
    fun testGenerateGitHubActions_Changelog() {
        val config = createConfigWithChangelog()

        val workflow = generator.generateGitHubActions(config)

        assertTrue(workflow.contains("changelog"), "Should include changelog generation")
    }

    @Test
    fun testGenerateGitHubActions_TestJob() {
        val config = createConfigWithAndroid()

        val workflow = generator.generateGitHubActions(config)

        assertTrue(workflow.contains("test:"), "Should have test job")
        assertTrue(workflow.contains("pull_request"), "Test job should run on PRs")
    }

    // ═════════════════════════════════════════════════════════════
    // GITLAB CI TESTS
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testGenerateGitLabCI_AndroidOnly() {
        val config = createConfigWithAndroid()

        val pipeline = generator.generateGitLabCI(config)

        assertNotNull(pipeline)
        assertTrue(pipeline.contains("build:android"), "Should have Android build job")
        assertTrue(pipeline.contains("deploy:android"), "Should have Android deploy job")
        assertTrue(pipeline.contains("stages:"), "Should define stages")
    }

    @Test
    fun testGenerateGitLabCI_IosOnly() {
        val config = createConfigWithIos()

        val pipeline = generator.generateGitLabCI(config)

        assertNotNull(pipeline)
        assertTrue(pipeline.contains("build:ios"), "Should have iOS build job")
        assertTrue(pipeline.contains("deploy:ios"), "Should have iOS deploy job")
        assertTrue(pipeline.contains("macos"), "Should use macOS runner")
    }

    @Test
    fun testGenerateGitLabCI_HasStages() {
        val config = createConfigWithAndroid()

        val pipeline = generator.generateGitLabCI(config)

        assertTrue(pipeline.contains("stages:"), "Should define stages")
        assertTrue(pipeline.contains("- build"), "Should have build stage")
        assertTrue(pipeline.contains("- deploy"), "Should have deploy stage")
    }

    @Test
    fun testGenerateGitLabCI_HasArtifacts() {
        val config = createConfigWithAndroid()

        val pipeline = generator.generateGitLabCI(config)

        assertTrue(pipeline.contains("artifacts:"), "Should define artifacts")
        assertTrue(pipeline.contains("paths:"), "Should specify artifact paths")
    }

    @Test
    fun testGenerateGitLabCI_OnlyTagsAndMain() {
        val config = createConfigWithAndroid()

        val pipeline = generator.generateGitLabCI(config)

        assertTrue(pipeline.contains("only:"), "Should have only clause")
        assertTrue(pipeline.contains("tags") || pipeline.contains("main"), "Should run on tags/main")
    }

    // ═════════════════════════════════════════════════════════════
    // WORKFLOW STRUCTURE VALIDATION
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testGitHubActions_ValidYaml() {
        val config = createConfigWithAndroid()

        val workflow = generator.generateGitHubActions(config)

        // Basic YAML structure validation
        assertTrue(workflow.contains("name:"), "Should have workflow name")
        assertTrue(workflow.contains("jobs:"), "Should define jobs")
        assertTrue(workflow.contains("steps:"), "Should have steps")
        assertTrue(workflow.contains("- name:"), "Should name steps")
    }

    @Test
    fun testGitLabCI_ValidYaml() {
        val config = createConfigWithAndroid()

        val pipeline = generator.generateGitLabCI(config)

        // Basic YAML structure validation
        assertTrue(pipeline.contains("stages:"), "Should define stages")
        assertTrue(pipeline.contains("script:"), "Should have scripts")
        assertTrue(pipeline.contains("image:") || pipeline.contains("tags:"), "Should specify runner")
    }

    @Test
    fun testGitHubActions_NoEmptyJobs() {
        val config = createConfigWithAndroid()

        val workflow = generator.generateGitHubActions(config)

        // Count jobs (rough check)
        val jobCount = workflow.split("runs-on:").size - 1
        assertTrue(jobCount > 0, "Should have at least one job")
    }

    @Test
    fun testGitLabCI_NoEmptyJobs() {
        val config = createConfigWithAndroid()

        val pipeline = generator.generateGitLabCI(config)

        // Should have actual job definitions
        assertTrue(pipeline.contains("script:"), "Should have scripts in jobs")
    }

    // ═════════════════════════════════════════════════════════════
    // SECURITY & BEST PRACTICES
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testGitHubActions_NoHardcodedSecrets() {
        val config = createConfigWithAndroid()

        val workflow = generator.generateGitHubActions(config)

        // Should use secrets, not hardcoded values
        assertFalse(
            workflow.contains("password: \"actual_password\""),
            "Should not have hardcoded passwords"
        )
        assertTrue(workflow.contains("secrets."), "Should use GitHub secrets")
    }

    @Test
    fun testGitHubActions_UsesLatestActions() {
        val config = createConfigWithAndroid()

        val workflow = generator.generateGitHubActions(config)

        // Should use recent action versions
        assertTrue(
            workflow.contains("@v4") || workflow.contains("@v3"),
            "Should use recent action versions"
        )
    }

    @Test
    fun testGitHubActions_HasCheckout() {
        val config = createConfigWithAndroid()

        val workflow = generator.generateGitHubActions(config)

        assertTrue(workflow.contains("checkout"), "Should checkout code")
        assertTrue(
            workflow.contains("actions/checkout"),
            "Should use official checkout action"
        )
    }

    // ═════════════════════════════════════════════════════════════
    // EDGE CASES
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testGenerateGitHubActions_MinimalConfig() {
        val config = Config(
            build = BuildConfig()
        )

        val workflow = generator.generateGitHubActions(config)

        assertNotNull(workflow)
        assertTrue(workflow.contains("name:"), "Should generate valid workflow even with minimal config")
    }

    @Test
    fun testGenerateGitLabCI_MinimalConfig() {
        val config = Config(
            build = BuildConfig()
        )

        val pipeline = generator.generateGitLabCI(config)

        assertNotNull(pipeline)
        assertTrue(pipeline.contains("stages:"), "Should generate valid pipeline even with minimal config")
    }

    @Test
    fun testGenerators_ProduceNonEmptyOutput() {
        val config = createConfigWithBothPlatforms()

        val githubWorkflow = generator.generateGitHubActions(config)
        val gitlabPipeline = generator.generateGitLabCI(config)

        assertTrue(githubWorkflow.length > 100, "GitHub workflow should have substantial content")
        assertTrue(gitlabPipeline.length > 100, "GitLab pipeline should have substantial content")
    }

    // ═════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═════════════════════════════════════════════════════════════

    private fun createConfigWithAndroid(): Config {
        return Config(
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    defaultFlavor = "release",
                    defaultType = "release"
                )
            )
        )
    }

    private fun createConfigWithIos(): Config {
        return Config(
            build = BuildConfig(
                ios = IosBuildConfig(
                    enabled = true,
                    projectPath = "ios/MyApp.xcworkspace",
                    scheme = "MyApp"
                )
            )
        )
    }

    private fun createConfigWithBothPlatforms(): Config {
        return Config(
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    defaultFlavor = "release",
                    defaultType = "release"
                ),
                ios = IosBuildConfig(
                    enabled = true,
                    projectPath = "ios/MyApp.xcworkspace",
                    scheme = "MyApp"
                )
            )
        )
    }

    private fun createConfigWithVersioning(): Config {
        return Config(
            build = BuildConfig(
                android = AndroidBuildConfig(enabled = true)
            ),
            version = VersionConfig(
                enabled = true,
                autoIncrement = true,
                bumpStrategy = "patch"
            )
        )
    }

    private fun createConfigWithChangelog(): Config {
        return Config(
            build = BuildConfig(
                android = AndroidBuildConfig(enabled = true)
            ),
            changelog = ChangelogConfig(
                enabled = true,
                format = "markdown",
                outputFile = "CHANGELOG.md"
            )
        )
    }
}
