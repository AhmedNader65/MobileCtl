package com.mobilectl.config

import com.mobilectl.model.buildConfig.AndroidBuildConfig
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.buildConfig.IosBuildConfig
import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertTrue(config.build.android.enabled)
    }

    @Test
    fun testParseFullConfig() {
        val yaml = """
            app:
              name: MyApp
              identifier: com.example.myapp
              version: "1.0.0"
            
            build:
              android:
                enabled: true
                gradle_task: "bundleRelease"
              ios:
                enabled: true
                scheme: "MyApp"
            
            deploy:
              destinations:
                - type: "local"
                  path: "./builds"
                - type: "firebase"
                  enabled: true
            
            notify:
              slack:
                enabled: true
                webhook_url: "https://hooks.slack.com/..."
        """.trimIndent()

        val parser = createConfigParser()
        val config = parser.parse(yaml)

        assertEquals("bundleRelease", config.build.android.gradleTask)
        assertEquals("MyApp", config.build.ios.scheme)
        assertEquals(2, config.deploy.destinations.size)
        assertTrue(config.notify.slack.enabled)
    }

    @Test
    fun testConfigValidation() {
        val config = Config(
            build = BuildConfig(
                android = AndroidBuildConfig(enabled = false),
                ios = IosBuildConfig(enabled = false)
            )
        )

        val validator = ConfigValidator()
        val errors = validator.validate(config)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("At least one platform") })
    }
}
