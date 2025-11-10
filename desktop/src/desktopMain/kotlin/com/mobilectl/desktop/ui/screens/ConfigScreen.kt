package com.mobilectl.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilectl.desktop.ui.components.*
import com.mobilectl.desktop.ui.theme.GradientColors
import com.mobilectl.desktop.viewmodel.ConfigViewModel
import java.awt.FileDialog
import java.awt.Frame

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConfigViewModel = viewModel { ConfigViewModel() }
) {
    val state = viewModel.state

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        item {
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
                            text = "Configuration",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Manage deployment settings and credentials",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Success/Error Messages
        item {
            state.successMessage?.let { message ->
                GradientCard(
                    modifier = Modifier.fillMaxWidth(),
                    gradientColors = GradientColors.successGradient
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Text(message, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            state.error?.let { error ->
                GradientCard(
                    modifier = Modifier.fillMaxWidth(),
                    gradientColors = GradientColors.warningGradient
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Error, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Text(error, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Firebase Configuration
        item {
            PremiumConfigSection(
                title = "Firebase App Distribution",
                subtitle = "Deploy to Firebase for testing",
                icon = Icons.Default.Cloud,
                gradientColors = GradientColors.primaryGradient,
                enabled = state.firebase.enabled,
                onEnabledChange = viewModel::updateFirebaseEnabled
            ) {
                PremiumFilePickerField(
                    label = "Service Account",
                    value = state.firebase.serviceAccountPath,
                    onValueChange = viewModel::updateFirebaseServiceAccount,
                    fileExtensions = listOf("json"),
                    error = state.validationErrors["firebaseServiceAccount"],
                    placeholder = "Select JSON file"
                )

                PremiumFilePickerField(
                    label = "Google Services (Optional)",
                    value = state.firebase.googleServicesPath,
                    onValueChange = viewModel::updateFirebaseGoogleServices,
                    fileExtensions = listOf("json"),
                    placeholder = "Select google-services.json"
                )

                PremiumTextField(
                    value = state.firebase.releaseNotes,
                    onValueChange = viewModel::updateFirebaseReleaseNotes,
                    label = "Release Notes",
                    minLines = 3,
                    maxLines = 5,
                    placeholder = "Describe this release..."
                )

                PremiumTextField(
                    value = state.firebase.testGroups,
                    onValueChange = viewModel::updateFirebaseTestGroups,
                    label = "Test Groups",
                    placeholder = "qa-team, developers, beta-testers"
                )
            }
        }

        // Play Console Configuration
        item {
            PremiumConfigSection(
                title = "Google Play Console",
                subtitle = "Publish to Google Play Store",
                icon = Icons.Default.Store,
                gradientColors = GradientColors.successGradient,
                enabled = state.playConsole.enabled,
                onEnabledChange = viewModel::updatePlayConsoleEnabled
            ) {
                PremiumFilePickerField(
                    label = "Service Account",
                    value = state.playConsole.serviceAccountPath,
                    onValueChange = viewModel::updatePlayConsoleServiceAccount,
                    fileExtensions = listOf("json"),
                    error = state.validationErrors["playConsoleServiceAccount"],
                    placeholder = "Select JSON file"
                )

                PremiumTextField(
                    value = state.playConsole.packageName,
                    onValueChange = viewModel::updatePlayConsolePackageName,
                    label = "Package Name",
                    placeholder = "com.example.app",
                    error = state.validationErrors["packageName"]
                )

                var trackExpanded by remember { mutableStateOf(false) }
                Column {
                    Text(
                        text = "Track",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = trackExpanded,
                        onExpandedChange = { trackExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = state.playConsole.track.replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = trackExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = trackExpanded,
                            onDismissRequest = { trackExpanded = false }
                        ) {
                            listOf("internal", "alpha", "beta", "production").forEach { track ->
                                DropdownMenuItem(
                                    text = { Text(track.replaceFirstChar { it.uppercase() }) },
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
        }

        // Signing Configuration
        item {
            PremiumConfigSection(
                title = "App Signing",
                subtitle = "Configure keystore for signing",
                icon = Icons.Default.Security,
                gradientColors = GradientColors.accentGradient,
                enabled = true,
                onEnabledChange = {}
            ) {
                PremiumFilePickerField(
                    label = "Keystore Path",
                    value = state.signing.keystorePath,
                    onValueChange = viewModel::updateSigningKeystorePath,
                    fileExtensions = listOf("jks", "keystore"),
                    error = state.validationErrors["keystorePath"],
                    placeholder = "Select keystore file"
                )

                PremiumPasswordField(
                    label = "Store Password",
                    value = state.signing.storePassword,
                    onValueChange = viewModel::updateSigningStorePassword
                )

                PremiumTextField(
                    value = state.signing.keyAlias,
                    onValueChange = viewModel::updateSigningKeyAlias,
                    label = "Key Alias",
                    placeholder = "my-key-alias"
                )

                PremiumPasswordField(
                    label = "Key Password",
                    value = state.signing.keyPassword,
                    onValueChange = viewModel::updateSigningKeyPassword
                )
            }
        }

        // Save Button
        item {
            GradientButton(
                onClick = viewModel::saveConfig,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving && state.validationErrors.isEmpty(),
                gradientColors = GradientColors.primaryGradient
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Icon(Icons.Default.Save, null, tint = Color.White)
                Spacer(Modifier.width(12.dp))
                Text(
                    if (state.isSaving) "Saving..." else "Save Configuration",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PremiumConfigSection(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    PremiumCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(gradientColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = gradientColors.first()
                    )
                )
            }

            if (enabled) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    minLines: Int = 1,
    maxLines: Int = 1
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
            isError = error != null,
            supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            shape = RoundedCornerShape(12.dp),
            minLines = minLines,
            maxLines = maxLines,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun PremiumFilePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    fileExtensions: List<String>,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                isError = error != null,
                supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Button(
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
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FolderOpen, "Browse", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PremiumPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
    }
}
