package com.mobilectl.commands.changelog

import com.mobilectl.changelog.ChangelogOrchestrator
import com.mobilectl.changelog.JGitCommitParser
import com.mobilectl.changelog.createChangelogStateManager
import com.mobilectl.changelog.createChangelogWriter
import com.mobilectl.config.ConfigLoader
import com.mobilectl.detector.createProjectDetector
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
                com.mobilectl.util.PremiumLogger.simpleWarning("Changelog is disabled in config")
                return
            }

            val writer = createChangelogWriter()
            val existingContent = writer.read(changelogConfig.outputFile)

            if (existingContent == null) {
                com.mobilectl.util.PremiumLogger.simpleWarning("No existing changelog found")
                com.mobilectl.util.PremiumLogger.info("Generate one with: mobilectl changelog generate")
                return
            }

            val parser = JGitCommitParser()
            val orchestrator = ChangelogOrchestrator(parser, writer,
                createChangelogStateManager(), changelogConfig)

            com.mobilectl.util.PremiumLogger.section("Updating Changelog")
            com.mobilectl.util.PremiumLogger.detail("File", changelogConfig.outputFile)

            val result = orchestrator.generate(
                fromTag = null,
                dryRun = false
            )

            if (!result.success) {
                com.mobilectl.util.PremiumLogger.error(result.error ?: "Update failed")
                com.mobilectl.util.PremiumLogger.sectionEnd()
                return
            }

            com.mobilectl.util.PremiumLogger.success("Changelog updated")
            com.mobilectl.util.PremiumLogger.detail("Commits", "${result.commitCount} processed", dim = true)
            com.mobilectl.util.PremiumLogger.sectionEnd()

            if (verbose && result.content != null) {
                println("\n${result.content}")
            }
        } catch (e: Exception) {
            out.println("❌ Error: ${e.message}")
        }
    }
}
