package com.rejowan.linky.util

import android.content.Context
import android.graphics.Bitmap
import com.rejowan.linky.domain.model.SnapshotType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

/**
 * Centralized file storage manager for preview images and snapshots
 * Handles saving, deleting, and cache management
 */
class FileStorageManager(private val context: Context) {

    private val previewDir = File(context.cacheDir, "previews")
    private val snapshotsDir = File(context.filesDir, "snapshots")

    init {
        // Create directories if they don't exist
        previewDir.mkdirs()
        snapshotsDir.mkdirs()
        Timber.d("FileStorageManager initialized - Preview dir: ${previewDir.absolutePath}, Snapshots dir: ${snapshotsDir.absolutePath}")
    }

    /**
     * Save a preview image from a URL
     * @param url The image URL to download and save
     * @param linkId The link ID to associate with this preview
     * @return The local file path if successful, null otherwise
     */
    suspend fun savePreviewImageFromUrl(url: String, linkId: String): String? = withContext(Dispatchers.IO) {
        try {
            Timber.d("Saving preview image from URL: $url for link: $linkId")

            val connection = URL(url).openConnection().apply {
                connectTimeout = 10000
                readTimeout = 10000
            }

            try {
                connection.connect()

                // Generate filename from linkId
                val fileName = generatePreviewFileName(linkId, url)
                val file = File(previewDir, fileName)

                connection.getInputStream().use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                Timber.d("Preview image saved successfully: ${file.absolutePath}")
                file.absolutePath
            } finally {
                if (connection is java.net.HttpURLConnection) {
                    connection.disconnect()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to save preview image from URL: $url")
            null
        }
    }

    /**
     * Save a preview image from a Bitmap
     * @param bitmap The bitmap to save
     * @param linkId The link ID to associate with this preview
     * @param format The image format (PNG or JPEG)
     * @param quality Compression quality (0-100, only for JPEG)
     * @return The local file path if successful, null otherwise
     */
    suspend fun savePreviewImage(
        bitmap: Bitmap,
        linkId: String,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 90
    ): String? = withContext(Dispatchers.IO) {
        try {
            Timber.d("Saving preview image bitmap for link: $linkId")

            val extension = when (format) {
                Bitmap.CompressFormat.PNG -> "png"
                Bitmap.CompressFormat.JPEG -> "jpg"
                else -> "jpg"
            }

            val fileName = "${linkId}_preview.$extension"
            val file = File(previewDir, fileName)

            FileOutputStream(file).use { outputStream ->
                bitmap.compress(format, quality, outputStream)
            }

            Timber.d("Preview bitmap saved successfully: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "Failed to save preview bitmap for link: $linkId")
            null
        }
    }

    /**
     * Delete a preview image
     * @param path The file path to delete
     * @return true if deleted successfully, false otherwise
     */
    suspend fun deletePreviewImage(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists() && file.parentFile == previewDir) {
                val deleted = file.delete()
                Timber.d("Preview image deleted: $path - Success: $deleted")
                deleted
            } else {
                Timber.w("Preview image not found or invalid path: $path")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete preview image: $path")
            false
        }
    }

    /**
     * Save a snapshot (HTML, PDF, or screenshot)
     * @param linkId The link ID to associate with this snapshot
     * @param content The content bytes to save
     * @param type The snapshot type (determines file extension)
     * @param timestamp Optional timestamp for versioning
     * @return The local file path if successful, null otherwise
     */
    suspend fun saveSnapshot(
        linkId: String,
        content: ByteArray,
        type: SnapshotType,
        timestamp: Long = System.currentTimeMillis()
    ): String? = withContext(Dispatchers.IO) {
        try {
            Timber.d("Saving snapshot for link: $linkId, type: $type")

            val extension = when (type) {
                SnapshotType.READER_MODE -> "html"
                SnapshotType.PDF -> "pdf"
                SnapshotType.SCREENSHOT -> "png"
            }

            val fileName = "${linkId}_${timestamp}.$extension"
            val file = File(snapshotsDir, fileName)

            FileOutputStream(file).use { outputStream ->
                outputStream.write(content)
            }

            Timber.d("Snapshot saved successfully: ${file.absolutePath}, size: ${content.size} bytes")
            file.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "Failed to save snapshot for link: $linkId")
            null
        }
    }

    /**
     * Delete a snapshot
     * @param path The file path to delete
     * @return true if deleted successfully, false otherwise
     */
    suspend fun deleteSnapshot(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists() && file.parentFile == snapshotsDir) {
                val deleted = file.delete()
                Timber.d("Snapshot deleted: $path - Success: $deleted")
                deleted
            } else {
                Timber.w("Snapshot not found or invalid path: $path")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete snapshot: $path")
            false
        }
    }

