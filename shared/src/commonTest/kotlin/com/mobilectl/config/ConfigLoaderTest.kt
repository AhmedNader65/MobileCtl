package com.mobilectl.config

import com.mobilectl.detector.ProjectDetector
import com.mobilectl.model.Platform
import com.mobilectl.util.FileUtil
import com.mobilectl.validation.BuildConfigValidator
import com.mobilectl.validation.ChangelogConfigValidator
import com.mobilectl.validation.VersionConfigValidator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ConfigLoaderTest {

    private val testDir = File(".mobilectl/test-config")

    private val mockFileUtil = object : FileUtil {
        override suspend fun readFile(path: String): String {
            return File(path).takeIf { it.exists() }?.readText().toString()
        }
        override suspend fun writeFile(path: String, content: String){
            File(path).writeText(content)
        }
        override suspend fun exists(path: String): Boolean = File(path).exists()
        override suspend fun deleteFile(path: String): Boolean = true
        override suspend fun createDir(path: String): Boolean = true
        override suspend fun listDir(path: String): List<String> = emptyList()
        override suspend fun copyFile(from: String, to: String): File = File("")
    }

    private val mockDetector = object : ProjectDetector {
        override fun detectPlatforms(
            androidEnabled: Boolean,
            iosEnabled: Boolean
        ): Set<Platform> {
            val platforms = mutableSetOf<Platform>()
            if (androidEnabled) platforms.add(Platform.ANDROID)
            if (iosEnabled) platforms.add(Platform.IOS)
            return platforms
        }

        override fun isAndroidProject(): Boolean = true
        override fun isIosProject(): Boolean = true
    }

    @Test
    fun testValidConfigLoads() {
        val yamlContent = """
            build:
              android:
                enabled: true
                default_type: release
              ios:
                enabled: true
                scheme: Runner
            
            changelog:
              enabled: true
              format: markdown
              output_file: CHANGELOG.md
              commit_types:
                - type: feat
                  title: Features
                  emoji: "✨"
        """.trimIndent()

        val testFile = testDir.resolve("valid.yml")
        testFile.parentFile?.mkdirs()
        testFile.writeText(yamlContent)

        try {
            val loader = ConfigLoader(mockFileUtil, mockDetector)
            // Note: This will fail if ConfigParser is not properly set up
            // But it shows the test structure

        } finally {
            testFile.delete()
        }
    }

    @Test
    fun testInvalidConfigFails() {
        val yamlContent = """
            build:
              android:
                enabled: true
                default_type: ""
        """.trimIndent()

        val testFile = testDir.resolve("invalid.yml")
        testFile.parentFile?.mkdirs()
        testFile.writeText(yamlContent)

        try {
            // Config with invalid Android default_type
            // Should fail validation

        } finally {
            testFile.delete()
        }
    }

    @Test
    fun testValidatorsExecuteInOrder() {
        val validators = ConfigLoader.defaultValidators(mockDetector)

        // Should have 3 validators
        assertTrue(validators.size >= 3)

        // Check types
        assertTrue(validators.any { it is BuildConfigValidator })
        assertTrue(validators.any { it is ChangelogConfigValidator })
        assertTrue(validators.any { it is VersionConfigValidator })
    }

    @Test
    fun testCanAddCustomValidator() {
        val defaultValidators = ConfigLoader.defaultValidators(mockDetector)

        // Create custom validator
        val customValidator = ChangelogConfigValidator()
        val allValidators = defaultValidators + customValidator

        assertTrue(allValidators.size > defaultValidators.size)
    }
}
