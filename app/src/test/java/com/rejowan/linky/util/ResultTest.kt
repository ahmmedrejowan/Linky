package com.rejowan.linky.util

import org.junit.Assert.*
import org.junit.Test

class ResultTest {

    @Test
    fun `Success result isSuccess returns true`() {
        val result = Result.Success("data")
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertFalse(result.isLoading)
    }

    @Test
    fun `Error result isError returns true`() {
        val result = Result.Error(Exception("error"))
        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertFalse(result.isLoading)
    }

    @Test
    fun `Loading result isLoading returns true`() {
        val result = Result.Loading
        assertFalse(result.isSuccess)
        assertFalse(result.isError)
        assertTrue(result.isLoading)
    }

    @Test
    fun `Success getOrNull returns data`() {
        val data = "test data"
        val result = Result.Success(data)
        assertEquals(data, result.getOrNull())
    }

    @Test
    fun `Error getOrNull returns null`() {
        val result = Result.Error(Exception("error"))
        assertNull(result.getOrNull())
    }

    @Test
    fun `Loading getOrNull returns null`() {
        val result = Result.Loading
        assertNull(result.getOrNull())
    }

    @Test
    fun `Success data is accessible`() {
        val data = listOf(1, 2, 3)
        val result = Result.Success(data)
        assertEquals(listOf(1, 2, 3), result.data)
    }

    @Test
    fun `Error exception is accessible`() {
        val exception = IllegalArgumentException("Invalid argument")
        val result = Result.Error(exception)
        assertEquals(exception, result.exception)
        assertEquals("Invalid argument", result.exception.message)
    }

    @Test
    fun `Success with null data is valid`() {
        val result = Result.Success<String?>(null)
        assertTrue(result.isSuccess)
        assertNull(result.data)
        assertNull(result.getOrNull())
    }

    @Test
    fun `Success with different types works correctly`() {
        val intResult = Result.Success(42)
        val stringResult = Result.Success("hello")
        val listResult = Result.Success(listOf("a", "b"))
        val mapResult = Result.Success(mapOf("key" to "value"))

        assertEquals(42, intResult.data)
        assertEquals("hello", stringResult.data)
        assertEquals(listOf("a", "b"), listResult.data)
        assertEquals(mapOf("key" to "value"), mapResult.data)
    }

    @Test
    fun `Error with different exception types`() {
        val illegalArg = Result.Error(IllegalArgumentException("bad arg"))
        val illegalState = Result.Error(IllegalStateException("bad state"))
        val runtime = Result.Error(RuntimeException("runtime error"))

        assertTrue(illegalArg.exception is IllegalArgumentException)
        assertTrue(illegalState.exception is IllegalStateException)
        assertTrue(runtime.exception is RuntimeException)
    }

    @Test
    fun `when expression matches correctly`() {
        val success: Result<String> = Result.Success("data")
        val error: Result<String> = Result.Error(Exception())
        val loading: Result<String> = Result.Loading

        val successMessage = when (success) {
            is Result.Success -> "success: ${success.data}"
            is Result.Error -> "error"
            is Result.Loading -> "loading"
        }

        val errorMessage = when (error) {
            is Result.Success -> "success"
            is Result.Error -> "error: ${error.exception.message}"
            is Result.Loading -> "loading"
        }

        val loadingMessage = when (loading) {
            is Result.Success -> "success"
            is Result.Error -> "error"
            is Result.Loading -> "loading"
        }

        assertEquals("success: data", successMessage)
        assertTrue(errorMessage.startsWith("error"))
        assertEquals("loading", loadingMessage)
    }
}
