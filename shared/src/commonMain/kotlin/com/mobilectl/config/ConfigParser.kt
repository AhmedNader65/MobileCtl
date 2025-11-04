package com.mobilectl.config

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

class ConfigValidator {
    fun validate(config: Config): List<String> {
        val errors = mutableListOf<String>()

        // Check if at least one platform is enabled
        if (!config.build.android.enabled && !config.build.ios.enabled) {
            errors.add("At least one platform (android or ios) must be enabled")
        }

        // Check deploy destinations
        if (config.deploy.destinations.isEmpty()) {
            errors.add("At least one deploy destination must be configured")
        }

        // Validate Android config
        if (config.build.android.enabled) {
            if (config.build.android.gradleTask.isBlank()) {
                errors.add("Android gradle_task cannot be empty")
            }
        }

        // Validate iOS config
        if (config.build.ios.enabled) {
            if (config.build.ios.scheme.isBlank()) {
                errors.add("iOS scheme cannot be empty (if iOS is enabled)")
            }
        }

        return errors
    }
}

fun String.parseAsConfig(): Config {
    val parser = createConfigParser()
    return parser.parse(this)
}
