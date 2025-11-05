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

fun String.parseAsConfig(): Config {
    val parser = createConfigParser()
    return parser.parse(this)
}
