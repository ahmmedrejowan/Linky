package com.rejowan.linky.presentation.feature.collectiondetail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.presentation.components.EmptyStates
import com.rejowan.linky.presentation.components.ErrorStates
import com.rejowan.linky.presentation.components.LinkCard
import com.rejowan.linky.presentation.components.LinkGridCard
import com.rejowan.linky.presentation.components.LoadingIndicator
import com.rejowan.linky.presentation.feature.home.SortType
import com.rejowan.linky.presentation.feature.home.ViewMode
import com.rejowan.linky.ui.theme.SoftAccents
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.HorizontalDivider
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import com.rejowan.linky.presentation.feature.home.AnimatedBulkActionsBar
import com.rejowan.linky.presentation.feature.home.FilterType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Collection Detail Screen
 * Shows all links within a specific collection
 *
 * Features:
 * - Collection info header (name, color)
 * - TopAppBar actions: add link, edit/delete menu
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
    var showSortSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Link info bottom sheet state
    var selectedLinkForInfo by remember { mutableStateOf<String?>(null) }
    val selectedLink = selectedLinkForInfo?.let { id -> state.links.find { it.id == id } }

    // Handle back press - exit selection mode first
    BackHandler(enabled = state.isSelectionMode) {
        viewModel.onEvent(CollectionDetailEvent.OnExitSelectionMode)
    }

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
                        // Undo the favorite toggle (silent to prevent another snackbar)
                        viewModel.onEvent(CollectionDetailEvent.OnToggleLinkFavorite(event.linkId, silent = true))
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
                        // Undo the archive toggle (silent to prevent another snackbar)
                        viewModel.onEvent(CollectionDetailEvent.OnArchiveLink(event.linkId, silent = true))
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
                is CollectionDetailUiEvent.ShowBulkOperationResult -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.collection?.name ?: "Collection",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                    // Three-dot menu only
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PullToRefreshBox(
                isRefreshing = state.isLoading && state.links.isNotEmpty(),
                onRefresh = { viewModel.onEvent(CollectionDetailEvent.OnRefresh) },
                modifier = Modifier.fillMaxSize()
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
                        // View Mode Toggle Row with Sort
                        ViewModeRow(
                            count = state.links.size,
                            isGridView = state.viewMode == ViewMode.GRID,
                            onViewModeToggle = {
                                val newMode = if (state.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                                viewModel.onEvent(CollectionDetailEvent.OnViewModeChange(newMode))
                            },
                            onSortClick = { showSortSheet = true }
                        )

                        // Links list/grid or empty state
                        if (state.links.isEmpty()) {
                            EmptyStates.EmptyCollection(
                                onAddLinkClick = {
                                    state.collection?.id?.let { onAddLinkClick(it) }
                                }
                            )
                        } else {
                            LinksContent(
                                links = state.links,
                                viewMode = state.viewMode,
                                onLinkClick = onLinkClick,
                                onFavoriteClick = { linkId ->
                                    viewModel.onEvent(CollectionDetailEvent.OnToggleLinkFavorite(linkId))
                                },
                                onMoreClick = { linkId ->
                                    selectedLinkForInfo = linkId
                                },
                                onAddLinkClick = {
                                    state.collection?.id?.let { onAddLinkClick(it) }
                                },
                                isSelectionMode = state.isSelectionMode,
                                selectedLinkIds = state.selectedLinkIds,
                                onLongPress = { linkId ->
                                    viewModel.onEvent(CollectionDetailEvent.OnEnterSelectionMode)
                                    viewModel.onEvent(CollectionDetailEvent.OnToggleLinkSelection(linkId))
                                },
                                onToggleSelection = { linkId ->
                                    viewModel.onEvent(CollectionDetailEvent.OnToggleLinkSelection(linkId))
                                }
                            )
                        }
                    }
                }
            }
        }

            // Bulk Actions Bar at bottom
            AnimatedBulkActionsBar(
                visible = state.isSelectionMode,
                selectedCount = state.selectedCount,
                allSelected = state.allSelected,
                filterType = FilterType.ALL,
                onClose = { viewModel.onEvent(CollectionDetailEvent.OnExitSelectionMode) },
                onSelectAll = { viewModel.onEvent(CollectionDetailEvent.OnSelectAll) },
                onDeselectAll = { viewModel.onEvent(CollectionDetailEvent.OnDeselectAll) },
                onDelete = { viewModel.onEvent(CollectionDetailEvent.OnBulkDelete) },
                onFavorite = { viewModel.onEvent(CollectionDetailEvent.OnBulkFavorite) },
                onUnfavorite = { viewModel.onEvent(CollectionDetailEvent.OnBulkUnfavorite) },
                onMove = { /* Not applicable in collection detail */ },
                totalCount = state.links.size,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // Edit Collection Dialog
    if (state.showEditDialog) {
        EditCollectionDialog(
            collectionName = state.editName,
            selectedColor = state.editColor,
            onCollectionNameChange = { viewModel.onEvent(CollectionDetailEvent.OnEditNameChange(it)) },
            onColorChange = { viewModel.onEvent(CollectionDetailEvent.OnEditColorChange(it)) },
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

    // Sort Options Sheet
    if (showSortSheet) {
        SortOptionsSheet(
            currentSort = state.sortType,
            onSortSelected = { viewModel.onEvent(CollectionDetailEvent.OnSortTypeChange(it)) },
            onDismiss = { showSortSheet = false }
        )
    }

    // Link Info Bottom Sheet
    if (selectedLink != null) {
        LinkInfoBottomSheet(
            link = selectedLink,
            onOpenClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(selectedLink.url))
                context.startActivity(intent)
            },
            onEditClick = {
                selectedLinkForInfo = null
                onLinkClick(selectedLink.id)
            },
            onFavoriteClick = {
                viewModel.onEvent(CollectionDetailEvent.OnToggleLinkFavorite(selectedLink.id))
            },
            onDeleteClick = {
                viewModel.onEvent(CollectionDetailEvent.OnTrashLink(selectedLink.id))
                selectedLinkForInfo = null
            },
            onDismiss = {
                selectedLinkForInfo = null
            }
        )
    }
}

