package com.mobilectl.commands.version

import com.mobilectl.config.ConfigLoader
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.util.createFileUtil
import com.mobilectl.version.SemanticVersion
import com.mobilectl.version.createVersionDetector
import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

class VersionShowHandler(
    private val verbose: Boolean
) {
    private val out = PrintWriter(System.out, true, StandardCharsets.UTF_8)
    private val workingPath = System.getProperty("user.dir")
    private val configFile = File(workingPath, "mobileops.yml").absolutePath

    suspend fun execute() {
        try {
            // Load config
            val fileUtil = createFileUtil()
            val platformDetector = createProjectDetector()
            val configLoader = ConfigLoader(fileUtil, platformDetector)
            val configResult = configLoader.loadConfig(configFile)
            val configExists = configResult.isSuccess

            // Detect versions
            val detector = createVersionDetector()
            val detectedVersion = detector.detectVersionFromApp(workingPath)
            val configVersion = configResult.getOrNull()?.version?.current?.let { SemanticVersion.parse(it) }

            // Show versions
            if (detectedVersion != null) {
                out.println("📱 Detected version: $detectedVersion")
            }

            if (configExists && configVersion != null && detectedVersion != configVersion) {
                out.println("📋 Config version: $configVersion")

                // Warn if mismatch
                if (detectedVersion != null && detectedVersion.toString() != configVersion.toString()) {
                    out.println("⚠️  Version mismatch!")
                    out.println("   App files: $detectedVersion")
                    out.println("   Config: $configVersion")
                    out.println("   → Will use app version as source of truth")
                }
            } else if (!configExists) {
                out.println("📋 No mobileops.yml found")
            }

            if (verbose) {
                out.println("\n📄 Files to update:")
                out.println("  • mobileops.yml")
                out.println("  • build.gradle.kts (or build.gradle)")
                out.println("  • package.json (if exists)")
            }
        } catch (e: Exception) {
            out.println("❌ Error: ${e.message}")
        }
    }
}
