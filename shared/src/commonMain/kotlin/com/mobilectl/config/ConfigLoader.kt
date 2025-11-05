package com.mobilectl.config

import com.mobilectl.detector.ProjectDetector
import com.mobilectl.util.FileUtil
import java.io.File

class ConfigLoader(private val fileUtil: FileUtil,
                   private val detector: ProjectDetector? = null) {

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
            val validator = ConfigValidator(detector)
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

    fun updateVersionOnly(
        version: String,
        filePath: String = "mobileops.yml"
    ): Result<Unit> {
        return try {
            val file = File(filePath)

            // Read existing content
            val existingContent = if (file.exists()) {
                file.readText()
            } else {
                ""
            }

            // Update just the version field
            val updatedContent = if (existingContent.contains("version:")) {
                existingContent.replace(
                    """version:[\s\S]*?current:\s*["']?[^"'\n]+["']?""".toRegex(),
                    """version:
  current: "$version""""
                )
            } else {
                existingContent + "\nversion:\n  current: \"$version\"\n"
            }

            file.writeText(updatedContent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}

class ConfigValidationException(val errors: List<String>) : Exception(
    "Config validation failed:\n" + errors.joinToString("\n") { "  - $it" }
)
