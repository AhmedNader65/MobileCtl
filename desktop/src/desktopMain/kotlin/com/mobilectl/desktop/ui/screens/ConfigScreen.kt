package com.mobilectl.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilectl.desktop.viewmodel.ConfigViewModel
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConfigViewModel = viewModel { ConfigViewModel() }
) {
    val state = viewModel.state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuration") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success/Error Messages
            item {
                state.successMessage?.let { message ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            Text(message, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                state.error?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // Firebase Configuration
            item {
                ConfigSection(
                    title = "Firebase App Distribution",
                    icon = Icons.Default.Cloud,
                    enabled = state.firebase.enabled,
                    onEnabledChange = viewModel::updateFirebaseEnabled
                ) {
                    FilePickerField(
                        label = "Service Account",
                        value = state.firebase.serviceAccountPath,
                        onValueChange = viewModel::updateFirebaseServiceAccount,
                        fileExtensions = listOf("json"),
                        error = state.validationErrors["firebaseServiceAccount"]
                    )

                    FilePickerField(
                        label = "Google Services (Optional)",
                        value = state.firebase.googleServicesPath,
                        onValueChange = viewModel::updateFirebaseGoogleServices,
                        fileExtensions = listOf("json")
                    )

                    OutlinedTextField(
                        value = state.firebase.releaseNotes,
                        onValueChange = viewModel::updateFirebaseReleaseNotes,
                        label = { Text("Release Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    OutlinedTextField(
                        value = state.firebase.testGroups,
                        onValueChange = viewModel::updateFirebaseTestGroups,
                        label = { Text("Test Groups (comma-separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("qa-team, developers, beta-testers") }
                    )
                }
            }

            // Play Console Configuration
            item {
                ConfigSection(
                    title = "Google Play Console",
                    icon = Icons.Default.Store,
                    enabled = state.playConsole.enabled,
                    onEnabledChange = viewModel::updatePlayConsoleEnabled
                ) {
                    FilePickerField(
                        label = "Service Account",
                        value = state.playConsole.serviceAccountPath,
                        onValueChange = viewModel::updatePlayConsoleServiceAccount,
                        fileExtensions = listOf("json"),
                        error = state.validationErrors["playConsoleServiceAccount"]
                    )

                    OutlinedTextField(
                        value = state.playConsole.packageName,
                        onValueChange = viewModel::updatePlayConsolePackageName,
                        label = { Text("Package Name") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("com.example.app") },
                        isError = state.validationErrors.containsKey("packageName"),
                        supportingText = {
                            state.validationErrors["packageName"]?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )

                    var trackExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = trackExpanded,
                        onExpandedChange = { trackExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = state.playConsole.track,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Track") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = trackExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = trackExpanded,
                            onDismissRequest = { trackExpanded = false }
                        ) {
                            listOf("internal", "alpha", "beta", "production").forEach { track ->
                                DropdownMenuItem(
                                    text = { Text(track) },
                                    onClick = {
                                        viewModel.updatePlayConsoleTrack(track)
                                        trackExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Signing Configuration
            item {
                ConfigSection(
                    title = "App Signing",
                    icon = Icons.Default.Security,
                    enabled = true,
                    onEnabledChange = {}
                ) {
                    FilePickerField(
                        label = "Keystore Path",
                        value = state.signing.keystorePath,
                        onValueChange = viewModel::updateSigningKeystorePath,
                        fileExtensions = listOf("jks", "keystore"),
                        error = state.validationErrors["keystorePath"]
                    )

                    PasswordTextField(
                        label = "Store Password",
                        value = state.signing.storePassword,
                        onValueChange = viewModel::updateSigningStorePassword
                    )

                    OutlinedTextField(
                        value = state.signing.keyAlias,
                        onValueChange = viewModel::updateSigningKeyAlias,
                        label = { Text("Key Alias") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    PasswordTextField(
                        label = "Key Password",
                        value = state.signing.keyPassword,
                        onValueChange = viewModel::updateSigningKeyPassword
                    )
                }
            }

            // Save Button
            item {
                Button(
                    onClick = viewModel::saveConfig,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving && state.validationErrors.isEmpty()
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isSaving) "Saving..." else "Save Configuration")
                }
            }
        }
    }
}

@Composable
private fun ConfigSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            if (enabled) {
                content()
            }
        }
    }
}

@Composable
private fun FilePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    fileExtensions: List<String>,
    error: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            isError = error != null,
            supportingText = {
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            },
            singleLine = true
        )

        IconButton(
            onClick = {
                val dialog = FileDialog(null as Frame?, "Choose $label", FileDialog.LOAD)
                dialog.setFilenameFilter { _, name ->
                    fileExtensions.any { ext -> name.endsWith(".$ext") }
                }
                dialog.isVisible = true
                val selectedFile = dialog.file
                val selectedDir = dialog.directory
                if (selectedFile != null && selectedDir != null) {
                    onValueChange("$selectedDir$selectedFile")
                }
            }
        ) {
            Icon(Icons.Default.FolderOpen, "Browse")
        }
    }
}

@Composable
private fun PasswordTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                )
            }
        },
        singleLine = true
    )
}
