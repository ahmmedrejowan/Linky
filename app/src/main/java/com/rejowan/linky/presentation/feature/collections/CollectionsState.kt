package com.rejowan.linky.presentation.feature.collections

import com.rejowan.linky.domain.model.CollectionWithLinkCount
import com.rejowan.linky.presentation.feature.home.ViewMode

data class CollectionsState(
    val collections: List<CollectionWithLinkCount> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortType: CollectionSortType = CollectionSortType.DATE_CREATED_DESC,
    val viewMode: ViewMode = ViewMode.LIST,
    val showCreateDialog: Boolean = false,
    val newCollectionName: String = "",
    val selectedCollectionColor: String? = null,
    // Edit dialog state
    val showEditDialog: Boolean = false,
    val editingCollection: CollectionWithLinkCount? = null,
    val editCollectionName: String = "",
    val editCollectionColor: String? = null,
    // Delete dialog state
    val showDeleteDialog: Boolean = false,
    val deletingCollection: CollectionWithLinkCount? = null
)

/**
 * Sort types for collections list
 */
enum class CollectionSortType(val displayName: String) {
    DATE_CREATED_DESC("Newest first"),
    DATE_CREATED_ASC("Oldest first"),
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    LAST_MODIFIED("Last modified"),
    MOST_LINKS("Most links"),
    LEAST_LINKS("Least links")
}
