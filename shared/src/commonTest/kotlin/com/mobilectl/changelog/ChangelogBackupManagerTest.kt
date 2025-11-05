package com.mobilectl.changelog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ChangelogBackupManagerTest {

    private val backupManager = FileSystemBackupManager()
    private val testFile = File("test-CHANGELOG.md")
    @Test
    fun testCreateBackup() {
        val testFile = File(".mobilectl/test-backup/CHANGELOG.md")
        testFile.parentFile?.mkdirs()
        testFile.writeText("# Changelog\n\n## [1.0.0]")

        try {
            val result = backupManager.createBackup(testFile.absolutePath)

            assertTrue(result.isSuccess, "Backup creation should succeed")
            val backup = result.getOrNull()
            assertNotNull(backup, "Backup should not be null")
            println("Created backup: ${backup.id} at ${backup.filePath}")
            // Check that backup file exists
            assertTrue(File(backup.filePath).exists(), "Backup file should exist at ${backup.filePath}")

            // Check that backup is in the right directory
            assertTrue(
                backup.filePath.contains(".mobilectl/changelog-backups/") || backup.filePath.contains(".mobilectl\\changelog-backups\\"),
                "Backup should be in .mobilectl/changelog-backups/ directory"
            )

            // Check that backup has correct format
            assertTrue(
                backup.id.matches(Regex("""CHANGELOG@\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}-\d{3}""")),
                "Backup ID should have format FILENAME@YYYY-MM-DD_HH-MM-SS-SSS"
            )

            // Check that backup file has content
            assertTrue(backup.size > 0, "Backup size should be greater than 0")

            // Check that backup content matches original
            val originalContent = testFile.readText()
            val backupContent = File(backup.filePath).readText()
            assertEquals(originalContent, backupContent, "Backup content should match original")

        } finally {
            // Cleanup
            File(".mobilectl/test-backup").deleteRecursively()
            File(".mobilectl/changelog-backups").deleteRecursively()
        }
    }

    @Test
    fun testCreateBackupLocations() {
        val testFile = File(".mobilectl/test-locations/CHANGELOG.md")
        testFile.parentFile?.mkdirs()
        testFile.writeText("test content")

        try {
            val result = backupManager.createBackup(testFile.absolutePath)
            assertTrue(result.isSuccess)

            val backup = result.getOrNull()!!
            val backupPath = backup.filePath

            // Verify directory structure
            println("Original file: ${testFile.absolutePath}")
            println("Backup file: $backupPath")

            // Should be in .mobilectl/changelog-backups/ directory
            assertTrue(backupPath.contains(".mobilectl"), "Should be in .mobilectl directory")
            assertTrue(backupPath.contains("changelog-backups"), "Should be in changelog-backups directory")

            // Original should NOT be in backup directory
            assertFalse(testFile.absolutePath.contains("changelog-backups"))

            // Backup file should exist and be readable
            val backupFile = File(backupPath)
            assertTrue(backupFile.exists())
            assertTrue(backupFile.isFile)
            assertTrue(backupFile.canRead())

        } finally {
            File(".mobilectl/test-locations").deleteRecursively()
            File(".mobilectl/changelog-backups").deleteRecursively()
        }
    }

    @Test
    fun testCreateBackupWithDifferentNames() {
        val files = listOf(
            ".mobilectl/test-names/CHANGELOG.md",
            ".mobilectl/test-names/RELEASES.md",
            ".mobilectl/test-names/NOTES.md"
        )

        try {
            files.forEach { filePath ->
                File(filePath).parentFile?.mkdirs()
                File(filePath).writeText("content for $filePath")
            }

            files.forEach { filePath ->
                val result = backupManager.createBackup(filePath)
                assertTrue(result.isSuccess, "Should create backup for $filePath")

                val backup = result.getOrNull()!!
                val fileName = File(filePath).nameWithoutExtension

                // Backup ID should contain original filename
                assertTrue(
                    backup.id.startsWith(fileName),
                    "Backup ID should start with $fileName, got ${backup.id}"
                )
            }

            // List all backups
            val allBackups = backupManager.listBackups(files[0])
            assertTrue(allBackups.isNotEmpty(), "Should have backups")

        } finally {
            File(".mobilectl/test-names").deleteRecursively()
            File(".mobilectl/changelog-backups").deleteRecursively()
        }
    }


    @Test
    fun testCreateBackupNonExistentFile() {
        val result = backupManager.createBackup("nonexistent-file.md")

        assertTrue(result.isSuccess)
        // Should still succeed (no file to backup yet)
    }

    @Test
    fun testListBackups() {
        // Create test file and backups
        testFile.writeText("content")
        backupManager.createBackup(testFile.absolutePath)
        backupManager.createBackup(testFile.absolutePath)

        val backups = backupManager.listBackups(testFile.absolutePath)

        assertEquals(backups.size , 2)

        // Cleanup
        testFile.delete()
        backups.forEach { File(it.filePath).delete() }
    }

    @Test
    fun testRestoreBackup() {
        // Create original file
        testFile.writeText("Original content")

        // Create backup
        val backupResult = backupManager.createBackup(testFile.absolutePath)
        val backup = backupResult.getOrNull()!!

        // Modify original
        testFile.writeText("Modified content")

        // Restore
        val restoreResult = backupManager.restoreBackup(backup.id, testFile.absolutePath)

        assertTrue(restoreResult.isSuccess)
        assertEquals("Original content", testFile.readText())

        // Cleanup
        testFile.delete()
        File(backup.filePath).delete()
    }

    @Test
    fun testRestoreNonExistentBackup() {
        val result = backupManager.restoreBackup("nonexistent-id", testFile.absolutePath)

        assertTrue(result.isFailure)
    }

    @Test
    fun testDeleteBackup() {
        testFile.writeText("content")
        val backupResult = backupManager.createBackup(testFile.absolutePath)
        val backup = backupResult.getOrNull()!!

        val deleteResult = backupManager.deleteBackup(backup.id)

        assertTrue(deleteResult.isSuccess)
        assertFalse(File(backup.filePath).exists())

        testFile.delete()
    }

    @Test
    fun testDeleteOldBackups() {
        val testFile = File(".mobilectl/test-deleteold/CHANGELOG.md")
        testFile.parentFile?.mkdirs()

        try {
            // Create 15 backups
            repeat(15) {
                testFile.writeText("content version $it")  // Different content each time
                val result = backupManager.createBackup(testFile.absolutePath)

                if (result.isFailure) {
                    println("Failed to create backup $it: ${result.exceptionOrNull()?.message}")
                }

                Thread.sleep(50)  // Small delay for timestamp differentiation
            }

            val backups = backupManager.listBackups(testFile.absolutePath)
            println("Created backups: ${backups.size}")
            backups.forEach { println("  - ${it.id} (${it.timestamp})") }

            assertEquals(15, backups.size, "Should have created 15 backups")

            // Delete old, keep 10
            val deleteResult = backupManager.deleteOldBackups(keepCount = 10)
            assertTrue(deleteResult.isSuccess)

            val deleted = deleteResult.getOrNull() ?: 0
            println("Deleted: $deleted backups")

            val remaining = backupManager.listBackups(testFile.absolutePath)
            println("Remaining: ${remaining.size} backups")
            remaining.forEach { println("  - ${it.id}") }

            assertEquals(10, remaining.size, "Should have 10 remaining backups")

        } finally {
            File(".mobilectl/test-deleteold").deleteRecursively()
            File(".mobilectl/changelog-backups").deleteRecursively()
        }
    }

    @Test
    fun testCreateMultipleBackups() {
        val testFile = File(".mobilectl/test-multi/CHANGELOG.md")
        testFile.parentFile?.mkdirs()
        testFile.writeText("original content")

        try {
            // Create 5 backups in quick succession
            val backupIds = mutableListOf<String>()
            repeat(5) { i ->
                testFile.writeText("version $i")
                val result = backupManager.createBackup(testFile.absolutePath)

                assertTrue(result.isSuccess, "Backup $i should succeed")
                backupIds.add(result.getOrNull()?.id ?: "")

                Thread.sleep(50)
            }

            // Verify all were created
            val backups = backupManager.listBackups(testFile.absolutePath)
            assertEquals(5, backups.size, "Should have created 5 unique backups")

            // Verify each can be restored
            backups.forEach { backup ->
                val restoreResult = backupManager.restoreBackup(backup.id, testFile.absolutePath)
                assertTrue(restoreResult.isSuccess, "Should be able to restore ${backup.id}")
            }

        } finally {
            File(".mobilectl/test-multi").deleteRecursively()
            File(".mobilectl/changelog-backups").deleteRecursively()
        }
    }


}
