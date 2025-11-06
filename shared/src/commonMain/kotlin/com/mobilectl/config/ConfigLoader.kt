package com.mobilectl.config

import com.mobilectl.detector.ProjectDetector
import com.mobilectl.model.ValidationError
import com.mobilectl.model.hasErrors
import com.mobilectl.model.printReport
import com.mobilectl.model.warnings
import com.mobilectl.util.FileUtil
import com.mobilectl.validation.BuildConfigValidator
import com.mobilectl.validation.ChangelogConfigValidator
import com.mobilectl.validation.ComponentValidator
import com.mobilectl.validation.VersionConfigValidator
import java.io.File

class ConfigLoader(
    private val fileUtil: FileUtil,
    private val projectDetector: ProjectDetector,
    private val validators: List<ComponentValidator> = defaultValidators(projectDetector)
) {

    suspend fun loadConfig(configPath: String?): Result<Config> {
        return try {
            val actualConfigPath = configPath ?: findDefaultConfigPath()

            if (!File(actualConfigPath).exists()) {
                return Result.failure(
                    Exception("Config file not found: $actualConfigPath")
                )
            }

            val dim = "\u001B[2m"
            val reset = "\u001B[0m"
            println("  ${dim}Loading config from: $actualConfigPath$reset")

            val yamlContent = fileUtil.readFile(actualConfigPath)

            val parser = createConfigParser()
            val rawConfig = try {
                parser.parse(yamlContent)
            } catch (e: Exception) {
                return Result.failure(Exception("Failed to parse config YAML: ${e.message}"))
            }

            val errors = validateConfig(rawConfig)

            if (errors.hasErrors()) {
                errors.printReport()
                return Result.failure(
                    IllegalArgumentException("Configuration validation failed")
                )
            }

            val warnings = errors.warnings()
            if (warnings.isNotEmpty()) {
                println()
                warnings.printReport()
                println()
            }

            Result.success(rawConfig)

        } catch (e: Exception) {
            Result.failure(Exception("Failed to load config: ${e.message}", e))
        }
    }

    // ✅ Now this is simple and extensible!
    private fun validateConfig(config: Config): List<ValidationError> {
        return validators.flatMap { validator ->
            validator.validate(config)
        }
    }

    private fun findDefaultConfigPath(): String {
        val possiblePaths = listOf(
            "mobileops.yaml",
            "mobileops.yaml",
            ".mobilectl/mobileops.yaml",
            ".mobilectl/mobileops.yaml"
        )
        return possiblePaths.find { File(it).exists() } ?: "mobileops.yaml"
    }

    companion object {
        fun defaultValidators(projectDetector: ProjectDetector): List<ComponentValidator> {
            return listOf(
                BuildConfigValidator(projectDetector),
                ChangelogConfigValidator(),
                VersionConfigValidator()
            )
        }
    }
}
