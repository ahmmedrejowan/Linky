package com.rejowan.linky.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dankito.readability4j.Readability4J
import org.jsoup.Jsoup
import timber.log.Timber
import kotlin.math.ceil

/**
 * Parses webpages into clean, reader-friendly Markdown content using Readability4J algorithm
 * Converts the extracted HTML content to Markdown format for better rendering
 */
class ReaderModeParser {

    /**
     * Fetches and parses a URL into reader-friendly content
     * @param url The URL to parse
     * @param timeoutMs Connection timeout in milliseconds (default 15 seconds)
     * @return Result with ReaderContent or error
     */
    suspend fun parseUrl(url: String, timeoutMs: Int = 15000): Result<ReaderContent> = withContext(Dispatchers.IO) {
        try {
            Timber.d("parseUrl: Starting to parse URL: $url")

            // Step 1: Fetch HTML
            Timber.d("parseUrl: Fetching HTML...")
            val html = fetchHtml(url, timeoutMs)
            Timber.d("parseUrl: HTML fetched successfully (${html.length} characters)")

            // Step 2: Parse with Readability4J
            Timber.d("parseUrl: Parsing with Readability4J...")
            val article = Readability4J(url, html).parse()

            if (article.content.isNullOrBlank()) {
                Timber.w("parseUrl: No content extracted. Page may require JavaScript or content is behind paywall")
                return@withContext Result.Error(
                    Exception("Could not extract readable content from this page. The page may require JavaScript or the content may be protected.")
                )
            }

            Timber.d("parseUrl: Content extracted successfully")
            Timber.d("parseUrl: Title: ${article.title}")
            Timber.d("parseUrl: Author: ${article.byline ?: "Unknown"}")
            Timber.d("parseUrl: Content length: ${article.content?.length} characters")

            // Step 3: Calculate statistics
            val textContent = article.textContent ?: ""
            val wordCount = calculateWordCount(textContent)
            val readTime = calculateReadTime(wordCount)
            val excerpt = generateExcerpt(textContent)

            Timber.d("parseUrl: Stats calculated | Words: $wordCount | Read time: $readTime min")

            // Step 4: Convert HTML to Markdown
            Timber.d("parseUrl: Converting HTML to Markdown...")
            val markdownContent = HtmlToMarkdownConverter.convert(article.content ?: "")
            Timber.d("parseUrl: Markdown conversion complete (${markdownContent.length} characters)")

            // Step 5: Extract site name
            val siteName = extractSiteName(url)

            return@withContext Result.Success(
                ReaderContent(
                    title = article.title ?: "Untitled",
                    author = article.byline,
                    content = markdownContent,
                    excerpt = excerpt,
                    wordCount = wordCount,
                    estimatedReadTime = readTime,
                    siteName = siteName
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "parseUrl: Failed to parse URL: $url")
            Result.Error(e)
        }
    }

    /**
     * Fetches HTML from URL using Jsoup
     */
    private suspend fun fetchHtml(url: String, timeoutMs: Int): String = withContext(Dispatchers.IO) {
        try {
            Jsoup.connect(url)
                .timeout(timeoutMs)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .followRedirects(true)
                .ignoreHttpErrors(false)  // Throw on HTTP errors
                .get()
                .html()
        } catch (e: Exception) {
            Timber.e(e, "fetchHtml: Failed to fetch HTML from $url")
            throw Exception("Failed to fetch webpage: ${e.message}")
        }
    }

    /**
     * Calculate word count from text content
     */
    private fun calculateWordCount(text: String): Int {
        if (text.isBlank()) return 0

        return text
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() && it.length > 1 }  // Filter out single characters
            .size
    }

    /**
     * Calculate estimated read time based on word count
     * Average reading speed: 200 words per minute
     */
    private fun calculateReadTime(wordCount: Int): Int {
        if (wordCount == 0) return 1

        val minutes = wordCount / 200.0
        return ceil(minutes).toInt().coerceAtLeast(1)
    }

    /**
     * Generate excerpt from text content (first 200 characters)
     */
    private fun generateExcerpt(text: String, maxLength: Int = 200): String {
        if (text.isBlank()) return ""

        val cleanText = text.trim()
        return if (cleanText.length <= maxLength) {
            cleanText
        } else {
            cleanText.take(maxLength).trim() + "..."
        }
    }

    /**
     * Extract site name from URL
     */
    private fun extractSiteName(url: String): String? {
        return try {
            val uri = java.net.URI(url)
            uri.host?.removePrefix("www.")
        } catch (e: Exception) {
            Timber.w(e, "extractSiteName: Failed to extract site name from URL: $url")
            null
        }
    }
}
