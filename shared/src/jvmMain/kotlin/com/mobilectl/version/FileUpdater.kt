package com.mobilectl.version

import java.io.File

actual fun createFileUpdater(): FileUpdater = JvmFileUpdater()

class JvmFileUpdater : FileUpdater {

    /**
     * Extract version code from a file for display purposes
     */
    fun extractVersionCode(filePath: String): Int? {
        return try {
            val baseDir = System.getProperty("user.dir")
            val file = File(baseDir, filePath)
            if (!file.exists()) return null

            val content = file.readText()
            val regex = """versionCode\s*=\s*(\d+)""".toRegex()
            regex.find(content)?.groupValues?.get(1)?.toInt()
        } catch (e: Exception) {
            null
        }
    }
    override fun updateVersionInFiles(
        oldVersion: String,
        newVersion: String,
        filesToUpdate: List<String>,
        incrementVersionCode: Boolean
    ): List<String> {
        val updated = mutableListOf<String>()
        val baseDir = System.getProperty("user.dir")

        // Deduplicate files to avoid processing the same file twice
        val uniqueFiles = filesToUpdate.distinct()

        uniqueFiles.forEach { filePath ->
            val file = File(baseDir, filePath)
            if (file.exists()) {
                try {
                    val content = file.readText()
                    val updatedContent = updateVersionInFile(
                        content,
                        oldVersion,
                        newVersion,
                        file.extension,
                        incrementVersionCode
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
        extension: String,
        incrementVersionCode: Boolean
    ): String {
        return when (extension) {
            "kts", "gradle" -> {
                var updated = content
                    // Update versionName
                    .replace(
                        """versionName\s*=\s*"${Regex.escape(oldVersion)}"""".toRegex(),
                        """versionName = "$newVersion""""
                    )
                    .replace(
                        """version\s*=\s*"${Regex.escape(oldVersion)}"""".toRegex(),
                        """version = "$newVersion""""
                    )

                // Update versionCode (increment integer)
                if (incrementVersionCode) {
                    updated = incrementVersionCode(updated)
                }

                updated
            }
            "json" -> {
                var updated = content.replace(
                    """"version"\s*:\s*"${Regex.escape(oldVersion)}"""".toRegex(),
                    """"version": "$newVersion""""
                )

                // Update version code in package.json if present
                if (incrementVersionCode) {
                    updated = incrementBuildNumber(updated)
                }

                updated
            }
            "plist" -> {
                var updated = content
                    // Update CFBundleShortVersionString (version name)
                    .replace(
                        """(<key>CFBundleShortVersionString</key>\s*<string>)${Regex.escape(oldVersion)}(</string>)""".toRegex(),
                        """$1$newVersion$2"""
                    )

                // Update CFBundleVersion (build number)
                if (incrementVersionCode) {
                    updated = incrementBundleVersion(updated)
                }

                updated
            }
            else -> {
                content.replace(oldVersion, newVersion)
            }
        }
    }

    /**
     * Increment Android versionCode
     * Matches: versionCode = 123 or versionCode=123
     * Only increments the FIRST occurrence to avoid double-incrementing
     */
    private fun incrementVersionCode(content: String): String {
        val regex = """versionCode\s*=\s*(\d+)""".toRegex()
        var replaced = false
        return regex.replace(content) { matchResult ->
            if (replaced) {
                // Keep subsequent matches unchanged
                matchResult.value
            } else {
                // Increment only the first match
                replaced = true
                val currentCode = matchResult.groupValues[1].toInt()
                val newCode = currentCode + 1
                "versionCode = $newCode"
            }
        }
    }

    /**
     * Increment iOS CFBundleVersion (build number)
     * Matches: <key>CFBundleVersion</key><string>123</string>
     */
    private fun incrementBundleVersion(content: String): String {
        val regex = """(<key>CFBundleVersion</key>\s*<string>)(\d+)(</string>)""".toRegex()
        return regex.replace(content) { matchResult ->
            val currentVersion = matchResult.groupValues[2].toInt()
            val newVersion = currentVersion + 1
            "${matchResult.groupValues[1]}$newVersion${matchResult.groupValues[3]}"
        }
    }

    /**
     * Increment build number in package.json
     * Matches: "buildNumber": 123
     */
    private fun incrementBuildNumber(content: String): String {
        val regex = """"buildNumber"\s*:\s*(\d+)""".toRegex()
        return regex.replace(content) { matchResult ->
            val currentNumber = matchResult.groupValues[1].toInt()
            val newNumber = currentNumber + 1
            """"buildNumber": $newNumber"""
        }
    }
}

