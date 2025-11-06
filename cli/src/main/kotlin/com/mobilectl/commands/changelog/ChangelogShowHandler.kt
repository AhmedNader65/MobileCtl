package com.mobilectl.commands.changelog

import com.mobilectl.changelog.createChangelogWriter
import com.mobilectl.config.ConfigLoader
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.util.createFileUtil
import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

class ChangelogShowHandler(
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
                out.println("⚠️  Changelog is disabled in config")
                return
            }

            // Read changelog file
            val writer = createChangelogWriter()
            val content = writer.read(changelogConfig.outputFile)

            if (content == null) {
                out.println("📄 Changelog not found at: ${changelogConfig.outputFile}")
                out.println("   Generate one with: mobilectl changelog generate")
                return
            }

            out.println("📄 Changelog: ${changelogConfig.outputFile}\n")
            out.println(content)

            if (verbose) {
                out.println("\n📊 Details:")
                out.println("   Format: ${changelogConfig.format}")
                out.println("   File size: ${File(changelogConfig.outputFile).length()} bytes")
                out.println("   Last modified: ${File(changelogConfig.outputFile).lastModified()}")
            }
        } catch (e: Exception) {
            out.println("❌ Error: ${e.message}")
        }
    }
}
