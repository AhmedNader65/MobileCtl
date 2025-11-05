package com.mobilectl.config

import com.mobilectl.model.appMetadata.AppConfig
import com.mobilectl.model.buildConfig.AndroidBuildConfig
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.buildConfig.IosBuildConfig
import com.mobilectl.model.buildConfig.KeystoreConfig
import com.mobilectl.model.buildConfig.OutputConfig
import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.changelog.CommitType
import com.mobilectl.model.changelog.getDefaultCommitTypes
import com.mobilectl.model.deploy.DeployConfig
import com.mobilectl.model.deploy.DeploymentDestination
import com.mobilectl.model.notifications.EmailNotifyConfig
import com.mobilectl.model.notifications.NotifyConfig
import com.mobilectl.model.notifications.SlackNotifyConfig
import com.mobilectl.model.notifications.WebhookNotifyConfig
import com.mobilectl.model.report.ReportConfig
import com.mobilectl.model.versionManagement.VersionConfig
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.representer.Representer
import kotlin.reflect.KClass

actual fun createConfigParser(): ConfigParser = SnakeYamlConfigParser()

actual interface ConfigParser {
    actual fun parse(yamlContent: String): Config
    actual fun toYaml(config: Config): String
}

class SnakeYamlConfigParser : ConfigParser {

    private val yaml = Yaml();


    override fun parse(yamlContent: String): Config {
        return try {
            // Substitute environment variables in YAML
            val processedContent = substituteEnvironmentVariables(yamlContent)

            // Parse YAML
            val rawConfig: Map<String, Any?>? = yaml.load(processedContent)

            // Convert to Config object
            rawConfig?.let { convertToConfig(it) } ?: Config()
        } catch (e: Exception) {
            throw ConfigParseException("Failed to parse YAML: ${e.message}", e)
        }
    }

    /**
     * Convert raw YAML map to Config data class
     */
    private fun convertToConfig(data: Map<String, Any?>): Config {
        return Config(
            app = convertToAppConfig(data["app"] as? Map<String, Any?> ?: emptyMap()),
            build = convertToBuildConfig(data["build"] as? Map<String, Any?> ?: emptyMap()),
            version = convertToVersionConfig(data["version"] as? Map<String, Any?> ?: emptyMap()),
            changelog = convertToChangelogConfig(data["changelog"] as? Map<String, Any?> ?: emptyMap()),
            deploy = convertToDeployConfig(data["deploy"] as? Map<String, Any?> ?: emptyMap()),
            notify = convertToNotifyConfig(data["notify"] as? Map<String, Any?> ?: emptyMap()),
            report = convertToReportConfig(data["report"] as? Map<String, Any?> ?: emptyMap()),
            env = data["env"] as? Map<String, String> ?: emptyMap()
        )
    }

    private fun convertToAppConfig(data: Map<String, Any?>): AppConfig {
        return AppConfig(
            name = data["name"] as? String,
            identifier = data["identifier"] as? String,
            version = data["version"] as? String
        )
    }

    private fun convertToBuildConfig(data: Map<String, Any?>): BuildConfig {
        return BuildConfig(
            android = convertToAndroidConfig(data["android"] as? Map<String, Any?> ?: emptyMap()),
            ios = convertToIosConfig(data["ios"] as? Map<String, Any?> ?: emptyMap())
        )
    }

    private fun convertToAndroidConfig(data: Map<String, Any?>): AndroidBuildConfig {
        return AndroidBuildConfig(
            enabled = data["enabled"] as? Boolean,
            projectPath = data["project_path"] as? String ?: ".",
            defaultFlavor = data["default_flavor"] as? String ?: "",
            defaultType = data["default_type"] as? String ?: "release",
            gradleProperties = (data["gradle_properties"] as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value.toString() } ?: emptyMap(),
            keystore = convertToKeystoreConfig(data["keystore"] as? Map<String, Any?>),
            output = convertToOutputConfig(data["output"] as? Map<String, Any?> ?: emptyMap(), "apk", "app-release.apk")
        )
    }

    private fun convertToIosConfig(data: Map<String, Any?>): IosBuildConfig {
        return IosBuildConfig(
            enabled = data["enabled"] as? Boolean,
            projectPath = data["project_path"] as? String ?: ".",
            scheme = data["scheme"] as? String ?: "",
            configuration = data["configuration"] as? String ?: "Release",
            destination = data["destination"] as? String ?: "generic/platform=iOS",
            codeSignIdentity = data["code_sign_identity"] as? String,
            provisioningProfile = data["provisioning_profile"] as? String,
            output = convertToOutputConfig(data["output"] as? Map<String, Any?> ?: emptyMap(), "ipa", "app-release.ipa")
        )
    }

    private fun convertToKeystoreConfig(data: Map<String, Any?>?): KeystoreConfig? {
        return data?.let {
            KeystoreConfig(
                path = it["path"] as? String ?: "",
                alias = it["alias"] as? String ?: "",
                storePassword = it["store_password"] as? String ?: "",
                keyPassword = it["key_password"] as? String ?: ""
            )
        }
    }

