package com.rejowan.linky.presentation.feature.addlink

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rejowan.linky.domain.model.Collection
import org.koin.androidx.compose.koinViewModel

/**
 * Add/Edit Link Screen
 * Allows users to add a new link or edit an existing one
 *
 * Features:
 * - URL input with validation
 * - Fetch preview from URL (Open Graph, Twitter Card, meta tags)
 * - Auto-fetch title and description from preview
 * - Compact preview card (left: image, right: title/description)
 * - Title, description, and note input fields
 * - Collection selection dropdown
 * - Favorite toggle switch
 * - Save icon in TopAppBar (alternative to bottom button)
 * - Focus management (tap outside to hide keyboard)
 * - Loading states for preview fetch and save
 * - Error handling with Snackbar
 *
 * @param linkId Optional link ID for edit mode. Null for add mode
 * @param onNavigateBack Callback to navigate back
 * @param modifier Modifier for styling
 * @param viewModel AddEditLinkViewModel injected via Koin
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLinkScreen(
    linkId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditLinkViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    var showBackConfirmDialog by remember { mutableStateOf(false) }
    var linkSaved by remember { mutableStateOf(false) }

    // Check if there's unsaved data
    val hasUnsavedData = remember(state.url, state.title, state.note, state.description, linkSaved) {
        !linkSaved && (state.url.isNotBlank() || state.title.isNotBlank() ||
            state.note.isNotBlank() || state.description.isNotBlank())
    }

    // Collect and handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is AddEditLinkUiEvent.LinkSaved -> {
                    linkSaved = true
                    onNavigateBack()
                }
            }
        }
    }

    // Handle back press with confirmation if there's unsaved data
    BackHandler(enabled = hasUnsavedData) {
        showBackConfirmDialog = true
    }

    // Show error in Snackbar when error state changes
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            // Note: Error is cleared automatically by ViewModel after showing
        }
    }

    Scaffold(
        topBar = {
        TopAppBar(
            title = {
            Text(
                text = if (state.isEditMode) "Edit Link" else "Add Link",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 22.sp
                )
            )
        }, navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back"
                )
            }
        }, actions = {
            // Save icon - Alternative to bottom save button for easier access
            IconButton(
                onClick = { viewModel.onEvent(AddEditLinkEvent.OnSave) },
                enabled = !state.isLoading && state.url.isNotBlank() && state.title.isNotBlank()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Save link"
                    )
                }
            }
        }, colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
        )
    }, snackbarHost = { SnackbarHost(snackbarHostState) },
        // Focus management: Tap outside text fields to hide keyboard
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            focusManager.clearFocus()
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // URL Input
            OutlinedTextField(
                value = state.url,
                onValueChange = { viewModel.onEvent(AddEditLinkEvent.OnUrlChange(it)) },
                label = { Text("URL *") },
                placeholder = { Text("https://example.com") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                singleLine = true,
                trailingIcon = {
                    if (state.url.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onEvent(AddEditLinkEvent.OnUrlChange("")) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear URL"
                            )
                        }
                    }
                },
                supportingText = {
                    Text(
                        text = "${state.url.length}/2048",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )

            // Preview Fetch Suggestion Card
            if (state.showPreviewFetchSuggestion) {
                PreviewFetchSuggestionCard(
                    onDismiss = { viewModel.onEvent(AddEditLinkEvent.OnDismissPreviewSuggestion) }
                )
            }

            // Fetch Preview Button - Auto-fetches title, description, and image from URL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.onEvent(AddEditLinkEvent.OnFetchPreview)
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isFetchingPreview && !state.isLoading && state.url.isNotBlank()
                ) {
                    if (state.isFetchingPreview) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp), strokeWidth = 2.dp
                        )
                    } else {
                        Text("Fetch Preview")
                    }
                }
            }

            // Compact Preview Card - Shows how the link will appear in home screen
            // Layout: Left (70dp image/placeholder) | Right (title + description)
            // Matches LinkCard design from home screen for consistency
            if (state.title.isNotBlank() || state.description.isNotBlank() ||
                state.previewImagePath != null || state.previewUrl != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Preview Image (left side) - 70dp × 70dp with rounded corners
                        if (state.previewImagePath != null || state.previewUrl != null) {
                            AsyncImage(
                                model = state.previewImagePath ?: state.previewUrl,
                                contentDescription = "Link preview",
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Placeholder when no image
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "No preview",
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Content (right side)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Title
                            if (state.title.isNotBlank()) {
                                Text(
                                    text = state.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Description
                            if (state.description.isNotBlank()) {
                                Text(
                                    text = state.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Title Input - Auto-filled by "Fetch Preview" or manually entered
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.onEvent(AddEditLinkEvent.OnTitleChange(it)) },
                label = { Text("Title *") },
                placeholder = { Text("Enter link title") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                singleLine = true,
                trailingIcon = {
                    if (state.title.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onEvent(AddEditLinkEvent.OnTitleChange("")) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear title"
                            )
                        }
                    }
                },
                supportingText = {
                    Text(
                        text = "${state.title.length}/200",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )

            // Description Input - Auto-filled from web metadata or manually entered
            // Brief summary of the link content (og:description, twitter:description)
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.onEvent(AddEditLinkEvent.OnDescriptionChange(it)) },
                label = { Text("Description (Optional)") },
                placeholder = { Text("Brief description of the link...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                enabled = !state.isLoading,
                maxLines = 3,
                trailingIcon = {
                    if (state.description.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onEvent(AddEditLinkEvent.OnDescriptionChange("")) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear description"
                            )
                        }
                    }
                },
                supportingText = {
                    Text(
                        text = "${state.description.length}/500",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )

            // Note Input - Personal notes/comments about the link (user-entered only)
            OutlinedTextField(
                value = state.note,
                onValueChange = { viewModel.onEvent(AddEditLinkEvent.OnNoteChange(it)) },
                label = { Text("Note (Optional)") },
                placeholder = { Text("Add a note about this link...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                enabled = !state.isLoading,
                maxLines = 5,
                trailingIcon = {
                    if (state.note.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onEvent(AddEditLinkEvent.OnNoteChange("")) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear note"
                            )
                        }
                    }
                },
                supportingText = {
                    Text(
                        text = "${state.note.length}/5000",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )

            // Collection Selection Dropdown
            CollectionDropdown(
                selectedCollectionId = state.selectedCollectionId,
                collections = state.collections,
                enabled = !state.isLoading,
                onCollectionSelected = { collectionId ->
                    viewModel.onEvent(AddEditLinkEvent.OnCollectionSelect(collectionId))
                },
                onCreateNewClick = {
                    viewModel.onEvent(AddEditLinkEvent.OnCreateCollectionClick)
                })

            // Add to Favourite Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add to Favourite",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = state.isFavorite,
                    onCheckedChange = { viewModel.onEvent(AddEditLinkEvent.OnToggleFavorite) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hide from Home toggle (only enabled when collection is selected)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hide from Home",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Only show in collection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.hideFromHome,
                    onCheckedChange = { viewModel.onEvent(AddEditLinkEvent.OnToggleHideFromHome) },
                    enabled = state.selectedCollectionId != null
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = { viewModel.onEvent(AddEditLinkEvent.OnSave) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isLoading && state.url.isNotBlank() && state.title.isNotBlank()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (state.isEditMode) "Update Link" else "Save Link",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Required fields note
            Text(
                text = "* Required fields",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    // Back Confirmation Dialog
    if (showBackConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBackConfirmDialog = false },
            title = {
                Text(
                    text = "Discard changes?",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "You have unsaved changes. Are you sure you want to go back?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackConfirmDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBackConfirmDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create Collection Dialog
    if (state.showCreateCollectionDialog) {
        CreateCollectionDialog(
            collectionName = state.newCollectionName,
            selectedColor = state.newCollectionColor,
            isFavorite = state.newCollectionIsFavorite,
            onCollectionNameChange = { viewModel.onEvent(AddEditLinkEvent.OnNewCollectionNameChange(it)) },
            onColorChange = { viewModel.onEvent(AddEditLinkEvent.OnNewCollectionColorChange(it)) },
            onToggleFavorite = { viewModel.onEvent(AddEditLinkEvent.OnNewCollectionToggleFavorite) },
            onSave = { viewModel.onEvent(AddEditLinkEvent.OnCreateCollectionConfirm) },
            onDismiss = { viewModel.onEvent(AddEditLinkEvent.OnCreateCollectionDismiss) }
        )
    }
}

/**
 * Collection Selection Dropdown
 * Allows users to select a collection or "No Collection"
 *
 * @param selectedCollectionId Currently selected collection ID, null for "No Collection"
 * @param collections List of available collections
 * @param enabled Whether the dropdown is enabled
 * @param onCollectionSelected Callback when collection is selected (null for "No Collection")
 * @param modifier Modifier for styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionDropdown(
    selectedCollectionId: String?,
    collections: List<Collection>,
    enabled: Boolean,
    onCollectionSelected: (String?) -> Unit,
    onCreateNewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Find selected collection name - Optimized with remember to avoid recalculation
    val selectedCollectionName = remember(selectedCollectionId, collections) {
        if (selectedCollectionId == null) {
            "(No Collection)"
        } else {
            collections.find { it.id == selectedCollectionId }?.name ?: "(No Collection)"
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded && enabled },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedCollectionName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Collection") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            enabled = enabled,
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded, onDismissRequest = { expanded = false }) {
            // "No Collection" option
            DropdownMenuItem(text = {
                Text(
                    text = "(No Collection)", style = MaterialTheme.typography.bodyMedium
                )
            }, onClick = {
                onCollectionSelected(null)
                expanded = false
            })

            // Collection options
            collections.forEach { collection ->
                DropdownMenuItem(text = {
                    Text(
                        text = collection.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }, onClick = {
                    onCollectionSelected(collection.id)
                    expanded = false
                })
            }

            // "Create New" option
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create new collection",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Create New Collection",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                onClick = {
                    onCreateNewClick()
                    expanded = false
                }
            )
        }
    }
}

/**
 * Create Collection Dialog
 * Allows users to create a new collection with name, color, and favorite status
 * Reused from CollectionsScreen
 */
