package com.rejowan.linky.presentation.feature.home

import com.rejowan.linky.domain.model.Link

data class HomeState(
    val links: List<Link> = emptyList(),
    val filterType: FilterType = FilterType.ALL,
    val sortType: SortType = SortType.DATE_DESC,
    val viewMode: ViewMode = ViewMode.LIST,
    val isLoading: Boolean = false,
    val error: String? = null, // For critical load failures (full-screen error state)
    val allLinksCount: Int = 0,
    val favoriteLinksCount: Int = 0,
    val archivedLinksCount: Int = 0,
    // Clipboard detection
    val clipboardUrl: String? = null,
    val showClipboardPrompt: Boolean = false,
    val promptedUrls: Set<String> = emptySet(), // Track URLs we've already prompted for
    // Advanced filtering
    val advancedFilter: AdvancedFilter = AdvancedFilter.EMPTY,
    val showAdvancedFilterSheet: Boolean = false,
    val availableDomains: List<DomainInfo> = emptyList(),
    val availableCollections: List<CollectionFilterInfo> = emptyList(),
    val availableTags: List<TagFilterInfo> = emptyList(),
    // Bulk selection
    val isSelectionMode: Boolean = false,
    val selectedLinkIds: Set<String> = emptySet(),
    val showBulkMoveSheet: Boolean = false
) {
    val selectedCount: Int get() = selectedLinkIds.size
    val allSelected: Boolean get() = links.isNotEmpty() && selectedLinkIds.size == links.size
}

enum class FilterType {
    ALL,
    FAVORITES,
    ARCHIVED,
    TRASH
}

enum class SortType(val displayName: String) {
    DATE_DESC("Newest"),
    DATE_ASC("Oldest"),
    NAME_ASC("A → Z"),
    NAME_DESC("Z → A")
}

enum class ViewMode {
    LIST,
    GRID
}
