package com.mobilectl.commands.deploy

import com.mobilectl.model.Platform
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

/**
 * Interactive wizard for deployment configuration
 * Guides users through platform and destination selection
 */
class InteractiveDeploymentWizard(
    private val out: PrintWriter = PrintWriter(System.out, true, StandardCharsets.UTF_8)
) {

    /**
    * Prompt user to select platform(s)
    */
    fun selectPlatforms(availablePlatforms: Set<Platform>): Set<Platform>? {
        out.println()
        out.println("📱 Select platform(s) to deploy:")
        out.println()

        val platforms = availablePlatforms.sorted()
        platforms.forEachIndexed { index, platform ->
            out.println("  [${index + 1}] ${platform.name}")
        }
        out.println("  [${platforms.size + 1}] Cancel")
        out.println()

        out.print("? Select (1-${platforms.size + 1}): ")
        out.flush()

        val input = readLine()?.trim()
        val choice = input?.toIntOrNull()

        return when {
            choice == null -> setOf(platforms.first())  // Default: first option
            choice < 1 || choice > platforms.size -> null  // Invalid
            choice == platforms.size + 1 -> null  // Cancel
            else -> setOf(platforms[choice - 1])  // Valid choice
        }
    }

    /**
     * Prompt user to select deployment destination(s)
     */
    /**
     * Prompt user to select deployment destination(s)
     */
    fun selectDestinations(
        platform: Platform,
        availableDestinations: List<String>
    ): List<String> {
        out.println()
        out.println("📍 Select deployment destination(s) for ${platform.name}:")
        out.println()

        availableDestinations.forEachIndexed { index, dest ->
            val emoji = getDestinationEmoji(dest)
            out.println("  [$emoji] ${index + 1}. $dest")  // Show with numbers
        }
        out.println()

        out.print("? Enter numbers (1-${availableDestinations.size}, comma-separated, or Enter for all): ")
        out.flush()

        val input = readLine()?.trim()

        return when {
            input.isNullOrBlank() -> {
                // Default: all destinations
                availableDestinations
            }
            else -> {
                // Parse numbers like "1,2" or "1, 3"
                val selected = input.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }  // Convert to numbers
                    .filter { it in 1..availableDestinations.size }  // Validate range
                    .map { availableDestinations[it - 1] }  // Get actual destination names

                if (selected.isNotEmpty()) {
                    out.println("   ✓ Selected: ${selected.joinToString(", ")}")
                    selected
                } else {
                    out.println("   ⚠️  No valid selections, using all destinations")
                    availableDestinations
                }
            }
        }
    }


    /**
     * Prompt for release notes
     */
    fun getReleaseNotes(currentNotes: String? = null): String? {
        out.println()
        if (currentNotes != null) {
            out.println("📝 Release notes (current: \"$currentNotes\"):")
        } else {
            out.println("📝 Release notes (optional, press Enter to skip):")
        }
        out.print("> ")
        out.flush()

        val input = readLine()?.trim()
        return when {
            input.isNullOrBlank() -> currentNotes
            else -> input
        }
    }

    /**
     * Prompt for test groups
     */
    fun getTestGroups(currentGroups: List<String> = emptyList()): List<String>? {
        out.println()
        val currentDisplay = if (currentGroups.isNotEmpty())
            currentGroups.joinToString(", ")
        else
            "qa-team (default)"

        out.println("👥 Test groups (comma-separated):")
        out.println("   Current: $currentDisplay")
        out.print("> ")
        out.flush()

        val input = readLine()?.trim()
        return when {
            input.isNullOrBlank() -> {
                if (currentGroups.isNotEmpty()) currentGroups else null
            }
            else -> input.split(",").map { it.trim() }
        }
    }

    /**
     * Show summary and ask for confirmation
     */
    fun confirmDeployment(
        platforms: Set<Platform>,
        destinations: Map<Platform, List<String>>,
        releaseNotes: String?,
        testGroups: List<String>?,
        environment: String
    ): Boolean {
        out.println()
        out.println("═══════════════════════════════════════════")
        out.println("📋 Deployment Configuration")
        out.println("═══════════════════════════════════════════")
        out.println()

        out.println("🚀 Platforms: ${platforms.joinToString(", ") { it.name }}")

        destinations.forEach { (platform, dests) ->
            out.println("   └─ Destinations: ${dests.joinToString(", ")}")
        }

        out.println("🌍 Environment: $environment")

        if (releaseNotes != null) {
            out.println("📝 Release notes: \"$releaseNotes\"")
        }

        if (testGroups != null && testGroups.isNotEmpty()) {
            out.println("👥 Test groups: ${testGroups.joinToString(", ")}")
        }

        out.println()
        out.println("═══════════════════════════════════════════")
        out.println()

        out.print("? Proceed with deployment? (Y/n): ")
        out.flush()

        val input = readLine()?.trim()?.lowercase() ?: "y"
        return input != "n" && input != "no"
    }

    /**
     * Show deployment options menu
     */
    fun showOptionsMenu(): String? {
        out.println()
        out.println("⚙️  Options:")
        out.println("  [1] Change platforms")
        out.println("  [2] Change destinations")
        out.println("  [3] Change release notes")
        out.println("  [4] Change test groups")
        out.println("  [5] Review & deploy")
        out.println("  [6] Cancel")
        out.println()

        out.print("? Select option (1-6): ")
        out.flush()

        return readLine()?.trim()
    }

    /**
     * Get emoji for destination
     */
    private fun getDestinationEmoji(destination: String): String {
        return when (destination.lowercase()) {
            "firebase" -> "🔥"
            "play-console" -> "▶️"
            "local" -> "📁"
            "testflight" -> "✈️"
            "app-store" -> "🍎"
            else -> "📍"
        }
    }
}

/**
 * Available destinations per platform
 */
object AvailableDestinations {
    fun forAndroid() = listOf("firebase", "play-console", "local")
    fun forIos() = listOf("testflight", "app-store", "local")
}
