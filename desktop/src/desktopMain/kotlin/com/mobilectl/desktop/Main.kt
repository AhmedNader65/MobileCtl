package com.mobilectl.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.window.*
import androidx.compose.ui.unit.dp
import com.mobilectl.desktop.ui.App
import com.mobilectl.desktop.ui.theme.MobileCtlTheme
import java.awt.Dimension

fun main() = application {
    var isDarkTheme by remember { mutableStateOf(false) }

    val windowState = rememberWindowState(
        width = 1200.dp,
        height = 800.dp,
        placement = WindowPlacement.Floating
    )

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
                onToggleTheme = { isDarkTheme = !isDarkTheme }
            )
        }
    }
}
