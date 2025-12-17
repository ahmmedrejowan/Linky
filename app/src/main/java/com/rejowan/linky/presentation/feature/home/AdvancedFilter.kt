package com.rejowan.linky.presentation.feature.home

/**
 * Advanced filter options for links
 */
data class AdvancedFilter(
    val dateRange: DateRangeFilter = DateRangeFilter.ALL_TIME,
    val domains: Set<String> = emptySet(),
    val collectionIds: Set<String> = emptySet(),
    val tagIds: Set<String> = emptySet(),
    val hasNote: Boolean? = null, // null = don't filter, true = with notes, false = without notes
    val hasPreview: Boolean? = null // null = don't filter, true = with preview, false = without preview
) {
    val isActive: Boolean
        get() = dateRange != DateRangeFilter.ALL_TIME ||
                domains.isNotEmpty() ||
                collectionIds.isNotEmpty() ||
                tagIds.isNotEmpty() ||
                hasNote != null ||
                hasPreview != null

    val activeFilterCount: Int
        get() = listOfNotNull(
            if (dateRange != DateRangeFilter.ALL_TIME) 1 else null,
            if (domains.isNotEmpty()) 1 else null,
            if (collectionIds.isNotEmpty()) 1 else null,
            if (tagIds.isNotEmpty()) 1 else null,
            if (hasNote != null) 1 else null,
            if (hasPreview != null) 1 else null
        ).size

    companion object {
        val EMPTY = AdvancedFilter()
    }
}

enum class DateRangeFilter(val displayName: String) {
    ALL_TIME("All time"),
    TODAY("Today"),
    LAST_7_DAYS("Last 7 days"),
    LAST_30_DAYS("Last 30 days"),
    LAST_90_DAYS("Last 90 days"),
    THIS_YEAR("This year")
}

/**
 * Domain info for filter display
 */
data class DomainInfo(
    val domain: String,
    val count: Int
)

/**
 * Collection info for filter display
 */
data class CollectionFilterInfo(
    val id: String,
    val name: String,
    val count: Int
)

/**
 * Tag info for filter display
 */
data class TagFilterInfo(
    val id: String,
    val name: String,
    val color: String?,
    val count: Int
)
