package com.mobilectl.deploy

import java.io.File

/**
 * Test helpers for deploy tests
 */
object DeployTestHelpers {

    /**
     * Create a temporary artifact file for testing
     */
    fun createTempArtifact(name: String, extension: String): File {
        return File.createTempFile(name, extension).apply {
            deleteOnExit()
        }
    }

    /**
     * Create temp APK for Android tests
     */
    fun createTempApk(): File {
        return createTempArtifact("test-app", ".apk")
    }

    /**
     * Create temp IPA for iOS tests
     */
    fun createTempIpa(): File {
        return createTempArtifact("test-app", ".ipa")
    }

    /**
     * Create temp AAB for Android Bundle tests
     */
    fun createTempAab(): File {
        return createTempArtifact("test-app", ".aab")
    }
}
