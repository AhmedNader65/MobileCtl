package com.mobilectl.commands.version

import com.mobilectl.config.ConfigLoader
import com.mobilectl.detector.createProjectDetector
import com.mobilectl.util.createFileUtil
import com.mobilectl.version.*
import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

class VersionBumpHandler(
    private val level: String,
    private val verbose: Boolean,
    private val dryRun: Boolean,
    private val skipBackup: Boolean
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
            val config = configResult.getOrNull()
            val configExists = configResult.isSuccess

            // Detect versions
            val detector = createVersionDetector()
            val detectedVersion = detector.detectVersionFromApp(workingPath)
            val configVersion = config?.version?.current?.let { SemanticVersion.parse(it) }

            // Determine current version
            val currentVersion = detectedVersion ?: configVersion ?: run {
                out.println("❌ Could not determine current version to bump")
                return
            }

            val newVersion = currentVersion.bump(level)

            com.mobilectl.util.PremiumLogger.section("Version Bump ($level)")
            com.mobilectl.util.PremiumLogger.detail("From", currentVersion.toString())
            com.mobilectl.util.PremiumLogger.detail("To", newVersion.toString())

            if (configExists && detectedVersion != null && configVersion != null &&
                detectedVersion.toString() != configVersion.toString()
            ) {
                com.mobilectl.util.PremiumLogger.warning("Version mismatch detected")
                com.mobilectl.util.PremiumLogger.detail("App Files", detectedVersion.toString(), dim = true)
                com.mobilectl.util.PremiumLogger.detail("Config", configVersion.toString(), dim = true)
                com.mobilectl.util.PremiumLogger.detail("Using", "App version ($detectedVersion)", dim = true)
            }

            if (verbose) {
                out.println("\n📄 Files that will be updated:")
                out.println("  • mobileops.yaml")
                out.println("  • build.gradle.kts")
            }

            if (dryRun) {
                out.println("\n📋 DRY-RUN: No files were modified")
                return
            }

            // Use orchestrator for actual bump
            val backup = createVersionBackup()
            val fileUpdater = createFileUpdater()
            val orchestrator = VersionOrchestrator(backup, fileUpdater)

            val filesToUpdate = config?.version?.filesToUpdate.orEmpty() + listOf(
                "app/build.gradle.kts",
                "build.gradle.kts",
                "package.json"
            )

            val result = orchestrator.bump(
                currentVersion = currentVersion,
                newVersion = newVersion,
                configPath = configFile,
                filesToUpdate = filesToUpdate,
                dryRun = false,
                skipBackup = skipBackup
            )

            if (!result.success) {
                out.println("❌ ${result.error}")
                return
            }

            result.filesUpdated.forEach { file ->
                com.mobilectl.util.PremiumLogger.success("Updated: $file")
            }

            result.backupResult?.let { backup ->
                if (backup.success) {
                    com.mobilectl.util.PremiumLogger.detail("Backup", File(backup.backupPath!!).name, dim = true)
                    if (backup.gitTagCreated) {
                        com.mobilectl.util.PremiumLogger.detail("Git Tag", "v$currentVersion", dim = true)
                    }
                }
            }

            com.mobilectl.util.PremiumLogger.sectionEnd()

            com.mobilectl.util.PremiumLogger.simpleSuccess("Version bumped: $currentVersion → $newVersion")
            out.println()
        } catch (e: Exception) {
            out.println("❌ Error: ${e.message}")
        }
    }
}
