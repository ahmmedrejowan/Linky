package com.rejowan.linky.util

import org.junit.Assert.*
import org.junit.Test

class LinkPreviewTest {

    @Test
    fun `creates LinkPreview with required fields`() {
        val preview = LinkPreview(
            title = "Test Title",
            description = "Test Description",
            imageUrl = "https://example.com/image.png",
            url = "https://example.com"
        )

        assertEquals("Test Title", preview.title)
        assertEquals("Test Description", preview.description)
        assertEquals("https://example.com/image.png", preview.imageUrl)
        assertEquals("https://example.com", preview.url)
    }

    @Test
    fun `creates LinkPreview with null description`() {
        val preview = LinkPreview(
            title = "Test Title",
            description = null,
            imageUrl = "https://example.com/image.png",
            url = "https://example.com"
        )

        assertNull(preview.description)
    }

    @Test
    fun `creates LinkPreview with null imageUrl`() {
        val preview = LinkPreview(
            title = "Test Title",
            description = "Description",
            imageUrl = null,
            url = "https://example.com"
        )

        assertNull(preview.imageUrl)
    }

    @Test
    fun `creates LinkPreview with optional favicon`() {
        val preview = LinkPreview(
            title = "Test Title",
            description = null,
            imageUrl = null,
            url = "https://example.com",
            favicon = "https://example.com/favicon.ico"
        )

        assertEquals("https://example.com/favicon.ico", preview.favicon)
    }

    @Test
    fun `creates LinkPreview with optional siteName`() {
        val preview = LinkPreview(
            title = "Test Title",
            description = null,
            imageUrl = null,
            url = "https://example.com",
            siteName = "Example Site"
        )

        assertEquals("Example Site", preview.siteName)
    }

    @Test
    fun `default values for optional fields are null`() {
        val preview = LinkPreview(
            title = "Test Title",
            description = null,
            imageUrl = null,
            url = "https://example.com"
        )

        assertNull(preview.favicon)
        assertNull(preview.siteName)
    }

    @Test
    fun `two LinkPreviews with same data are equal`() {
        val preview1 = LinkPreview(
            title = "Title",
            description = "Desc",
            imageUrl = "https://img.com",
            url = "https://example.com",
            favicon = "https://fav.ico",
            siteName = "Site"
        )

        val preview2 = LinkPreview(
            title = "Title",
            description = "Desc",
            imageUrl = "https://img.com",
            url = "https://example.com",
            favicon = "https://fav.ico",
            siteName = "Site"
        )

        assertEquals(preview1, preview2)
        assertEquals(preview1.hashCode(), preview2.hashCode())
    }

    @Test
    fun `two LinkPreviews with different data are not equal`() {
        val preview1 = LinkPreview(
            title = "Title 1",
            description = null,
            imageUrl = null,
            url = "https://example1.com"
        )

        val preview2 = LinkPreview(
            title = "Title 2",
            description = null,
            imageUrl = null,
            url = "https://example2.com"
        )

        assertNotEquals(preview1, preview2)
    }

    @Test
    fun `copy creates new instance with modified field`() {
        val original = LinkPreview(
            title = "Original Title",
            description = "Original Desc",
            imageUrl = null,
            url = "https://example.com"
        )

        val copied = original.copy(title = "New Title")

        assertEquals("New Title", copied.title)
        assertEquals("Original Desc", copied.description)
        assertNotEquals(original.title, copied.title)
    }

    @Test
    fun `toString contains all fields`() {
        val preview = LinkPreview(
            title = "Test",
            description = "Desc",
            imageUrl = "https://img.com",
            url = "https://example.com",
            favicon = "https://fav.ico",
            siteName = "Site"
        )

        val string = preview.toString()

        assertTrue(string.contains("Test"))
        assertTrue(string.contains("Desc"))
        assertTrue(string.contains("https://img.com"))
        assertTrue(string.contains("https://example.com"))
    }
}
