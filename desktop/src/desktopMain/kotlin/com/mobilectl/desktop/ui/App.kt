package com.mobilectl.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilectl.desktop.ui.screens.*

enum class Screen {
    DASHBOARD,
    DEPLOY_PROGRESS,
    CONFIG,
    ARTIFACT_PREVIEW,
    CHANGELOG_EDITOR
}

data class DeploymentParams(
    val platform: String = "Android",
    val flavor: String = "production",
    val track: String = "internal"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    currentProjectPath: String? = null,
    onProjectSelected: (String) -> Unit = {}
) {
    // Show welcome screen if no project is selected
    if (currentProjectPath == null) {
        WelcomeScreen(onProjectSelected = onProjectSelected)
        return
    }

    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var deploymentParams by remember { mutableStateOf(DeploymentParams()) }

    Scaffold(
        topBar = {
            // Minimal top bar
            Surface(
                tonalElevation = 0.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    // Left side - Logo
                    Text(
                        text = "MobileCtl",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp
                    )

                    // Center - Navigation tabs
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        NavButton(
                            icon = Icons.Outlined.Dashboard,
                            label = "Dashboard",
                            selected = currentScreen == Screen.DASHBOARD,
                            onClick = { currentScreen = Screen.DASHBOARD }
                        )
                        NavButton(
                            icon = Icons.Outlined.FolderOpen,
                            label = "Artifacts",
                            selected = currentScreen == Screen.ARTIFACT_PREVIEW,
                            onClick = { currentScreen = Screen.ARTIFACT_PREVIEW }
                        )
                        NavButton(
                            icon = Icons.Outlined.Article,
                            label = "Changelog",
                            selected = currentScreen == Screen.CHANGELOG_EDITOR,
                            onClick = { currentScreen = Screen.CHANGELOG_EDITOR }
                        )
                        NavButton(
                            icon = Icons.Outlined.Settings,
                            label = "Settings",
                            selected = currentScreen == Screen.CONFIG,
                            onClick = { currentScreen = Screen.CONFIG }
                        )
                    }

                    // Right side - Theme toggle
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = "Toggle theme",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            // Animated screen transitions
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(200))
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.DASHBOARD -> DashboardScreen(
                        onNavigateToProgress = { platform, flavor, track ->
                            deploymentParams = DeploymentParams(platform, flavor, track)
                            currentScreen = Screen.DEPLOY_PROGRESS
                        },
                        onNavigateToConfig = { currentScreen = Screen.CONFIG }
                    )
                    Screen.DEPLOY_PROGRESS -> DeployProgressScreen(
                        onNavigateBack = { currentScreen = Screen.DASHBOARD },
                        platform = deploymentParams.platform,
                        flavor = deploymentParams.flavor,
                        track = deploymentParams.track
                    )
                    Screen.ARTIFACT_PREVIEW -> ArtifactPreviewScreen(
                        onNavigateBack = { currentScreen = Screen.DASHBOARD }
                    )
                    Screen.CHANGELOG_EDITOR -> ChangelogEditorScreen(
                        onNavigateBack = { currentScreen = Screen.DASHBOARD }
                    )
                    Screen.CONFIG -> ConfigScreen(
                        onNavigateBack = { currentScreen = Screen.DASHBOARD }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        modifier = Modifier.height(36.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
