package com.rejowan.linky.presentation.feature.search

import com.rejowan.linky.domain.model.CollectionWithLinkCount
import com.rejowan.linky.domain.model.Link

/**
 * State for Search screen
 */
data class SearchState(
    val searchQuery: String = "",
    val linkResults: List<Link> = emptyList(),
    val collectionResults: List<CollectionWithLinkCount> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false  // Track if user has performed a search
) {
    // Total result count
    val totalResults: Int get() = linkResults.size + collectionResults.size

    // For backwards compatibility
    val searchResults: List<Link> get() = linkResults
}
