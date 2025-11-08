package com.mobilectl.commands.changelog

import com.mobilectl.changelog.ChangelogOrchestrator
import com.mobilectl.changelog.JGitCommitParser
import com.mobilectl.changelog.createChangelogStateManager
import com.mobilectl.changelog.createChangelogWriter
import com.mobilectl.config.ConfigLoader
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.util.PremiumLogger
import com.mobilectl.util.createFileUtil
import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

class ChangelogUpdateHandler(
    private val verbose: Boolean
) {
    private val out = PrintWriter(System.out, true, StandardCharsets.UTF_8)
    private val workingPath = System.getProperty("user.dir")
    private val configFile = File(workingPath, "mobileops.yaml").absolutePath

    suspend fun execute() {
        try {
            // Load config
            val fileUtil = createFileUtil()
            val detector = createProjectDetector()
            val configLoader = ConfigLoader(fileUtil, detector)
            val config = configLoader.loadConfig(configFile).getOrNull() ?: return

            val changelogConfig = config.changelog ?: return
            if (!changelogConfig.enabled) {
                PremiumLogger.simpleWarning("Changelog is disabled in config")
                return
            }

            val writer = createChangelogWriter()
            val existingContent = writer.read(changelogConfig.outputFile)

            if (existingContent == null) {
                PremiumLogger.simpleWarning("No existing changelog found")
                PremiumLogger.info("Generate one with: mobilectl changelog generate")
                return
            }

            val parser = JGitCommitParser()
            val orchestrator = ChangelogOrchestrator(parser, writer,
                createChangelogStateManager(), changelogConfig)

            PremiumLogger.section("Updating Changelog")
            PremiumLogger.detail("File", changelogConfig.outputFile)

            val result = orchestrator.generate(
                fromTag = null,
                dryRun = false
            )

            if (!result.success) {
                PremiumLogger.error(result.error ?: "Update failed")
                PremiumLogger.sectionEnd()
                return
            }

            PremiumLogger.success("Changelog updated")
            PremiumLogger.detail("Commits", "${result.commitCount} processed", dim = true)
            PremiumLogger.sectionEnd()

            if (verbose && result.content != null) {
                println("\n${result.content}")
            }
        } catch (e: Exception) {
            out.println("❌ Error: ${e.message}")
        }
    }
}
