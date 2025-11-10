package com.mobilectl.desktop.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilectl.desktop.ui.components.*
import com.mobilectl.desktop.ui.theme.AccentColors
import com.mobilectl.desktop.viewmodel.DashboardViewModel
import com.mobilectl.desktop.viewmodel.DeployHistoryItem
import com.mobilectl.model.Platform
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    onNavigateToProgress: (platform: String, flavor: String, track: String) -> Unit,
    onNavigateToConfig: () -> Unit,
    viewModel: DashboardViewModel = viewModel { DashboardViewModel() }
) {
    val formState = viewModel.formState
    val statsState = viewModel.statsState

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left side - Deploy form + Stats
        Column(
            modifier = Modifier.width(340.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Quick Deploy Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Deploy")

                MinimalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Platform selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = formState.platform == Platform.ANDROID,
                                onClick = { viewModel.updatePlatform(Platform.ANDROID) },
                                label = { Text("Android", fontSize = 13.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = formState.platform == Platform.IOS,
                                onClick = { viewModel.updatePlatform(Platform.IOS) },
                                label = { Text("iOS", fontSize = 13.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Flavor dropdown
                        CompactDropdown(
                            value = formState.flavor,
                            options = formState.availableFlavors,
                            onValueChange = { viewModel.updateFlavor(it) },
                            label = "Flavor",
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Track dropdown
                        CompactDropdown(
                            value = formState.track,
                            options = formState.availableTracks,
                            onValueChange = { viewModel.updateTrack(it) },
                            label = "Track",
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Deploy button
                        Button(
                            onClick = {
                                viewModel.startDeploy { platform, flavor, track ->
                                    onNavigateToProgress(platform, flavor, track)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.RocketLaunch,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Deploy", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Stats Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Statistics")

                MinimalCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(
                            label = "Total",
                            value = statsState.totalDeploys.toString()
                        )
                        StatItem(
                            label = "Success Rate",
                            value = "${String.format("%.0f", statsState.successRate)}%",
                            valueColor = AccentColors.success
                        )
                        StatItem(
                            label = "Avg Time",
                            value = formatDuration(statsState.avgTime)
                        )
                    }
                }
            }
        }

        // Right side - Recent Deployments
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = "Recent Deployments",
                action = {
                    TextButton(
                        onClick = { /* Refresh */ },
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Refresh", fontSize = 12.sp)
                    }
                }
            )

            if (statsState.recentDeploys.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.History,
                            null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            "No deployments yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Deployment table
                MinimalCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        // Table header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Platform",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(0.15f)
                            )
                            Text(
                                "Flavor",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(0.15f)
                            )
                            Text(
                                "Track",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(0.15f)
                            )
                            Text(
                                "Duration",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(0.15f)
                            )
                            Text(
                                "Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(0.25f)
                            )
                            Text(
                                "Status",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(0.15f)
                            )
                        }

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // Table rows
                        LazyColumn {
                            items(statsState.recentDeploys) { deploy ->
                                DeploymentRow(deploy)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeploymentRow(deploy: DeployHistoryItem) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    TableRow {
        Text(
            deploy.platform,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.15f)
        )
        Text(
            deploy.flavor,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.15f)
        )
        Text(
            deploy.track,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.15f)
        )
        Text(
            formatDuration(deploy.duration),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.15f)
        )
        Text(
            dateFormat.format(Date(deploy.timestamp)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.weight(0.25f)
        )
        StatusBadge(
            text = if (deploy.success) "Success" else "Failed",
            color = if (deploy.success) AccentColors.success else AccentColors.warning,
            modifier = Modifier.weight(0.15f)
        )
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) "${minutes}m ${secs}s" else "${secs}s"
}
