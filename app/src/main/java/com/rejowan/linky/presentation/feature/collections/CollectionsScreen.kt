package com.rejowan.linky.presentation.feature.collections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rejowan.linky.domain.model.Folder
import com.rejowan.linky.presentation.components.EmptyStates
import com.rejowan.linky.presentation.components.ErrorStates
import com.rejowan.linky.presentation.components.FolderCard
import com.rejowan.linky.presentation.components.LoadingIndicator
import org.koin.androidx.compose.koinViewModel

/**
 * Collections Screen
 * Shows all folders for organizing links
 *
 * Features:
 * - Folder list with FolderCard components
 * - Create folder dialog with name and color picker
 * - Loading and error states
 * - Empty state when no folders
 * - FAB handled by MainActivity
 *
 * @param snackbarHostState SnackbarHostState from MainActivity
 * @param onCreateFolderClick Callback to register create folder action for FAB
 * @param onFolderClick Callback when a folder is clicked
 * @param onNavigateToHome Callback to navigate to home
 * @param onNavigateToSettings Callback to navigate to settings
 * @param modifier Modifier for styling
 * @param viewModel CollectionsViewModel injected via Koin
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    snackbarHostState: SnackbarHostState,
    onCreateFolderClick: (() -> Unit) -> Unit,
    onFolderClick: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Register create folder action for MainActivity FAB
    LaunchedEffect(Unit) {
        onCreateFolderClick {
            viewModel.onEvent(CollectionsEvent.OnCreateFolder)
        }
    }

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
                state.isLoading && state.folders.isEmpty() -> {
                    LoadingIndicator(message = "Loading folders...")
                }

                // Error state
                state.error != null && state.folders.isEmpty() -> {
                    ErrorStates.GenericError(
                        errorMessage = state.error ?: "Unknown error",
                        onRetryClick = { viewModel.onEvent(CollectionsEvent.OnRefresh) }
                    )
                }

                // Empty state
                state.folders.isEmpty() -> {
                    EmptyStates.NoFolders(
                        onCreateFolderClick = { viewModel.onEvent(CollectionsEvent.OnCreateFolder) }
                    )
                }

                // Folders list
                else -> {
                    FoldersList(
                        folders = state.folders,
                        onFolderClick = onFolderClick
                    )
                }
            }
    }

    // Create Folder Dialog
    if (state.showCreateDialog) {
        CreateFolderDialog(
            folderName = state.newFolderName,
            selectedColor = state.selectedFolderColor,
            onFolderNameChange = { viewModel.onEvent(CollectionsEvent.OnFolderNameChange(it)) },
            onColorChange = { viewModel.onEvent(CollectionsEvent.OnFolderColorChange(it)) },
            onSave = { viewModel.onEvent(CollectionsEvent.OnSaveFolder) },
            onDismiss = { viewModel.onEvent(CollectionsEvent.OnDismissCreateDialog) }
        )
    }
}

/**
 * Folders list display
 */
@Composable
private fun FoldersList(
    folders: List<Folder>,
    onFolderClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = folders,
            key = { it.id }
        ) { folder ->
            FolderCard(
                folder = folder,
                linkCount = 0, // TODO: Get actual link count from ViewModel
                onClick = { onFolderClick(folder.id) }
            )
        }
    }
}

/**
 * Create Folder Dialog
 * Allows users to create a new folder with name and color
 */
@Composable
private fun CreateFolderDialog(
    folderName: String,
    selectedColor: String?,
    onFolderNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Folder",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Folder name input
                OutlinedTextField(
                    value = folderName,
                    onValueChange = onFolderNameChange,
                    label = { Text("Folder Name") },
                    placeholder = { Text("Enter folder name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Color picker
                Text(
                    text = "Color (Optional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ColorPicker(
                    selectedColor = selectedColor,
                    onColorSelected = onColorChange
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = folderName.isNotBlank()
            ) {
                Text("Create")
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

/**
 * Color picker with predefined colors
 */
@Composable
private fun ColorPicker(
    selectedColor: String?,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        "#FF6B6B" to "Red",
        "#4ECDC4" to "Teal",
        "#45B7D1" to "Blue",
        "#FFA07A" to "Orange",
        "#98D8C8" to "Green",
        "#F7B731" to "Yellow",
        "#5F27CD" to "Purple",
        "#EE5A6F" to "Pink"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colors.take(4).forEach { (colorHex, colorName) ->
                FilterChip(
                    selected = selectedColor == colorHex,
                    onClick = { onColorSelected(colorHex) },
                    label = { Text(colorName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colors.drop(4).forEach { (colorHex, colorName) ->
                FilterChip(
                    selected = selectedColor == colorHex,
                    onClick = { onColorSelected(colorHex) },
                    label = { Text(colorName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
