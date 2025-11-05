package com.mobilectl.changelog

import java.io.File

actual fun createChangelogWriter(): ChangelogWriter = JvmChangelogWriter()

class JvmChangelogWriter : ChangelogWriter {
    override fun write(content: String, filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun read(filePath: String): String? {
        return try {
            File(filePath).readText()
        } catch (e: Exception) {
            null
        }
    }
}
