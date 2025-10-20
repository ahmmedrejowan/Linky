package com.rejowan.linky.presentation.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.usecase.link.SearchLinksUseCase
import com.rejowan.linky.domain.usecase.link.ToggleFavoriteUseCase
import com.rejowan.linky.util.ErrorHandler
import com.rejowan.linky.util.LinkOperation
import com.rejowan.linky.util.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for Search screen
 * Handles search functionality across all links
 */
class SearchViewModel(
    private val searchLinksUseCase: SearchLinksUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    // Job to track and cancel search operations
    private var searchJob: Job? = null

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = event.query) }
            }
            is SearchEvent.OnSearch -> {
                if (_state.value.searchQuery.isNotBlank()) {
                    performSearch(_state.value.searchQuery)
                }
            }
            is SearchEvent.OnClearSearch -> {
                searchJob?.cancel()
                searchJob = null
                _state.update {
                    it.copy(
                        searchQuery = "",
                        searchResults = emptyList(),
                        hasSearched = false,
                        error = null
                    )
                }
            }
            is SearchEvent.OnToggleFavorite -> {
                toggleFavorite(event.linkId, event.isFavorite)
            }
        }
    }

    private fun performSearch(query: String) {
        // Cancel previous search
        searchJob?.cancel()

        // Start new search
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isSearching = true, error = null, hasSearched = true) }

            searchLinksUseCase(query)
                .catch { e ->
                    val errorMessage = ErrorHandler.getLinkErrorMessage(e, LinkOperation.SEARCH)
                    _state.update {
                        it.copy(
                            isSearching = false,
                            error = errorMessage
                        )
                    }
                    Timber.e(e, "Search failed for query: $query")
                }
                .collect { links ->
                    _state.update {
                        it.copy(
                            searchResults = links,
                            isSearching = false,
                            error = null
                        )
                    }
                    Timber.d("Search completed: ${links.size} results for query '$query'")
                }
        }
    }

    private fun toggleFavorite(linkId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            when (val result = toggleFavoriteUseCase(linkId, isFavorite)) {
                is Result.Success -> {
                    Timber.d("Toggled favorite for link: $linkId")
                }
                is Result.Error -> {
                    val errorMessage = ErrorHandler.getLinkErrorMessage(
                        result.exception,
                        LinkOperation.TOGGLE_FAVORITE
                    )
                    _state.update { it.copy(error = errorMessage) }
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }
}

/**
 * Events for Search screen
 */
sealed class SearchEvent {
    data class OnSearchQueryChange(val query: String) : SearchEvent()
    data object OnSearch : SearchEvent()
    data object OnClearSearch : SearchEvent()
    data class OnToggleFavorite(val linkId: String, val isFavorite: Boolean) : SearchEvent()
}
