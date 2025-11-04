package com.mobilectl.model

enum class Platform {
    ANDROID,
    IOS;

    override fun toString(): String = when (this) {
        ANDROID -> "Android"
        IOS -> "iOS"
    }

    companion object {
        val ALL = setOf(ANDROID, IOS)
    }
}

enum class BuildStatus {
    SUCCESS,
    FAILURE,
    IN_PROGRESS,
    SKIPPED
}

data class BuildMetadata(
    val platform: Platform,
    val version: String,
    val buildTime: Long,
    val outputPath: String? = null
)
