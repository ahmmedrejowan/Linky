package com.rejowan.linky.presentation.feature.search

import com.rejowan.linky.domain.model.Link

/**
 * State for Search screen
 */
data class SearchState(
    val searchQuery: String = "",
    val searchResults: List<Link> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false  // Track if user has performed a search
)
