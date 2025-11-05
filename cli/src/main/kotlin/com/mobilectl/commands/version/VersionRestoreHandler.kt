package com.mobilectl.commands.version

import com.mobilectl.version.createVersionBackup
import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

class VersionRestoreHandler(
    private val backupName: String?
) {
    private val out = PrintWriter(System.out, true, StandardCharsets.UTF_8)
    private val workingPath = System.getProperty("user.dir")

    fun execute() {
        try {
            val backup = createVersionBackup()

            if (backupName == null) {
                // List backups
                val backups = backup.listBackups()
                if (backups.isEmpty()) {
                    out.println("No backups found")
                    return
                }

                out.println("📦 Available backups:")
                backups.forEach { out.println("  • $it") }
                out.println("\nUsage: mobilectl version restore <backup-name>")
            } else {
                // Restore specific backup
                val backupPath = "$workingPath/.mobilectl/backups/$backupName"
                out.println("🔄 Restoring backup: $backupName")

                if (backup.restoreBackup(backupPath)) {
                    out.println("✅ Backup restored successfully!")
                } else {
                    out.println("❌ Failed to restore backup")
                }
            }
        } catch (e: Exception) {
            out.println("❌ Error: ${e.message}")
        }
    }
}
