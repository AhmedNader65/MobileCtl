package com.mobilectl.commands.deploy

import kotlin.test.*
import java.io.File

/**
 * Unit tests for SmartDefaultsProvider
 *
 * Tests:
 * - Environment detection from git branch
 * - Firebase config auto-detection
 * - Artifact auto-detection
 * - Smart defaults generation
 */
class SmartDefaultsProviderTest {

    private lateinit var provider: SmartDefaultsProvider
    private val workingPath = System.getProperty("user.dir")

    @BeforeTest
    fun setup() {
        provider = SmartDefaultsProvider(workingPath, verbose = false)
    }

    // ═════════════════════════════════════════════════════════════
    // ENVIRONMENT DETECTION
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testDetectEnvironment_ReturnsString() {
        val env = provider.detectEnvironment()

        assertNotNull(env, "Environment should not be null")
        assertTrue(
            env in listOf("production", "staging", "dev"),
            "Environment should be one of: production, staging, dev"
        )
    }

    @Test
    fun testDetectEnvironment_FallsBackToDev() {
        // When git is not available or errors occur, should default to "dev"
        val env = provider.detectEnvironment()

        assertNotNull(env)
        assertTrue(env.isNotEmpty())
    }

    // ═════════════════════════════════════════════════════════════
    // SMART DEFAULTS GENERATION
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testCreateSmartDefaults_ReturnsValidConfig() {
        val config = provider.createSmartDefaults()

        assertNotNull(config, "Config should not be null")
        assertNotNull(config.build, "Build config should not be null")
        assertNotNull(config.deploy, "Deploy config should not be null")
        assertNotNull(config.version, "Version config should not be null")
    }

    @Test
    fun testCreateSmartDefaults_AndroidConfigPresent() {
        val config = provider.createSmartDefaults()

        assertNotNull(config.deploy.android, "Android deploy config should be present")
        assertTrue(config.deploy.android!!.enabled, "Android should be enabled by default")
    }

    @Test
    fun testCreateSmartDefaults_FirebaseConfigPresent() {
        val config = provider.createSmartDefaults()

        val firebaseConfig = config.deploy.android?.firebase
        assertNotNull(firebaseConfig, "Firebase config should be present")
    }

    @Test
    fun testCreateSmartDefaults_ArtifactPathSet() {
        val config = provider.createSmartDefaults()

        val artifactPath = config.deploy.android?.artifactPath
        assertNotNull(artifactPath, "Artifact path should be set")
        assertTrue(
            artifactPath.endsWith(".apk") || artifactPath.contains("apk"),
            "Artifact path should reference APK file"
        )
    }

    @Test
    fun testCreateSmartDefaults_IosConfigPresent() {
        val config = provider.createSmartDefaults()

        assertNotNull(config.deploy.ios, "iOS config should be present")
        // iOS is disabled by default
        assertFalse(config.deploy.ios!!.enabled, "iOS should be disabled by default")
    }

    // ═════════════════════════════════════════════════════════════
    // ARTIFACT DETECTION
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testSmartDefaults_UsesExistingArtifactIfFound() {
        val config = provider.createSmartDefaults()

        // If artifact exists, path should point to it
        // If not, should use default path
        val artifactPath = config.deploy.android?.artifactPath
        assertNotNull(artifactPath)
        assertTrue(artifactPath.isNotEmpty())
    }

    @Test
    fun testSmartDefaults_IosArtifactPath() {
        val config = provider.createSmartDefaults()

        val iosPath = config.deploy.ios?.artifactPath
        assertNotNull(iosPath, "iOS artifact path should be set")
        assertTrue(
            iosPath.endsWith(".ipa") || iosPath.contains("ipa"),
            "iOS artifact should reference IPA file"
        )
    }

    // ═════════════════════════════════════════════════════════════
    // FIREBASE DETECTION
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testSmartDefaults_FirebaseServiceAccountPath() {
        val config = provider.createSmartDefaults()

        val serviceAccountPath = config.deploy.android?.firebase?.serviceAccount
        assertNotNull(serviceAccountPath, "Service account path should be set")
        assertTrue(
            serviceAccountPath.contains("firebase") || serviceAccountPath.contains("service"),
            "Service account path should reference firebase/service account"
        )
    }

    @Test
    fun testSmartDefaults_FirebaseEnabledByDefault() {
        val config = provider.createSmartDefaults()

        assertTrue(
            config.deploy.android?.firebase?.enabled == true,
            "Firebase should be enabled by default"
        )
    }

    @Test
    fun testSmartDefaults_PlayConsoleDisabledByDefault() {
        val config = provider.createSmartDefaults()

        assertFalse(
            config.deploy.android?.playConsole?.enabled == true,
            "Play Console should be disabled by default"
        )
    }

    @Test
    fun testSmartDefaults_LocalDisabledByDefault() {
        val config = provider.createSmartDefaults()

        assertFalse(
            config.deploy.android?.local?.enabled == true,
            "Local deployment should be disabled by default"
        )
    }

    // ═════════════════════════════════════════════════════════════
    // CONSISTENCY TESTS
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testMultipleCallsReturnConsistentResults() {
        val config1 = provider.createSmartDefaults()
        val config2 = provider.createSmartDefaults()

        // Should return consistent structure (not necessarily same instance)
        assertEquals(
            config1.deploy.android?.enabled,
            config2.deploy.android?.enabled,
            "Android enabled should be consistent"
        )

        assertEquals(
            config1.deploy.android?.firebase?.enabled,
            config2.deploy.android?.firebase?.enabled,
            "Firebase enabled should be consistent"
        )
    }

    @Test
    fun testEnvironmentDetection_Stable() {
        val env1 = provider.detectEnvironment()
        val env2 = provider.detectEnvironment()

        // Git branch doesn't change between calls
        assertEquals(env1, env2, "Environment should be stable across calls")
    }

    // ═════════════════════════════════════════════════════════════
    // EDGE CASES
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testSmartDefaults_WithVerboseMode() {
        val verboseProvider = SmartDefaultsProvider(workingPath, verbose = true)

        // Should not crash with verbose mode
        val config = verboseProvider.createSmartDefaults()
        assertNotNull(config)
    }

    @Test
    fun testSmartDefaults_InvalidWorkingPath() {
        val invalidProvider = SmartDefaultsProvider("/invalid/path/that/does/not/exist", verbose = false)

        // Should still return valid config with defaults
        val config = invalidProvider.createSmartDefaults()
        assertNotNull(config)
        assertNotNull(config.deploy.android)
    }

    @Test
    fun testEnvironmentDetection_NoGitRepo() {
        // When not in a git repo, should default to "dev"
        val nonGitProvider = SmartDefaultsProvider("/tmp", verbose = false)

        val env = nonGitProvider.detectEnvironment()
        assertEquals("dev", env, "Should default to 'dev' when not in git repo")
    }

    // ═════════════════════════════════════════════════════════════
    // INTEGRATION CHECKS
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testSmartDefaults_CanBeUsedInConfiguration() {
        val config = provider.createSmartDefaults()

        // Config should be usable for deployment
        assertTrue(config.deploy.android?.enabled == true)
        assertNotNull(config.deploy.android?.artifactPath)
        assertNotNull(config.deploy.android?.firebase)

        // Should have all required fields for deployment
        val firebase = config.deploy.android?.firebase
        assertNotNull(firebase?.serviceAccount)
    }
}
