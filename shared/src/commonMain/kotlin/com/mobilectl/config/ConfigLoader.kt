package com.mobilectl.config

import com.mobilectl.util.FileUtil

class ConfigLoader(private val fileUtil: FileUtil) {

    /**
     * Load config from mobileops.yml file
     * Falls back to defaults if file not found
     */
    suspend fun loadConfig(filePath: String = "mobileops.yml"): Result<Config> {
        return try {
            if (!fileUtil.exists(filePath)) {
                // Return default config if file not found
                return Result.success(Config())
            }

            val yamlContent = fileUtil.readFile(filePath)
            val parser = createConfigParser()
            val config = parser.parse(yamlContent)

            // Validate config
            val validator = ConfigValidator()
            val errors = validator.validate(config)

            if (errors.isNotEmpty()) {
                return Result.failure(ConfigValidationException(errors))
            }

            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save config to file
     */
    suspend fun saveConfig(config: Config, filePath: String = "mobileops.yml"): Result<Unit> {
        return try {
            val parser = createConfigParser()
            val yamlContent = parser.toYaml(config)
            fileUtil.writeFile(filePath, yamlContent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ConfigValidationException(val errors: List<String>) : Exception(
    "Config validation failed:\n" + errors.joinToString("\n") { "  - $it" }
)
