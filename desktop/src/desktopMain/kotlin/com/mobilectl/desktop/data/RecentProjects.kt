package com.mobilectl.desktop.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File

@Serializable
data class ProjectInfo(
    val path: String,
    val name: String,
    val lastOpened: Long = System.currentTimeMillis()
)

object RecentProjectsManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val dataDir = File(System.getProperty("user.home"), ".mobilectl")
    private val projectsFile = File(dataDir, "recent-projects.json")
    private const val MAX_RECENT_PROJECTS = 10

    init {
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }
    }

    fun getRecentProjects(): List<ProjectInfo> {
        return try {
            if (projectsFile.exists()) {
                val content = projectsFile.readText()
                json.decodeFromString<List<ProjectInfo>>(content)
                    .sortedByDescending { it.lastOpened }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("Failed to load recent projects: ${e.message}")
            emptyList()
        }
    }

    fun addProject(path: String) {
        try {
            val file = File(path)
            if (!file.exists() || !file.isDirectory) {
                return
            }

            val projects = getRecentProjects().toMutableList()

            // Remove if already exists
            projects.removeIf { it.path == path }

            // Add to beginning
            projects.add(
                0,
                ProjectInfo(
                    path = path,
                    name = file.name,
                    lastOpened = System.currentTimeMillis()
                )
            )

            // Keep only last MAX_RECENT_PROJECTS
            val trimmed = projects.take(MAX_RECENT_PROJECTS)

            val content = json.encodeToString(trimmed)
            projectsFile.writeText(content)
        } catch (e: Exception) {
            println("Failed to save recent project: ${e.message}")
        }
    }

    fun removeProject(path: String) {
        try {
            val projects = getRecentProjects().toMutableList()
            projects.removeIf { it.path == path }

            val content = json.encodeToString(projects)
            projectsFile.writeText(content)
        } catch (e: Exception) {
            println("Failed to remove project: ${e.message}")
        }
    }

    fun clearAll() {
        projectsFile.delete()
    }
}