    /**
     * Clear all preview images from cache
     * @return true if cleared successfully, false otherwise
     */
    suspend fun clearPreviewCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.d("Clearing preview cache...")
            var deletedCount = 0
            var failedCount = 0

            previewDir.listFiles()?.forEach { file ->
                if (file.delete()) {
                    deletedCount++
                } else {
                    failedCount++
                }
            }

            Timber.d("Preview cache cleared - Deleted: $deletedCount, Failed: $failedCount")
            failedCount == 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear preview cache")
            false
        }
    }

    /**
     * Clear all snapshots
     * WARNING: This will delete all saved snapshots
     * @return true if cleared successfully, false otherwise
     */
    suspend fun clearAllSnapshots(): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.d("Clearing all snapshots...")
            var deletedCount = 0
            var failedCount = 0

            snapshotsDir.listFiles()?.forEach { file ->
                if (file.delete()) {
                    deletedCount++
                } else {
                    failedCount++
                }
            }

            Timber.d("Snapshots cleared - Deleted: $deletedCount, Failed: $failedCount")
            failedCount == 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear snapshots")
            false
        }
    }

    /**
     * Get total preview cache size in bytes
     * @return Total size in bytes
     */
    suspend fun getPreviewCacheSize(): Long = withContext(Dispatchers.IO) {
        try {
            val size = previewDir.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
            Timber.d("Preview cache size: $size bytes")
            size
        } catch (e: Exception) {
            Timber.e(e, "Failed to calculate preview cache size")
            0L
        }
    }

    /**
     * Get total snapshots size in bytes
     * @return Total size in bytes
     */
    suspend fun getSnapshotsSize(): Long = withContext(Dispatchers.IO) {
        try {
            val size = snapshotsDir.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
            Timber.d("Snapshots size: $size bytes")
            size
        } catch (e: Exception) {
            Timber.e(e, "Failed to calculate snapshots size")
            0L
        }
    }

    /**
     * Get total storage used (previews + snapshots) in bytes
     * @return Total size in bytes
     */
    suspend fun getTotalStorageUsed(): Long {
        return getPreviewCacheSize() + getSnapshotsSize()
    }

    /**
     * Get all snapshots for a specific link
     * @param linkId The link ID
     * @return List of snapshot file paths
     */
    suspend fun getSnapshotsForLink(linkId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val snapshots = snapshotsDir.listFiles()
                ?.filter { it.name.startsWith(linkId) }
                ?.map { it.absolutePath }
                ?: emptyList()

            Timber.d("Found ${snapshots.size} snapshots for link: $linkId")
            snapshots
        } catch (e: Exception) {
            Timber.e(e, "Failed to get snapshots for link: $linkId")
            emptyList()
        }
    }

    /**
     * Check if a file exists
     * @param path The file path to check
     * @return true if file exists, false otherwise
     */
    fun fileExists(path: String): Boolean {
        return try {
            File(path).exists()
        } catch (e: Exception) {
            Timber.e(e, "Failed to check file existence: $path")
            false
        }
    }

    /**
     * Generate a unique filename for preview images based on URL
     * @param linkId The link ID
     * @param url The image URL
     * @return Generated filename
     */
    private fun generatePreviewFileName(linkId: String, url: String): String {
        return try {
            // Get file extension from URL
            val extension = url.substringAfterLast('.', "jpg")
                .substringBefore('?') // Remove query parameters
                .take(4) // Limit extension length

            // Create hash of URL for uniqueness
            val urlHash = MessageDigest.getInstance("MD5")
                .digest(url.toByteArray())
                .joinToString("") { "%02x".format(it) }
                .take(8)

            "${linkId}_${urlHash}.$extension"
        } catch (e: Exception) {
            Timber.w(e, "Failed to generate preview filename, using default")
            "${linkId}_preview.jpg"
        }
    }

    /**
     * Delete all files for a specific link (preview + snapshots)
     * @param linkId The link ID
     * @return true if all files deleted successfully, false otherwise
     */
    suspend fun deleteAllFilesForLink(linkId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.d("Deleting all files for link: $linkId")
            var allDeleted = true

            // Delete preview images
            previewDir.listFiles()
                ?.filter { it.name.startsWith(linkId) }
                ?.forEach { file ->
                    if (!file.delete()) {
                        allDeleted = false
                        Timber.w("Failed to delete preview: ${file.name}")
                    }
                }

            // Delete snapshots
            snapshotsDir.listFiles()
                ?.filter { it.name.startsWith(linkId) }
                ?.forEach { file ->
                    if (!file.delete()) {
                        allDeleted = false
                        Timber.w("Failed to delete snapshot: ${file.name}")
                    }
                }

            Timber.d("Deleted all files for link: $linkId - Success: $allDeleted")
            allDeleted
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete files for link: $linkId")
            false
        }
    }
}
