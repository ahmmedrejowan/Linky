package com.rejowan.linky.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method

class ReaderModeParserTest {

    private lateinit var parser: ReaderModeParser

    @Before
    fun setUp() {
        parser = ReaderModeParser()
    }

    // Helper to access private methods via reflection
    private fun getPrivateMethod(name: String, vararg parameterTypes: Class<*>): Method {
        val method = ReaderModeParser::class.java.getDeclaredMethod(name, *parameterTypes)
        method.isAccessible = true
        return method
    }

    // ============ calculateWordCount Tests ============

    @Test
    fun `calculateWordCount returns 0 for empty string`() {
        val method = getPrivateMethod("calculateWordCount", String::class.java)
        val result = method.invoke(parser, "") as Int
        assertEquals(0, result)
    }

    @Test
    fun `calculateWordCount returns 0 for blank string`() {
        val method = getPrivateMethod("calculateWordCount", String::class.java)
        val result = method.invoke(parser, "   ") as Int
        assertEquals(0, result)
    }

    @Test
    fun `calculateWordCount counts words correctly`() {
        val method = getPrivateMethod("calculateWordCount", String::class.java)
        // "a" is filtered out (single char), so 5 words remain
        val result = method.invoke(parser, "Hello world this is a test") as Int
        assertEquals(5, result)
    }

    @Test
    fun `calculateWordCount ignores single characters`() {
        val method = getPrivateMethod("calculateWordCount", String::class.java)
        // "I" and "a" are single characters, should be filtered
        // Remaining: "am" (2) and "developer" (9)
        val result = method.invoke(parser, "I am a developer") as Int
        assertEquals(2, result)
    }

    @Test
    fun `calculateWordCount handles multiple spaces`() {
        val method = getPrivateMethod("calculateWordCount", String::class.java)
        val result = method.invoke(parser, "Hello    world") as Int
        assertEquals(2, result)
    }

    @Test
    fun `calculateWordCount handles newlines and tabs`() {
        val method = getPrivateMethod("calculateWordCount", String::class.java)
        val result = method.invoke(parser, "Hello\nworld\ttest") as Int
        assertEquals(3, result)
    }

    // ============ calculateReadTime Tests ============

    @Test
    fun `calculateReadTime returns 1 for zero words`() {
        val method = getPrivateMethod("calculateReadTime", Int::class.java)
        val result = method.invoke(parser, 0) as Int
        assertEquals(1, result)
    }

    @Test
    fun `calculateReadTime returns 1 for small word count`() {
        val method = getPrivateMethod("calculateReadTime", Int::class.java)
        val result = method.invoke(parser, 50) as Int
        assertEquals(1, result)
    }

    @Test
    fun `calculateReadTime calculates correctly for 200 words`() {
        val method = getPrivateMethod("calculateReadTime", Int::class.java)
        val result = method.invoke(parser, 200) as Int
        assertEquals(1, result)
    }

    @Test
    fun `calculateReadTime calculates correctly for 400 words`() {
        val method = getPrivateMethod("calculateReadTime", Int::class.java)
        val result = method.invoke(parser, 400) as Int
        assertEquals(2, result)
    }

    @Test
    fun `calculateReadTime rounds up`() {
        val method = getPrivateMethod("calculateReadTime", Int::class.java)
        val result = method.invoke(parser, 250) as Int
        assertEquals(2, result) // 250/200 = 1.25, ceil = 2
    }

    @Test
    fun `calculateReadTime handles large word count`() {
        val method = getPrivateMethod("calculateReadTime", Int::class.java)
        val result = method.invoke(parser, 2000) as Int
        assertEquals(10, result) // 2000/200 = 10
    }

    // ============ generateExcerpt Tests ============

    @Test
    fun `generateExcerpt returns empty for blank text`() {
        val method = getPrivateMethod("generateExcerpt", String::class.java, Int::class.java)
        val result = method.invoke(parser, "", 200) as String
        assertEquals("", result)
    }

    @Test
    fun `generateExcerpt returns full text if under max length`() {
        val method = getPrivateMethod("generateExcerpt", String::class.java, Int::class.java)
        val text = "Short text"
        val result = method.invoke(parser, text, 200) as String
        assertEquals("Short text", result)
    }

    @Test
    fun `generateExcerpt truncates long text with ellipsis`() {
        val method = getPrivateMethod("generateExcerpt", String::class.java, Int::class.java)
        val text = "A".repeat(300)
        val result = method.invoke(parser, text, 200) as String
        assertEquals(203, result.length) // 200 + "..."
        assert(result.endsWith("..."))
    }

    @Test
    fun `generateExcerpt trims whitespace`() {
        val method = getPrivateMethod("generateExcerpt", String::class.java, Int::class.java)
        val result = method.invoke(parser, "  Hello world  ", 200) as String
        assertEquals("Hello world", result)
    }

    @Test
    fun `generateExcerpt respects custom max length`() {
        val method = getPrivateMethod("generateExcerpt", String::class.java, Int::class.java)
        val text = "Hello world this is a test"
        val result = method.invoke(parser, text, 10) as String
        assertEquals("Hello worl...", result)
    }

    // ============ extractSiteName Tests ============

    @Test
    fun `extractSiteName extracts domain from https url`() {
        val method = getPrivateMethod("extractSiteName", String::class.java)
        val result = method.invoke(parser, "https://example.com/path") as String?
        assertEquals("example.com", result)
    }

    @Test
    fun `extractSiteName removes www prefix`() {
        val method = getPrivateMethod("extractSiteName", String::class.java)
        val result = method.invoke(parser, "https://www.example.com/path") as String?
        assertEquals("example.com", result)
    }

    @Test
    fun `extractSiteName handles http url`() {
        val method = getPrivateMethod("extractSiteName", String::class.java)
        val result = method.invoke(parser, "http://blog.example.com") as String?
        assertEquals("blog.example.com", result)
    }

    @Test
    fun `extractSiteName returns null for invalid url`() {
        val method = getPrivateMethod("extractSiteName", String::class.java)
        val result = method.invoke(parser, "not-a-valid-url") as String?
        assertNull(result)
    }

    @Test
    fun `extractSiteName handles url with port`() {
        val method = getPrivateMethod("extractSiteName", String::class.java)
        val result = method.invoke(parser, "https://example.com:8080/path") as String?
        assertEquals("example.com", result)
    }
}
