package com.mobilectl.desktop.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilectl.desktop.viewmodel.DeployProgressViewModel
import com.mobilectl.desktop.viewmodel.DeployStep
import com.mobilectl.desktop.viewmodel.StepStatus

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deploy Progress") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Banner
            if (state.isComplete) {
                StatusBanner(
                    message = "Deployment completed successfully!",
                    isError = false
                )
            } else if (state.hasError) {
                StatusBanner(
                    message = "Deployment failed",
                    isError = true
                )
            }

            // Progress Steps
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Deployment Steps",
                        style = MaterialTheme.typography.titleLarge
                    )

                    state.steps.forEachIndexed { index, step ->
                        DeployStepItem(step)
                    }
                }
            }

            // Live Logs
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Live Logs",
                        style = MaterialTheme.typography.titleLarge
                    )

                    val listState = rememberLazyListState()
                    LaunchedEffect(state.logs.size) {
                        if (state.logs.isNotEmpty()) {
                            listState.animateScrollToItem(state.logs.size - 1)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(8.dp),
                        state = listState
                    ) {
                        items(state.logs) { log ->
                            LogItem(log)
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.canCancel) {
                    OutlinedButton(
                        onClick = viewModel::cancelDeploy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Cancel, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel")
                    }
                }

                if (state.isComplete || state.hasError) {
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Done, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(
    message: String,
    isError: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        }
    }
}

@Composable
private fun DeployStepItem(step: DeployStep) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                when (step.status) {
                    StepStatus.PENDING -> {
                        Icon(
                            Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StepStatus.IN_PROGRESS -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    StepStatus.COMPLETED -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    StepStatus.FAILED -> {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Text(
                    text = step.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when (step.status) {
                        StepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        StepStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                        StepStatus.COMPLETED -> MaterialTheme.colorScheme.onSurface
                        StepStatus.FAILED -> MaterialTheme.colorScheme.error
                    }
                )
            }

            if (step.status == StepStatus.IN_PROGRESS || step.status == StepStatus.COMPLETED) {
                Text(
                    text = "${(step.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (step.status == StepStatus.IN_PROGRESS || step.status == StepStatus.COMPLETED) {
            AnimatedLinearProgressIndicator(
                progress = step.progress,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AnimatedLinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "progress"
    )

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier.height(8.dp).clip(RoundedCornerShape(4.dp)),
    )
}

@Composable
private fun LogItem(log: String) {
    val (prefix, color) = when {
        log.startsWith("[SUCCESS]") -> "[SUCCESS]" to MaterialTheme.colorScheme.primary
        log.startsWith("[ERROR]") -> "[ERROR]" to MaterialTheme.colorScheme.error
        log.startsWith("[WARNING]") -> "[WARNING]" to MaterialTheme.colorScheme.tertiary
        log.startsWith("[INFO]") -> "[INFO]" to MaterialTheme.colorScheme.onSurfaceVariant
        else -> "" to MaterialTheme.colorScheme.onSurface
    }

    val message = if (prefix.isNotEmpty()) {
        log.substringAfter(prefix).trim()
    } else {
        log
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (prefix.isNotEmpty()) {
            Text(
                text = prefix,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}
