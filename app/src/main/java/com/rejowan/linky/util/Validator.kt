package com.rejowan.linky.util

object Validator {

    /**
     * Validates a URL format
     */
    fun validateUrl(url: String): ValidationResult {
        return when {
            url.isBlank() -> ValidationResult.Error("URL cannot be empty")
            !isValidUrlFormat(url) -> ValidationResult.Error("Invalid URL format. URL must start with http:// or https://")
            url.length > 2048 -> ValidationResult.Error("URL is too long (max 2048 characters)")
            else -> ValidationResult.Success
        }
    }

    /**
     * Validates a title/name field
     */
    fun validateTitle(title: String, fieldName: String = "Title"): ValidationResult {
        return when {
            title.isBlank() -> ValidationResult.Error("$fieldName cannot be empty")
            title.length < 2 -> ValidationResult.Error("$fieldName must be at least 2 characters")
            title.length > 200 -> ValidationResult.Error("$fieldName is too long (max 200 characters)")
            else -> ValidationResult.Success
        }
    }

    /**
     * Validates a collection name
     */
    fun validateCollectionName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Error("Collection name cannot be empty")
            name.length < 2 -> ValidationResult.Error("Collection name must be at least 2 characters")
            name.length > 50 -> ValidationResult.Error("Collection name is too long (max 50 characters)")
            else -> ValidationResult.Success
        }
    }

    /**
     * Validates a note field (optional)
     */
    fun validateNote(note: String?): ValidationResult {
        return when {
            note != null && note.length > 5000 -> ValidationResult.Error("Note is too long (max 5000 characters)")
            else -> ValidationResult.Success
        }
    }

    /**
     * Validates a link with all required fields
     */
    fun validateLink(
        url: String,
        title: String,
        note: String? = null
    ): ValidationResult {
        // Check URL first
        val urlValidation = validateUrl(url)
        if (urlValidation is ValidationResult.Error) {
            return urlValidation
        }

        // Check title
        val titleValidation = validateTitle(title)
        if (titleValidation is ValidationResult.Error) {
            return titleValidation
        }

        // Check note if provided
        val noteValidation = validateNote(note)
        if (noteValidation is ValidationResult.Error) {
            return noteValidation
        }

        return ValidationResult.Success
    }

    /**
     * Checks if a string is a valid URL format
     */
    private fun isValidUrlFormat(url: String): Boolean {
        return url.startsWith("http://", ignoreCase = true) ||
                url.startsWith("https://", ignoreCase = true)
    }

    /**
     * Validates collection color (hex format)
     */
    fun validateColor(color: String): ValidationResult {
        return when {
            color.isBlank() -> ValidationResult.Error("Color cannot be empty")
            !isValidHexColor(color) -> ValidationResult.Error("Invalid color format")
            else -> ValidationResult.Success
        }
    }

    /**
     * Checks if a string is a valid hex color
     */
    private fun isValidHexColor(color: String): Boolean {
        return color.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$"))
    }
}
