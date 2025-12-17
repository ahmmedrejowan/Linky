package com.rejowan.linky.presentation.feature.settings.healthcheck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

/**
 * Link Health Check Screen
 * Validates all saved links and shows their health status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkHealthCheckScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LinkHealthCheckViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteBrokenDialog by remember { mutableStateOf(false) }

    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is HealthCheckUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Link Health Check",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Control header
            HealthCheckHeader(
                state = state,
                onStartCheck = { viewModel.onEvent(HealthCheckEvent.OnStartHealthCheck) },
                onCancelCheck = { viewModel.onEvent(HealthCheckEvent.OnCancelHealthCheck) },
                onDeleteAllBroken = { showDeleteBrokenDialog = true }
            )

            HorizontalDivider()

            // Content
            when {
                state.isChecking -> {
                    CheckingProgress(
                        progress = state.progress,
                        checkedCount = state.checkedCount,
                        totalLinks = state.totalLinks
                    )

                    // Show results as they come in
                    if (state.healthResults.isNotEmpty()) {
                        ResultsList(
                            results = state.filteredResults,
                            filterStatus = state.filterStatus,
                            onFilterChange = { viewModel.onEvent(HealthCheckEvent.OnFilterByStatus(it)) },
                            onDeleteLink = { viewModel.onEvent(HealthCheckEvent.OnDeleteLink(it)) }
                        )
                    }
                }
                state.healthResults.isEmpty() && !state.isChecking -> {
                    NoResultsContent(
                        onStartCheck = { viewModel.onEvent(HealthCheckEvent.OnStartHealthCheck) }
                    )
                }
                else -> {
                    ResultsList(
                        results = state.filteredResults,
                        filterStatus = state.filterStatus,
                        onFilterChange = { viewModel.onEvent(HealthCheckEvent.OnFilterByStatus(it)) },
                        onDeleteLink = { viewModel.onEvent(HealthCheckEvent.OnDeleteLink(it)) }
                    )
                }
            }
        }
    }

    // Delete all broken dialog
    if (showDeleteBrokenDialog) {
        DeleteBrokenDialog(
            brokenCount = state.brokenCount,
            onConfirm = {
                viewModel.onEvent(HealthCheckEvent.OnDeleteAllBroken)
                showDeleteBrokenDialog = false
            },
            onDismiss = { showDeleteBrokenDialog = false }
        )
    }
}

@Composable
private fun HealthCheckHeader(
    state: LinkHealthCheckState,
    onStartCheck: () -> Unit,
    onCancelCheck: () -> Unit,
    onDeleteAllBroken: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary stats (when available)
            if (state.healthResults.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        count = state.healthyCount,
                        label = "Healthy",
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatItem(
                        count = state.slowCount,
                        label = "Slow",
                        color = Color(0xFFFFA000)
                    )
                    StatItem(
                        count = state.brokenCount,
                        label = "Broken",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.isChecking) {
                    OutlinedButton(
                        onClick = onCancelCheck,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel")
                    }
                } else {
                    Button(
                        onClick = onStartCheck,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (state.healthResults.isEmpty()) "Start Health Check" else "Recheck")
                    }
                }

                if (state.brokenCount > 0 && !state.isChecking) {
                    Button(
                        onClick = onDeleteAllBroken,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = !state.isDeleting
                    ) {
                        if (state.isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onError,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Broken")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CheckingProgress(
    progress: Float,
    checkedCount: Int,
    totalLinks: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Text(
            text = "Checking link $checkedCount of $totalLinks...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NoResultsContent(
    onStartCheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "No health check performed yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Start a health check to validate your saved links",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onStartCheck) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Health Check")
            }
        }
    }
}

@Composable
private fun ResultsList(
    results: List<LinkHealthResult>,
    filterStatus: LinkHealthStatus?,
    onFilterChange: (LinkHealthStatus?) -> Unit,
    onDeleteLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Filter chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { onFilterChange(null) },
                    label = { Text("All") }
                )
            }
            items(LinkHealthStatus.entries) { status ->
                FilterChip(
                    selected = filterStatus == status,
                    onClick = { onFilterChange(status) },
                    label = { Text(status.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = getStatusColor(status).copy(alpha = 0.2f)
                    )
                )
            }
        }

        // Results
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results, key = { it.link.id }) { result ->
                HealthResultItem(
                    result = result,
                    onDelete = { onDeleteLink(result.link.id) }
                )
            }
        }
    }
}

@Composable
private fun HealthResultItem(
    result: LinkHealthResult,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status icon
            Icon(
                imageVector = getStatusIcon(result.status),
                contentDescription = result.status.displayName,
                tint = getStatusColor(result.status),
                modifier = Modifier.size(24.dp)
            )

            // Link info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.link.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = result.link.url.shortenUrl(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = result.status.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = getStatusColor(result.status)
                )
            }

            // Delete button (only for broken/unreachable)
            if (result.status in listOf(LinkHealthStatus.BROKEN, LinkHealthStatus.UNREACHABLE)) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteBrokenDialog(
    brokenCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Broken Links?",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "This will move $brokenCount broken links to trash. " +
                        "You can restore them from trash if needed.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

@Composable
private fun getStatusIcon(status: LinkHealthStatus): ImageVector {
    return when (status) {
        LinkHealthStatus.HEALTHY -> Icons.Default.CheckCircle
        LinkHealthStatus.SLOW -> Icons.Default.Speed
        LinkHealthStatus.BROKEN -> Icons.Default.Error
        LinkHealthStatus.UNREACHABLE -> Icons.Default.Warning
        LinkHealthStatus.SSL_ERROR -> Icons.Default.Security
        LinkHealthStatus.UNKNOWN -> Icons.Default.Warning
    }
}

@Composable
private fun getStatusColor(status: LinkHealthStatus): Color {
    return when (status) {
        LinkHealthStatus.HEALTHY -> MaterialTheme.colorScheme.primary
        LinkHealthStatus.SLOW -> Color(0xFFFFA000)
        LinkHealthStatus.BROKEN -> MaterialTheme.colorScheme.error
        LinkHealthStatus.UNREACHABLE -> MaterialTheme.colorScheme.error
        LinkHealthStatus.SSL_ERROR -> Color(0xFFFF5722)
        LinkHealthStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun String.shortenUrl(): String {
    return this
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .take(50)
        .let { if (this.length > 50) "$it..." else it }
}
