package com.rejowan.linky.presentation.feature.addlink

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.ClipboardManager
import android.content.Context
import coil.compose.AsyncImage
import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.presentation.components.TagSelectorField
import com.rejowan.linky.ui.theme.SoftAccents
import org.koin.androidx.compose.koinViewModel

/**
 * Add/Edit Link Screen - Redesigned with modern UI
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
    val context = LocalContext.current
    var showBackConfirmDialog by remember { mutableStateOf(false) }
    var linkSaved by remember { mutableStateOf(false) }

    val hasUnsavedData = remember(state.url, state.title, state.note, state.description, linkSaved) {
        !linkSaved && (state.url.isNotBlank() || state.title.isNotBlank() ||
            state.note.isNotBlank() || state.description.isNotBlank())
    }

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

    BackHandler(enabled = hasUnsavedData) {
        showBackConfirmDialog = true
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            focusManager.clearFocus()
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp)
            ) {
                // Custom Header
                AddEditHeader(
                    isEditMode = state.isEditMode,
                    onBackClick = {
                        if (hasUnsavedData) showBackConfirmDialog = true
                        else onNavigateBack()
                    }
                )

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // URL Section
                    UrlInputSection(
                        url = state.url,
                        onUrlChange = { viewModel.onEvent(AddEditLinkEvent.OnUrlChange(it)) },
                        onPasteClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clipData = clipboard?.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0)?.text?.toString()
                                if (!text.isNullOrBlank()) {
                                    viewModel.onEvent(AddEditLinkEvent.OnUrlChange(text.trim()))
                                }
                            }
                        },
                        onClearClick = { viewModel.onEvent(AddEditLinkEvent.OnUrlChange("")) },
                        onFetchPreview = {
                            viewModel.onEvent(AddEditLinkEvent.OnFetchPreview)
                            focusManager.clearFocus()
                        },
                        isFetching = state.isFetchingPreview,
                        isEnabled = !state.isLoading
                    )

                    // Preview Card (when available)
                    AnimatedVisibility(
                        visible = !state.isFetchingPreview && (state.previewImagePath != null || state.previewUrl != null),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        PreviewCard(
                            imageUrl = state.previewImagePath ?: state.previewUrl,
                            title = state.title,
                            description = state.description
                        )
                    }

                    // Loading Preview Skeleton
                    AnimatedVisibility(
                        visible = state.isFetchingPreview,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        PreviewLoadingSkeleton()
                    }

                    // Details Section
                    SectionCard(
                        title = "Details",
                        icon = Icons.Filled.Title,
                        accentColor = SoftAccents.Blue
                    ) {
                        // Title
                        StyledTextField(
                            value = state.title,
                            onValueChange = { viewModel.onEvent(AddEditLinkEvent.OnTitleChange(it)) },
                            label = "Title",
                            placeholder = "Enter link title",
                            isRequired = true,
                            singleLine = true,
                            enabled = !state.isLoading
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Description
                        StyledTextField(
                            value = state.description,
                            onValueChange = { viewModel.onEvent(AddEditLinkEvent.OnDescriptionChange(it)) },
                            label = "Description",
                            placeholder = "Brief description...",
                            singleLine = false,
                            minLines = 2,
                            maxLines = 3,
                            enabled = !state.isLoading
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Note
                        StyledTextField(
                            value = state.note,
                            onValueChange = { viewModel.onEvent(AddEditLinkEvent.OnNoteChange(it)) },
                            label = "Personal Note",
                            placeholder = "Add a note about this link...",
                            singleLine = false,
                            minLines = 3,
                            maxLines = 5,
                            enabled = !state.isLoading
                        )
                    }

                    // Organization Section
                    SectionCard(
                        title = "Organization",
                        icon = Icons.Filled.Folder,
                        accentColor = SoftAccents.Purple
                    ) {
                        // Collection Dropdown
                        CollectionSelector(
                            selectedCollectionId = state.selectedCollectionId,
                            collections = state.collections,
                            enabled = !state.isLoading,
                            onCollectionSelected = { viewModel.onEvent(AddEditLinkEvent.OnCollectionSelect(it)) },
                            onCreateNewClick = { viewModel.onEvent(AddEditLinkEvent.OnCreateCollectionClick) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tags
                        TagSelectorField(
                            selectedTags = state.selectedTags,
                            allTags = state.allTags,
                            onTagSelected = { viewModel.onEvent(AddEditLinkEvent.OnTagSelected(it)) },
                            onTagRemoved = { viewModel.onEvent(AddEditLinkEvent.OnTagRemoved(it)) },
                            onCreateTag = { name, color ->
                                viewModel.onEvent(AddEditLinkEvent.OnCreateTag(name, color))
                            }
                        )
                    }

                    // Settings Section
                    SectionCard(
                        title = "Settings",
                        icon = Icons.Filled.Settings,
                        accentColor = SoftAccents.Teal
                    ) {
                        // Favorite Toggle
                        SettingToggleRow(
                            icon = if (state.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            iconTint = if (state.isFavorite) SoftAccents.Pink else MaterialTheme.colorScheme.onSurfaceVariant,
                            title = "Add to Favorites",
                            subtitle = "Quick access from favorites tab",
                            checked = state.isFavorite,
                            onCheckedChange = { viewModel.onEvent(AddEditLinkEvent.OnToggleFavorite) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Hide from Home Toggle
                        SettingToggleRow(
                            icon = Icons.Filled.VisibilityOff,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            title = "Hide from Home",
                            subtitle = "Only show in collection",
                            checked = state.hideFromHome,
                            onCheckedChange = { viewModel.onEvent(AddEditLinkEvent.OnToggleHideFromHome) },
                            enabled = state.selectedCollectionId != null
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Floating Save Button with background overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .windowInsetsPadding(WindowInsets.ime)
            ) {
                SaveButton(
                    isEditMode = state.isEditMode,
                    isLoading = state.isLoading,
                    isEnabled = !state.isLoading && state.url.isNotBlank() && state.title.isNotBlank(),
                    onClick = { viewModel.onEvent(AddEditLinkEvent.OnSave) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }

    // Dialogs
    if (showBackConfirmDialog) {
        DiscardChangesDialog(
            onDiscard = {
                showBackConfirmDialog = false
                onNavigateBack()
            },
            onCancel = { showBackConfirmDialog = false }
        )
    }

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

@Composable
private fun AddEditHeader(
    isEditMode: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = if (isEditMode) "Edit Link" else "Add Link",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isEditMode) "Update your saved link" else "Save a new link to your collection",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UrlInputSection(
    url: String,
    onUrlChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onClearClick: () -> Unit,
    onFetchPreview: () -> Unit,
    isFetching: Boolean,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with icon and label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Link URL",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.weight(1f))

                // Paste or Clear button
                if (url.isEmpty()) {
                    TextButton(onClick = onPasteClick) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste")
                    }
                } else {
                    IconButton(onClick = onClearClick) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // URL Input - full width, multi-line capable for visibility
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                placeholder = {
                    Text(
                        "https://example.com/your-link-here",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isEnabled,
                minLines = 1,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Fetch Preview Button
            Button(
                onClick = onFetchPreview,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isFetching && isEnabled && url.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (isFetching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetching...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetch Preview", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(
    imageUrl: String?,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Preview Image
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Link preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewLoadingSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Fetching preview...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp),
                        tint = accentColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            content()
        }
    }
}

@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (isRequired) "$label *" else label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionSelector(
    selectedCollectionId: String?,
    collections: List<Collection>,
    enabled: Boolean,
    onCollectionSelected: (String?) -> Unit,
    onCreateNewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedCollection = remember(selectedCollectionId, collections) {
        collections.find { it.id == selectedCollectionId }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Collection",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded && enabled },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCollection?.name ?: "No Collection",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                enabled = enabled,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = selectedCollection?.color?.let {
                            try { Color(android.graphics.Color.parseColor(it)) }
                            catch (e: Exception) { MaterialTheme.colorScheme.primary }
                        } ?: MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("No Collection") },
                    onClick = {
                        onCollectionSelected(null)
                        expanded = false
                    }
                )

                collections.forEach { collection ->
                    DropdownMenuItem(
                        text = { Text(collection.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            onCollectionSelected(collection.id)
                            expanded = false
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        collection.color?.let {
                                            try { Color(android.graphics.Color.parseColor(it)) }
                                            catch (e: Exception) { MaterialTheme.colorScheme.primary }
                                        } ?: MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    )
                }

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create New", color = MaterialTheme.colorScheme.primary)
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
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .clickable(enabled = enabled) { onCheckedChange() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = if (enabled) iconTint else iconTint.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun SaveButton(
    isEditMode: Boolean,
    isLoading: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = isEnabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isEditMode) "Update Link" else "Save Link",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun DiscardChangesDialog(
    onDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Discard changes?",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text("You have unsaved changes. Are you sure you want to go back?")
        },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text("Discard", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CreateCollectionDialog(
    collectionName: String,
    selectedColor: String?,
    isFavorite: Boolean,
    onCollectionNameChange: (String) -> Unit,
    onColorChange: (String?) -> Unit,
    onToggleFavorite: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Collection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = collectionName,
                    onValueChange = onCollectionNameChange,
                    label = { Text("Collection Name") },
                    placeholder = { Text("Enter name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Color (Optional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ColorPicker(
                    selectedColor = selectedColor,
                    onColorSelected = onColorChange
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add to Favorites")
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
        }
    )
}

@Composable
private fun ColorPicker(
    selectedColor: String?,
    onColorSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        null, "#FF6B6B", "#E74C3C", "#4ECDC4", "#45B7D1",
        "#3498DB", "#FFA07A", "#E67E22", "#98D8C8", "#2ECC71",
        "#F7B731", "#F39C12", "#5F27CD", "#9B59B6", "#EE5A6F"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.chunked(5).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { colorHex ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                colorHex?.let {
                                    try { Color(android.graphics.Color.parseColor(it)) }
                                    catch (e: Exception) { MaterialTheme.colorScheme.surfaceVariant }
                                } ?: MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (selectedColor == colorHex) 3.dp else 1.dp,
                                color = if (selectedColor == colorHex)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onColorSelected(colorHex) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedColor == colorHex) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = if (colorHex != null) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
