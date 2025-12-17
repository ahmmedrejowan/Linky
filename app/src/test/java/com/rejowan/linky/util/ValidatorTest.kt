package com.rejowan.linky.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorTest {

    // ============ URL Validation Tests ============

    @Test
    fun `validateUrl returns Success for valid https url`() {
        val result = Validator.validateUrl("https://example.com")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateUrl returns Success for valid http url`() {
        val result = Validator.validateUrl("http://example.com")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateUrl returns Success for url with path`() {
        val result = Validator.validateUrl("https://example.com/path/to/page")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateUrl returns Success for url with query params`() {
        val result = Validator.validateUrl("https://example.com?param=value&other=123")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateUrl returns Error for blank url`() {
        val result = Validator.validateUrl("")
        assertTrue(result is ValidationResult.Error)
        assertEquals("URL cannot be empty", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateUrl returns Error for whitespace only url`() {
        val result = Validator.validateUrl("   ")
        assertTrue(result is ValidationResult.Error)
        assertEquals("URL cannot be empty", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateUrl returns Error for url without protocol`() {
        val result = Validator.validateUrl("example.com")
        assertTrue(result is ValidationResult.Error)
        assertEquals("Invalid URL format. URL must start with http:// or https://", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateUrl returns Error for url with invalid protocol`() {
        val result = Validator.validateUrl("ftp://example.com")
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validateUrl returns Error for url exceeding max length`() {
        val longUrl = "https://example.com/" + "a".repeat(2048)
        val result = Validator.validateUrl(longUrl)
        assertTrue(result is ValidationResult.Error)
        assertEquals("URL is too long (max 2048 characters)", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateUrl accepts url at max length`() {
        val longUrl = "https://example.com/" + "a".repeat(2028) // Total 2048
        val result = Validator.validateUrl(longUrl)
        assertTrue(result is ValidationResult.Success)
    }

    // ============ Title Validation Tests ============

    @Test
    fun `validateTitle returns Success for valid title`() {
        val result = Validator.validateTitle("My Link Title")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateTitle returns Error for blank title`() {
        val result = Validator.validateTitle("")
        assertTrue(result is ValidationResult.Error)
        assertEquals("Title cannot be empty", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateTitle returns Error for title shorter than 2 characters`() {
        val result = Validator.validateTitle("A")
        assertTrue(result is ValidationResult.Error)
        assertEquals("Title must be at least 2 characters", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateTitle returns Success for title with exactly 2 characters`() {
        val result = Validator.validateTitle("AB")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateTitle returns Error for title exceeding 200 characters`() {
        val longTitle = "a".repeat(201)
        val result = Validator.validateTitle(longTitle)
        assertTrue(result is ValidationResult.Error)
        assertEquals("Title is too long (max 200 characters)", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateTitle returns Success for title at max length`() {
        val maxTitle = "a".repeat(200)
        val result = Validator.validateTitle(maxTitle)
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateTitle uses custom field name in error message`() {
        val result = Validator.validateTitle("", "Name")
        assertTrue(result is ValidationResult.Error)
        assertEquals("Name cannot be empty", (result as ValidationResult.Error).message)
    }

    // ============ Collection Name Validation Tests ============

    @Test
    fun `validateCollectionName returns Success for valid name`() {
        val result = Validator.validateCollectionName("Work Links")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateCollectionName returns Error for blank name`() {
        val result = Validator.validateCollectionName("")
        assertTrue(result is ValidationResult.Error)
        assertEquals("Collection name cannot be empty", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateCollectionName returns Error for name shorter than 2 characters`() {
        val result = Validator.validateCollectionName("A")
        assertTrue(result is ValidationResult.Error)
        assertEquals("Collection name must be at least 2 characters", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateCollectionName returns Error for name exceeding 50 characters`() {
        val longName = "a".repeat(51)
        val result = Validator.validateCollectionName(longName)
        assertTrue(result is ValidationResult.Error)
        assertEquals("Collection name is too long (max 50 characters)", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateCollectionName returns Success for name at max length`() {
        val maxName = "a".repeat(50)
        val result = Validator.validateCollectionName(maxName)
        assertTrue(result is ValidationResult.Success)
    }

    // ============ Description Validation Tests ============

    @Test
    fun `validateDescription returns Success for null description`() {
        val result = Validator.validateDescription(null)
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateDescription returns Success for valid description`() {
        val result = Validator.validateDescription("This is a description")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateDescription returns Success for empty description`() {
        val result = Validator.validateDescription("")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateDescription returns Error for description exceeding 500 characters`() {
        val longDescription = "a".repeat(501)
        val result = Validator.validateDescription(longDescription)
        assertTrue(result is ValidationResult.Error)
        assertEquals("Description is too long (max 500 characters)", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateDescription returns Success for description at max length`() {
        val maxDescription = "a".repeat(500)
        val result = Validator.validateDescription(maxDescription)
        assertTrue(result is ValidationResult.Success)
    }

    // ============ Note Validation Tests ============

    @Test
    fun `validateNote returns Success for null note`() {
        val result = Validator.validateNote(null)
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateNote returns Success for valid note`() {
        val result = Validator.validateNote("My personal notes about this link")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateNote returns Error for note exceeding 5000 characters`() {
        val longNote = "a".repeat(5001)
        val result = Validator.validateNote(longNote)
        assertTrue(result is ValidationResult.Error)
        assertEquals("Note is too long (max 5000 characters)", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateNote returns Success for note at max length`() {
        val maxNote = "a".repeat(5000)
        val result = Validator.validateNote(maxNote)
        assertTrue(result is ValidationResult.Success)
    }

    // ============ Color Validation Tests ============

    @Test
    fun `validateColor returns Success for valid 6-digit hex color`() {
        val result = Validator.validateColor("#FF5733")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateColor returns Success for valid 8-digit hex color with alpha`() {
        val result = Validator.validateColor("#FF5733AA")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateColor returns Success for lowercase hex color`() {
        val result = Validator.validateColor("#ff5733")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateColor returns Error for blank color`() {
        val result = Validator.validateColor("")
        assertTrue(result is ValidationResult.Error)
        assertEquals("Color cannot be empty", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateColor returns Error for color without hash`() {
        val result = Validator.validateColor("FF5733")
        assertTrue(result is ValidationResult.Error)
        assertEquals("Invalid color format", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateColor returns Error for 3-digit hex color`() {
        val result = Validator.validateColor("#F53")
        assertTrue(result is ValidationResult.Error)
        assertEquals("Invalid color format", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validateColor returns Error for invalid hex characters`() {
        val result = Validator.validateColor("#GGGGGG")
        assertTrue(result is ValidationResult.Error)
        assertEquals("Invalid color format", (result as ValidationResult.Error).message)
    }

    // ============ Full Link Validation Tests ============

    @Test
    fun `validateLink returns Success for valid link data`() {
        val result = Validator.validateLink(
            url = "https://example.com",
            title = "My Link",
            description = "A description",
            note = "Some notes"
        )
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateLink returns Error for invalid url`() {
        val result = Validator.validateLink(
            url = "invalid-url",
            title = "My Link"
        )
        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("URL"))
    }

    @Test
    fun `validateLink returns Error for invalid title`() {
        val result = Validator.validateLink(
            url = "https://example.com",
            title = ""
        )
        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Title"))
    }

    @Test
    fun `validateLink returns Error for invalid description`() {
        val result = Validator.validateLink(
            url = "https://example.com",
            title = "My Link",
            description = "a".repeat(501)
        )
        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Description"))
    }

    @Test
    fun `validateLink returns Error for invalid note`() {
        val result = Validator.validateLink(
            url = "https://example.com",
            title = "My Link",
            note = "a".repeat(5001)
        )
        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Note"))
    }

    @Test
    fun `validateLink returns Success with null optional fields`() {
        val result = Validator.validateLink(
            url = "https://example.com",
            title = "My Link",
            description = null,
            note = null
        )
        assertTrue(result is ValidationResult.Success)
    }
}
