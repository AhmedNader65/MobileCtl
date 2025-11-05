package com.mobilectl.changelog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SafeChangelogWriterTest {

    private val testFile = File("test-output.md")
    private val mockBackupManager = MockBackupManager()
    private val writer = SafeChangelogWriter(mockBackupManager)

    @Test
    fun testWriteNewFile() {
        val content = "# Changelog\n\n## [1.0.0]"

        val success = writer.write(content, testFile.absolutePath)

        assertTrue(success)
        assertEquals(content, testFile.readText())

        testFile.delete()
    }

    @Test
    fun testOverwriteExistingFile() {
        // Create initial file
        testFile.writeText("Old content")

        val newContent = "New content"
        val success = writer.write(newContent, testFile.absolutePath)

        assertTrue(success)
        assertEquals(newContent, testFile.readText())

        testFile.delete()
    }

    @Test
    fun testAtomicWrite() {
        // Write in parts to test atomicity
        val content1 = "Content 1"
        writer.write(content1, testFile.absolutePath)

        val content2 = "Content 2"
        writer.write(content2, testFile.absolutePath)

        // File should have content2, not partial/corrupted
        assertEquals(content2, testFile.readText())

        testFile.delete()
    }

    @Test
    fun testReadExistingFile() {
        val content = "Existing content"
        testFile.writeText(content)

        val read = writer.read(testFile.absolutePath)

        assertEquals(content, read)

        testFile.delete()
    }

    @Test
    fun testReadNonExistentFile() {
        val read = writer.read("nonexistent-file.md")

        assertEquals(null, read)
    }

    @Test
    fun testCreateBackupOnWrite() {
        testFile.writeText("Initial")

        writer.write("Updated", testFile.absolutePath)

        assertTrue(mockBackupManager.backupCreated)
    }

    private class MockBackupManager : ChangelogBackupManager {
        var backupCreated = false

        override fun createBackup(filePath: String): Result<BackupInfo> {
            backupCreated = true
            return Result.success(BackupInfo(
                id = "test-backup",
                timestamp = java.time.LocalDateTime.now(),
                filePath = filePath,
                size = 100
            ))
        }

        override fun listBackups(filePath: String): List<BackupInfo> = emptyList()
        override fun restoreBackup(backupId: String, filePath: String): Result<Boolean> = Result.success(true)
        override fun deleteBackup(backupId: String): Result<Boolean> = Result.success(true)
        override fun deleteOldBackups(keepCount: Int): Result<Int> = Result.success(0)
    }
}
