package com.rejowan.linky.presentation.feature.settings.data_storage

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rejowan.linky.data.export.ImportConflictStrategy
import com.rejowan.linky.presentation.feature.settings.ExportUiState
import com.rejowan.linky.presentation.feature.settings.ImportUiState
import com.rejowan.linky.presentation.feature.settings.SettingsEvent
import com.rejowan.linky.presentation.feature.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data & Storage Screen
 * Shows storage usage, trash management, and data export/import options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataStorageScreen(
    onNavigateToTrash: () -> Unit,
    onNavigateToDuplicateDetection: () -> Unit,
    onNavigateToHealthCheck: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var includeSnapshots by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }

    // File picker for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.onEvent(SettingsEvent.OnExportData(it, includeSnapshots))
        }
    }

    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingImportUri = it
            viewModel.onEvent(SettingsEvent.OnPreviewImport(it))
        }
    }

    // Show error in Snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    // Handle export success
    LaunchedEffect(state.exportState) {
        when (val exportState = state.exportState) {
            is ExportUiState.Success -> {
                snackbarHostState.showSnackbar(
                    "Exported ${exportState.summary.linksCount} links (${exportState.summary.fileSize})"
                )
                viewModel.onEvent(SettingsEvent.OnDismissExportResult)
            }
            is ExportUiState.Error -> {
                snackbarHostState.showSnackbar("Export failed: ${exportState.message}")
                viewModel.onEvent(SettingsEvent.OnDismissExportResult)
            }
            else -> {}
        }
    }

    // Handle import success
    LaunchedEffect(state.importState) {
        when (val importState = state.importState) {
            is ImportUiState.Preview -> {
                showImportConfirmDialog = true
            }
            is ImportUiState.Success -> {
                snackbarHostState.showSnackbar(
                    "Imported ${importState.summary.linksImported} links, ${importState.summary.collectionsImported} collections"
                )
                viewModel.onEvent(SettingsEvent.OnDismissImportResult)
                pendingImportUri = null
            }
            is ImportUiState.Error -> {
                snackbarHostState.showSnackbar("Import failed: ${importState.message}")
                viewModel.onEvent(SettingsEvent.OnDismissImportResult)
                pendingImportUri = null
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Data & Storage",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Overview Section
            SectionHeader(text = "Storage Overview")
            StorageOverviewCard(
                totalStorageUsed = state.totalStorageUsed,
                totalLinks = state.totalLinks
            )

            HorizontalDivider()

            // Trash Management Section
            SectionHeader(text = "Trash")
            TrashCard(
                totalTrashedLinks = state.totalTrashedLinks,
                onViewTrashClick = onNavigateToTrash
            )

            HorizontalDivider()

            // Cache Management Section
            SectionHeader(text = "Cache")
            CacheCard(
                isLoading = state.isLoading,
                onClearCacheClick = { showClearCacheDialog = true }
            )

            HorizontalDivider()

            // Duplicate Detection Section
            SectionHeader(text = "Duplicate Detection")
            DuplicateDetectionCard(
                onFindDuplicatesClick = onNavigateToDuplicateDetection
            )

            HorizontalDivider()

            // Link Health Check Section
            SectionHeader(text = "Link Health Check")
            HealthCheckCard(
                onHealthCheckClick = onNavigateToHealthCheck
            )

            HorizontalDivider()

            // Backup & Restore Section
            SectionHeader(text = "Backup & Restore")
            BackupRestoreCard(
                exportState = state.exportState,
                importState = state.importState,
                onExportClick = { showExportOptionsDialog = true },
                onImportClick = { importLauncher.launch(arrayOf("application/json")) }
            )

            HorizontalDivider()

            // Danger Zone Section
            SectionHeader(text = "Danger Zone")
            DangerZoneCard()
        }
    }

    // Clear Cache Confirmation Dialog
    if (showClearCacheDialog) {
        ClearCacheDialog(
            onConfirm = {
                viewModel.onEvent(SettingsEvent.OnClearCache)
                showClearCacheDialog = false
            },
            onDismiss = { showClearCacheDialog = false }
        )
    }

    // Export Options Dialog
    if (showExportOptionsDialog) {
        ExportOptionsDialog(
            includeSnapshots = includeSnapshots,
            onIncludeSnapshotsChange = { includeSnapshots = it },
            onConfirm = {
                showExportOptionsDialog = false
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val fileName = "linky-backup-${dateFormat.format(Date())}.json"
                exportLauncher.launch(fileName)
            },
            onDismiss = { showExportOptionsDialog = false }
        )
    }

    // Import Confirm Dialog
    if (showImportConfirmDialog && state.importState is ImportUiState.Preview) {
        val preview = (state.importState as ImportUiState.Preview).preview
        ImportConfirmDialog(
            linksCount = preview.totalLinks,
            collectionsCount = preview.totalCollections,
            hasSnapshots = preview.hasSnapshots,
            onConfirm = { strategy ->
                showImportConfirmDialog = false
                pendingImportUri?.let { uri ->
                    viewModel.onEvent(SettingsEvent.OnImportData(uri, strategy))
                }
            },
            onDismiss = {
                showImportConfirmDialog = false
                viewModel.onEvent(SettingsEvent.OnDismissImportResult)
                pendingImportUri = null
            }
        )
    }
}

