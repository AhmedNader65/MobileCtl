package com.mobilectl.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilectl.desktop.viewmodel.DashboardViewModel
import com.mobilectl.desktop.viewmodel.DeployHistoryItem
import com.mobilectl.model.Platform
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    onNavigateToProgress: () -> Unit,
    onNavigateToConfig: () -> Unit,
    viewModel: DashboardViewModel = viewModel { DashboardViewModel() }
) {
    val formState = viewModel.formState
    val statsState = viewModel.statsState

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineLarge
            )
        }

        // Stats Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CloudUpload,
                    title = "Total Deploys",
                    value = statsState.totalDeploys.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                StatsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    title = "Success Rate",
                    value = "${String.format("%.1f", statsState.successRate)}%",
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Timer,
                    title = "Avg Time",
                    value = formatDuration(statsState.avgTime),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Quick Deploy Form
        item {
            DeployFormCard(
                formState = formState,
                onPlatformChange = viewModel::updatePlatform,
                onFlavorChange = viewModel::updateFlavor,
                onTrackChange = viewModel::updateTrack,
                onDeploy = { viewModel.startDeploy(onNavigateToProgress) }
            )
        }

        // Recent Deployments
        item {
            Text(
                text = "Recent Deployments",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(statsState.recentDeploys) { deploy ->
            DeployHistoryCard(deploy)
        }
    }

    // Error Snackbar
    formState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error in snackbar
            viewModel.clearError()
        }
    }
}

@Composable
private fun StatsCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeployFormCard(
    formState: com.mobilectl.desktop.viewmodel.DeployFormState,
    onPlatformChange: (Platform) -> Unit,
    onFlavorChange: (String) -> Unit,
    onTrackChange: (String) -> Unit,
    onDeploy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Quick Deploy",
                style = MaterialTheme.typography.titleLarge
            )

            // Platform Selector
            var platformExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = platformExpanded,
                onExpandedChange = { platformExpanded = it }
            ) {
                OutlinedTextField(
                    value = formState.platform.name.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Platform") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = platformExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = platformExpanded,
                    onDismissRequest = { platformExpanded = false }
                ) {
                    Platform.entries.forEach { platform ->
                        DropdownMenuItem(
                            text = { Text(platform.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onPlatformChange(platform)
                                platformExpanded = false
                            }
                        )
                    }
                }
            }

            // Flavor Selector
            if (formState.availableFlavors.isNotEmpty()) {
                var flavorExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = flavorExpanded,
                    onExpandedChange = { flavorExpanded = it }
                ) {
                    OutlinedTextField(
                        value = formState.flavor,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Flavor") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = flavorExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = flavorExpanded,
                        onDismissRequest = { flavorExpanded = false }
                    ) {
                        formState.availableFlavors.forEach { flavor ->
                            DropdownMenuItem(
                                text = { Text(flavor) },
                                onClick = {
                                    onFlavorChange(flavor)
                                    flavorExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Track Selector
            var trackExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = trackExpanded,
                onExpandedChange = { trackExpanded = it }
            ) {
                OutlinedTextField(
                    value = formState.track,
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
                    formState.availableTracks.forEach { track ->
                        DropdownMenuItem(
                            text = { Text(track) },
                            onClick = {
                                onTrackChange(track)
                                trackExpanded = false
                            }
                        )
                    }
                }
            }

            // Deploy Button
            Button(
                onClick = onDeploy,
                modifier = Modifier.fillMaxWidth(),
                enabled = !formState.isDeploying
            ) {
                if (formState.isDeploying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Default.CloudUpload, null)
                Spacer(Modifier.width(8.dp))
                Text(if (formState.isDeploying) "Deploying..." else "Deploy Now")
            }
        }
    }
}

@Composable
private fun DeployHistoryCard(deploy: DeployHistoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (deploy.success) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (deploy.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Column {
                    Text(
                        text = "${deploy.platform} - ${deploy.flavor}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${deploy.track} • ${formatTimestamp(deploy.timestamp)} • ${formatDuration(deploy.duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        "${minutes}m ${remainingSeconds}s"
    } else {
        "${remainingSeconds}s"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
