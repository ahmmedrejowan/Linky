package com.rejowan.linky.presentation.feature.trash

import com.rejowan.linky.domain.model.Link

data class TrashState(
    val trashedLinks: List<Link> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
