package com.rejowan.linky.presentation.feature.collections

import com.rejowan.linky.domain.model.CollectionWithLinkCount

data class CollectionsState(
    val collections: List<CollectionWithLinkCount> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val newCollectionName: String = "",
    val selectedCollectionColor: String? = null
)
