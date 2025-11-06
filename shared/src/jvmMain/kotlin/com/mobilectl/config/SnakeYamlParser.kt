package com.mobilectl.config

import com.mobilectl.model.appMetadata.AppConfig
import com.mobilectl.model.buildConfig.AndroidBuildConfig
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.buildConfig.IosBuildConfig
import com.mobilectl.model.buildConfig.KeystoreConfig
import com.mobilectl.model.buildConfig.OutputConfig
import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.changelog.CommitType
import com.mobilectl.model.changelog.ReleaseNotes
import com.mobilectl.model.changelog.getDefaultCommitTypes
import com.mobilectl.model.deploy.AndroidDeployConfig
import com.mobilectl.model.deploy.AppStoreDestination
import com.mobilectl.model.deploy.DeployConfig
import com.mobilectl.model.deploy.DeploymentDestination
import com.mobilectl.model.deploy.FirebaseAndroidDestination
import com.mobilectl.model.deploy.IosDeployConfig
import com.mobilectl.model.deploy.LocalAndroidDestination
import com.mobilectl.model.deploy.PlayConsoleAndroidDestination
import com.mobilectl.model.deploy.TestFlightDestination
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
            changelog = convertToChangelogConfig(
                data["changelog"] as? Map<String, Any?> ?: emptyMap()
            ),
            deploy = convertToDeployConfig(data["deploy"] as? Map<String, Any?>),
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
        if (data == null) return AndroidBuildConfig()

        return AndroidBuildConfig(
            enabled = (data["enabled"] as? Boolean) ?: true,
            defaultFlavor = (data["default_flavor"] as? String)
                ?: (data["defaultFlavor"] as? String)
                ?: "release",
            defaultType = (data["default_type"] as? String)
                ?: (data["defaultType"] as? String)
                ?: "release",

            // Signing config
            keyStore = (data["key_store"] as? String)
                ?: (data["keyStore"] as? String)
                ?: System.getenv("MOBILECTL_KEYSTORE") ?: "keystore.jks",
            keyAlias = (data["key_alias"] as? String)
                ?: (data["keyAlias"] as? String)
                ?: System.getenv("MOBILECTL_KEY_ALIAS") ?: "",
            keyPassword = (data["key_password"] as? String)
                ?: (data["keyPassword"] as? String)
                ?: System.getenv("MOBILECTL_KEY_PASSWORD") ?: "",
            storePassword = (data["store_password"] as? String)
                ?: (data["storePassword"] as? String)
                ?: System.getenv("MOBILECTL_STORE_PASSWORD") ?: "",

            useEnvForPasswords = (data["use_env_for_passwords"] as? Boolean)
                ?: (data["useEnvForPasswords"] as? Boolean)
                ?: true
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
            output = convertToOutputConfig(
                data["output"] as? Map<String, Any?> ?: emptyMap(),
                "ipa",
                "app-release.ipa"
            )
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

    private fun convertToOutputConfig(
        data: Map<String, Any?>,
        defaultFormat: String,
        defaultName: String
    ): OutputConfig {
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
            filesToUpdate = (data["files_to_update"] as? List<*>)?.mapNotNull { it as? String }
                ?: emptyList()
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

        // Parse releases map
        val releasesMap = mutableMapOf<String, ReleaseNotes>()
        (data["releases"] as? Map<String, Any?>)?.forEach { (version, releaseData) ->
            if (releaseData is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val releaseMap = releaseData as Map<String, Any?>

                val highlights = releaseMap["highlights"] as? String
                val breakingChanges = (releaseMap["breaking_changes"] as? List<*>)
                    ?.mapNotNull { it as? String } ?: emptyList()
                val contributors = (releaseMap["contributors"] as? List<*>)
                    ?.mapNotNull { it as? String } ?: emptyList()

                releasesMap[version] = ReleaseNotes(
                    highlights = highlights,
                    breaking_changes = breakingChanges,
                    contributors = contributors
                )
            }
        }

        return ChangelogConfig(
            enabled = data["enabled"] as? Boolean ?: true,
            format = data["format"] as? String ?: "markdown",
            outputFile = data["output_file"] as? String ?: "CHANGELOG.md",
            includeBreakingChanges = data["include_breaking_changes"] as? Boolean ?: true,
            includeContributors = data["include_contributors"] as? Boolean ?: true,
            includeStats = data["include_stats"] as? Boolean ?: true,
            includeCompareLinks = data["include_compare_links"] as? Boolean ?: true,
            groupByVersion = data["group_by_version"] as? Boolean ?: true,
            releases = releasesMap,
            commitTypes = commitTypesList
        )
    }

    /**
     * Convert raw YAML data to DeployConfig
     */
    fun convertToDeployConfig(data: Map<String, Any?>?): DeployConfig {
        if (data == null) return DeployConfig()

        val android = convertToAndroidDeployConfig(data["android"] as? Map<String, Any?>)
        val ios = convertToIosDeployConfig(data["ios"] as? Map<String, Any?>)

        // If neither android nor ios, return empty config
        if (android == null && ios == null) {
            return DeployConfig()
        }

        return DeployConfig(
            android = android,
            ios = ios
        )
    }

    /**
     * Convert raw YAML data to AndroidDeployConfig
     */
    private fun convertToAndroidDeployConfig(data: Map<String, Any?>?): AndroidDeployConfig? {
        if (data == null) return null

        return AndroidDeployConfig(
            enabled = (data["enabled"] as? Boolean) ?: true,
            artifactPath = (data["artifact_path"] as? String)
                ?: (data["artifactPath"] as? String)
                ?: "build/outputs/apk/release/app-release.apk",

            firebase = convertToFirebaseAndroidDestination(data["firebase"] as? Map<String, Any?>),
            playConsole = convertToPlayConsoleAndroidDestination(data["play_console"] as? Map<String, Any?>),
            local = convertToLocalAndroidDestination(data["local"] as? Map<String, Any?>)
        )
    }

    /**
     * Convert raw YAML data to Firebase Android destination config
     */
    private fun convertToFirebaseAndroidDestination(
        data: Map<String, Any?>?
    ): FirebaseAndroidDestination {
        if (data == null) {
            return FirebaseAndroidDestination()
        }

        @Suppress("UNCHECKED_CAST")
        val testGroups = when (val tg = data["test_groups"] ?: data["testGroups"]) {
            is List<*> -> tg.mapNotNull { it?.toString() }
            is String -> listOf(tg)
            else -> listOf("qa-team")
        }

        return FirebaseAndroidDestination(
            enabled = (data["enabled"] as? Boolean) ?: true,
            serviceAccount = (data["service_account"] as? String)
                ?: (data["serviceAccount"] as? String)
                ?: "credentials/firebase-service-account.json",
            googleServices = (data["google_services"] as? String)
                ?: (data["googleServices"] as? String),
            testGroups = testGroups,
        )
    }

    /**
     * Convert raw YAML data to Play Console Android destination config
     */
    private fun convertToPlayConsoleAndroidDestination(
        data: Map<String, Any?>?
    ): PlayConsoleAndroidDestination {
        if (data == null) {
            return PlayConsoleAndroidDestination()
        }

        return PlayConsoleAndroidDestination(
            enabled = (data["enabled"] as? Boolean) ?: false,
            serviceAccount = (data["service_account"] as? String)
                ?: (data["serviceAccount"] as? String)
                ?: "credentials/play-console-service-account.json",
            packageName = (data["package_name"] as? String)
                ?: (data["packageName"] as? String)
                ?: ""
        )
    }

    /**
     * Convert raw YAML data to Local Android destination config
     */
    private fun convertToLocalAndroidDestination(
        data: Map<String, Any?>?
    ): LocalAndroidDestination {
        if (data == null) {
            return LocalAndroidDestination()
        }

        return LocalAndroidDestination(
            enabled = (data["enabled"] as? Boolean) ?: false,
            outputDir = (data["output_dir"] as? String)
                ?: (data["outputDir"] as? String)
                ?: "build/deploy"
        )
    }

    /**
     * Convert raw YAML data to IosDeployConfig
     */
    private fun convertToIosDeployConfig(data: Map<String, Any?>?): IosDeployConfig? {
        if (data == null) return null

        return IosDeployConfig(
            enabled = (data["enabled"] as? Boolean) ?: true,
            artifactPath = (data["artifact_path"] as? String)
                ?: (data["artifactPath"] as? String)
                ?: "build/outputs/ipa/release/app.ipa",

            testflight = convertToTestFlightDestination(data["testflight"] as? Map<String, Any?>),
            appStore = convertToAppStoreDestination(data["app_store"] as? Map<String, Any?>)
        )
    }

    /**
     * Convert raw YAML data to TestFlight iOS destination config
     */
    private fun convertToTestFlightDestination(
        data: Map<String, Any?>?
    ): TestFlightDestination {
        if (data == null) {
            return TestFlightDestination()
        }

        return TestFlightDestination(
            enabled = (data["enabled"] as? Boolean) ?: true,
            apiKeyPath = (data["api_key_path"] as? String)
                ?: (data["apiKeyPath"] as? String)
                ?: "credentials/app-store-connect-api-key.json",
            bundleId = (data["bundle_id"] as? String)
                ?: (data["bundleId"] as? String)
                ?: "",
            teamId = (data["team_id"] as? String)
                ?: (data["teamId"] as? String)
                ?: ""
        )
    }

    /**
     * Convert raw YAML data to App Store iOS destination config
     */
    private fun convertToAppStoreDestination(
        data: Map<String, Any?>?
    ): AppStoreDestination {
        if (data == null) {
            return AppStoreDestination()
        }

        return AppStoreDestination(
            enabled = (data["enabled"] as? Boolean) ?: false,
            apiKeyPath = (data["api_key_path"] as? String)
                ?: (data["apiKeyPath"] as? String)
                ?: "credentials/app-store-connect-api-key.json",
            bundleId = (data["bundle_id"] as? String)
                ?: (data["bundleId"] as? String)
                ?: "",
            teamId = (data["team_id"] as? String)
                ?: (data["teamId"] as? String)
                ?: ""
        )
    }
    private fun convertToNotifyConfig(data: Map<String, Any?>): NotifyConfig {
        return NotifyConfig(
            slack = convertToSlackNotifyConfig(data["slack"] as? Map<String, Any?> ?: emptyMap()),
            email = convertToEmailNotifyConfig(data["email"] as? Map<String, Any?> ?: emptyMap()),
            webhook = convertToWebhookNotifyConfig(
                data["webhook"] as? Map<String, Any?> ?: emptyMap()
            )
        )
    }

    private fun convertToSlackNotifyConfig(data: Map<String, Any?>): SlackNotifyConfig {
        return SlackNotifyConfig(
            enabled = data["enabled"] as? Boolean ?: false,
            webhookUrl = data["webhook_url"] as? String ?: "",
            channel = data["channel"] as? String,
            notifyOn = (data["notify_on"] as? List<*>)?.mapNotNull { it as? String }
                ?: listOf("success", "failure")
        )
    }

    private fun convertToEmailNotifyConfig(data: Map<String, Any?>): EmailNotifyConfig {
        return EmailNotifyConfig(
            enabled = data["enabled"] as? Boolean ?: false,
            recipients = (data["recipients"] as? List<*>)?.mapNotNull { it as? String }
                ?: emptyList(),
            notifyOn = (data["notify_on"] as? List<*>)?.mapNotNull { it as? String }
                ?: listOf("failure")
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
            include = (data["include"] as? List<*>)?.mapNotNull { it as? String }
                ?: listOf("build_info", "git_info", "build_duration"),
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
