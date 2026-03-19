package com.rejowan.linky.presentation.feature.collections

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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rejowan.linky.domain.model.CollectionWithLinkCount
import com.rejowan.linky.presentation.components.EmptyStates
import com.rejowan.linky.presentation.components.ErrorStates
import com.rejowan.linky.presentation.components.CollectionCard
import com.rejowan.linky.presentation.components.CollectionGridCard
import com.rejowan.linky.presentation.components.LoadingIndicator
import com.rejowan.linky.presentation.components.ShimmerCollectionGrid
import com.rejowan.linky.presentation.components.ShimmerCollectionList
import com.rejowan.linky.presentation.feature.home.ViewMode
import com.rejowan.linky.ui.theme.SoftAccents
import org.koin.androidx.compose.koinViewModel
import androidx.core.graphics.toColorInt

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
    var showSortSheet by remember { mutableStateOf(false) }
    var selectedCollectionForInfo by remember { mutableStateOf<CollectionWithLinkCount?>(null) }

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

    // Listen to UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is CollectionsUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isLoading && state.collections.isNotEmpty(),
        onRefresh = { viewModel.onEvent(CollectionsEvent.OnRefresh) },
        modifier = modifier.fillMaxSize()
    ) {
        when {
            // Loading state - show shimmer
            state.isLoading && state.collections.isEmpty() -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Show header even during loading
                    CollectionsHeader(
                        onSortClick = { showSortSheet = true }
                    )
                    // Shimmer based on view mode
                    when (state.viewMode) {
                        ViewMode.LIST -> ShimmerCollectionList()
                        ViewMode.GRID -> ShimmerCollectionGrid()
                    }
                }
            }

            // Error state
            state.error != null && state.collections.isEmpty() -> {
                ErrorStates.GenericError(
                    errorMessage = state.error ?: "Unknown error",
                    onRetryClick = { viewModel.onEvent(CollectionsEvent.OnRefresh) }
                )
            }

            // Empty state or Collections list - both show header
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header with title and sort button (always shown)
                    CollectionsHeader(
                        onSortClick = { showSortSheet = true }
                    )

                    if (state.collections.isEmpty()) {
                        // Empty state
                        EmptyStates.NoCollections(
                            onCreateCollectionClick = { viewModel.onEvent(CollectionsEvent.OnCreateCollection) }
                        )
                    } else {
                        // View mode toggle row with count
                        ViewModeRow(
                            count = state.collections.size,
                            isGridView = state.viewMode == ViewMode.GRID,
                            onViewModeToggle = {
                                val newMode = if (state.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                                viewModel.onEvent(CollectionsEvent.OnViewModeChange(newMode))
                            }
                        )

                        // Collections List/Grid
                        CollectionsContent(
                            collections = state.collections,
                            viewMode = state.viewMode,
                            onCollectionClick = onCollectionClick,
                            onCollectionLongPress = { collection ->
                                selectedCollectionForInfo = collection
                            }
                        )
                    }
                }
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

    // Sort Options Sheet
    if (showSortSheet) {
        SortOptionsSheet(
            currentSort = state.sortType,
            onSortSelected = { viewModel.onEvent(CollectionsEvent.OnSortTypeChange(it)) },
            onDismiss = { showSortSheet = false }
        )
    }

    // Collection Info Bottom Sheet
    selectedCollectionForInfo?.let { collection ->
        CollectionInfoBottomSheet(
            collection = collection,
            onDismiss = { selectedCollectionForInfo = null },
            onOpenClick = {
                selectedCollectionForInfo = null
                onCollectionClick(collection.collection.id)
            },
            onEditClick = {
                selectedCollectionForInfo = null
                viewModel.onEvent(CollectionsEvent.OnEditCollection(collection))
            },
            onDeleteClick = {
                selectedCollectionForInfo = null
                viewModel.onEvent(CollectionsEvent.OnShowDeleteDialog(collection))
            }
        )
    }

    // Edit Collection Dialog
    if (state.showEditDialog) {
        EditCollectionDialog(
            collectionName = state.editCollectionName,
            selectedColor = state.editCollectionColor,
            onCollectionNameChange = { viewModel.onEvent(CollectionsEvent.OnEditCollectionNameChange(it)) },
            onColorChange = { viewModel.onEvent(CollectionsEvent.OnEditCollectionColorChange(it)) },
            onSave = { viewModel.onEvent(CollectionsEvent.OnSaveEditedCollection) },
            onDismiss = { viewModel.onEvent(CollectionsEvent.OnDismissEditDialog) }
        )
    }

    // Delete Confirmation Dialog
    if (state.showDeleteDialog) {
        DeleteCollectionDialog(
            collectionName = state.deletingCollection?.collection?.name ?: "",
            linkCount = state.deletingCollection?.linkCount ?: 0,
            onConfirm = { viewModel.onEvent(CollectionsEvent.OnConfirmDelete) },
            onDismiss = { viewModel.onEvent(CollectionsEvent.OnDismissDeleteDialog) }
        )
    }
}

/**
 * Collections header with title and sort button
 */
