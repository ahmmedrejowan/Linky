package com.rejowan.linky.util

import org.junit.Assert.*
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class ErrorHandlerTest {

    // Basic error message tests

    @Test
    fun `getErrorMessage returns appropriate message for FileNotFoundException`() {
        val exception = FileNotFoundException("test.txt")

        val message = ErrorHandler.getErrorMessage(exception)

        assertEquals("File not found", message)
    }

    @Test
    fun `getErrorMessage returns appropriate message for UnknownHostException`() {
        val exception = UnknownHostException("example.com")

        val message = ErrorHandler.getErrorMessage(exception)

        assertEquals("No internet connection. Please check your network", message)
    }

    @Test
    fun `getErrorMessage returns appropriate message for SocketTimeoutException`() {
        val exception = SocketTimeoutException("timeout")

        val message = ErrorHandler.getErrorMessage(exception)

        assertEquals("Request timed out. Please try again", message)
    }

    @Test
    fun `getErrorMessage returns appropriate message for SSLException`() {
        val exception = SSLException("SSL error")

        val message = ErrorHandler.getErrorMessage(exception)

        assertEquals("Secure connection failed. Please check your network settings", message)
    }

    @Test
    fun `getErrorMessage returns appropriate message for IOException`() {
        val exception = IOException("IO error")

        val message = ErrorHandler.getErrorMessage(exception)

        assertEquals("Network error. Please check your connection", message)
    }

    @Test
    fun `getErrorMessage returns message from IllegalArgumentException`() {
        val exception = IllegalArgumentException("Invalid URL format")

        val message = ErrorHandler.getErrorMessage(exception)

        assertEquals("Invalid URL format", message)
    }

    @Test
    fun `getErrorMessage returns message from IllegalStateException`() {
        val exception = IllegalStateException("Operation not allowed")

        val message = ErrorHandler.getErrorMessage(exception)

        assertEquals("Operation not allowed", message)
    }

    @Test
    fun `getErrorMessage returns default message for unknown exception type`() {
        val exception = RuntimeException()

        val message = ErrorHandler.getErrorMessage(exception)

        assertEquals("Something went wrong. Please try again", message)
    }

    @Test
    fun `getErrorMessage returns exception message for generic exception with message`() {
        val exception = RuntimeException("Custom error message")

        val message = ErrorHandler.getErrorMessage(exception)

        assertEquals("Custom error message", message)
    }

    @Test
    fun `getErrorMessage truncates long error messages`() {
        val longMessage = "A".repeat(150)
        val exception = RuntimeException(longMessage)

        val message = ErrorHandler.getErrorMessage(exception)

        assertEquals("Something went wrong. Please try again", message)
    }

    // Context-based error message tests

    @Test
    fun `getErrorMessage includes context when provided`() {
        val exception = IOException("Connection failed")

        val message = ErrorHandler.getErrorMessage(exception, "loading data")

        assertEquals("Failed loading data: Network error. Please check your connection", message)
    }

    @Test
    fun `getErrorMessage without context returns base message`() {
        val exception = IOException("Connection failed")

        val message = ErrorHandler.getErrorMessage(exception)

        assertEquals("Network error. Please check your connection", message)
    }

    // Link operation error message tests

    @Test
    fun `getLinkErrorMessage returns appropriate message for SAVE operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getLinkErrorMessage(exception, LinkOperation.SAVE)

        assertTrue(message.contains("to save link"))
    }

    @Test
    fun `getLinkErrorMessage returns appropriate message for UPDATE operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getLinkErrorMessage(exception, LinkOperation.UPDATE)

        assertTrue(message.contains("to update link"))
    }

    @Test
    fun `getLinkErrorMessage returns appropriate message for DELETE operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getLinkErrorMessage(exception, LinkOperation.DELETE)

        assertTrue(message.contains("to delete link"))
    }

    @Test
    fun `getLinkErrorMessage returns appropriate message for LOAD operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getLinkErrorMessage(exception, LinkOperation.LOAD)

        assertTrue(message.contains("to load link"))
    }

    @Test
    fun `getLinkErrorMessage returns appropriate message for LOAD_ALL operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getLinkErrorMessage(exception, LinkOperation.LOAD_ALL)

        assertTrue(message.contains("to load links"))
    }

    @Test
    fun `getLinkErrorMessage returns appropriate message for SEARCH operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getLinkErrorMessage(exception, LinkOperation.SEARCH)

        assertTrue(message.contains("to search links"))
    }

    @Test
    fun `getLinkErrorMessage returns appropriate message for TOGGLE_FAVORITE operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getLinkErrorMessage(exception, LinkOperation.TOGGLE_FAVORITE)

        assertTrue(message.contains("to toggle favorite"))
    }

    @Test
    fun `getLinkErrorMessage returns appropriate message for TOGGLE_ARCHIVE operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getLinkErrorMessage(exception, LinkOperation.TOGGLE_ARCHIVE)

        assertTrue(message.contains("to toggle archive"))
    }

    @Test
    fun `getLinkErrorMessage returns appropriate message for RESTORE operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getLinkErrorMessage(exception, LinkOperation.RESTORE)

        assertTrue(message.contains("to restore link"))
    }

    @Test
    fun `getLinkErrorMessage returns appropriate message for PERMANENT_DELETE operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getLinkErrorMessage(exception, LinkOperation.PERMANENT_DELETE)

        assertTrue(message.contains("to permanently delete link"))
    }

    @Test
    fun `getLinkErrorMessage returns appropriate message for FETCH_PREVIEW operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getLinkErrorMessage(exception, LinkOperation.FETCH_PREVIEW)

        assertTrue(message.contains("to fetch link preview"))
    }

    // Collection operation error message tests

    @Test
    fun `getCollectionErrorMessage returns appropriate message for SAVE operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getCollectionErrorMessage(exception, CollectionOperation.SAVE)

        assertTrue(message.contains("to save collection"))
    }

    @Test
    fun `getCollectionErrorMessage returns appropriate message for UPDATE operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getCollectionErrorMessage(exception, CollectionOperation.UPDATE)

        assertTrue(message.contains("to update collection"))
    }

    @Test
    fun `getCollectionErrorMessage returns appropriate message for DELETE operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getCollectionErrorMessage(exception, CollectionOperation.DELETE)

        assertTrue(message.contains("to delete collection"))
    }

    @Test
    fun `getCollectionErrorMessage returns appropriate message for LOAD operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getCollectionErrorMessage(exception, CollectionOperation.LOAD)

        assertTrue(message.contains("to load collection"))
    }

    @Test
    fun `getCollectionErrorMessage returns appropriate message for LOAD_ALL operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getCollectionErrorMessage(exception, CollectionOperation.LOAD_ALL)

        assertTrue(message.contains("to load collections"))
    }

    // Snapshot operation error message tests

    @Test
    fun `getSnapshotErrorMessage returns appropriate message for SAVE operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getSnapshotErrorMessage(exception, SnapshotOperation.SAVE)

        assertTrue(message.contains("to save snapshot"))
    }

    @Test
    fun `getSnapshotErrorMessage returns appropriate message for DELETE operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getSnapshotErrorMessage(exception, SnapshotOperation.DELETE)

        assertTrue(message.contains("to delete snapshot"))
    }

    @Test
    fun `getSnapshotErrorMessage returns appropriate message for LOAD operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getSnapshotErrorMessage(exception, SnapshotOperation.LOAD)

        assertTrue(message.contains("to load snapshots"))
    }

    // Settings operation error message tests

    @Test
    fun `getSettingsErrorMessage returns appropriate message for LOAD_STATISTICS operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getSettingsErrorMessage(exception, SettingsOperation.LOAD_STATISTICS)

        assertTrue(message.contains("to load statistics"))
    }

    @Test
    fun `getSettingsErrorMessage returns appropriate message for CHANGE_THEME operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getSettingsErrorMessage(exception, SettingsOperation.CHANGE_THEME)

        assertTrue(message.contains("to change theme"))
    }

    @Test
    fun `getSettingsErrorMessage returns appropriate message for CLEAR_CACHE operation`() {
        val exception = IOException("Network error")

        val message = ErrorHandler.getSettingsErrorMessage(exception, SettingsOperation.CLEAR_CACHE)

        assertTrue(message.contains("to clear cache"))
    }
}
