package com.mobilectl.config

import com.mobilectl.detector.ProjectDetector

expect interface ConfigParser {
    /**
     * Parse YAML file into Config object
     */
    fun parse(yamlContent: String): Config

    /**
     * Serialize Config back to YAML
     */
    fun toYaml(config: Config): String
}

expect fun createConfigParser(): ConfigParser

class ConfigValidator(
    private val detector: ProjectDetector? = null
) {
    fun validate(config: Config): List<String> {
        val errors = mutableListOf<String>()

        // Get actual enabled platforms from detector if available
        val androidEnabled = config.build.android.enabled
            ?: (detector?.isAndroidProject() == true)
        val iosEnabled = config.build.ios.enabled
            ?: (detector?.isIosProject() == true)

        // Only validate what's actually enabled
        if (androidEnabled) {
            val androidConfig = config.build.android
            if (androidConfig.defaultType.isBlank() == true) {
                errors.add("Android default_type cannot be empty")
            }
        }

        if (iosEnabled) {
            val iosConfig = config.build.ios
            if (iosConfig.scheme.isBlank() == true) {
                errors.add("iOS scheme cannot be empty")
            }
        }

        return errors
    }
}

fun String.parseAsConfig(): Config {
    val parser = createConfigParser()
    return parser.parse(this)
}
