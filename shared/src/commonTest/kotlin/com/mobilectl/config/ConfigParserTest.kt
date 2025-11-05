package com.mobilectl.config

import com.mobilectl.model.buildConfig.AndroidBuildConfig
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.buildConfig.IosBuildConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigParserTest {

    @Test
    fun testParseMinimalConfig() {
        val yaml = """
            app:
              name: MyApp
              identifier: com.example.myapp
              version: "1.0.0"
            
            build:
              android:
                enabled: true
              ios:
                enabled: false
        """.trimIndent()

        val parser = createConfigParser()
        val config = parser.parse(yaml)

        assertEquals("MyApp", config.app.name)
        assertEquals("com.example.myapp", config.app.identifier)
        assertEquals("1.0.0", config.app.version)
        assertTrue(config.build.android.enabled == true)
    }

    @Test
    fun testParseFullConfig() {
        val yaml = """
        version:
          enabled: true
          current: "1.0.0"
          bumpStrategy: semver
          filesToUpdate:
            - pubspec.yaml
            - package.json
        
        build:
          android:
            enabled: true
            default_type: release
          ios:
            enabled: true
            scheme: MyApp
        
        changelog:
          enabled: true
          format: markdown
          output_file: CHANGELOG.md
          commit_types:
            - type: feat
              title: Features
              emoji: "✨"
            - type: fix
              title: Bug Fixes
              emoji: "🐛"
        
        deploy:
          enabled: true
          android:
            enabled: true
            destination: firebase
            appId: com.example.myapp
            token: FIREBASE_TOKEN
            artifactPath: build/outputs/apk/release/app-release.apk
          ios:
            enabled: true
            destination: testflight
            appId: com.example.myapp
            teamId: APPLE_TEAM_ID
            apiKey: APP_STORE_CONNECT_KEY
            artifactPath: build/outputs/app.ipa
    """.trimIndent()

        val parser = createConfigParser()
        val config = parser.parse(yaml)

        // Version config
        assertNotNull(config.version)
        assertEquals("1.0.0", config.version!!.current)
        assertEquals("semver", config.version!!.bumpStrategy)
        assertEquals(2, config.version!!.filesToUpdate.size)

        // Build config
        assertNotNull(config.build)
        assertTrue(config.build!!.android!!.enabled == true)
        assertEquals("release", config.build!!.android!!.defaultType)
        assertTrue(config.build!!.ios!!.enabled == true)
        assertEquals("MyApp", config.build!!.ios!!.scheme)

        // Changelog config
        assertNotNull(config.changelog)
        assertTrue(config.changelog!!.enabled)
        assertEquals("markdown", config.changelog!!.format)
        assertEquals("CHANGELOG.md", config.changelog!!.outputFile)
        assertEquals(2, config.changelog!!.commitTypes.size)

        // Deploy config
        assertNotNull(config.deploy)
        assertTrue(config.deploy!!.enabled)

        // Android deploy
        assertNotNull(config.deploy!!.android)
        assertTrue(config.deploy!!.android!!.enabled)
        assertEquals("firebase", config.deploy!!.android!!.destination)
        assertEquals("com.example.myapp", config.deploy!!.android!!.appId)
        assertEquals("build/outputs/apk/release/app-release.apk", config.deploy!!.android!!.artifactPath)

        // iOS deploy
        assertNotNull(config.deploy!!.ios)
        assertTrue(config.deploy!!.ios!!.enabled)
        assertEquals("testflight", config.deploy!!.ios!!.destination)
        assertEquals("com.example.myapp", config.deploy!!.ios!!.appId)
        assertEquals("build/outputs/app.ipa", config.deploy!!.ios!!.artifactPath)
    }

}
