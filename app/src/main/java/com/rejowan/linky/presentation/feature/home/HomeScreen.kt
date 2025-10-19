package com.rejowan.linky.presentation.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rejowan.linky.di.appModule
import com.rejowan.linky.di.databaseModule
import com.rejowan.linky.di.repositoryModule
import com.rejowan.linky.di.useCaseModule
import com.rejowan.linky.di.viewModelModule
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.presentation.components.EmptyStates
import com.rejowan.linky.presentation.components.ErrorStates
import com.rejowan.linky.presentation.components.LinkCard
import com.rejowan.linky.presentation.components.LoadingIndicator
import com.rejowan.linky.presentation.components.SearchBar
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinApplication

/**
 * Home Screen - Main entry point showing all saved links
 * Features: Search, filters, pull-to-refresh, link list
 *
 * @param onAddLinkClick Callback to navigate to add link screen (FAB in MainActivity)
 * @param onLinkClick Callback when a link is clicked
 * @param onNavigateToCollections Callback to navigate to collections (not used, handled by bottom nav)
 * @param onNavigateToSettings Callback to navigate to settings (not used, handled by bottom nav)
 * @param modifier Modifier for styling
 * @param viewModel HomeViewModel injected via Koin
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddLinkClick: () -> Unit,
    onLinkClick: (String) -> Unit,
    onNavigateToCollections: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Search Bar
        SearchBar(
            query = state.searchQuery,
            onQueryChange = { viewModel.onEvent(HomeEvent.OnSearchQueryChange(it)) },
            placeholder = "Search links...",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Filter Chips
        FilterChipRow(
            selectedFilter = state.filterType,
            onFilterSelected = { viewModel.onEvent(HomeEvent.OnFilterTypeChange(it)) },
            modifier = Modifier.padding(vertical = 8.dp)
        )

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
                onAddLinkClick = onAddLinkClick,
                onRetry = { viewModel.onEvent(HomeEvent.OnRefresh) })
        }
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
            onNavigateToSettings = {})
    }
}


/**
 * Filter Chip Row - Horizontal scrollable filter tabs
 */
@Composable
private fun FilterChipRow(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        FilterType.ALL to "All",
        FilterType.FAVORITES to "⭐ Favorites",
        FilterType.ARCHIVED to "📦 Archived",
        FilterType.TRASH to "🗑️ Trash"
    )

    ScrollableTabRow(
        selectedTabIndex = filters.indexOfFirst { it.first == selectedFilter },
        modifier = modifier.fillMaxWidth(),
        edgePadding = 16.dp,
        divider = {},
        indicator = {}) {
        filters.forEach { (filterType, label) ->
            Tab(
                selected = selectedFilter == filterType,
                onClick = { onFilterSelected(filterType) },
                text = {
                    FilterChip(
                        selected = selectedFilter == filterType,
                        onClick = { onFilterSelected(filterType) },
                        label = { Text(label) })
                })
        }
    }
}

/**
 * Content area - Shows loading, error, empty, or links list based on state
 */
@Composable
private fun HomeContent(
    state: HomeState,
    onLinkClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    onAddLinkClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
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
                    searchQuery = state.searchQuery,
                    onAddLinkClick = onAddLinkClick
                )
            }

            // Links list
            else -> {
                LinksList(
                    links = state.links,
                    onLinkClick = onLinkClick,
                    onFavoriteClick = onFavoriteClick
                )
            }
        }
    }
}

/**
 * Empty state content - Context-aware based on filter and search
 */
@Composable
private fun EmptyContent(
    filterType: FilterType,
    searchQuery: String,
    onAddLinkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            // Search returned no results
            searchQuery.isNotBlank() -> {
                EmptyStates.NoSearchResults(searchQuery = searchQuery)
            }

            // Filter-specific empty states
            filterType == FilterType.ALL -> {
                EmptyStates.NoLinks(onAddLinkClick = onAddLinkClick)
            }

            filterType == FilterType.FAVORITES -> {
                EmptyStates.NoFavorites()
            }

            filterType == FilterType.ARCHIVED -> {
                EmptyStates.NoArchivedLinks()
            }

            filterType == FilterType.TRASH -> {
                EmptyStates.NoTrashedLinks()
            }
        }
    }
}

/**
 * Links list - LazyColumn with LinkCards
 */
@Composable
private fun LinksList(
    links: List<Link>,
    onLinkClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = links, key = { it.id }) { link ->
            LinkCard(link = link, onClick = { onLinkClick(link.id) }, onFavoriteClick = {
                onFavoriteClick(link.id, !link.isFavorite)
            })
        }
    }
}
