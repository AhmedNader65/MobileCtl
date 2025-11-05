package com.mobilectl.changelog

import java.io.File

class SafeChangelogWriter(
    private val backupManager: ChangelogBackupManager
) : ChangelogWriter {

    override fun write(content: String, filePath: String): Boolean {
        return try {
            val targetFile = File(filePath)

            // Create backup first
            val backupResult = backupManager.createBackup(filePath)
            if (backupResult.isFailure) {
                val error = backupResult.exceptionOrNull()
                println("⚠️  Warning: Could not create backup: ${error?.message}")
                // Don't fail, continue with write
            }

            // Write to temp file first
            val tempFile = File(filePath + ".tmp")
            tempFile.writeText(content)

            // Atomic move
            if (targetFile.exists()) {
                targetFile.delete()
            }
            tempFile.renameTo(targetFile)

            // Verify write
            if (!targetFile.exists() || targetFile.readText() != content) {
                // Restore from backup
                val restoreResult = backupManager.listBackups(filePath).firstOrNull()?.let { backup ->
                    backupManager.restoreBackup(backup.id, filePath)
                }

                return if (restoreResult?.isSuccess == true) {
                    println("⚠️  Write verification failed, restored from backup")
                    false
                } else {
                    println("❌ Critical error: Could not write and restore backup")
                    false
                }
            }

            // Cleanup old backups
            backupManager.deleteOldBackups(keepCount = 10)

            true
        } catch (e: Exception) {
            println("❌ Error writing changelog: ${e.message}")

            // Try to restore
            backupManager.listBackups(filePath).firstOrNull()?.let { backup ->
                backupManager.restoreBackup(backup.id, filePath)
                println("✅ Restored from backup")
            }

            false
        }
    }

    override fun read(filePath: String): String? {
        return try {
            File(filePath).takeIf { it.exists() }?.readText()
        } catch (e: Exception) {
            null
        }
    }
}