@Composable
private fun CreateCollectionDialog(
    collectionName: String,
    selectedColor: String?,
    isFavorite: Boolean,
    onCollectionNameChange: (String) -> Unit,
    onColorChange: (String?) -> Unit,
    onToggleFavorite: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.AlertDialog(
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

                ColorBlockPicker(
                    selectedColor = selectedColor,
                    onColorSelected = onColorChange
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Favorite toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add to Favourite",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = isFavorite,
                        onCheckedChange = { onToggleFavorite() }
                    )
                }
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
 * Color block picker with visual color rectangles
 * 15 total: 1 no color + 14 colors, arranged in 3 rows of 5
 */
@Composable
private fun ColorBlockPicker(
    selectedColor: String?,
    onColorSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        null,           // No Color - default
        "#FF6B6B",      // Red
        "#E74C3C",      // Dark Red
        "#4ECDC4",      // Teal
        "#45B7D1",      // Blue
        "#3498DB",      // Strong Blue
        "#FFA07A",      // Orange
        "#E67E22",      // Dark Orange
        "#98D8C8",      // Green
        "#2ECC71",      // Emerald Green
        "#F7B731",      // Yellow
        "#F39C12",      // Golden Yellow
        "#5F27CD",      // Purple
        "#9B59B6",      // Light Purple
        "#EE5A6F"       // Pink
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: 5 colors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            colors.take(5).forEach { colorHex ->
                ColorBlock(
                    colorHex = colorHex,
                    isSelected = selectedColor == colorHex,
                    onClick = { onColorSelected(colorHex) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2: 5 colors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            colors.subList(5, 10).forEach { colorHex ->
                ColorBlock(
                    colorHex = colorHex,
                    isSelected = selectedColor == colorHex,
                    onClick = { onColorSelected(colorHex) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 3: 5 colors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            colors.subList(10, 15).forEach { colorHex ->
                ColorBlock(
                    colorHex = colorHex,
                    isSelected = selectedColor == colorHex,
                    onClick = { onColorSelected(colorHex) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Individual color block component
 */
@Composable
private fun ColorBlock(
    colorHex: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = if (colorHex != null) {
                    Color(android.graphics.Color.parseColor(colorHex))
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Show checkmark for selected color
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (colorHex != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Suggestion card explaining preview fetch behavior
 */
@Composable
private fun PreviewFetchSuggestionCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Info icon
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Information",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )

            // Content column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "About Preview Fetch",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "\"Fetch Preview\" will replace your current title and description with data from the URL. Any changes you've made will be overwritten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }

            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
