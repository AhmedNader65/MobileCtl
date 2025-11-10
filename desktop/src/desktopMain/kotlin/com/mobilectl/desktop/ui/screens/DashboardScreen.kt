package com.mobilectl.desktop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilectl.desktop.ui.components.*
import com.mobilectl.desktop.ui.theme.GradientColors
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Hero Section
        item {
            HeroSection()
        }

        // Stats Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CloudUpload,
                    title = "Total Deploys",
                    value = statsState.totalDeploys.toString(),
                    subtitle = "All time",
                    gradientColors = GradientColors.primaryGradient
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    title = "Success Rate",
                    value = "${String.format("%.1f", statsState.successRate)}%",
                    subtitle = "Last 30 days",
                    gradientColors = GradientColors.successGradient
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Timer,
                    title = "Avg Time",
                    value = formatDuration(statsState.avgTime),
                    subtitle = "Per deployment",
                    gradientColors = GradientColors.accentGradient
                )
            }
        }

        // Quick Deploy Section
        item {
            QuickDeploySection(
                formState = formState,
                onPlatformChange = viewModel::updatePlatform,
                onFlavorChange = viewModel::updateFlavor,
                onTrackChange = viewModel::updateTrack,
                onDeploy = { viewModel.startDeploy(onNavigateToProgress) }
            )
        }

        // Recent Activity Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = { /* View all */ }) {
                    Text("View All")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Recent Deployments
        items(statsState.recentDeploys) { deploy ->
            PremiumDeployHistoryCard(deploy)
        }

        // Empty State
        if (statsState.recentDeploys.isEmpty()) {
            item {
                EmptyStateCard()
            }
        }
    }
}

@Composable
private fun HeroSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            PremiumBadge(
                text = "v1.0.0",
                gradientColors = GradientColors.primaryGradient
            )
        }
        Text(
            text = "Manage your mobile deployments with ease",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickDeploySection(
    formState: com.mobilectl.desktop.viewmodel.DeployFormState,
    onPlatformChange: (Platform) -> Unit,
    onFlavorChange: (String) -> Unit,
    onTrackChange: (String) -> Unit,
    onDeploy: () -> Unit
) {
    PremiumCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(GradientColors.primaryGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Rocket,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Quick Deploy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Deploy to multiple platforms instantly",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // Form Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Platform Selector
                var platformExpanded by remember { mutableStateOf(false) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Platform",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = platformExpanded,
                        onExpandedChange = { platformExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = formState.platform.name.lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = platformExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
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
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (platform == Platform.ANDROID) Icons.Default.Android else Icons.Default.Apple,
                                            null
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Flavor Selector
                if (formState.availableFlavors.isNotEmpty()) {
                    var flavorExpanded by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Flavor",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ExposedDropdownMenuBox(
                            expanded = flavorExpanded,
                            onExpandedChange = { flavorExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = formState.flavor,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = flavorExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
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
                }

                // Track Selector
                var trackExpanded by remember { mutableStateOf(false) }
                Column(modifier = Modifier.weight(1f)) {
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
                            value = formState.track,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = trackExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = trackExpanded,
                            onDismissRequest = { trackExpanded = false }
                        ) {
                            formState.availableTracks.forEach { track ->
                                DropdownMenuItem(
                                    text = { Text(track.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        onTrackChange(track)
                                        trackExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Deploy Button
            GradientButton(
                onClick = onDeploy,
                modifier = Modifier.fillMaxWidth(),
                enabled = !formState.isDeploying,
                gradientColors = GradientColors.primaryGradient
            ) {
                if (formState.isDeploying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Icon(Icons.Default.Rocket, null, tint = Color.White)
                Spacer(Modifier.width(12.dp))
                Text(
                    if (formState.isDeploying) "Deploying..." else "Deploy Now",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PremiumDeployHistoryCard(deploy: DeployHistoryItem) {
    var isHovered by remember { mutableStateOf(false) }

    PremiumCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = if (isHovered) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Status Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (deploy.success)
                                Brush.linearGradient(GradientColors.successGradient)
                            else
                                Brush.linearGradient(GradientColors.warningGradient)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (deploy.success) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${deploy.platform} - ${deploy.flavor}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        PremiumBadge(
                            text = deploy.track,
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatTimestamp(deploy.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDuration(deploy.duration),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            IconButton(onClick = { /* View details */ }) {
                Icon(Icons.Default.ChevronRight, "View details")
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    PremiumCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.CloudUpload,
                    null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "No deployments yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Start your first deployment to see activity here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
