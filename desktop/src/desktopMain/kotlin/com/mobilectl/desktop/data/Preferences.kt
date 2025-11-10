package com.mobilectl.desktop.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.nio.file.Paths

@Serializable
data class WindowPreferences(
    val width: Int = 1200,
    val height: Int = 800,
    val x: Int = -1,
    val y: Int = -1,
    val isMaximized: Boolean = false
)

@Serializable
data class AppPreferences(
    val window: WindowPreferences = WindowPreferences(),
    val isDarkTheme: Boolean = false,
    val lastOpenedScreen: String = "DASHBOARD"
)

object PreferencesManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val prefsDir = File(System.getProperty("user.home"), ".mobilectl")
    private val prefsFile = File(prefsDir, "desktop-preferences.json")

    init {
        if (!prefsDir.exists()) {
            prefsDir.mkdirs()
        }
    }

    fun load(): AppPreferences {
        return try {
            if (prefsFile.exists()) {
                val content = prefsFile.readText()
                json.decodeFromString(content)
            } else {
                AppPreferences()
            }
        } catch (e: Exception) {
            println("Failed to load preferences: ${e.message}")
            AppPreferences()
        }
    }

    fun save(preferences: AppPreferences) {
        try {
            val content = json.encodeToString(preferences)
            prefsFile.writeText(content)
        } catch (e: Exception) {
            println("Failed to save preferences: ${e.message}")
        }
    }
}
