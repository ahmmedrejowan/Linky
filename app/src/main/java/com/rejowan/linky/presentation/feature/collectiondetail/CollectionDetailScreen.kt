package com.rejowan.linky.presentation.feature.collectiondetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.presentation.components.EmptyStates
import com.rejowan.linky.presentation.components.ErrorStates
import com.rejowan.linky.presentation.components.LinkCard
import com.rejowan.linky.presentation.components.LoadingIndicator
import com.rejowan.linky.presentation.feature.home.SortType
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Collection Detail Screen
 * Shows all links within a specific collection
 *
 * Features:
 * - Collection info header (name, color, favorite status)
 * - TopAppBar actions: favorite toggle, add link, edit/delete menu
 * - Info card with link count and dates
 * - List of links in the collection using LinkCard
 * - Edit collection dialog
 * - Delete confirmation dialog with checkbox for deleting links
 * - Empty state when no links
 * - Error handling
 * - Back navigation
 *
 * @param onNavigateBack Callback to navigate back
 * @param onLinkClick Callback when a link is clicked
 * @param onFavoriteClick Callback when favorite icon is clicked on a link
 * @param onAddLinkClick Callback to navigate to add link screen with collection preselected
 * @param modifier Modifier for styling
 * @param viewModel CollectionDetailViewModel injected via Koin
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    onNavigateBack: () -> Unit,
    onLinkClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onAddLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }

    // Show error in Snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    // Listen to UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is CollectionDetailUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
                    )
                }
                is CollectionDetailUiEvent.ShowLinkFavoriteToggled -> {
                    val message = if (event.isFavorite) {
                        "Added to favorites"
                    } else {
                        "Removed from favorites"
                    }
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        // Undo the favorite toggle
                        viewModel.onEvent(CollectionDetailEvent.OnToggleLinkFavorite(event.linkId))
                    }
                }
                is CollectionDetailUiEvent.ShowArchiveToggled -> {
                    val message = if (event.isArchived) {
                        "Link archived"
                    } else {
                        "Link unarchived"
                    }
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        // Undo the archive toggle
                        viewModel.onEvent(CollectionDetailEvent.OnArchiveLink(event.linkId))
                    }
                }
                is CollectionDetailUiEvent.ShowLinkTrashed -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Link moved to trash",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        // Undo the trash action by restoring the link
                        viewModel.onEvent(CollectionDetailEvent.OnRestoreLink(event.linkId))
                    }
                }
                is CollectionDetailUiEvent.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Collection color indicator
                        state.collection?.color?.let { colorHex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(colorHex)))
                            )
                        }

                        // Collection name
                        Text(
                            text = state.collection?.name ?: "Collection",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
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
                    // Favorite toggle button
                    IconButton(onClick = { viewModel.onEvent(CollectionDetailEvent.OnToggleFavorite) }) {
                        Icon(
                            imageVector = if (state.collection?.isFavorite == true) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = if (state.collection?.isFavorite == true) {
                                "Remove from favorites"
                            } else {
                                "Add to favorites"
                            },
                            tint = if (state.collection?.isFavorite == true) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    // Add link button
                    IconButton(onClick = {
                        state.collection?.id?.let { onAddLinkClick(it) }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add link to collection"
                        )
                    }

                    // Three-dot menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit collection") },
                                onClick = {
                                    showMenu = false
                                    viewModel.onEvent(CollectionDetailEvent.OnEditClick)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete collection") },
                                onClick = {
                                    showMenu = false
                                    viewModel.onEvent(CollectionDetailEvent.OnDeleteClick)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete"
                                    )
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.links.isNotEmpty(),
            onRefresh = { viewModel.onEvent(CollectionDetailEvent.OnRefresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Loading state
                state.isLoading && state.links.isEmpty() -> {
                    LoadingIndicator(message = "Loading links...")
                }

                // Error state
                state.error != null && state.links.isEmpty() && state.collection == null -> {
                    ErrorStates.GenericError(
                        errorMessage = state.error ?: "Unknown error",
                        onRetryClick = { viewModel.onEvent(CollectionDetailEvent.OnRefresh) }
                    )
                }

                // Empty state
                state.links.isEmpty() && state.collection != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No links in this collection yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Content with info card and links list
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Info card
                        state.collection?.let { collection ->
                            CollectionInfoCard(
                                linkCount = state.links.size,
                                createdAt = collection.createdAt,
                                updatedAt = collection.updatedAt,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }

                        // Count and Sort Row
                        if (state.links.isNotEmpty()) {
                            CountAndSortRow(
                                count = state.links.size,
                                sortType = state.sortType,
                                onSortClick = { viewModel.onEvent(CollectionDetailEvent.OnSortTypeChange(it)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp)
                            )
                        }

                        // Links list or empty state
                        if (state.links.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No links in this collection yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LinksList(
                                links = state.links,
                                onLinkClick = onLinkClick,
                                onFavoriteClick = { linkId ->
                                    viewModel.onEvent(CollectionDetailEvent.OnToggleLinkFavorite(linkId))
                                },
                                onArchiveClick = { linkId ->
                                    viewModel.onEvent(CollectionDetailEvent.OnArchiveLink(linkId))
                                },
                                onTrashClick = { linkId ->
                                    viewModel.onEvent(CollectionDetailEvent.OnTrashLink(linkId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Collection Dialog
    if (state.showEditDialog) {
        EditCollectionDialog(
            collectionName = state.editName,
            selectedColor = state.editColor,
            isFavorite = state.editIsFavorite,
            onCollectionNameChange = { viewModel.onEvent(CollectionDetailEvent.OnEditNameChange(it)) },
            onColorChange = { viewModel.onEvent(CollectionDetailEvent.OnEditColorChange(it)) },
            onToggleFavorite = { viewModel.onEvent(CollectionDetailEvent.OnEditIsFavoriteChange(!state.editIsFavorite)) },
            onSave = { viewModel.onEvent(CollectionDetailEvent.OnEditConfirm) },
            onDismiss = { viewModel.onEvent(CollectionDetailEvent.OnEditDismiss) }
        )
    }

    // Delete Confirmation Dialog
    if (state.showDeleteDialog) {
        DeleteCollectionDialog(
            collectionName = state.collection?.name ?: "",
            linkCount = state.links.size,
            deleteWithLinks = state.deleteWithLinks,
            onDeleteWithLinksChange = { viewModel.onEvent(CollectionDetailEvent.OnDeleteWithLinksChange(it)) },
            onConfirm = { viewModel.onEvent(CollectionDetailEvent.OnDeleteConfirm) },
            onDismiss = { viewModel.onEvent(CollectionDetailEvent.OnDeleteDismiss) }
        )
    }
}

/**
 * Collection info card
 * Shows link count, creation date, and last update date
 */
@Composable
private fun CollectionInfoCard(
    linkCount: Int,
    createdAt: Long,
    updatedAt: Long,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    OutlinedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Link count
            Column {
                Text(
                    text = "Links",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = linkCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Created date
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Created",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(createdAt)),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Updated date
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Updated",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(updatedAt)),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Links list display
 */
@Composable
private fun LinksList(
    links: List<Link>,
    onLinkClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onArchiveClick: (String) -> Unit,
    onTrashClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = links,
            key = { it.id }
        ) { link ->
            LinkCard(
                link = link,
                onClick = { onLinkClick(link.id) },
                onFavoriteClick = { onFavoriteClick(link.id) },
                onArchiveClick = { onArchiveClick(link.id) },
                onTrashClick = { onTrashClick(link.id) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

/**
 * Links list with archived section
 * Shows active links first, then archived links in a separate section if showArchived is true
 */
@Composable
private fun LinksListWithArchived(
    activeLinks: List<Link>,
    archivedLinks: List<Link>,
    showArchived: Boolean,
    onLinkClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onArchiveClick: (String) -> Unit,
    onTrashClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active Links
        items(
            items = activeLinks,
            key = { it.id }
        ) { link ->
            LinkCard(
                link = link,
                onClick = { onLinkClick(link.id) },
                onFavoriteClick = { onFavoriteClick(link.id) },
                onArchiveClick = { onArchiveClick(link.id) },
                onTrashClick = { onTrashClick(link.id) },
                modifier = Modifier.animateItem()
            )
        }

        // Archived Links Section (if enabled and there are archived links)
        if (showArchived && archivedLinks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Archived (${archivedLinks.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(
                items = archivedLinks,
                key = { it.id }
            ) { link ->
                LinkCard(
                    link = link,
                    onClick = { onLinkClick(link.id) },
                    onFavoriteClick = { onFavoriteClick(link.id) },
                    onArchiveClick = { onArchiveClick(link.id) },
                    onTrashClick = { onTrashClick(link.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

/**
 * Edit Collection Dialog
 * Allows users to edit collection name, color, and favorite status
 */
@Composable
private fun EditCollectionDialog(
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Collection",
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
                Text("Save")
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
 * Delete Collection Dialog
 * Confirmation dialog with option to delete all links in the collection
 */
@Composable
private fun DeleteCollectionDialog(
    collectionName: String,
    linkCount: Int,
    deleteWithLinks: Boolean,
    onDeleteWithLinksChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Collection?",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Are you sure you want to delete \"$collectionName\"?",
                    style = MaterialTheme.typography.bodyLarge
                )

                if (linkCount > 0) {
                    Text(
                        text = "This collection contains $linkCount ${if (linkCount == 1) "link" else "links"}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Checkbox for deleting links
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeleteWithLinksChange(!deleteWithLinks) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = deleteWithLinks,
                            onCheckedChange = onDeleteWithLinksChange
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Delete all links in the collection",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (deleteWithLinks) {
                                    "All links will be permanently deleted"
                                } else {
                                    "Links will remain in your library without a collection"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Warning text
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ This action can't be undone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
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

/**
 * Count and Sort Row - Shows total count and sort dropdown
 */
@Composable
private fun CountAndSortRow(
    count: Int,
    sortType: SortType,
    onSortClick: (SortType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Count display
        Text(
            text = if (count == 1) "1 link" else "$count links",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Right: Sort button with dropdown
        Box {
            OutlinedButton(
                onClick = { showSortMenu = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = sortType.displayName,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Expand sort options",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Dropdown menu
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                SortType.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = sort.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onSortClick(sort)
                            showSortMenu = false
                        },
                        leadingIcon = if (sort == sortType) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else null
                    )
                }
            }
        }
    }
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
