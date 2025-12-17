package com.rejowan.linky.util

import org.junit.Assert.*
import org.junit.Test

class ReaderContentTest {

    @Test
    fun `creates ReaderContent with all fields`() {
        val content = ReaderContent(
            title = "Article Title",
            author = "John Doe",
            content = "# Heading\n\nParagraph content",
            excerpt = "This is the excerpt...",
            wordCount = 500,
            estimatedReadTime = 3,
            siteName = "example.com"
        )

        assertEquals("Article Title", content.title)
        assertEquals("John Doe", content.author)
        assertEquals("# Heading\n\nParagraph content", content.content)
        assertEquals("This is the excerpt...", content.excerpt)
        assertEquals(500, content.wordCount)
        assertEquals(3, content.estimatedReadTime)
        assertEquals("example.com", content.siteName)
    }

    @Test
    fun `creates ReaderContent with null author`() {
        val content = ReaderContent(
            title = "Title",
            author = null,
            content = "Content",
            excerpt = "Excerpt",
            wordCount = 100,
            estimatedReadTime = 1,
            siteName = null
        )

        assertNull(content.author)
    }

    @Test
    fun `creates ReaderContent with null siteName`() {
        val content = ReaderContent(
            title = "Title",
            author = "Author",
            content = "Content",
            excerpt = "Excerpt",
            wordCount = 100,
            estimatedReadTime = 1,
            siteName = null
        )

        assertNull(content.siteName)
    }

    @Test
    fun `creates ReaderContent with zero word count`() {
        val content = ReaderContent(
            title = "Empty Article",
            author = null,
            content = "",
            excerpt = "",
            wordCount = 0,
            estimatedReadTime = 1,
            siteName = null
        )

        assertEquals(0, content.wordCount)
    }

    @Test
    fun `two ReaderContents with same data are equal`() {
        val content1 = ReaderContent(
            title = "Title",
            author = "Author",
            content = "Content",
            excerpt = "Excerpt",
            wordCount = 100,
            estimatedReadTime = 1,
            siteName = "site.com"
        )

        val content2 = ReaderContent(
            title = "Title",
            author = "Author",
            content = "Content",
            excerpt = "Excerpt",
            wordCount = 100,
            estimatedReadTime = 1,
            siteName = "site.com"
        )

        assertEquals(content1, content2)
        assertEquals(content1.hashCode(), content2.hashCode())
    }

    @Test
    fun `two ReaderContents with different data are not equal`() {
        val content1 = ReaderContent(
            title = "Title 1",
            author = null,
            content = "Content 1",
            excerpt = "Excerpt",
            wordCount = 100,
            estimatedReadTime = 1,
            siteName = null
        )

        val content2 = ReaderContent(
            title = "Title 2",
            author = null,
            content = "Content 2",
            excerpt = "Excerpt",
            wordCount = 200,
            estimatedReadTime = 2,
            siteName = null
        )

        assertNotEquals(content1, content2)
    }

    @Test
    fun `copy creates new instance with modified field`() {
        val original = ReaderContent(
            title = "Original Title",
            author = "Author",
            content = "Content",
            excerpt = "Excerpt",
            wordCount = 100,
            estimatedReadTime = 1,
            siteName = "site.com"
        )

        val copied = original.copy(title = "New Title", wordCount = 200)

        assertEquals("New Title", copied.title)
        assertEquals(200, copied.wordCount)
        assertEquals("Author", copied.author) // Unchanged
    }

    @Test
    fun `toString contains all fields`() {
        val content = ReaderContent(
            title = "Test Title",
            author = "Test Author",
            content = "Test Content",
            excerpt = "Test Excerpt",
            wordCount = 500,
            estimatedReadTime = 3,
            siteName = "test.com"
        )

        val string = content.toString()

        assertTrue(string.contains("Test Title"))
        assertTrue(string.contains("Test Author"))
        assertTrue(string.contains("500"))
        assertTrue(string.contains("3"))
        assertTrue(string.contains("test.com"))
    }

    @Test
    fun `estimated read time matches word count`() {
        // 200 words = 1 minute, 400 words = 2 minutes, etc.
        val content = ReaderContent(
            title = "Title",
            author = null,
            content = "Content",
            excerpt = "Excerpt",
            wordCount = 400,
            estimatedReadTime = 2,
            siteName = null
        )

        assertEquals(400, content.wordCount)
        assertEquals(2, content.estimatedReadTime)
    }

    @Test
    fun `content can contain markdown formatting`() {
        val markdownContent = """
            # Main Heading

            Some paragraph with **bold** and *italic* text.

            ## Subheading

            - List item 1
            - List item 2

            ```kotlin
            val code = "example"
            ```
        """.trimIndent()

        val content = ReaderContent(
            title = "Markdown Article",
            author = "Author",
            content = markdownContent,
            excerpt = "Some paragraph...",
            wordCount = 15,
            estimatedReadTime = 1,
            siteName = "github.com"
        )

        assertTrue(content.content.contains("# Main Heading"))
        assertTrue(content.content.contains("**bold**"))
        assertTrue(content.content.contains("```kotlin"))
    }

    @Test
    fun `excerpt can be truncated version of content`() {
        val fullContent = "This is a very long article with lots of content that goes on and on."
        val excerpt = "This is a very long article..."

        val content = ReaderContent(
            title = "Long Article",
            author = null,
            content = fullContent,
            excerpt = excerpt,
            wordCount = 14,
            estimatedReadTime = 1,
            siteName = null
        )

        assertTrue(content.excerpt.endsWith("..."))
        assertTrue(content.content.length > content.excerpt.length)
    }
}
