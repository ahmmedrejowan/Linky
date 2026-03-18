package com.rejowan.linky.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class UrlExtractorTest {

    @Before
    fun setUp() {
        stopKoin() // Ensure clean state
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    // ============ extractUrls Tests ============

    @Test
    fun `extractUrls returns empty list for empty text`() {
        val result = UrlExtractor.extractUrls("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `extractUrls returns empty list for text without urls`() {
        val result = UrlExtractor.extractUrls("This is just plain text without any URLs")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `extractUrls extracts single https url`() {
        val text = "Check out https://example.com for more info"
        val result = UrlExtractor.extractUrls(text)
        assertEquals(1, result.size)
        assertEquals("https://example.com", result[0])
    }

    @Test
    fun `extractUrls extracts single http url`() {
        val text = "Visit http://example.com"
        val result = UrlExtractor.extractUrls(text)
        assertEquals(1, result.size)
        assertEquals("http://example.com", result[0])
    }

    @Test
    fun `extractUrls extracts url with path`() {
        val text = "Link: https://example.com/path/to/page"
        val result = UrlExtractor.extractUrls(text)
        assertEquals(1, result.size)
        assertEquals("https://example.com/path/to/page", result[0])
    }

    @Test
    fun `extractUrls extracts url with query parameters`() {
        val text = "https://example.com/search?q=test&page=1"
        val result = UrlExtractor.extractUrls(text)
        assertEquals(1, result.size)
        assertEquals("https://example.com/search?q=test&page=1", result[0])
    }

    @Test
    fun `extractUrls extracts multiple urls`() {
        val text = "Check https://google.com and https://github.com"
        val result = UrlExtractor.extractUrls(text)
        assertEquals(2, result.size)
        assertTrue(result.contains("https://google.com"))
        assertTrue(result.contains("https://github.com"))
    }

    @Test
    fun `extractUrls removes duplicate urls`() {
        val text = "Visit https://example.com twice: https://example.com"
        val result = UrlExtractor.extractUrls(text)
        assertEquals(1, result.size)
    }

    @Test
    fun `extractUrls normalizes url without protocol`() {
        val text = "Visit www.example.com today"
        val result = UrlExtractor.extractUrls(text)
        assertEquals(1, result.size)
        assertEquals("https://www.example.com", result[0])
    }

    @Test
    fun `extractUrls extracts url without www prefix`() {
        val text = "Check example.com/page"
        val result = UrlExtractor.extractUrls(text)
        assertEquals(1, result.size)
        assertEquals("https://example.com/page", result[0])
    }

    @Test
    fun `extractUrls handles urls with subdomain`() {
        val text = "Go to blog.example.com"
        val result = UrlExtractor.extractUrls(text)
        assertEquals(1, result.size)
        assertEquals("https://blog.example.com", result[0])
    }

    @Test
    fun `extractUrls handles complex urls`() {
        val text = "https://subdomain.example.co.uk/path?param=value#anchor"
        val result = UrlExtractor.extractUrls(text)
        assertEquals(1, result.size)
        assertTrue(result[0].startsWith("https://subdomain.example.co.uk"))
    }

    // ============ normalizeUrl Tests ============

    @Test
    fun `normalizeUrl keeps https url unchanged`() {
        val url = "https://example.com"
        val result = UrlExtractor.normalizeUrl(url)
        assertEquals("https://example.com", result)
    }

    @Test
    fun `normalizeUrl keeps http url unchanged`() {
        val url = "http://example.com"
        val result = UrlExtractor.normalizeUrl(url)
        assertEquals("http://example.com", result)
    }

    @Test
    fun `normalizeUrl adds https to url without protocol`() {
        val url = "example.com"
        val result = UrlExtractor.normalizeUrl(url)
        assertEquals("https://example.com", result)
    }

    @Test
    fun `normalizeUrl adds https to www url`() {
        val url = "www.example.com"
        val result = UrlExtractor.normalizeUrl(url)
        assertEquals("https://www.example.com", result)
    }

    // ============ isValidUrl Tests ============

    @Test
    fun `isValidUrl returns true for valid https url`() {
        val result = UrlExtractor.isValidUrl("https://example.com")
        assertTrue(result)
    }

    @Test
    fun `isValidUrl returns true for valid http url`() {
        val result = UrlExtractor.isValidUrl("http://example.com")
        assertTrue(result)
    }

    @Test
    fun `isValidUrl returns true for url with path`() {
        val result = UrlExtractor.isValidUrl("https://example.com/path/to/page")
        assertTrue(result)
    }

    @Test
    fun `isValidUrl returns true for url with query params`() {
        val result = UrlExtractor.isValidUrl("https://example.com?q=test")
        assertTrue(result)
    }

    @Test
    fun `isValidUrl returns false for empty string`() {
        val result = UrlExtractor.isValidUrl("")
        assertFalse(result)
    }

    @Test
    fun `isValidUrl returns false for plain text`() {
        val result = UrlExtractor.isValidUrl("not a url")
        assertFalse(result)
    }

    @Test
    fun `isValidUrl returns false for url without scheme`() {
        val result = UrlExtractor.isValidUrl("example.com")
        assertFalse(result)
    }

    // ============ extractDomain Tests ============

    @Test
    fun `extractDomain returns domain from https url`() {
        val result = UrlExtractor.extractDomain("https://example.com/path")
        assertEquals("example.com", result)
    }

    @Test
    fun `extractDomain removes www prefix`() {
        val result = UrlExtractor.extractDomain("https://www.example.com/path")
        assertEquals("example.com", result)
    }

    @Test
    fun `extractDomain preserves subdomain`() {
        val result = UrlExtractor.extractDomain("https://blog.example.com")
        assertEquals("blog.example.com", result)
    }

    @Test
    fun `extractDomain handles url with port`() {
        val result = UrlExtractor.extractDomain("https://example.com:8080/path")
        assertEquals("example.com", result)
    }

    @Test
    fun `extractDomain returns original string for invalid url`() {
        val invalidUrl = "not-a-valid-url"
        val result = UrlExtractor.extractDomain(invalidUrl)
        assertEquals(invalidUrl, result)
    }

    @Test
    fun `extractDomain handles url with authentication`() {
        val result = UrlExtractor.extractDomain("https://user:pass@example.com/path")
        assertEquals("example.com", result)
    }
}
