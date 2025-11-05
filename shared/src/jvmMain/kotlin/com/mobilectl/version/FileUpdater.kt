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
            val updatedContent = if (content.contains("version:")) {
                content.replace(
                    """version:[\s\S]*?current:\s*["']?[^"'\n]+["']?""".toRegex(),
                    """version:
  current: "$newVersion""""
                )
            } else {
                content + "\nversion:\n  current: \"$newVersion\"\n"
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

