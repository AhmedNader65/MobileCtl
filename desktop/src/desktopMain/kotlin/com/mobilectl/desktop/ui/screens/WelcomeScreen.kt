package com.mobilectl.desktop.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilectl.desktop.data.ProjectInfo
import com.mobilectl.desktop.data.RecentProjectsManager
import com.mobilectl.desktop.ui.theme.AccentColors
import java.io.File
import javax.swing.JFileChooser

@Composable
fun WelcomeScreen(
    onProjectSelected: (String) -> Unit
) {
    var recentProjects by remember { mutableStateOf(RecentProjectsManager.getRecentProjects()) }
    var showError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 80.dp, vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // Logo/Brand Area
            Text(
                text = "MobileCtl",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Mobile deployment automation",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(60.dp))

            // Main Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Open Project Button
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.FolderOpen,
                    title = "Open Project",
                    description = "Select a project directory",
                    onClick = {
                        val path = selectProjectDirectory()
                        if (path != null) {
                            if (isValidMobileCtlProject(path)) {
                                RecentProjectsManager.addProject(path)
                                onProjectSelected(path)
                            } else {
                                showError = "No mobilectl.yaml found in selected directory"
                            }
                        }
                    }
                )

                // Clone Project Button (Disabled for now)
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.CloudDownload,
                    title = "Clone Repository",
                    description = "Coming soon",
                    onClick = { },
                    enabled = false
                )
            }

            Spacer(Modifier.height(48.dp))

            // Recent Projects
            if (recentProjects.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Projects",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        TextButton(
                            onClick = {
                                RecentProjectsManager.clearAll()
                                recentProjects = emptyList()
                            }
                        ) {
                            Text("Clear all", fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentProjects) { project ->
                            RecentProjectItem(
                                project = project,
                                onClick = {
                                    if (File(project.path).exists()) {
                                        RecentProjectsManager.addProject(project.path)
                                        onProjectSelected(project.path)
                                    } else {
                                        showError = "Project directory no longer exists"
                                        RecentProjectsManager.removeProject(project.path)
                                        recentProjects = RecentProjectsManager.getRecentProjects()
                                    }
                                },
                                onRemove = {
                                    RecentProjectsManager.removeProject(project.path)
                                    recentProjects = RecentProjectsManager.getRecentProjects()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Error Snackbar
        AnimatedVisibility(
            visible = showError != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = showError ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { showError = null }) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            }

            LaunchedEffect(showError) {
                if (showError != null) {
                    kotlinx.coroutines.delay(5000)
                    showError = null
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier
            .height(180.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = if (enabled) 0.dp else 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun RecentProjectItem(
    project: ProjectInfo,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isHovered) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = project.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun selectProjectDirectory(): String? {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Select Project Directory"
        currentDirectory = File(System.getProperty("user.home"))
    }

    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile.absolutePath
    } else {
        null
    }
}

private fun isValidMobileCtlProject(path: String): Boolean {
    val configFile = File(path, "mobilectl.yaml")
    val altConfigFile = File(path, "mobilectl.yml")
    return configFile.exists() || altConfigFile.exists()
}
