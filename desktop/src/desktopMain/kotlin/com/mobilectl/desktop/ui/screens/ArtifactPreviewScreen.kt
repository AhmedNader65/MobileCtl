package com.mobilectl.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilectl.desktop.ui.components.*
import com.mobilectl.desktop.ui.theme.AccentColors
import com.mobilectl.desktop.viewmodel.ArtifactItem
import com.mobilectl.desktop.viewmodel.ArtifactPreviewViewModel

@Composable
fun ArtifactPreviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: ArtifactPreviewViewModel = viewModel { ArtifactPreviewViewModel() }
) {
    val state = viewModel.state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Build Artifacts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!state.isLoading) {
                    Text(
                        text = "${state.artifacts.size} found",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { viewModel.refresh() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                }
                TextButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Back", fontSize = 13.sp)
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
            }
        } else if (state.artifacts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.FolderOff,
                        null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        "No build artifacts found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Artifacts table
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MinimalCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            // Table header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    "File",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(0.3f)
                                )
                                Text(
                                    "Type",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(0.1f)
                                )
                                Text(
                                    "Version",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(0.15f)
                                )
                                Text(
                                    "Size",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(0.1f)
                                )
                                Text(
                                    "Built",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(0.15f)
                                )
                                Text(
                                    "Status",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(0.1f)
                                )
                            }

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            LazyColumn {
                                items(state.artifacts) { artifact ->
                                    ArtifactRow(
                                        artifact = artifact,
                                        isSelected = state.selectedArtifact == artifact,
                                        onClick = { viewModel.selectArtifact(artifact) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Details panel (when artifact selected)
                state.selectedArtifact?.let { artifact ->
                    Column(
                        modifier = Modifier.width(320.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionHeader("Details")

                        MinimalCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                DetailRow("Package", artifact.packageId)
                                DetailRow("Version Name", artifact.versionName ?: "N/A")
                                DetailRow("Version Code", artifact.versionCode ?: "N/A")
                                DetailRow("File Type", artifact.fileType)
                                DetailRow("Size", "${String.format("%.2f", artifact.fileSizeMB)} MB")
                                DetailRow("Flavor", artifact.flavor ?: "N/A")
                                DetailRow("Build Type", artifact.buildType ?: "N/A")
                                DetailRow("Signed", if (artifact.isSigned) "Yes" else "No")
                                DetailRow("Path", artifact.file.absolutePath, isPath = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtifactRow(
    artifact: ArtifactItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    TableRow(onClick = onClick) {
        Column(modifier = Modifier.weight(0.3f)) {
            Text(
                artifact.file.name,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
        Text(
            artifact.fileType,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.weight(0.1f)
        )
        Text(
            artifact.versionName ?: "—",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.15f)
        )
        Text(
            "${String.format("%.1f", artifact.fileSizeMB)} MB",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.1f)
        )
        Text(
            artifact.buildDate,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.weight(0.15f)
        )
        StatusBadge(
            text = if (artifact.isSigned) "Signed" else "Unsigned",
            color = if (artifact.isSigned) AccentColors.success else AccentColors.neutral500,
            modifier = Modifier.weight(0.1f)
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, isPath: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = if (isPath) FontFamily.Monospace else FontFamily.Default,
            fontSize = if (isPath) 11.sp else 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
