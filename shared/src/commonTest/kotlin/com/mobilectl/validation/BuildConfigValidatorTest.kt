package com.mobilectl.validation

import com.mobilectl.config.Config
import com.mobilectl.detector.ProjectDetector
import com.mobilectl.model.Platform
import com.mobilectl.model.ValidationSeverity
import com.mobilectl.model.buildConfig.AndroidBuildConfig
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.buildConfig.IosBuildConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuildConfigValidatorTest {

    private val mockDetector = object : ProjectDetector {
        override fun detectPlatforms(
            androidEnabled: Boolean,
            iosEnabled: Boolean
        ): Set<Platform> {
            val platforms = mutableSetOf<Platform>()
            if (isAndroidProject()) {
                platforms.add(Platform.ANDROID)
            }
            if (isIosProject()) {
                platforms.add(Platform.IOS)
            }
            return platforms
        }

        override fun isAndroidProject(): Boolean = true
        override fun isIosProject(): Boolean = true
    }

    private val validator = BuildConfigValidator(mockDetector)

    @Test
    fun testValidBuildConfig() {
        val config = Config(
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    defaultType = "release"
                ),
                ios = IosBuildConfig(
                    enabled = true,
                    scheme = "Runner"
                )
            )
        )

        val errors = validator.validate(config)
        assertEquals(0, errors.size, "Valid config should have no errors")
    }

    @Test
    fun testAndroidDefaultTypeEmpty() {
        val config = Config(
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    defaultType = ""
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.field == "build.android.default_type" })
        assertTrue(errors.any { it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun testAndroidDefaultTypeBlank() {
        val config = Config(
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    defaultType = "   "
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.field == "build.android.default_type" })
    }

    @Test
    fun testIosSchemeEmpty() {
        val config = Config(
            build = BuildConfig(
                ios = IosBuildConfig(
                    enabled = true,
                    scheme = ""
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.field == "build.ios.scheme" })
        assertTrue(errors.any { it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun testAndroidDisabled() {
        val config = Config(
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = false,
                    defaultType = ""  // Empty but should not error
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(
            errors.none { it.field == "build.android.default_type" },
            "Should not validate disabled Android config"
        )
    }

    @Test
    fun testIosDisabled() {
        val config = Config(
            build = BuildConfig(
                ios = IosBuildConfig(
                    enabled = false,
                    scheme = ""  // Empty but should not error
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(
            errors.none { it.field == "build.ios.scheme" },
            "Should not validate disabled iOS config"
        )
    }

    @Test
    fun testBothPlatformsErrors() {
        val config = Config(
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    defaultType = ""
                ),
                ios = IosBuildConfig(
                    enabled = true,
                    scheme = ""
                )
            )
        )

        val errors = validator.validate(config)
        assertEquals(2, errors.size, "Should have 2 errors")
        assertTrue(errors.any { it.field == "build.android.default_type" })
        assertTrue(errors.any { it.field == "build.ios.scheme" })
    }

    @Test
    fun testAutoDetectAndroid() {
        val config = Config(
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = null,  // Auto-detect
                    defaultType = ""
                )
            )
        )

        val errors = validator.validate(config)
        // Should validate because detector says isAndroidProject = true
        assertTrue(errors.any { it.field == "build.android.default_type" })
    }

    @Test
    fun testSuggestionProvided() {
        val config = Config(
            build = BuildConfig(
                android = AndroidBuildConfig(
                    enabled = true,
                    defaultType = ""
                )
            )
        )

        val errors = validator.validate(config)
        val error = errors.first()
        assertTrue(error.suggestion != null, "Error should have suggestion")
        assertTrue(error.suggestion!!.contains("debug") || error.suggestion!!.contains("release"))
    }
}
