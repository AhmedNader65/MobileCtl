package com.mobilectl.config

import com.mobilectl.model.buildConfig.AndroidBuildConfig
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.buildConfig.IosBuildConfig
import com.mobilectl.model.deploy.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigParserTest {

        /**
         * Test minimal config with only required fields
         */
        @Test
        fun testParseMinimalConfig() {
                val yaml = """
            version: "1.0"
            
            build:
              android:
                enabled: true
              ios:
                enabled: false
        """.trimIndent()

                val parser = SnakeYamlConfigParser()
                val config = parser.parse(yaml)

                assertNotNull(config)
                assertEquals("1.0.0", config.version?.current)
                assertTrue(config.build.android.enabled)
                assertTrue(config.build.ios.enabled == false)
        }

        /**
         * Test complete config with all deploy options
         */
        @Test
        fun testParseFullAndroidConfig() {
                val yaml = """
            version: "1.0"
            
            build:
              android:
                enabled: true
                defaultFlavor: release
                defaultType: release
                keyStore: keystore.jks
                keyAlias: my-app-key
                keyPassword: password1
                storePassword: password1
            
            deploy:
              android:
                enabled: true
                artifactPath: build/outputs/apk/release/app-release.apk
                firebase:
                  enabled: true
                  serviceAccount: credentials/firebase-service-account.json
                  testGroups:
                    - qa-team
                    - beta-testers
                playConsole:
                  enabled: false
                  serviceAccount: credentials/play-console.json
                  packageName: com.example.myapp
                local:
                  enabled: false
                  outputDir: build/deploy
        """.trimIndent()

                val parser = SnakeYamlConfigParser()
                val config = parser.parse(yaml)

                // Build config
                assertNotNull(config.build)
                assertTrue(config.build.android.enabled)
                assertEquals("release", config.build.android.defaultFlavor)
                assertEquals("release", config.build.android.defaultType)
                assertEquals("keystore.jks", config.build.android.keyStore)
                assertEquals("my-app-key", config.build.android.keyAlias)

                // Deploy config
                assertNotNull(config.deploy)
                assertNotNull(config.deploy?.android)
                assertTrue(config.deploy?.android?.enabled == true)
                assertEquals("build/outputs/apk/release/app-release.apk", config.deploy?.android?.artifactPath)

                // Firebase
                assertTrue(config.deploy?.android?.firebase?.enabled == true)
                assertEquals("credentials/firebase-service-account.json", config.deploy?.android?.firebase?.serviceAccount)
                assertEquals(2, config.deploy?.android?.firebase?.testGroups?.size)

                // Play Console
                assertTrue(config.deploy?.android?.playConsole?.enabled == false)

                // Local
                assertTrue(config.deploy?.android?.local?.enabled == false)
        }

        /**
         * Test iOS deploy configuration
         */
        @Test
        fun testParseFullIosConfig() {
                val yaml = """
            version: "1.0"
            
            build:
              ios:
                enabled: true
                scheme: MyApp
            
            deploy:
              ios:
                enabled: true
                artifactPath: build/outputs/ipa/release/app.ipa
                testflight:
                  enabled: true
                  apiKeyPath: credentials/app-store-connect-api-key.json
                  bundleId: com.example.myapp
                  teamId: ABC123XYZ
                appStore:
                  enabled: false
                  apiKeyPath: credentials/app-store-connect-api-key.json
                  bundleId: com.example.myapp
                  teamId: ABC123XYZ
        """.trimIndent()

                val parser = SnakeYamlConfigParser()
                val config = parser.parse(yaml)

                // Build config
                assertNotNull(config.build)
                assertTrue(config.build.ios.enabled == true)
                assertEquals("MyApp", config.build.ios.scheme)

                // Deploy config
                assertNotNull(config.deploy)
                assertNotNull(config.deploy?.ios)
                assertTrue(config.deploy?.ios?.enabled == true)
                assertEquals("build/outputs/ipa/release/app.ipa", config.deploy?.ios?.artifactPath)

                // TestFlight
                assertTrue(config.deploy?.ios?.testflight?.enabled == true)
                assertEquals("credentials/app-store-connect-api-key.json", config.deploy?.ios?.testflight?.apiKeyPath)
                assertEquals("com.example.myapp", config.deploy?.ios?.testflight?.bundleId)
                assertEquals("ABC123XYZ", config.deploy?.ios?.testflight?.teamId)

                // App Store
                assertTrue(config.deploy?.ios?.appStore?.enabled == false)
        }

        /**
         * Test snake_case to camelCase conversion
         */
        @Test
        fun testSnakeCaseConversion() {
                val yaml = """
            version: "1.0"
            
            build:
              android:
                enabled: true
                default_flavor: staging
                default_type: debug
                key_store: keystore.jks
                key_alias: staging-key
                key_password: pass123
                store_password: pass123
            
            deploy:
              android:
                enabled: true
                artifact_path: build/outputs/apk/staging/debug/app-staging-debug.apk
                firebase:
                  enabled: true
                  service_account: credentials/firebase-account.json
                  test_groups:
                    - internal
        """.trimIndent()

                val parser = SnakeYamlConfigParser()
                val config = parser.parse(yaml)

                // Should handle snake_case
                assertEquals("staging", config.build.android.defaultFlavor)
                assertEquals("debug", config.build.android.defaultType)
                assertEquals("keystore.jks", config.build.android.keyStore)
                assertEquals("staging-key", config.build.android.keyAlias)
                assertEquals("build/outputs/apk/staging/debug/app-staging-debug.apk", config.deploy?.android?.artifactPath)
                assertEquals(1, config.deploy?.android?.firebase?.testGroups?.size)
        }

        /**
         * Test environment variable overrides
         */
        @Test
        fun testEnvironmentVariableDefaults() {
                // Set environment variable
                System.setProperty("MOBILECTL_KEY_ALIAS", "env-key")
                System.setProperty("MOBILECTL_KEY_PASSWORD", "env-pass")
                System.setProperty("MOBILECTL_STORE_PASSWORD", "env-store-pass")

                val yaml = """
            version: "1.0"
            
            build:
              android:
                enabled: true
                keyStore: keystore.jks
        """.trimIndent()

                val parser = SnakeYamlConfigParser()
                val config = parser.parse(yaml)

                // Should fall back to environment variables
                val keyAlias = config.build.android.keyAlias.ifBlank {
                        System.getProperty("MOBILECTL_KEY_ALIAS") ?: ""
                }
                assertEquals("env-key", keyAlias)

                // Cleanup
                System.clearProperty("MOBILECTL_KEY_ALIAS")
                System.clearProperty("MOBILECTL_KEY_PASSWORD")
                System.clearProperty("MOBILECTL_STORE_PASSWORD")
        }

        /**
         * Test defaults when config is missing
         */
        @Test
        fun testDefaultsWhenMissing() {
                val yaml = """
            version: "1.0"
        """.trimIndent()

                val parser = SnakeYamlConfigParser()
                val config = parser.parse(yaml)

                // Build should exist with defaults
                assertNotNull(config.build)
                assertEquals(false, config.build.android.enabled)
                assertEquals(false, config.build.ios.enabled)

                // Deploy should be null or empty
                assertEquals(null, config.deploy?.android)
                assertEquals(null, config.deploy?.ios)
        }

        /**
         * Test multiple test groups
         */
        @Test
        fun testMultipleTestGroups() {
                val yaml = """
            version: "1.0"
            
            deploy:
              android:
                firebase:
                  enabled: true
                  service_account: creds.json
                  test_groups:
                    - qa-team
                    - beta-testers
                    - internal-users
        """.trimIndent()

                val parser = SnakeYamlConfigParser()
                val config = parser.parse(yaml)

                val groups = config.deploy?.android?.firebase?.testGroups ?: emptyList()
                assertEquals(3, groups.size)
                assertTrue(groups.contains("qa-team"))
                assertTrue(groups.contains("beta-testers"))
                assertTrue(groups.contains("internal-users"))
        }
}
