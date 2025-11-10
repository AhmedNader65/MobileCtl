package com.mobilectl.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilectl.desktop.ui.screens.DashboardScreen
import com.mobilectl.desktop.ui.screens.ConfigScreen
import com.mobilectl.desktop.ui.screens.DeployProgressScreen
import com.mobilectl.desktop.ui.screens.ArtifactPreviewScreen
import com.mobilectl.desktop.ui.screens.ChangelogEditorScreen

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
    onToggleTheme: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var deploymentParams by remember { mutableStateOf(DeploymentParams()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("MobileCtl")
                        Badge {
                            Text("v0.3.2")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme"
                        )
                    }

                    IconButton(onClick = { currentScreen = Screen.DASHBOARD }) {
                        Icon(Icons.Default.Dashboard, "Dashboard")
                    }

                    IconButton(onClick = { currentScreen = Screen.ARTIFACT_PREVIEW }) {
                        Icon(Icons.Default.FolderOpen, "Artifacts")
                    }

                    IconButton(onClick = { currentScreen = Screen.CHANGELOG_EDITOR }) {
                        Icon(Icons.Default.Article, "Changelog")
                    }

                    IconButton(onClick = { currentScreen = Screen.CONFIG }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            // Animated screen transitions
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
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
