package com.mobilectl.desktop.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilectl.util.ApkAnalyzer
import com.mobilectl.util.ApkInfo
import com.mobilectl.util.ArtifactDetector
import com.mobilectl.util.ArtifactType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class ArtifactPreviewState(
    val isLoading: Boolean = false,
    val artifacts: List<ArtifactItem> = emptyList(),
    val selectedArtifact: ArtifactItem? = null,
    val error: String? = null
)

data class ArtifactItem(
    val file: File,
    val packageId: String,
    val versionCode: String?,
    val versionName: String?,
    val fileSizeMB: Double,
    val fileType: String,
    val buildDate: String,
    val flavor: String?,
    val buildType: String?,
    val isSigned: Boolean
)

class ArtifactPreviewViewModel : ViewModel() {
    var state by mutableStateOf(ArtifactPreviewState())
        private set

    init {
        scanForArtifacts()
    }

    /**
     * Scan project directory for build artifacts
     */
    fun scanForArtifacts(projectDir: String = System.getProperty("user.dir")) {
        state = state.copy(isLoading = true, error = null)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val baseDir = File(projectDir)
                    val artifacts = mutableListOf<ArtifactItem>()

                    // Find all APKs and AABs
                    val apks = ArtifactDetector.findAllApks(baseDir)
                    val aabs = findAllAabs(baseDir)

                    // Analyze each artifact
                    for (apk in apks) {
                        val info = ApkAnalyzer.getApkInfo(apk)
                        if (info != null) {
                            artifacts.add(createArtifactItem(apk, info))
                        }
                    }

                    for (aab in aabs) {
                        val info = ApkAnalyzer.getApkInfo(aab)
                        if (info != null) {
                            artifacts.add(createArtifactItem(aab, info))
                        }
                    }

                    // Sort by build date (newest first)
                    artifacts.sortByDescending { it.file.lastModified() }

                    state = state.copy(
                        isLoading = false,
                        artifacts = artifacts,
                        selectedArtifact = artifacts.firstOrNull()
                    )

                } catch (e: Exception) {
                    state = state.copy(
                        isLoading = false,
                        error = "Failed to scan artifacts: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Analyze a specific artifact file
     */
    fun analyzeArtifact(filePath: String) {
        state = state.copy(isLoading = true, error = null)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(filePath)
                    if (!file.exists()) {
                        state = state.copy(
                            isLoading = false,
                            error = "File not found: $filePath"
                        )
                        return@withContext
                    }

                    val info = ApkAnalyzer.getApkInfo(file)
                    if (info == null) {
                        state = state.copy(
                            isLoading = false,
                            error = "Failed to analyze artifact"
                        )
                        return@withContext
                    }

                    val artifact = createArtifactItem(file, info)
                    val updatedArtifacts = state.artifacts.toMutableList()

                    // Remove if already exists, add to front
                    updatedArtifacts.removeIf { it.file.absolutePath == file.absolutePath }
                    updatedArtifacts.add(0, artifact)

                    state = state.copy(
                        isLoading = false,
                        artifacts = updatedArtifacts,
                        selectedArtifact = artifact
                    )

                } catch (e: Exception) {
                    state = state.copy(
                        isLoading = false,
                        error = "Failed to analyze artifact: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Select an artifact for detailed view
     */
    fun selectArtifact(artifact: ArtifactItem) {
        state = state.copy(selectedArtifact = artifact)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        state = state.copy(error = null)
    }

    /**
     * Refresh artifacts list
     */
    fun refresh() {
        scanForArtifacts()
    }

    private fun createArtifactItem(file: File, info: ApkInfo): ArtifactItem {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val buildDate = dateFormat.format(Date(file.lastModified()))

        // Extract flavor and build type from file name
        val fileName = file.nameWithoutExtension
        val flavorAndType = extractFlavorAndType(fileName)

        return ArtifactItem(
            file = file,
            packageId = info.packageId,
            versionCode = info.versionCode,
            versionName = info.versionName,
            fileSizeMB = info.fileSizeMB,
            fileType = info.fileType,
            buildDate = buildDate,
            flavor = flavorAndType.first,
            buildType = flavorAndType.second,
            isSigned = !fileName.contains("unsigned", ignoreCase = true)
        )
    }

    private fun extractFlavorAndType(fileName: String): Pair<String?, String?> {
        // Try to extract flavor and build type from file name
        // Examples: app-production-release.apk, app-debug.apk, app-staging-release.aab

        val parts = fileName.split("-")
        if (parts.size < 2) return null to null

        // Common build types
        val buildTypes = listOf("debug", "release")
        val buildType = parts.lastOrNull { it.lowercase() in buildTypes }

        // Flavor is usually between app name and build type
        val flavor = if (parts.size >= 3) {
            parts[1].takeIf { it.lowercase() !in buildTypes }
        } else null

        return flavor to buildType
    }

    private fun findAllAabs(baseDir: File): List<File> {
        val moduleDir = findAndroidModuleDir(baseDir) ?: baseDir
        val bundleDir = File(moduleDir, "build/outputs/bundle")

        return if (bundleDir.exists()) {
            bundleDir.walk()
                .filter { it.name.endsWith(".aab") }
                .toList()
        } else {
            emptyList()
        }
    }

    private fun findAndroidModuleDir(baseDir: File): File? {
        val commonNames = listOf("app", "android", "mobile")
        for (name in commonNames) {
            val moduleDir = File(baseDir, name)
            if (moduleDir.exists() && isAndroidModule(moduleDir)) {
                return moduleDir
            }
        }
        return null
    }

    private fun isAndroidModule(dir: File): Boolean {
        return File(dir, "build.gradle.kts").exists()
                || File(dir, "build.gradle").exists()
    }
}
