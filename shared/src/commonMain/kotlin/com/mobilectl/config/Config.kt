package com.mobilectl.config

import com.mobilectl.model.appMetadata.AppConfig
import com.mobilectl.model.buildConfig.BuildConfig
import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.deploy.DeployConfig
import com.mobilectl.model.notifications.NotifyConfig
import com.mobilectl.model.versionManagement.VersionConfig
import com.mobilectl.model.report.ReportConfig
import kotlinx.serialization.Serializable


@Serializable
data class Config(
    val app: AppConfig = AppConfig(),
    val build: BuildConfig = BuildConfig(),
    val version: VersionConfig = VersionConfig(),
    val changelog: ChangelogConfig = ChangelogConfig(),
    val deploy: DeployConfig = DeployConfig(),
    val notify: NotifyConfig = NotifyConfig(),
    val report: ReportConfig = ReportConfig(),
    val env: Map<String, String> = emptyMap()
)




