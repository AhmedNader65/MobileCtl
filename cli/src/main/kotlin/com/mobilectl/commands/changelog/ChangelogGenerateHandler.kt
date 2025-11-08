package com.mobilectl.commands.changelog

import com.mobilectl.changelog.ChangelogOrchestrator
import com.mobilectl.changelog.JGitCommitParser
import com.mobilectl.changelog.SafeChangelogWriter
import com.mobilectl.changelog.createBackupManager
import com.mobilectl.changelog.createChangelogStateManager
import com.mobilectl.config.Config
import com.mobilectl.config.ConfigLoader
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.util.PremiumLogger
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
    private val configFile = File(workingPath, "mobileops.yaml").absolutePath

    suspend fun execute() {
        try {
            // 1. Load config (validation happens here!)
            val fileUtil = createFileUtil()
            val detector = createProjectDetector()
            val configLoader = ConfigLoader(fileUtil, detector)

            val config = configLoader.loadConfig(configFile).getOrNull()
                ?: Config()

            val changelogConfig = config.changelog

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

            PremiumLogger.section("Generating Changelog")
            PremiumLogger.detail("Output", changelogConfig.outputFile)
            if (fromTag != null) {
                PremiumLogger.detail("From Tag", fromTag, dim = true)
            }

            val result = orchestrator.generate(
                fromTag = fromTag,
                dryRun = dryRun,
                append = append,
                useLastState = useLastState
            )

            if (!result.success) {
                PremiumLogger.error(result.error ?: "Generation failed")
                PremiumLogger.sectionEnd()
                return
            }

            PremiumLogger.success("Generated successfully")
            PremiumLogger.detail("Commits Found", "${result.commitCount}", dim = true)
            PremiumLogger.detail("Saved To", result.changelogPath ?: changelogConfig.outputFile, dim = true)
            PremiumLogger.sectionEnd()

        } catch (e: Exception) {
            PremiumLogger.simpleError("Error: ${e.message}")
        }
    }
}
