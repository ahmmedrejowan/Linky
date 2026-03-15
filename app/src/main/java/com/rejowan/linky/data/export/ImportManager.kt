package com.rejowan.linky.data.export

import android.content.Context
import android.net.Uri
import com.rejowan.linky.data.local.database.dao.CollectionDao
import com.rejowan.linky.data.local.database.dao.LinkDao
import com.rejowan.linky.data.local.database.dao.SnapshotDao
import com.rejowan.linky.data.local.database.entity.CollectionEntity
import com.rejowan.linky.data.local.database.entity.LinkEntity
import com.rejowan.linky.data.local.database.entity.SnapshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID

/**
 * Handles importing app data from JSON format
 */
class ImportManager(
    private val context: Context,
    private val linkDao: LinkDao,
    private val collectionDao: CollectionDao,
    private val snapshotDao: SnapshotDao
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Validate and preview an import file without actually importing
     */
    suspend fun previewImport(uri: Uri): Result<ImportPreview> = withContext(Dispatchers.IO) {
        try {
            val jsonString = readFileContent(uri)
            val exportData = json.decodeFromString<ExportData>(jsonString)

            val preview = ImportPreview(
                version = exportData.version,
                exportDate = exportData.exportDate,
                totalLinks = exportData.data.links.size,
                totalCollections = exportData.data.collections.size,
                hasSnapshots = exportData.data.snapshots != null,
                snapshotsCount = exportData.data.snapshots?.size ?: 0
            )

            Result.success(preview)
        } catch (e: Exception) {
            Timber.e(e, "Failed to preview import file")
            Result.failure(e)
        }
    }

    /**
     * Import data from a URI
     */
    suspend fun importFromUri(
        uri: Uri,
        conflictStrategy: ImportConflictStrategy = ImportConflictStrategy.SKIP,
        onProgress: (Int) -> Unit = {}
    ): Result<ImportSummary> = withContext(Dispatchers.IO) {
        try {
            onProgress(5)

            val jsonString = readFileContent(uri)

            onProgress(10)

            val exportData = json.decodeFromString<ExportData>(jsonString)

            // Validate version
            if (exportData.version > ExportData.CURRENT_VERSION) {
                return@withContext Result.failure(
                    IllegalStateException("Import file version ${exportData.version} is newer than supported version ${ExportData.CURRENT_VERSION}")
                )
            }

            onProgress(15)

            val errors = mutableListOf<String>()
            var duplicatesSkipped = 0

            // Import collections first (links may reference them)
            val collectionIdMap = mutableMapOf<String, String>() // old ID -> new ID
            val collectionsImported = importCollections(
                exportData.data.collections,
                conflictStrategy,
                collectionIdMap,
                errors
            ) { duplicatesSkipped++ }

            onProgress(40)

            // Import links (with mapped collection IDs)
            val linkIdMap = mutableMapOf<String, String>()
            val linksImported = importLinks(
                exportData.data.links,
                conflictStrategy,
                collectionIdMap,
                linkIdMap,
                errors
            ) { duplicatesSkipped++ }

            onProgress(70)

            // Import snapshots if present
            val snapshotsImported = exportData.data.snapshots?.let { snapshots ->
                importSnapshots(snapshots, linkIdMap, errors)
            } ?: 0

            onProgress(100)

            val summary = ImportSummary(
                linksImported = linksImported,
                collectionsImported = collectionsImported,
                snapshotsImported = snapshotsImported,
                duplicatesSkipped = duplicatesSkipped,
                errors = errors
            )

            Timber.d("Import completed: $linksImported links, $collectionsImported collections")
            Result.success(summary)
        } catch (e: Exception) {
            Timber.e(e, "Import failed")
            Result.failure(e)
        }
    }

    private suspend fun importCollections(
        collections: List<CollectionExport>,
        conflictStrategy: ImportConflictStrategy,
        idMap: MutableMap<String, String>,
        errors: MutableList<String>,
        onDuplicate: () -> Unit
    ): Int {
        var imported = 0
        val existingCollections = collectionDao.getAllCollectionsSync()
        val existingNames = existingCollections.map { it.name.lowercase() }.toSet()

        for (collection in collections) {
            try {
                val nameExists = collection.name.lowercase() in existingNames

                when {
                    nameExists && conflictStrategy == ImportConflictStrategy.SKIP -> {
                        // Map to existing collection with same name
                        val existing = existingCollections.first { it.name.equals(collection.name, ignoreCase = true) }
                        idMap[collection.id] = existing.id
                        onDuplicate()
                    }
                    nameExists && conflictStrategy == ImportConflictStrategy.REPLACE -> {
                        val existing = existingCollections.first { it.name.equals(collection.name, ignoreCase = true) }
                        val updated = CollectionEntity(
                            id = existing.id,
                            name = collection.name,
                            icon = collection.icon,
                            color = collection.color,
                            isFavorite = collection.isFavorite,
                            sortOrder = collection.sortOrder,
                            createdAt = existing.createdAt,
                            updatedAt = System.currentTimeMillis()
                        )
                        collectionDao.update(updated)
                        idMap[collection.id] = existing.id
                        imported++
                    }
                    else -> {
                        // Create new collection
                        val newId = UUID.randomUUID().toString()
                        val entity = CollectionEntity(
                            id = newId,
                            name = collection.name,
                            icon = collection.icon,
                            color = collection.color,
                            isFavorite = collection.isFavorite,
                            sortOrder = collection.sortOrder,
                            createdAt = collection.createdAt,
                            updatedAt = collection.updatedAt
                        )
                        collectionDao.insert(entity)
                        idMap[collection.id] = newId
                        imported++
                    }
                }
            } catch (e: Exception) {
                errors.add("Failed to import collection '${collection.name}': ${e.message}")
            }
        }
        return imported
    }

    private suspend fun importLinks(
        links: List<LinkExport>,
        conflictStrategy: ImportConflictStrategy,
        collectionIdMap: Map<String, String>,
        linkIdMap: MutableMap<String, String>,
        errors: MutableList<String>,
        onDuplicate: () -> Unit
    ): Int {
        var imported = 0
        val existingUrls = linkDao.getAllLinksSync()
            .filter { it.deletedAt == null }
            .map { it.url.lowercase() }
            .toSet()

        for (link in links) {
            try {
                val urlExists = link.url.lowercase() in existingUrls

                when {
                    urlExists && conflictStrategy == ImportConflictStrategy.SKIP -> {
                        onDuplicate()
                        // Don't map ID - link won't be imported
                    }
                    urlExists && conflictStrategy == ImportConflictStrategy.REPLACE -> {
                        val existing = linkDao.getAllLinksSync().first {
                            it.url.equals(link.url, ignoreCase = true) && it.deletedAt == null
                        }
                        val mappedCollectionId = link.collectionId?.let { collectionIdMap[it] }
                        val updated = LinkEntity(
                            id = existing.id,
                            title = link.title,
                            url = link.url,
                            description = link.description,
                            note = link.note,
                            collectionId = mappedCollectionId,
                            previewUrl = link.previewUrl,
                            previewImagePath = link.previewImagePath,
                            isFavorite = link.isFavorite,
                            isArchived = link.isArchived,
                            hideFromHome = link.hideFromHome,
                            deletedAt = null,
                            createdAt = existing.createdAt,
                            updatedAt = System.currentTimeMillis()
                        )
                        linkDao.update(updated)
                        linkIdMap[link.id] = existing.id
                        imported++
                    }
                    else -> {
                        val newId = UUID.randomUUID().toString()
                        val mappedCollectionId = link.collectionId?.let { collectionIdMap[it] }
                        val entity = LinkEntity(
                            id = newId,
                            title = link.title,
                            url = link.url,
                            description = link.description,
                            note = link.note,
                            collectionId = mappedCollectionId,
                            previewUrl = link.previewUrl,
                            previewImagePath = link.previewImagePath,
                            isFavorite = link.isFavorite,
                            isArchived = link.isArchived,
                            hideFromHome = link.hideFromHome,
                            deletedAt = null,
                            createdAt = link.createdAt,
                            updatedAt = link.updatedAt
                        )
                        linkDao.insert(entity)
                        linkIdMap[link.id] = newId
                        imported++
                    }
                }
            } catch (e: Exception) {
                errors.add("Failed to import link '${link.title}': ${e.message}")
            }
        }
        return imported
    }

    private suspend fun importSnapshots(
        snapshots: List<SnapshotExport>,
        linkIdMap: Map<String, String>,
        errors: MutableList<String>
    ): Int {
        var imported = 0
        for (snapshot in snapshots) {
            try {
                val newLinkId = linkIdMap[snapshot.linkId]
                if (newLinkId != null) {
                    val newId = UUID.randomUUID().toString()
                    val entity = SnapshotEntity(
                        id = newId,
                        linkId = newLinkId,
                        type = snapshot.type,
                        filePath = snapshot.filePath,
                        fileSize = snapshot.fileSize,
                        title = snapshot.title,
                        author = snapshot.author,
                        excerpt = snapshot.excerpt,
                        wordCount = snapshot.wordCount,
                        estimatedReadTime = snapshot.estimatedReadTime,
                        createdAt = snapshot.createdAt
                    )
                    snapshotDao.insert(entity)
                    imported++
                }
            } catch (e: Exception) {
                errors.add("Failed to import snapshot: ${e.message}")
            }
        }
        return imported
    }

    private fun readFileContent(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().readText()
        } ?: throw IllegalStateException("Could not read file")
    }
}
