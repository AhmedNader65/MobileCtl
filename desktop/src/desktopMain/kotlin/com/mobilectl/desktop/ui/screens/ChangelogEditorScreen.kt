package com.mobilectl.desktop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilectl.desktop.ui.components.*
import com.mobilectl.desktop.ui.theme.AccentColors
import com.mobilectl.desktop.viewmodel.ChangelogEditorViewModel

@Composable
fun ChangelogEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChangelogEditorViewModel = viewModel { ChangelogEditorViewModel() }
) {
    val state = viewModel.state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Compact Header
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
                    text = "Changelog Editor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (state.hasUnsavedChanges) {
                    StatusBadge(
                        text = "Unsaved",
                        color = AccentColors.warning
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { viewModel.togglePreview() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (state.previewMode) Icons.Default.Edit else Icons.Default.Visibility,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                }
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

        // Success/Error Messages
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            AnimatedVisibility(visible = state.successMessage != null) {
                MinimalCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AccentColors.success,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = state.successMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearMessages() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state.error != null) {
                MinimalCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = state.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearMessages() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sidebar with generation controls
            Column(
                modifier = Modifier.width(320.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Generation Section
                SectionHeader("Generate")

                MinimalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Generate changelog from git commits",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Tag Filters
                        CompactTextField(
                            value = state.fromTag ?: "",
                            onValueChange = { viewModel.updateFromTag(it) },
                            label = "From Tag",
                            placeholder = "v1.0.0",
                            modifier = Modifier.fillMaxWidth()
                        )

                        CompactTextField(
                            value = state.toTag ?: "",
                            onValueChange = { viewModel.updateToTag(it) },
                            label = "To Tag",
                            placeholder = "v1.1.0",
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Generate Buttons
                        Button(
                            onClick = { viewModel.generateChangelog(append = false) },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            enabled = !state.isGenerating
                        ) {
                            if (state.isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (state.isGenerating) "Generating..." else "Replace",
                                fontSize = 14.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.generateChangelog(append = true) },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            enabled = !state.isGenerating
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Append", fontSize = 14.sp)
                        }
                    }
                }

                // Save/Discard Section
                SectionHeader("Actions")

                MinimalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.saveChangelog() },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            enabled = state.hasUnsavedChanges && !state.isSaving
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (state.isSaving) "Saving..." else "Save",
                                fontSize = 14.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.discardChanges() },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            enabled = state.hasUnsavedChanges
                        ) {
                            Icon(Icons.Default.Undo, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Discard", fontSize = 14.sp)
                        }
                    }
                }
            }

            // Editor/Preview
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader(if (state.previewMode) "Preview" else "Editor")

                MinimalCard(modifier = Modifier.fillMaxSize()) {
                    if (state.previewMode) {
                        // Preview Mode
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = state.content.ifEmpty { "No content to preview" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        // Editor Mode
                        OutlinedTextField(
                            value = state.content,
                            onValueChange = { viewModel.updateContent(it) },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            ),
                            placeholder = {
                                Text(
                                    "Write or generate your changelog here...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    }
}
