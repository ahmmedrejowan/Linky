package com.rejowan.linky.domain.model

data class CollectionWithLinkCount(
    val collection: Collection,
    val linkCount: Int,
    val linkPreviews: List<String?> = emptyList() // Preview image paths for up to 3 recent links
)
