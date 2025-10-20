package com.rejowan.linky.presentation.feature.collectiondetail

import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.model.Link

data class CollectionDetailState(
    val collection: Collection? = null,
    val links: List<Link> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
