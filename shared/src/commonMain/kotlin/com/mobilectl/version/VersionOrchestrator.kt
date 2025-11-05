package com.mobilectl.version

data class VersionBumpResult(
    val success: Boolean,
    val previousVersion: SemanticVersion,
    val newVersion: SemanticVersion,
    val filesUpdated: List<String> = emptyList(),
    val backupResult: BackupResult? = null,
    val error: String? = null
)

class VersionOrchestrator(
    private val backup: VersionBackup,
    private val fileUpdater: FileUpdater,
) {
    fun bump(
        currentVersion: SemanticVersion,
        newVersion: SemanticVersion,
        dryRun: Boolean = false,
        skipBackup: Boolean = false,
        configPath: String,
        filesToUpdate: List<String>
    ): VersionBumpResult {
        return try {
            if (dryRun) {
                return VersionBumpResult(
                    success = true,
                    previousVersion = currentVersion,
                    newVersion = newVersion
                )
            }

            // Create backup
            val backupResult = if (!skipBackup) {
                backup.createBackup(currentVersion.toString())
            } else {
                null
            }

            if (backupResult?.success == false) {
                return VersionBumpResult(
                    success = false,
                    previousVersion = currentVersion,
                    newVersion = newVersion,
                    error = "Backup failed: ${backupResult.error}"
                )
            }
            val filesUpdated = fileUpdater.updateVersionInFiles(
                currentVersion.toString(),
                newVersion.toString(),
                filesToUpdate
            )

            // Step 3: Update config
            val configUpdated = fileUpdater.updateConfig(configPath, newVersion.toString())

            if (!configUpdated) {
                return VersionBumpResult(
                    success = false,
                    previousVersion = currentVersion,
                    newVersion = newVersion,
                    filesUpdated = filesUpdated,
                    backupResult = backupResult,
                    error = "Failed to update config"
                )
            }

            VersionBumpResult(
                success = true,
                previousVersion = currentVersion,
                newVersion = newVersion,
                filesUpdated = filesUpdated,
                backupResult = backupResult
            )
        } catch (e: Exception) {
            VersionBumpResult(
                success = false,
                previousVersion = currentVersion,
                newVersion = currentVersion,
                error = e.message
            )
        }
    }

    fun listBackups(): List<String> = backup.listBackups()
}
