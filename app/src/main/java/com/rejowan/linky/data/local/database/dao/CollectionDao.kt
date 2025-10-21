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

    // Get collection with link count
    @Query("""
        SELECT collections.*, COUNT(links.id) as linkCount
        FROM collections
        LEFT JOIN links ON collections.id = links.collectionId AND links.deletedAt IS NULL
        GROUP BY collections.id
        ORDER BY collections.sortOrder ASC
    """)
    fun getCollectionsWithCount(): Flow<List<CollectionWithCount>>

    // Get collection with link count and preview images (up to 3 most recent links)
    @Query("""
        SELECT collections.*, COUNT(links.id) as linkCount,
        GROUP_CONCAT(links.previewImagePath, '|') as previewImages
        FROM collections
        LEFT JOIN (
            SELECT * FROM links
            WHERE deletedAt IS NULL
            ORDER BY createdAt DESC
            LIMIT 3
        ) as links ON collections.id = links.collectionId
        GROUP BY collections.id
        ORDER BY collections.sortOrder ASC
    """)
    fun getCollectionsWithCountAndPreviews(): Flow<List<CollectionWithCountAndPreviews>>

    // Get preview images for a specific collection (up to 3 most recent)
    @Query("""
        SELECT previewImagePath FROM links
        WHERE collectionId = :collectionId AND deletedAt IS NULL
        ORDER BY createdAt DESC
        LIMIT 3
    """)
    suspend fun getPreviewsForCollection(collectionId: String): List<String>

    // Count collections
    @Query("SELECT COUNT(*) FROM collections")
    suspend fun countCollections(): Int

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
