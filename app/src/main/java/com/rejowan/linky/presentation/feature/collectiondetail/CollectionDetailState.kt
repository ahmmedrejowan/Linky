package com.rejowan.linky.presentation.feature.collectiondetail

import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.presentation.feature.home.SortType

data class CollectionDetailState(
    val collection: Collection? = null,
    val links: List<Link> = emptyList(),
    val sortType: SortType = SortType.DATE_ADDED_DESC,
    val showArchivedLinks: Boolean = true, // Show archived links by default
    val isLoading: Boolean = false,
    val error: String? = null,
    // Edit dialog state
    val showEditDialog: Boolean = false,
    val editName: String = "",
    val editColor: String? = null,
    val editIsFavorite: Boolean = false,
    // Delete dialog state
    val showDeleteDialog: Boolean = false,
    val deleteWithLinks: Boolean = false
)
