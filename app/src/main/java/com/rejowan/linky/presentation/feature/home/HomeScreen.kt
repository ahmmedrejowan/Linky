package com.rejowan.linky.presentation.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import java.util.Calendar
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rejowan.linky.di.appModule
import com.rejowan.linky.di.databaseModule
import com.rejowan.linky.di.repositoryModule
import com.rejowan.linky.di.useCaseModule
import com.rejowan.linky.di.viewModelModule
import com.rejowan.linky.presentation.components.EmptyStates
import com.rejowan.linky.presentation.components.ErrorStates
import com.rejowan.linky.presentation.components.LinkCard
import com.rejowan.linky.presentation.components.LinkGridCard
import com.rejowan.linky.presentation.components.LoadingIndicator
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinApplication
import timber.log.Timber

/**
 * Home Screen - Main entry point showing all saved links
 * Features: Header with search, filters, pull-to-refresh, link list
 *
 * @param onAddLinkClick Callback to navigate to add link screen (FAB in MainActivity), accepts optional URL
 * @param onLinkClick Callback when a link is clicked
 * @param onNavigateToCollections Callback to navigate to collections (not used, handled by bottom nav)
 * @param onNavigateToSettings Callback to navigate to settings (not used, handled by bottom nav)
 * @param onSearchClick Callback to navigate to search screen
 * @param onSelectionModeChange Callback when selection mode changes (to hide/show bottom nav)
 * @param lastShareIntentHandledTime Timestamp of when share intent was last handled (0 = never)
 * @param modifier Modifier for styling
 * @param viewModel HomeViewModel injected via Koin
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    snackbarHostState: SnackbarHostState,
    lastShareIntentHandledTime: Long = 0L,
    onAddLinkClick: (String?) -> Unit,
    onLinkClick: (String) -> Unit,
    onNavigateToCollections: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSearchClick: () -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {},
    onExitRequest: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Collect clipboard checking preference
    // Use null as initial to avoid checking before preference is loaded from DataStore
    val themePreferences = remember { com.rejowan.linky.data.local.preferences.ThemePreferences(context) }
    val isClipboardCheckingEnabled by themePreferences.isClipboardCheckingEnabled()
        .collectAsState(initial = null)

    // Log preference changes
    LaunchedEffect(isClipboardCheckingEnabled) {
        Timber.tag("HomeScreen").d("Clipboard checking preference loaded/changed: $isClipboardCheckingEnabled")
    }

    // Throttle clipboard checks - only check once per 5 seconds
    var lastClipboardCheckTime by remember { mutableStateOf(0L) }

    // Collect and handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is HomeUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
                    )
                }
                is HomeUiEvent.ShowFavoriteToggled -> {
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
                        viewModel.onEvent(HomeEvent.OnToggleFavorite(event.linkId, !event.isFavorite, silent = true))
                    }
                }
                is HomeUiEvent.ShowArchiveToggled -> {
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
                        viewModel.onEvent(HomeEvent.OnArchiveLink(event.linkId, !event.isArchived, silent = true))
                    }
                }
                is HomeUiEvent.ShowLinkTrashed -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Link moved to trash",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        // Undo the trash action by restoring the link
                        // Need to implement restore functionality
                        viewModel.onEvent(HomeEvent.OnRestoreLink(event.linkId))
                    }
                }
                is HomeUiEvent.ShowBulkOperationResult -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Handle back press - exit selection mode first, then delegate to parent
    BackHandler {
        if (state.isSelectionMode) {
            viewModel.onEvent(HomeEvent.OnExitSelectionMode)
        } else {
            onExitRequest()
        }
    }

    // Check clipboard when screen resumes (throttled to once per 5 seconds)
    // Skip clipboard check for 5 seconds after share intent to avoid conflicts
    // Also skip if user has disabled clipboard checking in settings or preference not loaded yet
    DisposableEffect(lifecycleOwner, lastShareIntentHandledTime, isClipboardCheckingEnabled) {
        Timber.tag("HomeScreen").d("DisposableEffect created with isClipboardCheckingEnabled=$isClipboardCheckingEnabled")

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Skip if preference not loaded yet or clipboard checking is disabled in settings
                if (isClipboardCheckingEnabled == null) {
                    Timber.tag("HomeScreen").d("Screen resumed, skipping clipboard check (preference not loaded yet)")
                    return@LifecycleEventObserver
                }

                if (isClipboardCheckingEnabled == false) {
                    Timber.tag("HomeScreen").d("Screen resumed, skipping clipboard check (disabled in settings)")
                    return@LifecycleEventObserver
                }

                val currentTime = System.currentTimeMillis()
                val timeSinceShareIntent = currentTime - lastShareIntentHandledTime
                val shareIntentSuppressionMillis = 5000L // 5 seconds

                // Skip clipboard check if share intent was handled recently
                if (lastShareIntentHandledTime > 0 && timeSinceShareIntent < shareIntentSuppressionMillis) {
                    Timber.tag("HomeScreen").d("Screen resumed, skipping clipboard check (share intent handled ${timeSinceShareIntent}ms ago)")
                } else {
                    val timeSinceLastCheck = currentTime - lastClipboardCheckTime
                    val throttleMillis = 5000L // 5 seconds

                    if (timeSinceLastCheck >= throttleMillis) {
                        Timber.tag("HomeScreen").d("Screen resumed, checking clipboard (${timeSinceLastCheck}ms since last check)")
                        checkClipboardForUrl(context, viewModel)
                        lastClipboardCheckTime = currentTime
                    } else {
                        Timber.tag("HomeScreen").d("Screen resumed, skipping clipboard check (throttled, ${timeSinceLastCheck}ms since last check)")
                    }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Report selection mode changes to parent
    LaunchedEffect(state.isSelectionMode) {
        onSelectionModeChange(state.isSelectionMode)
    }

    // Scroll state for header collapse effect
    var headerScrollOffset by remember { mutableStateOf(0f) }
    val maxHeaderScroll = 200f // Pixels to scroll before fully collapsed

    // Calculate collapse progress (0 = expanded, 1 = collapsed)
    val headerCollapseProgress = (headerScrollOffset / maxHeaderScroll).coerceIn(0f, 1f)

    // Sort options sheet state
    var showSortSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Collapsing Header with Search Bar
            HomeHeader(
                collapseProgress = headerCollapseProgress,
                onSearchClick = onSearchClick,
                onSortClick = { showSortSheet = true }
            )

            // Filter Chips Row with View Mode Toggle
            CompactFilterRow(
                selectedFilter = state.filterType,
                onFilterSelected = { viewModel.onEvent(HomeEvent.OnFilterTypeChange(it)) },
                isGridView = state.viewMode == ViewMode.GRID,
                onViewModeToggle = {
                    val newMode = if (state.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                    viewModel.onEvent(HomeEvent.OnViewModeChange(newMode))
                },
                enabled = !state.isSelectionMode
            )

            // Content Area with Pull-to-Refresh
            PullToRefreshBox(
                isRefreshing = state.isLoading && state.links.isNotEmpty(),
                onRefresh = { viewModel.onEvent(HomeEvent.OnRefresh) },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                HomeContent(
                    state = state,
                    viewMode = state.viewMode,
                    onLinkClick = onLinkClick,
                    onFavoriteClick = { linkId, isFavorite ->
                        viewModel.onEvent(HomeEvent.OnToggleFavorite(linkId, isFavorite))
                    },
                    onMoreClick = { linkId ->
                        // Navigate to link detail (info sheet)
                        onLinkClick(linkId)
                    },
                    onAddLinkClick = { onAddLinkClick(null) },
                    onRetry = { viewModel.onEvent(HomeEvent.OnRefresh) },
                    onSortTypeChange = { viewModel.onEvent(HomeEvent.OnSortTypeChange(it)) },
                    onScrollOffsetChanged = { offset -> headerScrollOffset = offset },
                    // Bulk selection callbacks
                    onLongPress = { linkId ->
                        viewModel.onEvent(HomeEvent.OnEnterSelectionMode)
                        viewModel.onEvent(HomeEvent.OnToggleLinkSelection(linkId))
                    },
                    onToggleSelection = { linkId ->
                        viewModel.onEvent(HomeEvent.OnToggleLinkSelection(linkId))
                    }
                )
            }
        }

        // Selection Action Bar at the bottom (overlays content)
        AnimatedBulkActionsBar(
            visible = state.isSelectionMode,
            selectedCount = state.selectedCount,
            allSelected = state.allSelected,
            filterType = state.filterType,
            onClose = { viewModel.onEvent(HomeEvent.OnExitSelectionMode) },
            onSelectAll = { viewModel.onEvent(HomeEvent.OnSelectAll) },
            onDeselectAll = { viewModel.onEvent(HomeEvent.OnDeselectAll) },
            onDelete = { viewModel.onEvent(HomeEvent.OnBulkDelete) },
            onArchive = { viewModel.onEvent(HomeEvent.OnBulkArchive) },
            onUnarchive = { viewModel.onEvent(HomeEvent.OnBulkUnarchive) },
            onFavorite = { viewModel.onEvent(HomeEvent.OnBulkFavorite) },
            onUnfavorite = { viewModel.onEvent(HomeEvent.OnBulkUnfavorite) },
            onMove = { viewModel.onEvent(HomeEvent.OnShowBulkMoveSheet) },
            totalCount = state.links.size,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Sort Options Sheet
    if (showSortSheet) {
        SortOptionsSheet(
            currentSort = state.sortType,
            onSortSelected = { viewModel.onEvent(HomeEvent.OnSortTypeChange(it)) },
            onDismiss = { showSortSheet = false }
        )
    }

    // Clipboard URL Bottom Sheet
    val clipboardUrl = state.clipboardUrl
    if (state.showClipboardPrompt && clipboardUrl != null) {
        ClipboardUrlBottomSheet(
            url = clipboardUrl,
            onAddLink = {
                viewModel.onEvent(HomeEvent.OnDismissClipboardPrompt)
                onAddLinkClick(clipboardUrl)
            },
            onDismiss = {
                viewModel.onEvent(HomeEvent.OnDismissClipboardPrompt)
            }
        )
    }

    // Advanced Filter Sheet
    if (state.showAdvancedFilterSheet) {
        AdvancedFilterSheet(
            currentFilter = state.advancedFilter,
            availableDomains = state.availableDomains,
            availableCollections = state.availableCollections,
            availableTags = state.availableTags,
            onApply = { filter ->
                viewModel.onEvent(HomeEvent.OnApplyAdvancedFilter(filter))
            },
            onDismiss = {
                viewModel.onEvent(HomeEvent.OnDismissAdvancedFilterSheet)
            }
        )
    }

    // Bulk Move Sheet
    if (state.showBulkMoveSheet) {
        BulkMoveSheet(
            selectedCount = state.selectedCount,
            collections = state.availableCollections,
            onMoveToCollection = { collectionId ->
                viewModel.onEvent(HomeEvent.OnBulkMoveToCollection(collectionId))
            },
            onDismiss = {
                viewModel.onEvent(HomeEvent.OnDismissBulkMoveSheet)
            }
        )
    }
}


@Composable
@Preview
private fun HomeScreenPreview() {

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    KoinApplication(application = {
        androidContext(context)
        modules(
            listOf(
                appModule, databaseModule, repositoryModule, useCaseModule, viewModelModule
            )
        )
    }) {

        HomeScreen(
            snackbarHostState = snackbarHostState,
            onAddLinkClick = {},
            onLinkClick = {},
            onNavigateToCollections = {},
            onNavigateToSettings = {}
        )
    }
}


/**
 * Sort Options Bottom Sheet
 */
