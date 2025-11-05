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
    private val configFile = File(workingPath, "mobileops.yml").absolutePath

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

            out.println("🔢 Bumping version: $currentVersion → $newVersion")

            // Warn about mismatch
            if (configExists && detectedVersion != null && configVersion != null &&
                detectedVersion.toString() != configVersion.toString()
            ) {
                out.println("⚠️  Version mismatch detected!")
                out.println("   App files: $detectedVersion")
                out.println("   Config: $configVersion")
                out.println("   Using app version ($detectedVersion) as source")
            }

            if (verbose) {
                out.println("\n📄 Files that will be updated:")
                out.println("  • mobileops.yml")
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

            // Print results
            out.println("\n📝 Files updated: ${result.filesUpdated.size}")
            result.filesUpdated.forEach { out.println("  ✅ $it") }

            result.backupResult?.let { backup ->
                if (backup.success) {
                    out.println("\n💾 Backup: ${File(backup.backupPath!!).name}")
                    if (backup.gitTagCreated) {
                        out.println("🏷️  Git tag: v$currentVersion")
                    }
                }
            }

            out.println("\n✅ Version bumped: $currentVersion → $newVersion")
        } catch (e: Exception) {
            out.println("❌ Error: ${e.message}")
        }
    }
}
