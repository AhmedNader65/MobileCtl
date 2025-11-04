package com.mobilectl.model.report

import kotlinx.serialization.Serializable

@Serializable
data class ReportConfig(
    val enabled: Boolean = false,
    val format: String = "html",
    val include: List<String> = listOf("build_info", "git_info", "build_duration"),
    val outputPath: String = "./build-reports"
)