// Accent colors for sort categories
private val SortAccentPurple = Color(0xFF9575CD)
private val SortAccentBlue = Color(0xFF64B5F6)

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
                accentColor = SortAccentPurple,
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
                accentColor = SortAccentBlue,
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
        SortType.DATE_DESC -> Icons.Rounded.ArrowDownward to "Newest"
        SortType.DATE_ASC -> Icons.Rounded.ArrowUpward to "Oldest"
    }
}

/**
 * Content area - LazyColumn with links
 * Tracks scroll offset for header collapse effect
 */
@Composable
private fun HomeContent(
    state: HomeState,
    viewMode: ViewMode,
    onLinkClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    onMoreClick: (String) -> Unit,
    onAddLinkClick: () -> Unit,
    onRetry: () -> Unit,
    onSortTypeChange: (SortType) -> Unit,
    onScrollOffsetChanged: (Float) -> Unit,
    // Selection callbacks
    onLongPress: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Scroll states for both list and grid
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    // Report scroll offset to parent for header collapse (works for both modes)
    LaunchedEffect(
        viewMode,
        listState.firstVisibleItemIndex,
        listState.firstVisibleItemScrollOffset,
        gridState.firstVisibleItemIndex,
        gridState.firstVisibleItemScrollOffset
    ) {
        val offset = when (viewMode) {
            ViewMode.LIST -> {
                if (listState.firstVisibleItemIndex > 0) 200f
                else listState.firstVisibleItemScrollOffset.toFloat()
            }
            ViewMode.GRID -> {
                if (gridState.firstVisibleItemIndex > 0) 200f
                else gridState.firstVisibleItemScrollOffset.toFloat()
            }
        }
        onScrollOffsetChanged(offset)
    }

    // Legacy offset tracking for list (kept for compatibility)
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val offset = if (listState.firstVisibleItemIndex > 0) {
            200f // Fully collapsed after first item
        } else {
            listState.firstVisibleItemScrollOffset.toFloat()
        }
        onScrollOffsetChanged(offset)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Content area - changes based on state
        when {
            // Loading state (initial load only)
            state.isLoading && state.links.isEmpty() -> {
                LoadingIndicator(message = "Loading links...")
            }

            // Error state
            state.error != null -> {
                ErrorStates.GenericError(
                    errorMessage = state.error, onRetryClick = onRetry
                )
            }

            // Empty states
            state.links.isEmpty() -> {
                EmptyContent(
                    filterType = state.filterType,
                    onAddLinkClick = onAddLinkClick
                )
            }

            // Links list or grid
            else -> {
                when (viewMode) {
                    ViewMode.LIST -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Count header
                            item(key = "count") {
                                Text(
                                    text = if (state.links.size == 1) "1 link" else "${state.links.size} links",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }

                            // Link Items
                            items(
                                items = state.links,
                                key = { it.id }
                            ) { link ->
                                LinkCard(
                                    link = link,
                                    onClick = { onLinkClick(link.id) },
                                    onFavoriteClick = {
                                        onFavoriteClick(link.id, !link.isFavorite)
                                    },
                                    onMoreClick = { onMoreClick(link.id) },
                                    isSelectionMode = state.isSelectionMode,
                                    isSelected = state.selectedLinkIds.contains(link.id),
                                    onLongPress = { onLongPress(link.id) },
                                    onToggleSelection = { onToggleSelection(link.id) },
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .animateItem()
                                )
                            }
                        }
                    }

                    ViewMode.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 100.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Count header - spans full width
                            item(key = "count", span = { GridItemSpan(2) }) {
                                Text(
                                    text = if (state.links.size == 1) "1 link" else "${state.links.size} links",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Link Grid Items
                            items(
                                items = state.links,
                                key = { it.id }
                            ) { link ->
                                LinkGridCard(
                                    link = link,
                                    onClick = { onLinkClick(link.id) },
                                    onFavoriteClick = {
                                        onFavoriteClick(link.id, !link.isFavorite)
                                    },
                                    onMoreClick = { onMoreClick(link.id) },
                                    isSelectionMode = state.isSelectionMode,
                                    isSelected = state.selectedLinkIds.contains(link.id),
                                    onLongPress = { onLongPress(link.id) },
                                    onToggleSelection = { onToggleSelection(link.id) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Empty state content - Context-aware based on filter
 */
@Composable
private fun EmptyContent(
    filterType: FilterType,
    onAddLinkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (filterType) {
            FilterType.ALL -> {
                EmptyStates.NoLinks(onAddLinkClick = onAddLinkClick)
            }

            FilterType.FAVORITES -> {
                EmptyStates.NoFavorites()
            }

            FilterType.ARCHIVED -> {
                EmptyStates.NoArchivedLinks()
            }

            else -> {
                // Default empty state for any other filter types
                EmptyStates.NoLinks(onAddLinkClick = onAddLinkClick)
            }
        }
    }
}



/**
 * Bottom sheet for clipboard URL detection
 * Shows when a valid URL is detected in clipboard
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClipboardUrlBottomSheet(
    url: String,
    onAddLink: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with icon and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Link detected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Link Detected",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // URL Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Copied URL",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dismiss button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Dismiss")
                }

                // Add Link button
                Button(
                    onClick = onAddLink,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add link",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Link")
                }
            }
        }
    }
}

/**
 * Exit Confirmation Bottom Sheet
 * Asks user to confirm before exiting the app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExitConfirmationBottomSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with icon and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Exit app",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Exit Linky?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Message
            Text(
                text = "Are you sure you want to exit the app?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                // Exit button
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Exit")
                }
            }
        }
    }
}

/**
 * Check clipboard for URL and trigger ViewModel event if valid URL found
 */
private fun checkClipboardForUrl(context: Context, viewModel: HomeViewModel) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipData = clipboard?.primaryClip

        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0)?.text?.toString()
            if (!text.isNullOrBlank()) {
                Timber.tag("HomeScreen").d("Clipboard content detected: ${text.take(100)}")
                viewModel.onEvent(HomeEvent.OnClipboardUrlDetected(text))
            }
        }
    } catch (e: Exception) {
        Timber.tag("HomeScreen").e(e, "Error checking clipboard")
    }
}

// Search bar height for collapse calculation
private val SEARCH_BAR_HEIGHT = 50.dp

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

private fun getGreetingEmoji(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "\u2600\uFE0F" // Sun
        hour < 17 -> "\uD83C\uDF24\uFE0F" // Sun behind cloud
        hour < 21 -> "\uD83C\uDF19" // Moon
        else -> "\u2728" // Sparkles
    }
}

/**
 * Collapsing Home Header with greeting, search bar, and sort button
 */
@Composable
private fun HomeHeader(
    collapseProgress: Float,
    onSearchClick: () -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = getGreeting()
    val emoji = getGreetingEmoji()

    // Clamp progress between 0 and 1
    val progress = collapseProgress.coerceIn(0f, 1f)

    // Search bar collapses: height shrinks and alpha fades
    val searchBarHeight = SEARCH_BAR_HEIGHT * (1f - progress)
    val searchBarAlpha = (1f - progress * 1.5f).coerceIn(0f, 1f)

    // Search icon in toolbar appears as search bar disappears
    val searchIconAlpha = ((progress - 0.3f) * 2f).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Welcome header - stays fixed
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$greeting $emoji",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Your saved links",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Row {
                // Search icon - appears when search bar is collapsed
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.graphicsLayer { alpha = searchIconAlpha }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onSortClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Search bar - collapses as user scrolls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(searchBarHeight)
                .graphicsLayer { alpha = searchBarAlpha }
        ) {
            if (progress < 0.95f) { // Don't render when fully collapsed
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onSearchClick),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Search your links...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact Tab Row with filter chips and view mode toggle
 */
@Composable
private fun CompactFilterRow(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit,
    isGridView: Boolean,
    onViewModeToggle: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val disabledAlpha = if (enabled) 1f else 0.5f

    val filters = listOf(
        FilterType.ALL to "All",
        FilterType.FAVORITES to "Favorites"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Filter chips - Left aligned
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.alpha(disabledAlpha)
        ) {
            filters.forEach { (filterType, label) ->
                CompactFilterChip(
                    title = label,
                    isSelected = selectedFilter == filterType,
                    accentColor = accentColor,
                    onClick = { if (enabled) onFilterSelected(filterType) },
                    enabled = enabled
                )
            }
        }

        // View Mode Toggle - Right aligned
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.alpha(disabledAlpha)
        ) {
            ViewModeIcon(
                isSelected = !isGridView,
                onClick = { if (enabled && isGridView) onViewModeToggle() },
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ViewList,
                    contentDescription = "List view",
                    modifier = Modifier.size(18.dp)
                )
            }
            ViewModeIcon(
                isSelected = isGridView,
                onClick = { if (enabled && !isGridView) onViewModeToggle() },
                enabled = enabled
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
    enabled: Boolean = true,
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
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
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

@Composable
private fun CompactFilterChip(
    title: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.18f) else Color.Transparent,
        label = "chip background"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        label = "chip text"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
