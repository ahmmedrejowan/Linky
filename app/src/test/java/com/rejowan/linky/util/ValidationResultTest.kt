package com.rejowan.linky.util

import org.junit.Assert.*
import org.junit.Test

class ValidationResultTest {

    @Test
    fun `Success is singleton instance`() {
        val success1 = ValidationResult.Success
        val success2 = ValidationResult.Success
        assertSame(success1, success2)
    }

    @Test
    fun `Error contains message`() {
        val error = ValidationResult.Error("Invalid input")
        assertEquals("Invalid input", error.message)
    }

    @Test
    fun `Error with different messages are not equal`() {
        val error1 = ValidationResult.Error("Error 1")
        val error2 = ValidationResult.Error("Error 2")
        assertNotEquals(error1, error2)
    }

    @Test
    fun `Error with same message are equal`() {
        val error1 = ValidationResult.Error("Same message")
        val error2 = ValidationResult.Error("Same message")
        assertEquals(error1, error2)
    }

    @Test
    fun `when expression matches Success correctly`() {
        val result: ValidationResult = ValidationResult.Success

        val message = when (result) {
            is ValidationResult.Success -> "success"
            is ValidationResult.Error -> "error: ${result.message}"
        }

        assertEquals("success", message)
    }

    @Test
    fun `when expression matches Error correctly`() {
        val result: ValidationResult = ValidationResult.Error("test error")

        val message = when (result) {
            is ValidationResult.Success -> "success"
            is ValidationResult.Error -> "error: ${result.message}"
        }

        assertEquals("error: test error", message)
    }

    @Test
    fun `Error message can be empty`() {
        val error = ValidationResult.Error("")
        assertEquals("", error.message)
    }

    @Test
    fun `Error message with special characters`() {
        val error = ValidationResult.Error("Error: <special> & \"chars\"")
        assertEquals("Error: <special> & \"chars\"", error.message)
    }

    @Test
    fun `Success is ValidationResult type`() {
        val success: ValidationResult = ValidationResult.Success
        assertTrue(success is ValidationResult.Success)
        assertFalse(success is ValidationResult.Error)
    }

    @Test
    fun `Error is ValidationResult type`() {
        val error: ValidationResult = ValidationResult.Error("message")
        assertTrue(error is ValidationResult.Error)
        assertFalse(error is ValidationResult.Success)
    }
}
