package com.mobilectl.desktop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilectl.desktop.ui.components.*
import com.mobilectl.desktop.ui.theme.GradientColors
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
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                Column {
                    Text(
                        text = "Changelog Editor",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.changelogPath,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.hasUnsavedChanges) {
                            PremiumBadge(
                                text = "Unsaved",
                                gradientColors = GradientColors.warningGradient
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Preview Toggle
                IconButton(
                    onClick = { viewModel.togglePreview() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.previewMode) {
                                Brush.linearGradient(GradientColors.primaryGradient)
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        )
                ) {
                    Icon(
                        if (state.previewMode) Icons.Default.Edit else Icons.Default.Visibility,
                        "Toggle Preview",
                        tint = if (state.previewMode) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Refresh
                IconButton(
                    onClick = { viewModel.refresh() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Refresh, "Refresh")
                }
            }
        }

        // Success/Error Messages
        AnimatedVisibility(visible = state.successMessage != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = state.successMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearMessages() }) {
                        Icon(Icons.Default.Close, "Dismiss")
                    }
                }
            }
        }

        AnimatedVisibility(visible = state.error != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = state.error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearMessages() }) {
                        Icon(Icons.Default.Close, "Dismiss")
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Sidebar with generation controls
            PremiumCard(
                modifier = Modifier.width(350.dp),
                elevation = 2.dp
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Generation Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(GradientColors.primaryGradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Generate",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Text(
                        text = "Generate changelog from git commits",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Tag Filters
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.fromTag ?: "",
                            onValueChange = { viewModel.updateFromTag(it) },
                            label = { Text("From Tag (optional)") },
                            placeholder = { Text("e.g., v1.0.0") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(Icons.Default.Tag, null)
                            }
                        )

                        OutlinedTextField(
                            value = state.toTag ?: "",
                            onValueChange = { viewModel.updateToTag(it) },
                            label = { Text("To Tag (optional)") },
                            placeholder = { Text("e.g., v1.1.0") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(Icons.Default.Tag, null)
                            }
                        )
                    }

                    // Generate Buttons
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        GradientButton(
                            onClick = { viewModel.generateChangelog(append = false) },
                            modifier = Modifier.fillMaxWidth(),
                            gradientColors = GradientColors.primaryGradient,
                            enabled = !state.isGenerating
                        ) {
                            if (state.isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Default.AutoAwesome, null, tint = Color.White)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (state.isGenerating) "Generating..." else "Generate (Replace)",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.generateChangelog(append = true) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !state.isGenerating
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Append to Existing",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Save/Discard Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(GradientColors.successGradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Save,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Actions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    GradientButton(
                        onClick = { viewModel.saveChangelog() },
                        modifier = Modifier.fillMaxWidth(),
                        gradientColors = GradientColors.successGradient,
                        enabled = state.hasUnsavedChanges && !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Default.Save, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (state.isSaving) "Saving..." else "Save Changes",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.discardChanges() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = state.hasUnsavedChanges
                    ) {
                        Icon(Icons.Default.Undo, null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Discard Changes",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Editor/Preview
            PremiumCard(
                modifier = Modifier.weight(1f),
                elevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(GradientColors.accentGradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (state.previewMode) Icons.Default.Visibility else Icons.Default.Edit,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = if (state.previewMode) "Preview" else "Editor",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        if (state.previewMode) {
                            // Preview Mode (simple text display - markdown rendering would require a library)
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = state.content.ifEmpty { "No content to preview" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            // Editor Mode
                            OutlinedTextField(
                                value = state.content,
                                onValueChange = { viewModel.updateContent(it) },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                placeholder = {
                                    Text(
                                        "Write or generate your changelog here...",
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
}
