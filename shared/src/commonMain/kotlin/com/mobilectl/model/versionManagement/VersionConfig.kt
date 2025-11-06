package com.mobilectl.model.versionManagement

import kotlinx.serialization.Serializable

@Serializable
data class VersionConfig(
    val enabled: Boolean = true,
    val current: String = "1.0.0",
    val autoIncrement: Boolean = false,
    val bumpStrategy: String = "patch",
    val filesToUpdate: List<String> = emptyList()
)

/**
 * Valid bump strategies:
 * - "patch"   → x.y.z → x.y.(z+1)
 * - "minor"   → x.y.z → x.(y+1).0
 * - "major"   → x.y.z → (x+1).0.0
 * - "auto"    → Detect from conventional commits
 * - "manual"  → Don't auto-bump (require explicit)
 */
enum class VersionBumpStrategy {
    PATCH,
    MINOR,
    MAJOR,
    AUTO,
    MANUAL
}
