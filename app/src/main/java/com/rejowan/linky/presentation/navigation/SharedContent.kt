package com.rejowan.linky.presentation.navigation

/**
 * Represents content shared from other apps via ACTION_SEND or ACTION_VIEW intent
 *
 * @param text The full shared text
 * @param urls List of URLs extracted from the text
 * @param title Optional title/subject from EXTRA_SUBJECT (often contains page title from browsers)
 * @param urlCount Number of URLs found
 */
data class SharedContent(
    val text: String,
    val urls: List<String>,
    val title: String? = null,
    val urlCount: Int = urls.size
) {
    /**
     * Get the first URL, or null if none found
     */
    val firstUrl: String? get() = urls.firstOrNull()

    /**
     * Check if this content has multiple URLs (2 or more)
     */
    val hasMultipleUrls: Boolean get() = urlCount >= 2

    /**
     * Check if this content has a single URL
     */
    val hasSingleUrl: Boolean get() = urlCount == 1

    /**
     * Check if this content has no URLs
     */
    val hasNoUrls: Boolean get() = urlCount == 0

    /**
     * Check if this content has a title
     */
    val hasTitle: Boolean get() = !title.isNullOrBlank()

    companion object {
        /**
         * Create SharedContent from a single URL (e.g., from ACTION_VIEW)
         */
        fun fromUrl(url: String, title: String? = null): SharedContent {
            return SharedContent(
                text = url,
                urls = listOf(url),
                title = title
            )
        }
    }
}
