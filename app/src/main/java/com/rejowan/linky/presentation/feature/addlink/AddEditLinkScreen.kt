package com.rejowan.linky.presentation.feature.addlink

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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rejowan.linky.domain.model.Folder
import org.koin.androidx.compose.koinViewModel

/**
 * Add/Edit Link Screen
 * Allows users to add a new link or edit an existing one
 *
 * Features:
 * - URL input with validation
 * - Fetch preview from URL (Open Graph, Twitter Card)
 * - Title and note input
 * - Folder selection
 * - Favorite toggle
 * - Preview image display
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

    // Navigate back on successful save
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }

    // Show error in Snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditMode) "Edit Link" else "Add Link",
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
                },
                actions = {
                    // Favorite toggle
                    IconButton(
                        onClick = { viewModel.onEvent(AddEditLinkEvent.OnToggleFavorite) }
                    ) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = if (state.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (state.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
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
                singleLine = true
            )

            // Fetch Preview Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.onEvent(AddEditLinkEvent.OnFetchPreview) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isFetchingPreview && !state.isLoading && state.url.isNotBlank()
                ) {
                    if (state.isFetchingPreview) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Fetch Preview")
                    }
                }
            }

            // Preview Image Card (if available)
            if (state.previewImagePath != null || state.previewUrl != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = state.previewImagePath ?: state.previewUrl,
                            contentDescription = "Link preview image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Title Input
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.onEvent(AddEditLinkEvent.OnTitleChange(it)) },
                label = { Text("Title *") },
                placeholder = { Text("Enter link title") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                singleLine = true
            )

            // Note Input (Multiline)
            OutlinedTextField(
                value = state.note,
                onValueChange = { viewModel.onEvent(AddEditLinkEvent.OnNoteChange(it)) },
                label = { Text("Note (Optional)") },
                placeholder = { Text("Add a note about this link...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                enabled = !state.isLoading,
                maxLines = 5
            )

            // Folder Selection Dropdown
            FolderDropdown(
                selectedFolderId = state.selectedFolderId,
                folders = state.folders,
                enabled = !state.isLoading,
                onFolderSelected = { folderId ->
                    viewModel.onEvent(AddEditLinkEvent.OnFolderSelect(folderId))
                }
            )

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
}

/**
 * Folder Selection Dropdown
 * Allows users to select a folder or "No Folder"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderDropdown(
    selectedFolderId: String?,
    folders: List<Folder>,
    enabled: Boolean,
    onFolderSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Find selected folder name
    val selectedFolderName = if (selectedFolderId == null) {
        "No Folder"
    } else {
        folders.find { it.id == selectedFolderId }?.name ?: "No Folder"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded && enabled },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedFolderName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Folder") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            enabled = enabled,
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // "No Folder" option
            DropdownMenuItem(
                text = {
                    Text(
                        text = "No Folder",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                onClick = {
                    onFolderSelected(null)
                    expanded = false
                }
            )

            // Folder options
            folders.forEach { folder ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onFolderSelected(folder.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
