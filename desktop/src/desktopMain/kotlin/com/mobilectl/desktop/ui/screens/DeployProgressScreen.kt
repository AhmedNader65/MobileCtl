package com.mobilectl.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilectl.desktop.ui.components.*
import com.mobilectl.desktop.ui.theme.AccentColors
import com.mobilectl.desktop.viewmodel.DeployProgressViewModel
import com.mobilectl.desktop.viewmodel.DeployStep
import com.mobilectl.desktop.viewmodel.StepStatus

@Composable
fun DeployProgressScreen(
    onNavigateBack: () -> Unit,
    platform: String = "Android",
    flavor: String = "production",
    track: String = "internal",
    useRealDeployment: Boolean = false // Set to true when ready to test real deployments
) {
    val viewModel: DeployProgressViewModel = viewModel {
        DeployProgressViewModel(
            platform = platform,
            flavor = flavor,
            track = track,
            useRealDeployment = useRealDeployment
        )
    }
    val state = viewModel.state

    // Auto-navigate back when complete
    LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            kotlinx.coroutines.delay(3000)
            onNavigateBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Compact Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Deployment Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!state.isComplete && !state.hasError) {
                    StatusBadge(
                        text = "In Progress",
                        color = AccentColors.info
                    )
                } else if (state.isComplete) {
                    StatusBadge(
                        text = "Completed",
                        color = AccentColors.success
                    )
                } else if (state.hasError) {
                    StatusBadge(
                        text = "Failed",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.canCancel) {
                    OutlinedButton(
                        onClick = viewModel::cancelDeploy,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel", fontSize = 13.sp)
                    }
                }
                if (state.isComplete || state.hasError) {
                    Button(
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Progress Steps
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader(
                    title = "Deployment Steps",
                    action = {
                        Text(
                            text = "${state.steps.count { it.status == StepStatus.COMPLETED }}/${state.steps.size} completed",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                MinimalCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        state.steps.forEachIndexed { index, step ->
                            DeployStepItem(step, index + 1)
                        }
                    }
                }
            }

            // Live Logs
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader(
                    title = "Live Logs",
                    action = {
                        Text(
                            text = "${state.logs.size} entries",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                val listState = rememberLazyListState()
                LaunchedEffect(state.logs.size) {
                    if (state.logs.isNotEmpty()) {
                        listState.animateScrollToItem(state.logs.size - 1)
                    }
                }

                MinimalCard(modifier = Modifier.fillMaxWidth().height(500.dp)) {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.logs) { log ->
                            LogItem(log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeployStepItem(step: DeployStep, stepNumber: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Step indicator
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when (step.status) {
                        StepStatus.COMPLETED -> AccentColors.success
                        StepStatus.IN_PROGRESS -> AccentColors.info
                        StepStatus.FAILED -> MaterialTheme.colorScheme.error
                        StepStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .border(
                    width = 1.dp,
                    color = when (step.status) {
                        StepStatus.PENDING -> MaterialTheme.colorScheme.outlineVariant
                        else -> androidx.compose.ui.graphics.Color.Transparent
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when (step.status) {
                StepStatus.PENDING -> {
                    Text(
                        text = "$stepNumber",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                StepStatus.IN_PROGRESS -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
                StepStatus.COMPLETED -> {
                    Icon(
                        Icons.Default.Check,
                        null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                StepStatus.FAILED -> {
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = step.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = when (step.status) {
                        StepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                if (step.status == StepStatus.IN_PROGRESS || step.status == StepStatus.COMPLETED) {
                    Text(
                        text = "${(step.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (step.status == StepStatus.IN_PROGRESS || step.status == StepStatus.COMPLETED) {
                LinearProgressIndicator(
                    progress = { step.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = when (step.status) {
                        StepStatus.COMPLETED -> AccentColors.success
                        else -> AccentColors.info
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LogItem(log: String) {
    val (prefix, color) = when {
        log.startsWith("[SUCCESS]") -> "[SUCCESS]" to AccentColors.success
        log.startsWith("[ERROR]") -> "[ERROR]" to MaterialTheme.colorScheme.error
        log.startsWith("[WARNING]") -> "[WARNING]" to AccentColors.warning
        log.startsWith("[INFO]") -> "[INFO]" to AccentColors.info
        else -> "" to null
    }

    val message = if (prefix.isNotEmpty()) {
        log.substringAfter(prefix).trim()
    } else {
        log
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (prefix.isNotEmpty() && color != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = prefix,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = color,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}