@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}

@Composable
private fun StorageOverviewCard(
    totalStorageUsed: String,
    totalLinks: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
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
                Text(
                    text = "Total Used",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = totalStorageUsed,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { 0.45f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Breakdown",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            StorageBreakdownRow(label = "Links", value = "$totalLinks items")
            StorageBreakdownRow(label = "Previews", value = "~${(totalLinks * 0.08).toInt()} MB")
            StorageBreakdownRow(label = "Snapshots", value = "Coming soon")
        }
    }
}

@Composable
private fun StorageBreakdownRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "• $label",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TrashCard(
    totalTrashedLinks: Int,
    onViewTrashClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
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
                Column {
                    Text(
                        text = "Deleted Items",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$totalTrashedLinks ${if (totalTrashedLinks == 1) "item" else "items"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Deleted links can be restored or permanently removed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto-delete after",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "30 days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedButton(
                onClick = onViewTrashClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = totalTrashedLinks > 0
            ) {
                Text("Manage Trash")
            }
        }
    }
}

@Composable
private fun CacheCard(
    isLoading: Boolean,
    onClearCacheClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Preview Cache",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Clear cached preview images to free up space. Images will be downloaded again when needed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onClearCacheClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("Clear Cache")
            }
        }
    }
}

@Composable
private fun DuplicateDetectionCard(
    onFindDuplicatesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Find Duplicate Links",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Scan your links to find and remove duplicates. Duplicates are identified by URL, ignoring tracking parameters.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onFindDuplicatesClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Find Duplicates")
            }
        }
    }
}

@Composable
private fun HealthCheckCard(
    onHealthCheckClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Check Link Health",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Validate your saved links to find broken URLs, slow-loading pages, or SSL errors.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onHealthCheckClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run Health Check")
            }
        }
    }
}

@Composable
private fun BackupRestoreCard(
    exportState: ExportUiState,
    importState: ImportUiState,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExporting = exportState is ExportUiState.Exporting
    val isImporting = importState is ImportUiState.Importing || importState is ImportUiState.Validating

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Data Backup",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Export your data as JSON or import from a backup file to transfer your links between devices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onExportClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExporting && !isImporting
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                    Text("Exporting...")
                } else {
                    Text("Export All Data")
                }
            }

            OutlinedButton(
                onClick = onImportClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExporting && !isImporting
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                    Text("Importing...")
                } else {
                    Text("Import Data")
                }
            }
        }
    }
}

@Composable
private fun DangerZoneCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
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
                Text(
                    text = "Delete All Data",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Permanently delete all your data including links, collections, tags, and vault items. This action cannot be undone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
                )
            ) {
                Text("Delete All Data")
            }
        }
    }
}

@Composable
private fun ClearCacheDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Clear Cache?",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "This will delete all cached preview images. The app will download them again when needed. This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Clear")
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
private fun ExportOptionsDialog(
    includeSnapshots: Boolean,
    onIncludeSnapshotsChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Export Data",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Your links, collections, and tags will be exported as a JSON file.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeSnapshots,
                        onCheckedChange = onIncludeSnapshotsChange
                    )
                    Column {
                        Text(
                            text = "Include snapshots",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "May significantly increase file size",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Export")
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
private fun ImportConfirmDialog(
    linksCount: Int,
    collectionsCount: Int,
    hasSnapshots: Boolean,
    onConfirm: (ImportConflictStrategy) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStrategy by remember { mutableStateOf(ImportConflictStrategy.SKIP) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Import Data",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "This file contains:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("- $linksCount links", style = MaterialTheme.typography.bodyMedium)
                    Text("- $collectionsCount collections", style = MaterialTheme.typography.bodyMedium)
                    if (hasSnapshots) {
                        Text("- Snapshots included", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "How to handle duplicates:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedStrategy == ImportConflictStrategy.SKIP,
                        onCheckedChange = { if (it) selectedStrategy = ImportConflictStrategy.SKIP }
                    )
                    Text("Skip duplicates", style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedStrategy == ImportConflictStrategy.REPLACE,
                        onCheckedChange = { if (it) selectedStrategy = ImportConflictStrategy.REPLACE }
                    )
                    Text("Replace existing", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedStrategy) }) {
                Text("Import")
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
