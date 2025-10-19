package com.rejowan.linky.util

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Utility object for mapping exceptions to user-friendly error messages
 */
object ErrorHandler {

    /**
     * Convert exception to user-friendly error message
     * @param exception The exception to convert
     * @param context Additional context about what operation failed (e.g., "saving link", "loading collections")
     * @return User-friendly error message
     */
    fun getErrorMessage(exception: Throwable, context: String? = null): String {
        Timber.e(exception, "Error occurred${context?.let { " while $it" } ?: ""}")

        val baseMessage = when (exception) {

            // File errors
            is FileNotFoundException -> "File not found"

            // Database errors
            is SQLiteConstraintException -> "This item already exists or violates a constraint"
            is SQLiteException -> "Database error occurred. Please try again"

            // Network errors
            is UnknownHostException -> "No internet connection. Please check your network"
            is SocketTimeoutException -> "Request timed out. Please try again"
            is SSLException -> "Secure connection failed. Please check your network settings"
            is IOException -> "Network error. Please check your connection"

            // Validation errors
            is IllegalArgumentException -> exception.message ?: "Invalid input provided"
            is IllegalStateException -> exception.message ?: "Operation not allowed at this time"

            // Generic errors
            else -> {
                // Try to extract meaningful message from exception
                val message = exception.message
                when {
                    message.isNullOrBlank() -> "Something went wrong. Please try again"
                    message.length > 100 -> "Something went wrong. Please try again"
                    else -> message
                }
            }
        }

        return if (context != null) {
            "Failed $context: $baseMessage"
        } else {
            baseMessage
        }
    }

    /**
     * Get user-friendly error message for link operations
     */
    fun getLinkErrorMessage(exception: Throwable, operation: LinkOperation): String {
        val context = when (operation) {
            LinkOperation.SAVE -> "to save link"
            LinkOperation.UPDATE -> "to update link"
            LinkOperation.DELETE -> "to delete link"
            LinkOperation.LOAD -> "to load link"
            LinkOperation.LOAD_ALL -> "to load links"
            LinkOperation.SEARCH -> "to search links"
            LinkOperation.TOGGLE_FAVORITE -> "to toggle favorite"
            LinkOperation.TOGGLE_ARCHIVE -> "to toggle archive"
            LinkOperation.RESTORE -> "to restore link"
            LinkOperation.FETCH_PREVIEW -> "to fetch link preview"
        }
        return getErrorMessage(exception, context)
    }

    /**
     * Get user-friendly error message for collection operations
     */
    fun getCollectionErrorMessage(exception: Throwable, operation: CollectionOperation): String {
        val context = when (operation) {
            CollectionOperation.SAVE -> "to save collection"
            CollectionOperation.UPDATE -> "to update collection"
            CollectionOperation.DELETE -> "to delete collection"
            CollectionOperation.LOAD -> "to load collection"
            CollectionOperation.LOAD_ALL -> "to load collections"
        }
        return getErrorMessage(exception, context)
    }

    /**
     * Get user-friendly error message for snapshot operations
     */
    fun getSnapshotErrorMessage(exception: Throwable, operation: SnapshotOperation): String {
        val context = when (operation) {
            SnapshotOperation.SAVE -> "to save snapshot"
            SnapshotOperation.DELETE -> "to delete snapshot"
            SnapshotOperation.LOAD -> "to load snapshots"
        }
        return getErrorMessage(exception, context)
    }

    /**
     * Get user-friendly error message for settings operations
     */
    fun getSettingsErrorMessage(exception: Throwable, operation: SettingsOperation): String {
        val context = when (operation) {
            SettingsOperation.LOAD_STATISTICS -> "to load statistics"
            SettingsOperation.CHANGE_THEME -> "to change theme"
            SettingsOperation.CLEAR_CACHE -> "to clear cache"
        }
        return getErrorMessage(exception, context)
    }
}

/**
 * Link operations enum
 */
enum class LinkOperation {
    SAVE,
    UPDATE,
    DELETE,
    LOAD,
    LOAD_ALL,
    SEARCH,
    TOGGLE_FAVORITE,
    TOGGLE_ARCHIVE,
    RESTORE,
    FETCH_PREVIEW
}

/**
 * Collection operations enum
 */
enum class CollectionOperation {
    SAVE,
    UPDATE,
    DELETE,
    LOAD,
    LOAD_ALL
}

/**
 * Snapshot operations enum
 */
enum class SnapshotOperation {
    SAVE,
    DELETE,
    LOAD
}

/**
 * Settings operations enum
 */
enum class SettingsOperation {
    LOAD_STATISTICS,
    CHANGE_THEME,
    CLEAR_CACHE
}
