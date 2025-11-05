package com.mobilectl.commands.changelog

import com.mobilectl.changelog.createBackupManager
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

class ChangelogRestoreHandler(
    private val backupId: String?,
    private val verbose: Boolean
) {
    private val out = PrintWriter(System.out, true, StandardCharsets.UTF_8)

    suspend fun execute() {
        try {
            val backupManager = createBackupManager()
            val backups = backupManager.listBackups("CHANGELOG.md")

            if (backups.isEmpty()) {
                out.println("❌ No backups found")
                return
            }

            if (backupId == null) {
                out.println("📦 Available backups:")
                backups.forEach { backup ->
                    out.println("   • ${backup.id}")
                    if (verbose) {
                        out.println("     Date: ${backup.timestamp}")
                        out.println("     Size: ${backup.size} bytes")
                    }
                }
                out.println("\nRestore with: mobilectl changelog restore <backup-id>")
                return
            }

            val result = backupManager.restoreBackup(backupId, "CHANGELOG.md")

            result.onSuccess {
                out.println("✅ Restored backup: $backupId")
            }
            result.onFailure { error ->
                out.println("❌ ${error.message}")
            }
        } catch (e: Exception) {
            out.println("❌ Error: ${e.message}")
        }
    }
}
