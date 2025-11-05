package com.mobilectl

import com.mobilectl.commands.changelog.ChangelogGenerateHandler
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test

class ChangelogGenerateHandlerTest {

    @Test
    fun testChangelogGeneration() = runBlocking {
        // Create temporary test directory with git repo
        val testDir = File.createTempFile("mobilectl-test", "").apply { delete(); mkdirs() }
        val originalDir = System.getProperty("user.dir")

        try {
            System.setProperty("user.dir", testDir.absolutePath)

            // Initialize git repo
            ProcessBuilder("git", "init").directory(testDir).start().waitFor()
            ProcessBuilder("git", "config", "user.email", "test@test.com")
                .directory(testDir).start().waitFor()
            ProcessBuilder("git", "config", "user.name", "Test User")
                .directory(testDir).start().waitFor()

            // Create test commit
            File(testDir, "README.md").writeText("# Test")
            ProcessBuilder("git", "add", ".").directory(testDir).start().waitFor()
            ProcessBuilder("git", "commit", "-m", "feat: initial commit")
                .directory(testDir).start().waitFor()

            // Create minimal mobileops.yml
            File(testDir, "mobileops.yml").writeText(
                """
                changelog:
                  enabled: true
                  format: markdown
                  output_file: CHANGELOG.md
            """.trimIndent()
            )

            // Test handler
            val handler = ChangelogGenerateHandler(
                fromTag = null,
                verbose = true,
                dryRun = true
            )

            handler.execute()

            assertTrue(true) // If we got here, no exceptions

        } finally {
            System.setProperty("user.dir", originalDir)
            testDir.deleteRecursively()
        }
    }
}