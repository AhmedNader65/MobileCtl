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

            // Create orchestrator
            val parser = JvmGitCommitParser()
            val writer = createChangelogWriter()
            val orchestrator = ChangelogOrchestrator(
                parser, writer,
                createChangelogStateManager(), changelogConfig
            )

            out.println("📝 Generating changelog...")
            out.println("   Include: " + listOfNotNull(
                if (changelogConfig.includeBreakingChanges) "breaking changes" else null,
                if (changelogConfig.includeContributors) "contributors" else null,
                if (changelogConfig.includeStats) "stats" else null,
                if (changelogConfig.includeCompareLinks) "compare links" else null
            ).joinToString(", "))
            out.println("   Mode: ${if (append) "append" else "overwrite"}")
            if (fromTag != null) {
                out.println("   From tag: $fromTag")
            } else if (useLastState) {
                out.println("   Mode: Resume from last generation")
            }

            // Generate
            val result = orchestrator.generate(
                fromTag = fromTag,
                dryRun = dryRun,
                append = append,
                useLastState = useLastState  // ← PASS THIS
            )

            if (!result.success) {
                out.println("❌ ${result.error}")
                return
            }

            out.println("✅ Found ${result.commitCount} commits")

            if (dryRun) {
                out.println("\n📋 DRY-RUN: Preview\n")
                out.println(result.content)
                return
            }

            out.println("💾 Changelog saved to: ${result.changelogPath}")
            if (verbose && result.content != null) {
                out.println("\n📄 Preview:\n")
                out.println(result.content)
            }
        } catch (e: Exception) {
            out.println("❌ Error: ${e.message}")
        }
    }
}
