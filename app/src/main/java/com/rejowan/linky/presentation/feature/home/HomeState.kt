package com.rejowan.linky.presentation.feature.home

import com.rejowan.linky.domain.model.Link

data class HomeState(
    val links: List<Link> = emptyList(),
    val searchQuery: String = "",
    val filterType: FilterType = FilterType.ALL,
    val sortType: SortType = SortType.DATE_ADDED_DESC,
    val isLoading: Boolean = false,
    val error: String? = null,
    val allLinksCount: Int = 0,
    val favoriteLinksCount: Int = 0,
    val archivedLinksCount: Int = 0
)

enum class FilterType {
    ALL,
    FAVORITES,
    ARCHIVED,
    TRASH
}

enum class SortType(val displayName: String) {
    DATE_ADDED_DESC("Newest first"),
    DATE_ADDED_ASC("Oldest first"),
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    LAST_MODIFIED("Last modified")
}
