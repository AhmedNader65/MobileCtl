package com.mobilectl.changelog

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

actual fun createBackupManager(): ChangelogBackupManager = FileSystemBackupManager()
class FileSystemBackupManager : ChangelogBackupManager {
    private val backupDir = File(".mobilectl/changelog-backups")
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS")

    override fun createBackup(filePath: String): Result<BackupInfo> {
        return try {
            val sourceFile = File(filePath)
            if (!sourceFile.exists()) {
                return Result.success(BackupInfo(
                    id = "none",
                    timestamp = LocalDateTime.now(),
                    filePath = filePath,
                    size = 0L
                ))
            }

            backupDir.mkdirs()

            val timestamp = LocalDateTime.now()
            // Use @ separator between filename and timestamp to avoid ambiguity
            val backupId = "${sourceFile.nameWithoutExtension}@${timestamp.format(dateFormat)}"
            val backupFile = File(backupDir, "$backupId.md")

            sourceFile.copyTo(backupFile, overwrite = true)

            if (!backupFile.exists()) {
                return Result.failure(Exception("Backup file was not created"))
            }

            Result.success(BackupInfo(
                id = backupId,
                timestamp = timestamp,
                filePath = backupFile.absolutePath,
                size = backupFile.length()
            ))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to create backup: ${e.message}", e))
        }
    }

    override fun listBackups(filePath: String): List<BackupInfo> {
        return try {
            if (!backupDir.exists()) return emptyList()

            val targetFile = File(filePath)
            val fileName = targetFile.nameWithoutExtension

            // Pattern: FILENAME@YYYY-MM-DD_HH-MM-SS-SSS.md
            // e.g.: test-CHANGELOG@2025-11-05_15-47-52-123.md
            val pattern = """^${Regex.escape(fileName)}@\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}-\d{3}\.md$""".toRegex()

            backupDir.listFiles()?.filter { pattern.matches(it.name) }
                ?.mapNotNull { file ->
                    try {
                        val backupId = file.nameWithoutExtension

                        // Extract timestamp: everything after @
                        val timestampStr = backupId.substringAfter("@")

                        if (timestampStr.isBlank()) return@mapNotNull null

                        val timestamp = try {
                            LocalDateTime.parse(timestampStr, dateFormat)
                        } catch (e: Exception) {
                            return@mapNotNull null
                        }

                        BackupInfo(
                            id = backupId,
                            timestamp = timestamp,
                            filePath = file.absolutePath,
                            size = file.length()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                ?.sortedByDescending { it.timestamp }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun restoreBackup(backupId: String, filePath: String): Result<Boolean> {
        return try {
            val backupFile = File(backupDir, "$backupId.md")
            if (!backupFile.exists()) {
                return Result.failure(Exception("Backup '$backupId' not found"))
            }

            val targetFile = File(filePath)
            backupFile.copyTo(targetFile, overwrite = true)

            if (!targetFile.exists() || targetFile.length() != backupFile.length()) {
                return Result.failure(Exception("Restore verification failed"))
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to restore backup: ${e.message}", e))
        }
    }

    override fun deleteBackup(backupId: String): Result<Boolean> {
        return try {
            val backupFile = File(backupDir, "$backupId.md")
            if (!backupFile.exists()) {
                return Result.failure(Exception("Backup '$backupId' not found"))
            }

            val deleted = backupFile.delete()
            if (!deleted) {
                return Result.failure(Exception("Failed to delete backup file"))
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete backup: ${e.message}", e))
        }
    }

    override fun deleteOldBackups(keepCount: Int): Result<Int> {
        return try {
            val allBackups = backupDir.listFiles()
                ?.filter { it.isFile && it.extension == "md" }
                ?.mapNotNull { file ->
                    try {
                        val fileName = file.nameWithoutExtension
                        val timestampStr = fileName.substringAfter("@")

                        if (timestampStr.isBlank()) return@mapNotNull null

                        val timestamp = LocalDateTime.parse(timestampStr, dateFormat)
                        file to timestamp
                    } catch (e: Exception) {
                        null
                    }
                }
                ?.sortedByDescending { it.second }
                ?: return Result.success(0)

            if (allBackups.size <= keepCount) {
                return Result.success(0)
            }

            var deleted = 0
            allBackups.drop(keepCount).forEach { (file, _) ->
                try {
                    if (file.delete()) {
                        deleted++
                    }
                } catch (e: Exception) {
                    // Continue with next file
                }
            }

            Result.success(deleted)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to cleanup old backups: ${e.message}", e))
        }
    }
}