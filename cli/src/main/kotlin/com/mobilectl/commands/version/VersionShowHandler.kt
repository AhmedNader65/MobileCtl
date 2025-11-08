package com.mobilectl.commands.version

import com.mobilectl.config.ConfigLoader
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.util.PremiumLogger
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
    private val configFile = File(workingPath, "mobileops.yaml").absolutePath

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

            val items = mutableMapOf<String, String>()

            if (detectedVersion != null) {
                items["Detected Version"] = detectedVersion.toString()
            }

            if (configExists && configVersion != null) {
                items["Config Version"] = configVersion.toString()
            } else if (!configExists) {
                items["Config"] = "No mobileops.yaml found"
            }

            if (detectedVersion != null && configVersion != null && detectedVersion.toString() != configVersion.toString()) {
                items["Status"] = "⚠️  Version mismatch"
                items["Will Use"] = "App version ($detectedVersion)"
            }

            if (verbose) {
                items["Files to Update"] = "mobileops.yaml, build.gradle.kts, package.json"
            }

            PremiumLogger.box("Version Information", items, success = true)
        } catch (e: Exception) {
            out.println("❌ Error: ${e.message}")
        }
    }
}
