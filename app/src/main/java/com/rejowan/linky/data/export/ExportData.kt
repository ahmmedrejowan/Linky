package com.rejowan.linky.data.export

import kotlinx.serialization.Serializable

/**
 * Root export data structure
 */
@Serializable
data class ExportData(
    val version: Int = CURRENT_VERSION,
    val exportDate: String,
    val app: String = "Linky",
    val data: ExportPayload
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

/**
 * Payload containing all exported data
 */
@Serializable
data class ExportPayload(
    val links: List<LinkExport>,
    val collections: List<CollectionExport>,
    val tags: List<TagExport>,
    val linkTagRelations: List<LinkTagRelation>,
    val snapshots: List<SnapshotExport>? = null
)

/**
 * Link data for export
 */
@Serializable
data class LinkExport(
    val id: String,
    val title: String,
    val url: String,
    val description: String? = null,
    val note: String? = null,
    val collectionId: String? = null,
    val previewUrl: String? = null,
    val previewImagePath: String? = null,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val hideFromHome: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Collection data for export
 */
@Serializable
data class CollectionExport(
    val id: String,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    val isFavorite: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Tag data for export
 */
@Serializable
data class TagExport(
    val id: String,
    val name: String,
    val color: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Link-Tag relationship for export
 */
@Serializable
data class LinkTagRelation(
    val linkId: String,
    val tagId: String
)

/**
 * Snapshot data for export (optional, can be large)
 */
@Serializable
data class SnapshotExport(
    val id: String,
    val linkId: String,
    val type: String,
    val filePath: String,
    val fileSize: Long,
    val title: String? = null,
    val author: String? = null,
    val excerpt: String? = null,
    val wordCount: Int? = null,
    val estimatedReadTime: Int? = null,
    val createdAt: Long
)

/**
 * Summary of export operation
 */
data class ExportSummary(
    val linksCount: Int,
    val collectionsCount: Int,
    val tagsCount: Int,
    val snapshotsCount: Int,
    val fileSize: String,
    val filePath: String
)

/**
 * Preview of import file before importing
 */
data class ImportPreview(
    val version: Int,
    val exportDate: String,
    val totalLinks: Int,
    val totalCollections: Int,
    val totalTags: Int,
    val hasSnapshots: Boolean,
    val snapshotsCount: Int
)

/**
 * Summary of import operation
 */
data class ImportSummary(
    val linksImported: Int,
    val collectionsImported: Int,
    val tagsImported: Int,
    val snapshotsImported: Int,
    val duplicatesSkipped: Int,
    val errors: List<String>
)

/**
 * Import conflict resolution strategy
 */
enum class ImportConflictStrategy {
    SKIP,       // Skip existing items
    REPLACE,    // Replace existing with imported
    KEEP_BOTH   // Import with new ID
}
