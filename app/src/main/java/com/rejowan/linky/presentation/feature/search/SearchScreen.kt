package com.rejowan.linky.presentation.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rejowan.linky.presentation.components.EmptyStates
import com.rejowan.linky.presentation.components.ErrorStates
import com.rejowan.linky.presentation.components.LinkCard
import com.rejowan.linky.presentation.components.LoadingIndicator
import com.rejowan.linky.presentation.components.SearchBar
import org.koin.androidx.compose.koinViewModel

/**
 * Search Screen - Dedicated search functionality
 * Features: Search bar, search results, empty states
 *
 * @param onLinkClick Callback when a link is clicked
 * @param modifier Modifier for styling
 * @param viewModel SearchViewModel injected via Koin
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is SearchUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
                    )
                }
                is SearchUiEvent.ShowFavoriteToggled -> {
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
                        viewModel.onEvent(SearchEvent.OnToggleFavorite(event.linkId, !event.isFavorite, silent = true))
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isSearching,
            onRefresh = {
                if (state.searchQuery.isNotBlank()) {
                    viewModel.onEvent(SearchEvent.OnSearch)
                }
            },
            // Focus management: Tap outside search bar to hide keyboard
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Search Bar - Always visible
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.onEvent(SearchEvent.OnSearchQueryChange(it)) },
                onSearch = {
                    viewModel.onEvent(SearchEvent.OnSearch)
                    focusManager.clearFocus()
                },
                onClear = { viewModel.onEvent(SearchEvent.OnClearSearch) },
                placeholder = "Search all links...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp)
            )

            // Content Area
            SearchContent(
                state = state,
                onLinkClick = onLinkClick,
                onFavoriteClick = { linkId, isFavorite ->
                    viewModel.onEvent(SearchEvent.OnToggleFavorite(linkId, isFavorite))
                },
                    onRetry = { viewModel.onEvent(SearchEvent.OnSearch) }
                )
            }
        }
    }
}

/**
 * Content area - Shows different states based on search
 */
@Composable
private fun SearchContent(
    state: SearchState,
    onLinkClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            // Loading state
            state.isSearching -> {
                LoadingIndicator(message = "Searching...")
            }

            // Error state
            state.error != null -> {
                ErrorStates.GenericError(
                    errorMessage = state.error,
                    onRetryClick = onRetry
                )
            }

            // No search performed yet
            !state.hasSearched -> {
                EmptySearchState()
            }

            // No results found
            state.searchResults.isEmpty() && state.hasSearched -> {
                EmptyStates.NoSearchResults(searchQuery = state.searchQuery)
            }

            // Search results
            else -> {
                SearchResultsList(
                    results = state.searchResults,
                    onLinkClick = onLinkClick,
                    onFavoriteClick = onFavoriteClick
                )
            }
        }
    }
}

/**
 * Empty state when no search has been performed
 */
@Composable
private fun EmptySearchState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔍",
            style = MaterialTheme.typography.displayLarge
        )
        Text(
            text = "Search for links",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Enter keywords to search across all your saved links",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp)
        )
    }
}

/**
 * Search results list
 */
@Composable
private fun SearchResultsList(
    results: List<com.rejowan.linky.domain.model.Link>,
    onLinkClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Results count header
        item(key = "results_count") {
            Text(
                text = if (results.size == 1) "1 result" else "${results.size} results",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Link cards
        items(
            items = results,
            key = { it.id }
        ) { link ->
            LinkCard(
                link = link,
                onClick = { onLinkClick(link.id) },
                onFavoriteClick = {
                    onFavoriteClick(link.id, !link.isFavorite)
                },
                modifier = Modifier.animateItem()
            )
        }
    }
}
