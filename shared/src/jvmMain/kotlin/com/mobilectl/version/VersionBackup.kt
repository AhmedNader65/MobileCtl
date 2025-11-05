package com.mobilectl.version

import com.mobilectl.util.createLogger
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

actual fun createVersionBackup(): VersionBackup = JvmVersionBackup()

class JvmVersionBackup : VersionBackup {
    private val logger = createLogger("VersionBackup")

    override fun createBackup(fromVersion: String): BackupResult {
        return try {
            val timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))

            val backupDir = File(".", ".mobilectl/backups/$fromVersion-$timestamp")
            backupDir.mkdirs()

            val filesBackedUp = mutableListOf<String>()

            // Backup files
            val filesToBackup = listOf(
                "app/build.gradle.kts",
                "build.gradle.kts",
                "build.gradle",
                "package.json",
                "mobileops.yml"
            )

            filesToBackup.forEach { fileName ->
                val file = File(".", fileName)
                if (file.exists()) {
                    val backupFile = File(backupDir, fileName)
                    backupFile.parentFile?.mkdirs()
                    file.copyTo(backupFile, overwrite = true)
                    filesBackedUp.add(fileName)
                }
            }

            val gitTagCreated = createGitTag(fromVersion)

            logger.info("✅ Backup created: ${backupDir.absolutePath}")

            BackupResult(
                success = true,
                backupPath = backupDir.absolutePath,
                filesBackedUp = filesBackedUp,
                gitTagCreated = gitTagCreated
            )
        } catch (e: Exception) {
            logger.error("❌ Backup failed: ${e.message}")
            BackupResult(success = false, error = e.message)
        }
    }

    override fun restoreBackup(backupPath: String): Boolean {
        return try {
            val backupDir = File(backupPath)
            if (!backupDir.exists()) return false

            val baseDir = backupDir.parentFile.parentFile.parentFile

            backupDir.walkTopDown()
                .filter { it.isFile }
                .forEach { backupFile ->
                    val relativePath = backupFile.relativeTo(backupDir).path
                    val originalFile = File(baseDir, relativePath)
                    originalFile.parentFile?.mkdirs()
                    backupFile.copyTo(originalFile, overwrite = true)
                }

            true
        } catch (e: Exception) {
            false
        }
    }

    override fun listBackups(): List<String> {
        val backupDir = File(".", ".mobilectl/backups")
        return backupDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.sortedDescending()
            ?: emptyList()
    }

    private fun createGitTag(version: String): Boolean {
        return try {
            val gitDir = File(".", ".git")
            if (!gitDir.exists()) return false

            ProcessBuilder("git", "tag", "-a", "v$version", "-m", "Version $version")
                .directory(File("."))
                .redirectErrorStream(true)
                .start()
                .waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}
