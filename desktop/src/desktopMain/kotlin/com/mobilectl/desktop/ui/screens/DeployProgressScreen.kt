package com.mobilectl.desktop.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilectl.desktop.ui.components.*
import com.mobilectl.desktop.ui.theme.GradientColors
import com.mobilectl.desktop.viewmodel.DeployProgressViewModel
import com.mobilectl.desktop.viewmodel.DeployStep
import com.mobilectl.desktop.viewmodel.StepStatus

@Composable
fun DeployProgressScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeployProgressViewModel = viewModel { DeployProgressViewModel() }
) {
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
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
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
                        text = "Deployment in Progress",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Track your deployment status in real-time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Overall progress indicator
            if (!state.isComplete && !state.hasError) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = "Processing...",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Status Banner
        if (state.isComplete || state.hasError) {
            PremiumStatusBanner(
                message = if (state.isComplete)
                    "Deployment completed successfully!"
                else
                    "Deployment failed. Please check the logs.",
                isError = state.hasError
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Progress Steps Card
            PremiumCard(
                modifier = Modifier.weight(1f),
                elevation = 2.dp
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
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
                                Icons.Default.Checklist,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Deployment Steps",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${state.steps.count { it.status == StepStatus.COMPLETED }}/${state.steps.size} completed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        state.steps.forEachIndexed { index, step ->
                            PremiumDeployStepItem(step, index + 1)
                        }
                    }
                }
            }

            // Live Logs Card
            PremiumCard(
                modifier = Modifier.weight(1f),
                elevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.height(500.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(GradientColors.accentGradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Terminal,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Live Logs",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${state.logs.size} entries",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val listState = rememberLazyListState()
                    LaunchedEffect(state.logs.size) {
                        if (state.logs.isNotEmpty()) {
                            listState.animateScrollToItem(state.logs.size - 1)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        LazyColumn(
                            modifier = Modifier.padding(16.dp),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(state.logs) { log ->
                                PremiumLogItem(log)
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.canCancel) {
                OutlinedButton(
                    onClick = viewModel::cancelDeploy,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Cancel, null)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Cancel Deployment",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (state.isComplete || state.hasError) {
                GradientButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                    gradientColors = if (state.isComplete)
                        GradientColors.successGradient
                    else
                        GradientColors.primaryGradient
                ) {
                    Icon(
                        if (state.isComplete) Icons.Default.CheckCircle else Icons.Default.ArrowBack,
                        null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (state.isComplete) "Done" else "Back to Dashboard",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumStatusBanner(
    message: String,
    isError: Boolean
) {
    GradientCard(
        modifier = Modifier.fillMaxWidth(),
        gradientColors = if (isError) GradientColors.warningGradient else GradientColors.successGradient
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun PremiumDeployStepItem(step: DeployStep, stepNumber: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Step number/status indicator
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when (step.status) {
                        StepStatus.COMPLETED -> Brush.linearGradient(GradientColors.successGradient)
                        StepStatus.IN_PROGRESS -> Brush.linearGradient(GradientColors.primaryGradient)
                        StepStatus.FAILED -> Brush.linearGradient(GradientColors.warningGradient)
                        StepStatus.PENDING -> Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (step.status) {
                StepStatus.PENDING -> {
                    Text(
                        text = "$stepNumber",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                StepStatus.IN_PROGRESS -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
                StepStatus.COMPLETED -> {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                StepStatus.FAILED -> {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = step.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when (step.status) {
                        StepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                if (step.status == StepStatus.IN_PROGRESS || step.status == StepStatus.COMPLETED) {
                    Text(
                        text = "${(step.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (step.status == StepStatus.IN_PROGRESS || step.status == StepStatus.COMPLETED) {
                GradientProgressBar(
                    progress = step.progress,
                    modifier = Modifier.fillMaxWidth(),
                    gradientColors = when (step.status) {
                        StepStatus.COMPLETED -> GradientColors.successGradient
                        else -> GradientColors.primaryGradient
                    },
                    height = 6.dp
                )
            }
        }
    }
}

@Composable
private fun PremiumLogItem(log: String) {
    val (prefix, gradientColors) = when {
        log.startsWith("[SUCCESS]") -> "[SUCCESS]" to GradientColors.successGradient
        log.startsWith("[ERROR]") -> "[ERROR]" to GradientColors.warningGradient
        log.startsWith("[WARNING]") -> "[WARNING]" to listOf(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.tertiary
        )
        log.startsWith("[INFO]") -> "[INFO]" to GradientColors.primaryGradient
        else -> "" to null
    }

    val message = if (prefix.isNotEmpty()) {
        log.substringAfter(prefix).trim()
    } else {
        log
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (prefix.isNotEmpty() && gradientColors != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .background(Brush.linearGradient(gradientColors))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = prefix,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}
