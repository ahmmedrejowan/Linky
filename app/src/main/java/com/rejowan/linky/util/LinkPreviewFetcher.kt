package com.rejowan.linky.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import timber.log.Timber

class LinkPreviewFetcher {

    /**
     * Fetches link preview data using Open Graph tags and HTML metadata
     * @param url The URL to fetch preview for
     * @param timeoutMs Connection timeout in milliseconds (default 10 seconds)
     * @return LinkPreview object if successful, null if failed
     */
    suspend fun fetchPreview(
        url: String,
        timeoutMs: Int = 10000
    ): LinkPreview? = withContext(Dispatchers.IO) {
        try {
            // Validate URL first
            if (!isValidUrl(url)) {
                Timber.e("Invalid URL format: $url")
                return@withContext null
            }

            // Connect and fetch the document
            val document = Jsoup.connect(url)
                .timeout(timeoutMs)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .followRedirects(true)
                .get()

            // Extract Open Graph metadata (priority)
            val ogTitle = document.select("meta[property=og:title]").attr("content")
            val ogDescription = document.select("meta[property=og:description]").attr("content")
            val ogImage = document.select("meta[property=og:image]").attr("content")
            val ogSiteName = document.select("meta[property=og:site_name]").attr("content")

            // Fallback to Twitter Card metadata
            val twitterTitle = document.select("meta[name=twitter:title]").attr("content")
            val twitterDescription = document.select("meta[name=twitter:description]").attr("content")
            val twitterImage = document.select("meta[name=twitter:image]").attr("content")

            // Fallback to standard HTML meta tags
            val metaDescription = document.select("meta[name=description]").attr("content")
            val htmlTitle = document.title()

            // Extract favicon
            val favicon = extractFavicon(document, url)

            // Determine best values with priority: OG > Twitter > HTML
            val title = when {
                ogTitle.isNotBlank() -> ogTitle
                twitterTitle.isNotBlank() -> twitterTitle
                htmlTitle.isNotBlank() -> htmlTitle
                else -> url // Fallback to URL if no title found
            }

            val description = when {
                ogDescription.isNotBlank() -> ogDescription
                twitterDescription.isNotBlank() -> twitterDescription
                metaDescription.isNotBlank() -> metaDescription
                else -> null
            }

            val imageUrl = when {
                ogImage.isNotBlank() -> resolveUrl(url, ogImage)
                twitterImage.isNotBlank() -> resolveUrl(url, twitterImage)
                else -> null
            }

            val siteName = ogSiteName.ifBlank { extractDomainName(url) }

            LinkPreview(
                title = title,
                description = description,
                imageUrl = imageUrl,
                url = url,
                favicon = favicon,
                siteName = siteName
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch preview for URL: $url")
            null
        }
    }

    /**
     * Extract favicon from the webpage
     */
    private fun extractFavicon(document: org.jsoup.nodes.Document, baseUrl: String): String? {
        return try {
            // Try different favicon selectors
            val favicon = document.select("link[rel~=icon]").firstOrNull()?.attr("href")
                ?: document.select("link[rel~=shortcut icon]").firstOrNull()?.attr("href")
                ?: document.select("link[rel~=apple-touch-icon]").firstOrNull()?.attr("href")

            if (favicon.isNullOrBlank()) {
                // Fallback to default favicon location
                val domain = extractDomain(baseUrl)
                "$domain/favicon.ico"
            } else {
                resolveUrl(baseUrl, favicon)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to extract favicon")
            null
        }
    }

    /**
     * Resolve relative URLs to absolute URLs
     */
    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
                relativeUrl
            } else if (relativeUrl.startsWith("//")) {
                "https:$relativeUrl"
            } else if (relativeUrl.startsWith("/")) {
                val domain = extractDomain(baseUrl)
                "$domain$relativeUrl"
            } else {
                val domain = extractDomain(baseUrl)
                "$domain/$relativeUrl"
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to resolve URL: $relativeUrl")
            relativeUrl
        }
    }

    /**
     * Extract domain from URL
     */
    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            "${uri.scheme}://${uri.host}"
        } catch (e: Exception) {
            Timber.w(e, "Failed to extract domain from URL: $url")
            url
        }
    }

    /**
     * Extract domain name from URL (for site name fallback)
     */
    private fun extractDomainName(url: String): String? {
        return try {
            val uri = java.net.URI(url)
            uri.host?.removePrefix("www.")
        } catch (e: Exception) {
            Timber.w(e, "Failed to extract domain name from URL: $url")
            null
        }
    }

    /**
     * Validate URL format
     */
    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://", ignoreCase = true) ||
                url.startsWith("https://", ignoreCase = true)
    }
}