@Composable
private fun CollectionsHeader(
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Collections",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSortClick),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = "Sort",
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * View mode toggle row with count
 */
@Composable
private fun ViewModeRow(
    count: Int,
    isGridView: Boolean,
    onViewModeToggle: () -> Unit,
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
            text = if (count == 1) "1 collection" else "$count collections",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // View Mode Toggle
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
 * Collections content - supports both list and grid view
 */
@Composable
private fun CollectionsContent(
    collections: List<CollectionWithLinkCount>,
    viewMode: ViewMode,
    onCollectionClick: (String) -> Unit,
    onCollectionLongPress: (CollectionWithLinkCount) -> Unit,
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
                    items = collections,
                    key = { it.collection.id }
                ) { collectionWithCount ->
                    CollectionCard(
                        collection = collectionWithCount.collection,
                        linkCount = collectionWithCount.linkCount,
                        onClick = { onCollectionClick(collectionWithCount.collection.id) },
                        onLongPress = { onCollectionLongPress(collectionWithCount) },
                        linkPreviews = collectionWithCount.linkPreviews,
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
                    items = collections,
                    key = { it.collection.id }
                ) { collectionWithCount ->
                    CollectionGridCard(
                        collection = collectionWithCount.collection,
                        linkCount = collectionWithCount.linkCount,
                        onClick = { onCollectionClick(collectionWithCount.collection.id) },
                        onLongPress = { onCollectionLongPress(collectionWithCount) },
                        modifier = Modifier.animateItem()
                    )
                }
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
    currentSort: CollectionSortType,
    onSortSelected: (CollectionSortType) -> Unit,
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
                description = "Sort by collection name",
                icon = Icons.Outlined.SortByAlpha,
                accentColor = SoftAccents.Purple,
                options = listOf(CollectionSortType.NAME_ASC, CollectionSortType.NAME_DESC),
                currentSort = currentSort,
                onSortSelected = {
                    onSortSelected(it)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Date Sort Category
            SortCategoryCard(
                title = "Date Created",
                description = "Sort by creation time",
                icon = Icons.Outlined.AccessTime,
                accentColor = SoftAccents.Blue,
                options = listOf(CollectionSortType.DATE_CREATED_DESC, CollectionSortType.DATE_CREATED_ASC),
                currentSort = currentSort,
                onSortSelected = {
                    onSortSelected(it)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Link count category
            SortCategoryCard(
                title = "Link Count",
                description = "Sort by number of links",
                icon = Icons.Outlined.GridView,
                accentColor = SoftAccents.Teal,
                options = listOf(CollectionSortType.MOST_LINKS, CollectionSortType.LEAST_LINKS),
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
                text = "Sort Collections",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Choose how to sort your collections",
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
    options: List<CollectionSortType>,
    currentSort: CollectionSortType,
    onSortSelected: (CollectionSortType) -> Unit,
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
    option: CollectionSortType,
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

private fun getSortOptionDetails(option: CollectionSortType): Pair<ImageVector, String> {
    return when (option) {
        CollectionSortType.NAME_ASC -> Icons.Rounded.ArrowUpward to "A → Z"
        CollectionSortType.NAME_DESC -> Icons.Rounded.ArrowDownward to "Z → A"
        CollectionSortType.DATE_CREATED_DESC -> Icons.Rounded.ArrowDownward to "Newest"
        CollectionSortType.DATE_CREATED_ASC -> Icons.Rounded.ArrowUpward to "Oldest"
        CollectionSortType.LAST_MODIFIED -> Icons.Rounded.ArrowDownward to "Recent"
        CollectionSortType.MOST_LINKS -> Icons.Rounded.ArrowDownward to "Most"
        CollectionSortType.LEAST_LINKS -> Icons.Rounded.ArrowUpward to "Least"
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
    onColorChange: (String?) -> Unit,
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
                    Color(colorHex.toColorInt())
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
 * Collection Info Bottom Sheet
 * Shows options for a collection: Open, Edit, Delete
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionInfoBottomSheet(
    collection: CollectionWithLinkCount,
    onDismiss: () -> Unit,
    onOpenClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val collectionColor = collection.collection.color?.let {
        try { Color(it.toColorInt()) } catch (e: Exception) { null }
    } ?: MaterialTheme.colorScheme.primary

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
            // Header with collection info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = collectionColor.copy(alpha = 0.15f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp),
                        tint = collectionColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = collection.collection.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = if (collection.linkCount == 1) "1 link" else "${collection.linkCount} links",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            CollectionSheetAction(
                icon = Icons.Outlined.FolderOpen,
                label = "Open Collection",
                color = SoftAccents.Blue,
                onClick = onOpenClick
            )

            Spacer(modifier = Modifier.height(8.dp))

            CollectionSheetAction(
                icon = Icons.Outlined.Edit,
                label = "Edit Collection",
                color = SoftAccents.Purple,
                onClick = onEditClick
            )

            Spacer(modifier = Modifier.height(8.dp))

            CollectionSheetAction(
                icon = Icons.Outlined.Delete,
                label = "Delete Collection",
                color = SoftAccents.Pink,
                onClick = onDeleteClick
            )
        }
    }
}

@Composable
private fun CollectionSheetAction(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp),
                    tint = color
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Edit Collection Dialog
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
                OutlinedTextField(
                    value = collectionName,
                    onValueChange = onCollectionNameChange,
                    label = { Text("Collection Name") },
                    placeholder = { Text("Enter collection name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

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
 * Delete Collection Confirmation Dialog
 */
@Composable
private fun DeleteCollectionDialog(
    collectionName: String,
    linkCount: Int,
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
            Column {
                Text(
                    text = "Are you sure you want to delete \"$collectionName\"?",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (linkCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The $linkCount ${if (linkCount == 1) "link" else "links"} in this collection will be moved to uncategorized.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

