package com.mobilectl.validation

import java.io.File

/**
 * Test helpers for validation tests
 */
object ValidationTestHelpers {

    /**
     * Create temporary APK file for testing
     */
    fun createTempApk(): File {
        return File.createTempFile("test-app", ".apk").apply {
            deleteOnExit()
        }
    }

    /**
     * Create temporary AAB file for testing
     */
    fun createTempAab(): File {
        return File.createTempFile("test-app", ".aab").apply {
            deleteOnExit()
        }
    }

    /**
     * Create temporary IPA file for testing
     */
    fun createTempIpa(): File {
        return File.createTempFile("test-app", ".ipa").apply {
            deleteOnExit()
        }
    }

    /**
     * Create temporary Firebase service account JSON file
     */
    fun createTempFirebaseServiceAccount(): File {
        val file = File.createTempFile("firebase-service-account", ".json").apply {
            deleteOnExit()
        }
        // Write valid service account structure
        file.writeText("""
            {
              "type": "service_account",
              "project_id": "test-project",
              "private_key_id": "test-key-id",
              "private_key": "-----BEGIN RSA PRIVATE KEY-----\ntest\n-----END RSA PRIVATE KEY-----\n",
              "client_email": "test@test.iam.gserviceaccount.com",
              "client_id": "123456789",
              "auth_uri": "https://accounts.google.com/o/oauth2/auth",
              "token_uri": "https://oauth2.googleapis.com/token",
              "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
              "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/test%40test.iam.gserviceaccount.com"
            }
        """.trimIndent())
        return file
    }

    /**
     * Create temporary Play Console service account JSON file
     */
    fun createTempPlayConsoleServiceAccount(): File {
        return createTempFirebaseServiceAccount()  // Same structure
    }

    /**
     * Create temporary App Store Connect API key JSON file
     */
    fun createTempAppStoreApiKey(): File {
        val file = File.createTempFile("app-store-api-key", ".json").apply {
            deleteOnExit()
        }
        // Write valid API key structure
        file.writeText("""
            {
              "key_id": "ABC123XYZ",
              "issuer_id": "12345678-1234-1234-1234-123456789012",
              "private_key": "-----BEGIN RSA PRIVATE KEY-----\ntest\n-----END RSA PRIVATE KEY-----\n"
            }
        """.trimIndent())
        return file
    }
}
