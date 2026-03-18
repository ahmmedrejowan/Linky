package com.rejowan.linky.presentation.feature.settings.importexport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.rejowan.linky.ui.theme.SoftAccents
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Import/Export Screen - Dedicated screen for data backup and restore
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
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

    val isExporting = state.exportState is ExportUiState.Exporting
    val isImporting = state.importState is ImportUiState.Importing || state.importState is ImportUiState.Validating

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Import / Export",
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
            // Info Card
            InfoCard()

            // Export Section
            ExportCard(
                isExporting = isExporting,
                isImporting = isImporting,
                onExportClick = { showExportOptionsDialog = true }
            )

            // Import Section
            ImportCard(
                isExporting = isExporting,
                isImporting = isImporting,
                onImportClick = { importLauncher.launch(arrayOf("application/json")) }
            )

            // Format Info
            FormatInfoCard()
        }
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
private fun InfoCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SoftAccents.Blue.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = SoftAccents.Blue,
                modifier = Modifier.size(24.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Backup Your Data",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Export your links, collections, and tags to a JSON file. You can import this file on another device or restore after reinstalling.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExportCard(
    isExporting: Boolean,
    isImporting: Boolean,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SoftAccents.Teal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        tint = SoftAccents.Teal,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Export Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Save your data to a JSON file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onExportClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isExporting && !isImporting,
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Exporting...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Export All Data")
                }
            }
        }
    }
}

@Composable
private fun ImportCard(
    isExporting: Boolean,
    isImporting: Boolean,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SoftAccents.Purple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = SoftAccents.Purple,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Import Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Restore from a backup file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = onImportClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isExporting && !isImporting,
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Importing...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Select Backup File")
                }
            }
        }
    }
}

@Composable
private fun FormatInfoCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "What's Included",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            IncludedItem(text = "All saved links with metadata")
            IncludedItem(text = "Collections and organization")
            IncludedItem(text = "Tags and categories")
            IncludedItem(text = "Favorites and archived items")
            IncludedItem(text = "Snapshots (optional)")

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Note: Vault links are not included in exports for security reasons.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IncludedItem(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SoftAccents.Teal,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
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
                text = "Export Options",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Your links, collections, and tags will be exported as a JSON file.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp),
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
                text = "Import Preview",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "This backup contains:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("• $linksCount links", style = MaterialTheme.typography.bodyMedium)
                    Text("• $collectionsCount collections", style = MaterialTheme.typography.bodyMedium)
                    if (hasSnapshots) {
                        Text("• Snapshots included", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Handle duplicates:",
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
            Button(
                onClick = { onConfirm(selectedStrategy) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    )
}
