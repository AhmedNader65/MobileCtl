package com.mobilectl.commands.deploy

import com.mobilectl.config.Config

/**
 * Selects flavors based on CLI options
 */
class FlavorSelector {

    fun selectFlavors(config: Config, options: FlavorOptions): List<String> {
        val baseFlavors = when {
            options.allFlavors -> getAllFlavors(config)
            options.group != null -> getGroupFlavors(config, options.group)
            options.flavors != null -> getSpecificFlavors(options.flavors)
            else -> getDefaultFlavors(config)
        }

        // Apply exclusions
        val excludeSet = options.exclude?.split(",")?.map { it.trim() }?.toSet()
        return if (excludeSet != null) {
            baseFlavors.filter { it !in excludeSet }
        } else {
            baseFlavors
        }
    }

    private fun getAllFlavors(config: Config): List<String> {
        return config.build.android.flavors.ifEmpty {
            listOf(config.build.android.defaultFlavor)
        }
    }

    private fun getGroupFlavors(config: Config, group: String): List<String> {
        return config.deploy.flavorGroups[group]?.flavors ?: emptyList()
    }

    private fun getSpecificFlavors(flavors: String): List<String> {
        return flavors.split(",").map { it.trim() }
    }

    private fun getDefaultFlavors(config: Config): List<String> {
        return config.deploy.defaultGroup?.let { groupName ->
            config.deploy.flavorGroups[groupName]?.flavors
        } ?: listOf(config.build.android.defaultFlavor)
    }
}
