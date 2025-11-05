package com.mobilectl.commands.changelog

import com.mobilectl.changelog.ChangelogOrchestrator
import com.mobilectl.changelog.JGitCommitParser
import com.mobilectl.changelog.SafeChangelogWriter
import com.mobilectl.changelog.createBackupManager
import com.mobilectl.changelog.createChangelogStateManager
import com.mobilectl.config.ConfigLoader
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.util.createFileUtil
import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

class ChangelogGenerateHandler(
    private val fromTag: String?,
    private val verbose: Boolean,
    private val dryRun: Boolean,
    private val append: Boolean = true,
    private val useLastState: Boolean = true
) {
    private val out = PrintWriter(System.out, true, StandardCharsets.UTF_8)
    private val workingPath = System.getProperty("user.dir")
    private val configFile = File(workingPath, "mobileops.yml").absolutePath

    suspend fun execute() {
        try {
            // 1. Load config (validation happens here!)
            val fileUtil = createFileUtil()
            val detector = createProjectDetector()
            val configLoader = ConfigLoader(fileUtil, detector)

            val config = configLoader.loadConfig(configFile).getOrNull()
                ?: return  // Error already printed by ConfigLoader

            val changelogConfig = config.changelog ?: return
            if (!changelogConfig.enabled) {
                println("⚠️  Changelog is disabled in config")
                return
            }

            // 2. Create orchestrator and generate
            val parser = JGitCommitParser()
            val backupManager = createBackupManager()
            val writer = SafeChangelogWriter(backupManager)
            val stateManager = createChangelogStateManager()

            val orchestrator = ChangelogOrchestrator(
                parser,
                writer,
                stateManager,
                changelogConfig
            )

            println("📝 Generating changelog...")

            val result = orchestrator.generate(
                fromTag = fromTag,
                dryRun = dryRun,
                append = append,
                useLastState = useLastState
            )

            if (!result.success) {
                println("❌ ${result.error}")
                return
            }

            println("✅ Generated successfully")
            println("   Found ${result.commitCount} commits")
            println("   Saved to: ${result.changelogPath}")

        } catch (e: Exception) {
            println("❌ Error: ${e.message}")
        }
    }
}
