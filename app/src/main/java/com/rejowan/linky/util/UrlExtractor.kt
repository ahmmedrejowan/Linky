package com.rejowan.linky.util

import android.net.Uri

/**
 * Utility object for extracting and validating URLs from text
 * Uses improved regex for better URL detection
 */
object UrlExtractor {

    // Enhanced regex for better URL detection
    // Matches: http(s)://domain.com, www.domain.com, domain.com/path
    private val URL_REGEX = Regex(
        """(?:https?://)?(?:www\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b(?:[-a-zA-Z0-9()@:%_+.~#?&/=]*)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Extract all URLs from text
     * @param text The text to extract URLs from
     * @return List of unique, normalized URLs
     */
    fun extractUrls(text: String): List<String> {
        return URL_REGEX.findAll(text)
            .map { normalizeUrl(it.value.trim()) }
            .distinct() // Remove duplicates from paste
            .filter { isValidUrl(it) } // Additional validation
            .toList()
    }

    /**
     * Normalize a URL by ensuring it has a scheme
     * @param url The URL to normalize
     * @return Normalized URL with https:// scheme
     */
    fun normalizeUrl(url: String): String {
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            else -> "https://$url"
        }
    }

    /**
     * Check if a URL is valid
     * @param url The URL to validate
     * @return true if valid, false otherwise
     */
    fun isValidUrl(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            uri.scheme != null && uri.host != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extract domain from URL
     * @param url The URL to extract domain from
     * @return Domain name without www prefix
     */
    fun extractDomain(url: String): String {
        return try {
            val uri = Uri.parse(url)
            uri.host?.removePrefix("www.") ?: url
        } catch (e: Exception) {
            url
        }
    }
}
