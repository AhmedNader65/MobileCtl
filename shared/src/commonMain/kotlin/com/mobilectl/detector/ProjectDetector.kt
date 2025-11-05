package com.mobilectl.detector

import com.mobilectl.model.Platform

/**
 * Detects which platforms are available in the project
 */
interface ProjectDetector {
    fun detectPlatforms(androidEnabled: Boolean, iosEnabled: Boolean): Set<Platform>
    fun isAndroidProject(): Boolean
    fun isIosProject(): Boolean
}