package com.mobilectl.version

data class BackupResult(
    val success: Boolean,
    val backupPath: String? = null,
    val filesBackedUp: List<String> = emptyList(),
    val gitTagCreated: Boolean = false,
    val error: String? = null
)

interface VersionBackup {
    fun createBackup(fromVersion: String): BackupResult
    fun restoreBackup(backupPath: String): Boolean
    fun listBackups(): List<String>
}

expect fun createVersionBackup(): VersionBackup