package com.mobilectl.commands.deploy

import com.mobilectl.config.Config
import com.mobilectl.model.Platform

/**
 * Selects platforms based on CLI options and config
 */
class PlatformSelector {

    fun parsePlatforms(platformArg: String?, config: Config): Set<Platform>? {
        return when {
            platformArg != null -> parsePlatformArg(platformArg)
            else -> autoDetectFromConfig(config)
        }
    }

    private fun parsePlatformArg(arg: String): Set<Platform>? {
        return when (arg) {
            "all" -> setOf(Platform.ANDROID, Platform.IOS)
            "android" -> setOf(Platform.ANDROID)
            "ios" -> setOf(Platform.IOS)
            else -> null
        }
    }

    private fun autoDetectFromConfig(config: Config): Set<Platform>? {
        val detected = mutableSetOf<Platform>()
        if (config.deploy?.android?.enabled == true) {
            detected.add(Platform.ANDROID)
        }
        if (config.deploy?.ios?.enabled == true) {
            detected.add(Platform.IOS)
        }

        return if (detected.isEmpty()) null else detected
    }
}
