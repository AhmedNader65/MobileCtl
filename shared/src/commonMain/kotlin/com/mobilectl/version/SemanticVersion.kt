package com.mobilectl.version

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) {
    override fun toString(): String = "$major.$minor.$patch"

    fun bump(level: String): SemanticVersion {
        return when (level.lowercase()) {
            "major" -> copy(major = major + 1, minor = 0, patch = 0)
            "minor" -> copy(minor = minor + 1, patch = 0)
            "patch" -> copy(patch = patch + 1)
            else -> this
        }
    }

    companion object {
        fun parse(version: String): SemanticVersion {
            val parts = version.split(".")
            return SemanticVersion(
                major = parts.getOrNull(0)?.toIntOrNull() ?: 0,
                minor = parts.getOrNull(1)?.toIntOrNull() ?: 0,
                patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            )
        }
    }
}
