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
import com.rejowan.linky.domain.model.CollectionWithLinkCount
import com.rejowan.linky.presentation.components.EmptyStates
import com.rejowan.linky.presentation.components.ErrorStates
import com.rejowan.linky.presentation.components.CollectionCard
import com.rejowan.linky.presentation.components.LoadingIndicator
import org.koin.androidx.compose.koinViewModel

/**
 * Collections Screen
 * Shows all collections for organizing links
 *
 * Features:
 * - Collection list with CollectionCard components
 * - Create collection dialog with name and color picker
 * - Loading and error states
 * - Empty state when no collections
 * - FAB handled by MainActivity
 *
 * @param snackbarHostState SnackbarHostState from MainActivity
 * @param onCreateCollectionClick Callback to register create collection action for FAB
 * @param onCollectionClick Callback when a collection is clicked
 * @param onNavigateToHome Callback to navigate to home
 * @param onNavigateToSettings Callback to navigate to settings
 * @param modifier Modifier for styling
 * @param viewModel CollectionsViewModel injected via Koin
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    snackbarHostState: SnackbarHostState,
    onCreateCollectionClick: (() -> Unit) -> Unit,
    onCollectionClick: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Register create collection action for MainActivity FAB
    LaunchedEffect(Unit) {
        onCreateCollectionClick {
            viewModel.onEvent(CollectionsEvent.OnCreateCollection)
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
                state.isLoading && state.collections.isEmpty() -> {
                    LoadingIndicator(message = "Loading collections...")
                }

                // Error state
                state.error != null && state.collections.isEmpty() -> {
                    ErrorStates.GenericError(
                        errorMessage = state.error ?: "Unknown error",
                        onRetryClick = { viewModel.onEvent(CollectionsEvent.OnRefresh) }
                    )
                }

                // Empty state
                state.collections.isEmpty() -> {
                    EmptyStates.NoCollections(
                        onCreateCollectionClick = { viewModel.onEvent(CollectionsEvent.OnCreateCollection) }
                    )
                }

                // Collections list
                else -> {
                    CollectionsList(
                        collections = state.collections,
                        onCollectionClick = onCollectionClick
                    )
                }
            }
    }

    // Create Collection Dialog
    if (state.showCreateDialog) {
        CreateCollectionDialog(
            collectionName = state.newCollectionName,
            selectedColor = state.selectedCollectionColor,
            onCollectionNameChange = { viewModel.onEvent(CollectionsEvent.OnCollectionNameChange(it)) },
            onColorChange = { viewModel.onEvent(CollectionsEvent.OnCollectionColorChange(it)) },
            onSave = { viewModel.onEvent(CollectionsEvent.OnSaveCollection) },
            onDismiss = { viewModel.onEvent(CollectionsEvent.OnDismissCreateDialog) }
        )
    }
}

/**
 * Collections list display
 */
@Composable
private fun CollectionsList(
    collections: List<CollectionWithLinkCount>,
    onCollectionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = collections,
            key = { it.collection.id }
        ) { collectionWithCount ->
            CollectionCard(
                collection = collectionWithCount.collection,
                linkCount = collectionWithCount.linkCount,
                onClick = { onCollectionClick(collectionWithCount.collection.id) }
            )
        }
    }
}

/**
 * Create Collection Dialog
 * Allows users to create a new collection with name and color
 */
@Composable
private fun CreateCollectionDialog(
    collectionName: String,
    selectedColor: String?,
    onCollectionNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Collection",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Collection name input
                OutlinedTextField(
                    value = collectionName,
                    onValueChange = onCollectionNameChange,
                    label = { Text("Collection Name") },
                    placeholder = { Text("Enter collection name") },
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
                enabled = collectionName.isNotBlank()
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
