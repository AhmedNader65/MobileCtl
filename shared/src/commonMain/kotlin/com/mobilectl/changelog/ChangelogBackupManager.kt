package com.mobilectl.changelog

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class BackupInfo(
    val id: String,
    val timestamp: LocalDateTime,
    val filePath: String,
    val size: Long
)

interface ChangelogBackupManager {
    fun createBackup(filePath: String): Result<BackupInfo>
    fun listBackups(filePath: String): List<BackupInfo>
    fun restoreBackup(backupId: String, filePath: String): Result<Boolean>
    fun deleteBackup(backupId: String): Result<Boolean>
    fun deleteOldBackups(keepCount: Int = 10): Result<Int>
}


expect fun createBackupManager(): ChangelogBackupManager
