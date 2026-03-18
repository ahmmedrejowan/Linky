package com.rejowan.linky.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rejowan.linky.data.local.database.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY sortOrder ASC, name ASC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    // Get all collections synchronously (for export)
    @Query("SELECT * FROM collections ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllCollectionsSync(): List<CollectionEntity>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getById(id: String): CollectionEntity?

    @Query("SELECT * FROM collections WHERE id = :id")
    fun getByIdFlow(id: String): Flow<CollectionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: CollectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(collections: List<CollectionEntity>)

    @Update
    suspend fun update(collection: CollectionEntity)

    @Delete
    suspend fun delete(collection: CollectionEntity)

    // Delete all collections
    @Query("DELETE FROM collections")
    suspend fun deleteAll()

    // Get collection with link count (excludes archived and deleted)
    @Query("""
        SELECT collections.*, COUNT(links.id) as linkCount
        FROM collections
        LEFT JOIN links ON collections.id = links.collectionId AND links.deletedAt IS NULL AND links.isArchived = 0
        GROUP BY collections.id
        ORDER BY collections.sortOrder ASC
    """)
    fun getCollectionsWithCount(): Flow<List<CollectionWithCount>>

    // Get collection with link count and preview images (up to 3 most recent links, excludes archived)
    @Query("""
        SELECT collections.*, COUNT(links.id) as linkCount,
        GROUP_CONCAT(links.previewImagePath, '|') as previewImages
        FROM collections
        LEFT JOIN (
            SELECT * FROM links
            WHERE deletedAt IS NULL AND isArchived = 0
            ORDER BY createdAt DESC
            LIMIT 3
        ) as links ON collections.id = links.collectionId
        GROUP BY collections.id
        ORDER BY collections.sortOrder ASC
    """)
    fun getCollectionsWithCountAndPreviews(): Flow<List<CollectionWithCountAndPreviews>>

    // Get preview images for a specific collection (up to 3 most recent, excludes archived)
    @Query("""
        SELECT previewImagePath FROM links
        WHERE collectionId = :collectionId
            AND deletedAt IS NULL
            AND isArchived = 0
            AND previewImagePath IS NOT NULL
        ORDER BY createdAt DESC
        LIMIT 3
    """)
    suspend fun getPreviewsForCollection(collectionId: String): List<String>

    // Count collections
    @Query("SELECT COUNT(*) FROM collections")
    suspend fun countCollections(): Int

    // Search collections by name
    @Query("""
        SELECT collections.*, COUNT(links.id) as linkCount
        FROM collections
        LEFT JOIN links ON collections.id = links.collectionId AND links.deletedAt IS NULL AND links.isArchived = 0
        WHERE collections.name LIKE '%' || :query || '%'
        GROUP BY collections.id
        ORDER BY collections.name ASC
    """)
    fun searchCollections(query: String): Flow<List<CollectionWithCount>>

    // Phase 2: Sync queries
    @Query("SELECT * FROM collections WHERE syncToRemote = 1")
    suspend fun getDirtyCollections(): List<CollectionEntity>

    @Query("UPDATE collections SET syncToRemote = 0 WHERE id = :id")
    suspend fun markSynced(id: String)
}

data class CollectionWithCount(
    @Embedded val collection: CollectionEntity,
    val linkCount: Int
)

data class CollectionWithCountAndPreviews(
    @Embedded val collection: CollectionEntity,
    val linkCount: Int,
    val previewImages: String? // Pipe-separated preview image paths (|)
)
