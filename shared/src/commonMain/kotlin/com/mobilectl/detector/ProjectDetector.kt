package com.mobilectl.detector

import com.mobilectl.model.Platform

/**
 * Detects which platforms are available in the project
 */
interface ProjectDetector {
    fun detectPlatforms(
        baseDir: String, androidEnabled: Boolean, iosEnabled: Boolean): Set<Platform>
    fun isAndroidProject(baseDir: String): Boolean
    fun isIosProject(baseDir: String): Boolean
}