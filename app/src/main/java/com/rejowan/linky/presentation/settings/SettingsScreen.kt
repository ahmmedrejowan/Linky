package com.rejowan.linky.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rejowan.linky.presentation.components.ErrorStates
import com.rejowan.linky.presentation.components.LoadingIndicator
import org.koin.androidx.compose.koinViewModel

/**
 * Settings Screen
 * Provides app settings and information
 *
 * Features:
 * - Theme selection (Light/Dark/System)
 * - App statistics (Links, Folders, Storage)
 * - Clear cache functionality
 * - App version display
 * - Loading and error states
 *
 * @param snackbarHostState SnackbarHostState from MainActivity
 * @param onNavigateToAdvanced Callback to navigate to advanced settings (Phase 3/4)
 * @param onNavigateToHome Callback to navigate to home
 * @param onNavigateToCollections Callback to navigate to collections
 * @param onLogout Callback when logout is clicked (Phase 2)
 * @param modifier Modifier for styling
 * @param viewModel SettingsViewModel injected via Koin
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    snackbarHostState: SnackbarHostState,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToCollections: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showClearCacheDialog by remember { mutableStateOf(false) }

    // Show error in Snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
            when {
                // Loading state
                state.isLoading && state.totalLinks == 0 -> {
                    LoadingIndicator(message = "Loading settings...")
                }

                // Error state
                state.error != null && state.totalLinks == 0 -> {
                    ErrorStates.GenericError(
                        errorMessage = state.error ?: "Unknown error",
                        onRetryClick = { viewModel.onEvent(SettingsEvent.OnRefresh) }
                    )
                }

                // Settings content
                else -> {
                    SettingsContent(
                        state = state,
                        onThemeChange = { viewModel.onEvent(SettingsEvent.OnThemeChange(it)) },
                        onClearCacheClick = { showClearCacheDialog = true }
                    )
                }
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
}

/**
 * Settings content display
 */
@Composable
private fun SettingsContent(
    state: SettingsState,
    onThemeChange: (String) -> Unit,
    onClearCacheClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Appearance Section
        SectionHeader(text = "Appearance")
        ThemeSelector(
            selectedTheme = state.theme,
            onThemeChange = onThemeChange
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Statistics Section
        SectionHeader(text = "App Statistics")
        StatisticsCard(
            totalLinks = state.totalLinks,
            totalFolders = state.totalFolders,
            totalStorageUsed = state.totalStorageUsed
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Storage Section
        SectionHeader(text = "Storage Management")
        StorageSection(
            isLoading = state.isLoading,
            onClearCacheClick = onClearCacheClick
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // About Section
        SectionHeader(text = "About")
        AboutCard(appVersion = state.appVersion)
    }
}

/**
 * Section header text
 */
@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

/**
 * Theme selector with segmented buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelector(
    selectedTheme: String,
    onThemeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val themes = listOf("Light", "Dark", "System")

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                themes.forEachIndexed { index, theme ->
                    SegmentedButton(
                        selected = selectedTheme == theme,
                        onClick = { onThemeChange(theme) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = themes.size
                        )
                    ) {
                        Text(theme)
                    }
                }
            }
        }
    }
}

/**
 * Statistics display card
 */
@Composable
private fun StatisticsCard(
    totalLinks: Int,
    totalFolders: Int,
    totalStorageUsed: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatisticRow(
                label = "Total Links",
                value = "$totalLinks ${if (totalLinks == 1) "item" else "items"}"
            )
            StatisticRow(
                label = "Total Folders",
                value = "$totalFolders ${if (totalFolders == 1) "collection" else "collections"}"
            )
            StatisticRow(
                label = "Storage Used",
                value = totalStorageUsed
            )
        }
    }
}

/**
 * Statistic row for label-value display
 */
@Composable
private fun StatisticRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Storage management section
 */
@Composable
private fun StorageSection(
    isLoading: Boolean,
    onClearCacheClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Clear cached preview images to free up space",
                style = MaterialTheme.typography.bodyMedium,
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

/**
 * About section card
 */
@Composable
private fun AboutCard(
    appVersion: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Version",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = appVersion,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Text(
                text = "Linky - Link Management Made Simple",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Clear cache confirmation dialog
 */
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
