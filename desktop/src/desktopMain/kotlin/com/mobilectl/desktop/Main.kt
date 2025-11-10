package com.mobilectl.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.mobilectl.desktop.data.AppPreferences
import com.mobilectl.desktop.data.PreferencesManager
import com.mobilectl.desktop.data.WindowPreferences
import com.mobilectl.desktop.ui.App
import com.mobilectl.desktop.ui.theme.MobileCtlTheme
import java.awt.Dimension

fun main() = application {
    // Load preferences
    val savedPrefs = remember { PreferencesManager.load() }
    var isDarkTheme by remember { mutableStateOf(savedPrefs.isDarkTheme) }
    var currentProjectPath by remember { mutableStateOf(savedPrefs.currentProjectPath) }

    // Create window state from saved preferences
    val windowState = rememberWindowState(
        width = savedPrefs.window.width.dp,
        height = savedPrefs.window.height.dp,
        position = if (savedPrefs.window.x >= 0 && savedPrefs.window.y >= 0) {
            WindowPosition(savedPrefs.window.x.dp, savedPrefs.window.y.dp)
        } else {
            WindowPosition.Aligned(Alignment.Center)
        },
        placement = if (savedPrefs.window.isMaximized) {
            WindowPlacement.Maximized
        } else {
            WindowPlacement.Floating
        }
    )

    // Save preferences on close
    DisposableEffect(Unit) {
        onDispose {
            val currentPrefs = AppPreferences(
                window = WindowPreferences(
                    width = windowState.size.width.value.toInt(),
                    height = windowState.size.height.value.toInt(),
                    x = windowState.position.x.value.toInt(),
                    y = windowState.position.y.value.toInt(),
                    isMaximized = windowState.placement == WindowPlacement.Maximized
                ),
                isDarkTheme = isDarkTheme,
                currentProjectPath = currentProjectPath
            )
            PreferencesManager.save(currentPrefs)
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "MobileCtl - Mobile Deployment Automation",
        state = windowState,
        onKeyEvent = { keyEvent ->
            when {
                // Cmd/Ctrl + Q to quit
                keyEvent.isCtrlPressed && keyEvent.key == Key.Q && keyEvent.type == KeyEventType.KeyDown -> {
                    exitApplication()
                    true
                }
                // Cmd/Ctrl + , for settings
                keyEvent.isCtrlPressed && keyEvent.key == Key.Comma && keyEvent.type == KeyEventType.KeyDown -> {
                    // Navigate to settings - handled in App
                    false
                }
                // Cmd/Ctrl + D for deploy
                keyEvent.isCtrlPressed && keyEvent.key == Key.D && keyEvent.type == KeyEventType.KeyDown -> {
                    // Deploy action - handled in App
                    false
                }
                else -> false
            }
        }
    ) {
        // Set minimum window size
        window.minimumSize = Dimension(800, 600)

        MenuBar {
            Menu("File") {
                Item("Deploy", onClick = { /* Trigger deploy */ }, shortcut = KeyShortcut(Key.D, ctrl = true))
                Item("Settings", onClick = { /* Open settings */ }, shortcut = KeyShortcut(Key.Comma, ctrl = true))
                Separator()
                Item("Exit", onClick = ::exitApplication, shortcut = KeyShortcut(Key.Q, ctrl = true))
            }
            Menu("View") {
                Item("Toggle Dark Mode", onClick = { isDarkTheme = !isDarkTheme })
                Item("Dashboard", onClick = { /* Go to dashboard */ })
            }
            Menu("Help") {
                Item("Documentation", onClick = { /* Open docs */ })
                Item("About MobileCtl", onClick = { /* Show about dialog */ })
            }
        }

        MobileCtlTheme(darkTheme = isDarkTheme) {
            App(
                isDarkTheme = isDarkTheme,
                onToggleTheme = { isDarkTheme = !isDarkTheme },
                currentProjectPath = currentProjectPath,
                onProjectSelected = { path ->
                    currentProjectPath = path
                    // Change working directory
                    System.setProperty("user.dir", path)
                }
            )
        }
    }
}
