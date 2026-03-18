package com.rejowan.linky.data.export

import android.content.Context
import android.net.Uri
import com.rejowan.linky.data.local.database.dao.CollectionDao
import com.rejowan.linky.data.local.database.dao.LinkDao
import com.rejowan.linky.data.local.database.dao.SnapshotDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles exporting app data to .linky format
 */
class ExportManager(
    private val context: Context,
    private val linkDao: LinkDao,
    private val collectionDao: CollectionDao,
    private val snapshotDao: SnapshotDao
) {
    /**
     * Export all data to a URI (user-selected file location) in .linky format
     */
    suspend fun exportToUri(
        uri: Uri,
        includeSnapshots: Boolean = false,
        onProgress: (Int) -> Unit = {}
    ): Result<ExportSummary> = withContext(Dispatchers.IO) {
        try {
            onProgress(10)

            // Gather all data
            val exportData = gatherExportData(includeSnapshots, onProgress)

            // Write to .linky format
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                LinkyBackupFormat.writeBackup(outputStream, exportData, onProgress)
            } ?: throw IllegalStateException("Could not open output stream")

            val summary = ExportSummary(
                linksCount = exportData.data.links.size,
                collectionsCount = exportData.data.collections.size,
                snapshotsCount = 0,
                fileSize = "~", // Size is calculated after ZIP compression
                filePath = uri.toString()
            )

            Timber.d("Export completed: ${summary.linksCount} links, ${summary.collectionsCount} collections")
            Result.success(summary)
        } catch (e: Exception) {
            Timber.e(e, "Export failed")
            Result.failure(e)
        }
    }

    /**
     * Export to an output stream (for sharing) in .linky format
     */
    suspend fun exportToStream(
        outputStream: OutputStream,
        includeSnapshots: Boolean = false,
        onProgress: (Int) -> Unit = {}
    ): Result<ExportSummary> = withContext(Dispatchers.IO) {
        try {
            onProgress(10)

            val exportData = gatherExportData(includeSnapshots, onProgress)

            LinkyBackupFormat.writeBackup(outputStream, exportData, onProgress)

            val summary = ExportSummary(
                linksCount = exportData.data.links.size,
                collectionsCount = exportData.data.collections.size,
                snapshotsCount = 0,
                fileSize = "~",
                filePath = ""
            )

            Result.success(summary)
        } catch (e: Exception) {
            Timber.e(e, "Export to stream failed")
            Result.failure(e)
        }
    }

    private suspend fun gatherExportData(
        includeSnapshots: Boolean,
        onProgress: (Int) -> Unit
    ): ExportData {
        onProgress(20)

        // Get all links (excluding trashed)
        val links = linkDao.getAllLinksSync()
            .filter { it.deletedAt == null }
            .map { entity ->
                LinkExport(
                    id = entity.id,
                    title = entity.title,
                    url = entity.url,
                    description = entity.description,
                    note = entity.note,
                    collectionId = entity.collectionId,
                    previewUrl = entity.previewUrl,
                    previewImagePath = entity.previewImagePath,
                    isFavorite = entity.isFavorite,
                    isArchived = entity.isArchived,
                    hideFromHome = entity.hideFromHome,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
            }

        onProgress(35)

        // Get all collections
        val collections = collectionDao.getAllCollectionsSync().map { entity ->
            CollectionExport(
                id = entity.id,
                name = entity.name,
                icon = entity.icon,
                color = entity.color,
                sortOrder = entity.sortOrder,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }

        onProgress(55)

        // Get snapshots if requested
        val snapshots = if (includeSnapshots) {
            snapshotDao.getAllSnapshotsSync().map { entity ->
                SnapshotExport(
                    id = entity.id,
                    linkId = entity.linkId,
                    type = entity.type,
                    filePath = entity.filePath,
                    fileSize = entity.fileSize,
                    title = entity.title,
                    author = entity.author,
                    excerpt = entity.excerpt,
                    wordCount = entity.wordCount,
                    estimatedReadTime = entity.estimatedReadTime,
                    createdAt = entity.createdAt
                )
            }
        } else {
            null
        }

        onProgress(65)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

        return ExportData(
            exportDate = dateFormat.format(Date()),
            data = ExportPayload(
                links = links,
                collections = collections,
                snapshots = snapshots
            )
        )
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}
