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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilectl.desktop.ui.components.*
import com.mobilectl.desktop.viewmodel.ConfigViewModel

@Composable
fun ConfigScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConfigViewModel = viewModel { ConfigViewModel() }
) {
    val state = viewModel.state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Compact header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.hasUnsavedChanges) {
                    OutlinedButton(
                        onClick = { viewModel.discardChanges() },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Discard", fontSize = 13.sp)
                    }
                    Button(
                        onClick = { viewModel.saveConfig() },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Save Changes", fontSize = 13.sp)
                    }
                } else {
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
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Firebase Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("Firebase App Distribution")
                    Switch(
                        checked = state.firebase.enabled,
                        onCheckedChange = { viewModel.updateFirebaseEnabled(it) },
                        modifier = Modifier.height(24.dp)
                    )
                }

                if (state.firebase.enabled) {
                    MinimalCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CompactTextField(
                                value = state.firebase.serviceAccount,
                                onValueChange = { viewModel.updateFirebaseServiceAccount(it) },
                                label = "Service Account JSON",
                                placeholder = "path/to/service-account.json",
                                modifier = Modifier.fillMaxWidth()
                            )
                            CompactTextField(
                                value = state.firebase.appId,
                                onValueChange = { viewModel.updateFirebaseAppId(it) },
                                label = "App ID",
                                placeholder = "1:1234567890:android:abcdef",
                                modifier = Modifier.fillMaxWidth()
                            )
                            CompactTextField(
                                value = state.firebase.testGroups,
                                onValueChange = { viewModel.updateFirebaseTestGroups(it) },
                                label = "Test Groups (comma separated)",
                                placeholder = "testers, beta-users",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Play Console Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("Google Play Console")
                    Switch(
                        checked = state.playConsole.enabled,
                        onCheckedChange = { viewModel.updatePlayConsoleEnabled(it) },
                        modifier = Modifier.height(24.dp)
                    )
                }

                if (state.playConsole.enabled) {
                    MinimalCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CompactTextField(
                                value = state.playConsole.serviceAccount,
                                onValueChange = { viewModel.updatePlayConsoleServiceAccount(it) },
                                label = "Service Account JSON",
                                placeholder = "path/to/play-service-account.json",
                                modifier = Modifier.fillMaxWidth()
                            )
                            CompactTextField(
                                value = state.playConsole.packageName,
                                onValueChange = { viewModel.updatePlayConsolePackageName(it) },
                                label = "Package Name",
                                placeholder = "com.example.app",
                                modifier = Modifier.fillMaxWidth()
                            )
                            CompactTextField(
                                value = state.playConsole.track,
                                onValueChange = { viewModel.updatePlayConsoleTrack(it) },
                                label = "Track",
                                placeholder = "internal",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // App Signing Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("App Signing")

                MinimalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CompactTextField(
                            value = state.signing.keystorePath,
                            onValueChange = { viewModel.updateKeystorePath(it) },
                            label = "Keystore Path",
                            placeholder = "path/to/keystore.jks",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CompactTextField(
                                value = state.signing.keystorePassword,
                                onValueChange = { viewModel.updateKeystorePassword(it) },
                                label = "Keystore Password",
                                placeholder = "••••••••",
                                modifier = Modifier.weight(1f)
                            )
                            CompactTextField(
                                value = state.signing.keyAlias,
                                onValueChange = { viewModel.updateKeyAlias(it) },
                                label = "Key Alias",
                                placeholder = "key0",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