/**
 * View mode toggle row with count and sort
 */
@Composable
private fun ViewModeRow(
    count: Int,
    isGridView: Boolean,
    onViewModeToggle: () -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Count display
        Text(
            text = if (count == 1) "1 link" else "$count links",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Sort and View Mode Toggle
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sort button
            ViewModeIcon(
                isSelected = false,
                onClick = onSortClick
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                    contentDescription = "Sort",
                    modifier = Modifier.size(18.dp)
                )
            }

            // View mode toggles
            ViewModeIcon(
                isSelected = !isGridView,
                onClick = { if (isGridView) onViewModeToggle() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ViewList,
                    contentDescription = "List view",
                    modifier = Modifier.size(18.dp)
                )
            }
            ViewModeIcon(
                isSelected = isGridView,
                onClick = { if (!isGridView) onViewModeToggle() }
            ) {
                Icon(
                    imageVector = Icons.Outlined.GridView,
                    contentDescription = "Grid view",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ViewModeIcon(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.surfaceContainerHighest
        else
            Color.Transparent,
        label = "view mode background"
    )
    val tint by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.onSurface
        else
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        label = "view mode tint"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor,
        contentColor = tint
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

/**
 * Links content - supports both list and grid view with Add Link button at end
 */
@Composable
private fun LinksContent(
    links: List<Link>,
    viewMode: ViewMode,
    onLinkClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onMoreClick: (String) -> Unit,
    onAddLinkClick: () -> Unit,
    isSelectionMode: Boolean,
    selectedLinkIds: Set<String>,
    onLongPress: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (viewMode) {
        ViewMode.LIST -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
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
                        onMoreClick = { onMoreClick(link.id) },
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedLinkIds.contains(link.id),
                        onLongPress = { onLongPress(link.id) },
                        onToggleSelection = { onToggleSelection(link.id) },
                        modifier = Modifier.animateItem()
                    )
                }
                // Add Link button at the end
                item(key = "add_link") {
                    AddLinkCard(
                        onClick = onAddLinkClick,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
        ViewMode.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = links,
                    key = { it.id }
                ) { link ->
                    LinkGridCard(
                        link = link,
                        onClick = { onLinkClick(link.id) },
                        onFavoriteClick = { onFavoriteClick(link.id) },
                        onMoreClick = { onMoreClick(link.id) },
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedLinkIds.contains(link.id),
                        onLongPress = { onLongPress(link.id) },
                        onToggleSelection = { onToggleSelection(link.id) },
                        modifier = Modifier.animateItem()
                    )
                }
                // Add Link button at the end (spans full width or single cell)
                item(key = "add_link") {
                    AddLinkGridCard(
                        onClick = onAddLinkClick,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

/**
 * Add Link card for list view
 */
@Composable
private fun AddLinkCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add Link",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Add Link card for grid view
 */
@Composable
private fun AddLinkGridCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "Add Link",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Sort Options Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortOptionsSheet(
    currentSort: SortType,
    onSortSelected: (SortType) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            SortSheetHeader()

            Spacer(modifier = Modifier.height(12.dp))

            // Name Sort Category
            SortCategoryCard(
                title = "Alphabetical",
                description = "Sort by link title",
                icon = Icons.Outlined.SortByAlpha,
                accentColor = SoftAccents.Purple,
                options = listOf(SortType.NAME_ASC, SortType.NAME_DESC),
                currentSort = currentSort,
                onSortSelected = {
                    onSortSelected(it)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Date Sort Category
            SortCategoryCard(
                title = "Date Added",
                description = "Sort by creation time",
                icon = Icons.Outlined.AccessTime,
                accentColor = SoftAccents.Blue,
                options = listOf(SortType.DATE_DESC, SortType.DATE_ASC),
                currentSort = currentSort,
                onSortSelected = {
                    onSortSelected(it)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun SortSheetHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = null,
                modifier = Modifier
                    .padding(6.dp)
                    .size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = "Sort Links",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Choose how to sort your links",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SortCategoryCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    options: List<SortType>,
    currentSort: SortType,
    onSortSelected: (SortType) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSelection = options.contains(currentSort)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (hasSelection) {
            accentColor.copy(alpha = 0.06f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Category header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp),
                        tint = accentColor
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Selection indicator
                if (hasSelection) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Selected",
                            modifier = Modifier
                                .padding(4.dp)
                                .size(14.dp),
                            tint = accentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sort options row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { option ->
                    SortOptionChip(
                        option = option,
                        isSelected = option == currentSort,
                        accentColor = accentColor,
                        onClick = { onSortSelected(option) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SortOptionChip(
    option: SortType,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(200),
        label = "chip bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.5f)
        else Color.Transparent,
        animationSpec = tween(200),
        label = "chip border"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) accentColor
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "chip content"
    )

    val (chipIcon, chipLabel) = getSortOptionDetails(option)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(
                    width = 1.5.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(10.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Direction icon
            Icon(
                imageVector = chipIcon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = contentColor
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Label
            Text(
                text = chipLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = contentColor
            )

            // Selected check
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.size(9.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private fun getSortOptionDetails(option: SortType): Pair<ImageVector, String> {
    return when (option) {
        SortType.NAME_ASC -> Icons.Rounded.ArrowUpward to "A → Z"
        SortType.NAME_DESC -> Icons.Rounded.ArrowDownward to "Z → A"
        SortType.DATE_DESC -> Icons.Rounded.ArrowDownward to "Latest"
        SortType.DATE_ASC -> Icons.Rounded.ArrowUpward to "Earliest"
    }
}

/**
 * Edit Collection Dialog
 * Allows users to edit collection name and color
 */
@Composable
private fun EditCollectionDialog(
    collectionName: String,
    selectedColor: String?,
    onCollectionNameChange: (String) -> Unit,
    onColorChange: (String?) -> Unit,
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
 * Link Info Bottom Sheet - Shows link details with quick actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkInfoBottomSheet(
    link: Link,
    onOpenClick: () -> Unit,
    onEditClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Preview Image
            if (link.previewImagePath != null || link.previewUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = link.previewImagePath ?: link.previewUrl,
                        contentDescription = "Link preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Title
            Text(
                text = link.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // URL
            Text(
                text = link.url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Description
            if (!link.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = link.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Note
            if (!link.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = link.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Created date
            Text(
                text = "Added ${dateFormatter.format(Date(link.createdAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Open in browser
                LinkInfoActionButton(
                    icon = Icons.Filled.OpenInBrowser,
                    label = "Open",
                    onClick = {
                        onOpenClick()
                        onDismiss()
                    }
                )

                // Edit
                LinkInfoActionButton(
                    icon = Icons.Filled.Edit,
                    label = "Edit",
                    onClick = {
                        onEditClick()
                    }
                )

                // Favorite toggle
                LinkInfoActionButton(
                    icon = if (link.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    label = if (link.isFavorite) "Unfavorite" else "Favorite",
                    tint = if (link.isFavorite) SoftAccents.Pink else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        onFavoriteClick()
                    }
                )

                // Delete
                LinkInfoActionButton(
                    icon = Icons.Filled.Delete,
                    label = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        onDeleteClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun LinkInfoActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}
