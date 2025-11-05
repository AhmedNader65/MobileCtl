package com.mobilectl.commands.changelog

import com.mobilectl.changelog.ChangelogOrchestrator
import com.mobilectl.changelog.JvmGitCommitParser
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
    private val configFile = File(workingPath, "mobileops.yml").absolutePath

    suspend fun execute() {
        try {
            // Load config
            val fileUtil = createFileUtil()
            val detector = createProjectDetector()
            val configLoader = ConfigLoader(fileUtil, detector)
            val config = configLoader.loadConfig(configFile).getOrNull() ?: return

            val changelogConfig = config.changelog ?: return
            if (!changelogConfig.enabled) {
                out.println("⚠️  Changelog is disabled in config")
                return
            }

            // Check if changelog exists
            val writer = createChangelogWriter()
            val existingContent = writer.read(changelogConfig.outputFile)

            if (existingContent == null) {
                out.println("⚠️  No existing changelog found")
                out.println("   Generate one with: mobilectl changelog generate")
                return
            }

            // Create orchestrator
            val parser = JvmGitCommitParser()
            val orchestrator = ChangelogOrchestrator(parser, writer,
                createChangelogStateManager(), changelogConfig)

            out.println("🔄 Updating changelog...")
            out.println("   File: ${changelogConfig.outputFile}")

            // Regenerate
            val result = orchestrator.generate(
                fromTag = null,
                dryRun = false
            )

            if (!result.success) {
                out.println("❌ ${result.error}")
                return
            }

            out.println("✅ Changelog updated!")
            out.println("   Commits processed: ${result.commitCount}")

            if (verbose && result.content != null) {
                out.println("\n📄 Updated content:\n")
                out.println(result.content)
            }
        } catch (e: Exception) {
            out.println("❌ Error: ${e.message}")
        }
    }
}
