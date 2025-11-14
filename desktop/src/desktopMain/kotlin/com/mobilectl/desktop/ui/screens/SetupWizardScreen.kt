package com.mobilectl.desktop.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilectl.desktop.ui.components.*
import com.mobilectl.desktop.ui.theme.AccentColors
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SetupWizardScreen(
    projectPath: String,
    onSetupComplete: () -> Unit,
    onCancel: () -> Unit
) {
    var projectName by remember { mutableStateOf(File(projectPath).name) }
    var packageName by remember { mutableStateOf("com.example.app") }
    var enableFirebase by remember { mutableStateOf(false) }
    var enablePlayConsole by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }

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
            Column {
                Text(
                    text = "Setup New Project",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = projectPath,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = onCancel,
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cancel", fontSize = 13.sp)
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // Success Message
        if (showSuccess) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AccentColors.success.copy(alpha = 0.1f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = AccentColors.success,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Configuration file created successfully!",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    color = AccentColors.success
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Info Section
            MinimalCard {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = AccentColors.info,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "No configuration file found",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Let's create a mobileops.yaml configuration file for this project",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Project Details
            SectionHeader("Project Details")

            MinimalCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CompactTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = "Project Name",
                        placeholder = "My Project",
                        modifier = Modifier.fillMaxWidth()
                    )

                    CompactTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = "Package Name (Android/iOS)",
                        placeholder = "com.example.app",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Distribution Options
            SectionHeader("Distribution Platforms")

            MinimalCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Firebase App Distribution",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Beta testing and internal distribution",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableFirebase,
                            onCheckedChange = { enableFirebase = it },
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Google Play Console",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Production releases to Play Store",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enablePlayConsole,
                            onCheckedChange = { enablePlayConsole = it },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            // Info about editing later
            MinimalCard {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "You can edit these settings later in the Configuration screen",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(36.dp),
                    enabled = !isCreating
                ) {
                    Text("Cancel", fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        isCreating = true
                        createConfigFile(
                            projectPath = projectPath,
                            projectName = projectName,
                            packageName = packageName,
                            enableFirebase = enableFirebase,
                            enablePlayConsole = enablePlayConsole
                        )
                        showSuccess = true
                        isCreating = false
                        // Wait a moment then complete
                        kotlinx.coroutines.GlobalScope.launch {
                            kotlinx.coroutines.delay(1500)
                            onSetupComplete()
                        }
                    },
                    modifier = Modifier.weight(1f).height(36.dp),
                    enabled = !isCreating && projectName.isNotBlank() && packageName.isNotBlank()
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (isCreating) "Creating..." else "Create Configuration",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun createConfigFile(
    projectPath: String,
    projectName: String,
    packageName: String,
    enableFirebase: Boolean,
    enablePlayConsole: Boolean
) {
    val configContent = buildString {
        appendLine("# MobileOps Configuration")
        appendLine("# Generated by MobileCtl")
        appendLine()
        appendLine("project:")
        appendLine("  name: $projectName")
        appendLine()
        appendLine("android:")
        appendLine("  packageName: $packageName")
        appendLine()
        appendLine("deploy:")
        appendLine("  android:")
        if (enableFirebase) {
            appendLine("    firebase:")
            appendLine("      enabled: true")
            appendLine("      serviceAccount: credentials/firebase-service-account.json")
            appendLine("      releaseNotes: \"Automated build\"")
            appendLine("      testGroups:")
            appendLine("        - testers")
        }
        if (enablePlayConsole) {
            appendLine("    playConsole:")
            appendLine("      enabled: true")
            appendLine("      serviceAccount: credentials/play-console-service-account.json")
            appendLine("      packageName: $packageName")
            appendLine("      track: internal")
            appendLine("      status: draft")
        }
    }

    try {
        val configFile = File(projectPath, "mobileops.yaml")
        configFile.writeText(configContent)
    } catch (e: Exception) {
        println("Failed to create config file: ${e.message}")
    }
}
