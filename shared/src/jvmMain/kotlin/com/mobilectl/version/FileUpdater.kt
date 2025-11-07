package com.mobilectl.version

import java.io.File

actual fun createFileUpdater(): FileUpdater = JvmFileUpdater()

class JvmFileUpdater : FileUpdater {
    override fun updateVersionInFiles(
        oldVersion: String,
        newVersion: String,
        filesToUpdate: List<String>
    ): List<String> {
        val updated = mutableListOf<String>()
        val baseDir = System.getProperty("user.dir")
        filesToUpdate.forEach { filePath ->
            val file = File(baseDir, filePath)
            if (file.exists()) {
                try {
                    val content = file.readText()
                    val updatedContent = updateVersionInFile(
                        content,
                        oldVersion,
                        newVersion,
                        file.extension
                    )
                    file.writeText(updatedContent)
                    updated.add(filePath)
                } catch (e: Exception) {
                    // Silently skip failed files
                }
            }
        }

        return updated
    }

    override fun updateConfig(configPath: String, newVersion: String): Boolean {
        return try {
            val file = File(configPath)
            if (!file.exists()) {
                // Create minimal config
                file.parentFile?.mkdirs()
                file.writeText("version:\n  current: \"$newVersion\"\n")
                return true
            }

            val content = file.readText()
            val lines = content.lines().toMutableList()
            var updated = false

            // Find and update the current version line
            for (i in lines.indices) {
                val line = lines[i]
                // Match "  current: "1.0.0"" or "  current: 1.0.0"
                if (line.trimStart().startsWith("current:")) {
                    val indent = line.takeWhile { it.isWhitespace() }
                    lines[i] = "${indent}current: \"$newVersion\""
                    updated = true
                    break
                }
            }

            val updatedContent = if (updated) {
                lines.joinToString("\n")
            } else {
                // If current: doesn't exist, add version block
                if (content.contains("version:")) {
                    // Find version: line and add current: after it
                    val versionIndex = lines.indexOfFirst { it.trimStart().startsWith("version:") }
                    if (versionIndex != -1) {
                        lines.add(versionIndex + 1, "  current: \"$newVersion\"")
                    }
                    lines.joinToString("\n")
                } else {
                    content + "\nversion:\n  current: \"$newVersion\"\n"
                }
            }

            file.writeText(updatedContent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun updateVersionInFile(
        content: String,
        oldVersion: String,
        newVersion: String,
        extension: String
    ): String {
        return when (extension) {
            "kts", "gradle" -> {
                content
                    .replace(
                        """versionName\s*=\s*"${Regex.escape(oldVersion)}"""".toRegex(),
                        """versionName = "$newVersion""""
                    )
                    .replace(
                        """version\s*=\s*"${Regex.escape(oldVersion)}"""".toRegex(),
                        """version = "$newVersion""""
                    )
            }
            "json" -> {
                content.replace(
                    """"version"\s*:\s*"${Regex.escape(oldVersion)}"""".toRegex(),
                    """"version": "$newVersion""""
                )
            }
            else -> {
                content.replace(oldVersion, newVersion)
            }
        }
    }
}

