package com.rejowan.linky.presentation.feature.home

import com.rejowan.linky.domain.model.Link

data class HomeState(
    val links: List<Link> = emptyList(),
    val searchQuery: String = "",
    val filterType: FilterType = FilterType.ALL,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class FilterType {
    ALL,
    FAVORITES,
    ARCHIVED,
    TRASH
}