    private fun convertToOutputConfig(data: Map<String, Any?>, defaultFormat: String, defaultName: String): OutputConfig {
        return OutputConfig(
            format = data["format"] as? String ?: defaultFormat,
            name = data["name"] as? String ?: defaultName
        )
    }

    private fun convertToVersionConfig(data: Map<String, Any?>): VersionConfig {
        return VersionConfig(
            current = data["current"] as? String ?: "1.0.0",
            autoIncrement = data["auto_increment"] as? Boolean ?: false,
            bumpStrategy = data["bump_strategy"] as? String ?: "semver",
            filesToUpdate = (data["files_to_update"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        )
    }

    private fun convertToChangelogConfig(data: Map<String, Any?>): ChangelogConfig {
        val commitTypesList = (data["commit_types"] as? List<*>)?.mapNotNull { item ->
            (item as? Map<String, Any?>)?.let {
                CommitType(
                    type = it["type"] as? String ?: "",
                    title = it["title"] as? String ?: "",
                    emoji = it["emoji"] as? String ?: ""
                )
            }
        } ?: getDefaultCommitTypes()

        return ChangelogConfig(
            enabled = data["enabled"] as? Boolean ?: true,
            format = data["format"] as? String ?: "markdown",
            includeConventionalCommits = data["include_conventional_commits"] as? Boolean ?: true,
            commitTypes = commitTypesList,
            outputFile = data["output_file"] as? String ?: "CHANGELOG.md"
        )
    }

    private fun convertToDeployConfig(data: Map<String, Any?>): DeployConfig {
        val destinations = (data["destinations"] as? List<*>)?.mapNotNull { item ->
            (item as? Map<String, Any?>)?.let {
                DeploymentDestination(
                    type = it["type"] as? String ?: "",
                    config = (it["config"] as? Map<*, *>)?.mapKeys { (k, _) -> k.toString() }?.mapValues { (_, v) -> v.toString() } ?: (it as? Map<String, Any?>)?.filterKeys { k -> k != "type" && k != "enabled" }?.mapValues { (_, v) -> v.toString() } ?: emptyMap(),
                    enabled = it["enabled"] as? Boolean ?: true
                )
            }
        } ?: listOf(DeploymentDestination(type = "local", config = mapOf("path" to "./builds")))

        return DeployConfig(destinations = destinations)
    }

    private fun convertToNotifyConfig(data: Map<String, Any?>): NotifyConfig {
        return NotifyConfig(
            slack = convertToSlackNotifyConfig(data["slack"] as? Map<String, Any?> ?: emptyMap()),
            email = convertToEmailNotifyConfig(data["email"] as? Map<String, Any?> ?: emptyMap()),
            webhook = convertToWebhookNotifyConfig(data["webhook"] as? Map<String, Any?> ?: emptyMap())
        )
    }

    private fun convertToSlackNotifyConfig(data: Map<String, Any?>): SlackNotifyConfig {
        return SlackNotifyConfig(
            enabled = data["enabled"] as? Boolean ?: false,
            webhookUrl = data["webhook_url"] as? String ?: "",
            channel = data["channel"] as? String,
            notifyOn = (data["notify_on"] as? List<*>)?.mapNotNull { it as? String } ?: listOf("success", "failure")
        )
    }

    private fun convertToEmailNotifyConfig(data: Map<String, Any?>): EmailNotifyConfig {
        return EmailNotifyConfig(
            enabled = data["enabled"] as? Boolean ?: false,
            recipients = (data["recipients"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            notifyOn = (data["notify_on"] as? List<*>)?.mapNotNull { it as? String } ?: listOf("failure")
        )
    }

    private fun convertToWebhookNotifyConfig(data: Map<String, Any?>): WebhookNotifyConfig {
        return WebhookNotifyConfig(
            enabled = data["enabled"] as? Boolean ?: false,
            url = data["url"] as? String ?: "",
            events = (data["events"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        )
    }

    private fun convertToReportConfig(data: Map<String, Any?>): ReportConfig {
        return ReportConfig(
            enabled = data["enabled"] as? Boolean ?: false,
            format = data["format"] as? String ?: "html",
            include = (data["include"] as? List<*>)?.mapNotNull { it as? String } ?: listOf("build_info", "git_info", "build_duration"),
            outputPath = data["output_path"] as? String ?: "./build-reports"
        )
    }

    override fun toYaml(config: Config): String {
        return yaml.dump(config)
    }

    /**
     * Replace ${VAR_NAME} with environment variable values
     */
    private fun substituteEnvironmentVariables(content: String): String {
        val pattern = Regex("""\$\{([^}]+)\}""")
        return pattern.replace(content) { matchResult ->
            val varName = matchResult.groupValues[1]
            System.getenv(varName) ?: matchResult.value  // Keep original if not found
        }
    }
}

class ConfigParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
