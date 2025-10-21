package com.rejowan.linky.presentation.feature.home

import android.app.Activity
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.rejowan.linky.presentation.components.LoadingIndicator
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinApplication
import timber.log.Timber

/**
 * Home Screen - Main entry point showing all saved links
 * Features: Search, filters, pull-to-refresh, link list
 *
 * @param onAddLinkClick Callback to navigate to add link screen (FAB in MainActivity), accepts optional URL
 * @param onLinkClick Callback when a link is clicked
 * @param onNavigateToCollections Callback to navigate to collections (not used, handled by bottom nav)
 * @param onNavigateToSettings Callback to navigate to settings (not used, handled by bottom nav)
 * @param modifier Modifier for styling
 * @param viewModel HomeViewModel injected via Koin
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddLinkClick: (String?) -> Unit,
    onLinkClick: (String) -> Unit,
    onNavigateToCollections: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showExitDialog by remember { mutableStateOf(false) }

    // Handle back press - show exit confirmation
    BackHandler {
        showExitDialog = true
    }

    // Check clipboard when screen resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Timber.tag("HomeScreen").d("Screen resumed, checking clipboard")
                checkClipboardForUrl(context, viewModel)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Content Area with Pull-to-Refresh
    PullToRefreshBox(
        isRefreshing = state.isLoading && state.links.isNotEmpty(),
        onRefresh = { viewModel.onEvent(HomeEvent.OnRefresh) },
        modifier = Modifier.fillMaxSize()
    ) {
        HomeContent(
            state = state,
            onLinkClick = onLinkClick,
            onFavoriteClick = { linkId, isFavorite ->
                viewModel.onEvent(HomeEvent.OnToggleFavorite(linkId, isFavorite))
            },
            onAddLinkClick = { onAddLinkClick(null) },
            onRetry = { viewModel.onEvent(HomeEvent.OnRefresh) },
            onFilterTypeChange = { viewModel.onEvent(HomeEvent.OnFilterTypeChange(it)) },
            onSortTypeChange = { viewModel.onEvent(HomeEvent.OnSortTypeChange(it)) }
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

    // Exit Confirmation Bottom Sheet
    if (showExitDialog) {
        ExitConfirmationBottomSheet(
            onConfirm = {
                (context as? Activity)?.finish()
            },
            onDismiss = {
                showExitDialog = false
            }
        )
    }
}


@Composable
@Preview
private fun HomeScreenPreview() {

    val context = LocalContext.current
    KoinApplication(application = {
        androidContext(context)
        modules(
            listOf(
                appModule, databaseModule, repositoryModule, useCaseModule, viewModelModule
            )
        )
    }) {

        HomeScreen(
            onAddLinkClick = {},
            onLinkClick = {},
            onNavigateToCollections = {},
            onNavigateToSettings = {}
        )
    }
}


/**
 * Filter Segmented Buttons - Material 3 segmented button row for filters
 */
@Composable
private fun FilterSegmentedButtons(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        FilterType.ALL to "All",
        FilterType.FAVORITES to "Favorites",
        FilterType.ARCHIVED to "Archive"
    )

    SingleChoiceSegmentedButtonRow(
        modifier = modifier
    ) {
        filters.forEachIndexed { index, (filterType, label) ->
            SegmentedButton(
                selected = selectedFilter == filterType,
                onClick = { onFilterSelected(filterType) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = filters.size
                )
            ) {
                Text(text = label)
            }
        }
    }
}

/**
 * Content area - LazyColumn with header items and links
 * Filter tabs are always visible, even when list is empty
 */
@Composable
private fun HomeContent(
    state: HomeState,
    onLinkClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    onAddLinkClick: () -> Unit,
    onRetry: () -> Unit,
    onFilterTypeChange: (FilterType) -> Unit,
    onSortTypeChange: (SortType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Filter Segmented Buttons - ALWAYS VISIBLE
        FilterSegmentedButtons(
            selectedFilter = state.filterType,
            onFilterSelected = onFilterTypeChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        )

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

            // Links list with count/sort header
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header: Count and Sort Row
                    item(key = "count_sort") {
                        CountAndSortRow(
                            count = state.links.size,
                            sortType = state.sortType,
                            onSortClick = onSortTypeChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 0.dp)
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
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem()
                        )
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
                    imageVector = Icons.Filled.Sort,
                    contentDescription = null,
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
                    contentDescription = null,
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
                                    contentDescription = null,
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
                    contentDescription = null,
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
                        contentDescription = null,
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
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
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
