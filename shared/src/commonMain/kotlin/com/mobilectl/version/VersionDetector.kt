package com.mobilectl.version

interface VersionDetector {
    fun detectVersionFromApp(baseDir: String): SemanticVersion?
}