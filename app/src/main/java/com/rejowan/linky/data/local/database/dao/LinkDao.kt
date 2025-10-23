package com.rejowan.linky.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rejowan.linky.data.local.database.entity.LinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkDao {
    // Get all active links (not deleted, not archived, not hidden from home) - favorites first, then by date
    @Query("SELECT * FROM links WHERE deletedAt IS NULL AND isArchived = 0 AND hideFromHome = 0 ORDER BY isFavorite DESC, updatedAt DESC")
    fun getAllActiveLinks(): Flow<List<LinkEntity>>

    // Get all links (excluding archived, deleted, and hidden from home) - used for "All" filter - favorites first, then by date
    @Query("SELECT * FROM links WHERE deletedAt IS NULL AND isArchived = 0 AND hideFromHome = 0 ORDER BY isFavorite DESC, updatedAt DESC")
    fun getAllLinks(): Flow<List<LinkEntity>>

    // Get favorites (excluding archived, deleted, and hidden from home)
    @Query("SELECT * FROM links WHERE deletedAt IS NULL AND isArchived = 0 AND hideFromHome = 0 AND isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteLinks(): Flow<List<LinkEntity>>

    // Get archived (excluding hidden from home) - favorites first, then by date
    @Query("SELECT * FROM links WHERE deletedAt IS NULL AND isArchived = 1 AND hideFromHome = 0 ORDER BY isFavorite DESC, updatedAt DESC")
    fun getArchivedLinks(): Flow<List<LinkEntity>>

    // Get trashed (soft deleted)
    @Query("SELECT * FROM links WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getTrashedLinks(): Flow<List<LinkEntity>>

    // Get links by collection (excludes archived and deleted)
    @Query("SELECT * FROM links WHERE deletedAt IS NULL AND isArchived = 0 AND collectionId = :collectionId ORDER BY updatedAt DESC")
    fun getLinksByCollection(collectionId: String): Flow<List<LinkEntity>>

    // Search links
    @Query("""
        SELECT * FROM links
        WHERE deletedAt IS NULL
        AND (title LIKE '%' || :query || '%'
             OR url LIKE '%' || :query || '%'
             OR note LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    fun searchLinks(query: String): Flow<List<LinkEntity>>

    // Get by ID
    @Query("SELECT * FROM links WHERE id = :id")
    suspend fun getById(id: String): LinkEntity?

    // Get by ID as Flow
    @Query("SELECT * FROM links WHERE id = :id")
    fun getByIdFlow(id: String): Flow<LinkEntity?>

    // Check if URL exists (excluding deleted links)
    @Query("SELECT EXISTS(SELECT 1 FROM links WHERE url = :url AND deletedAt IS NULL)")
    suspend fun existsByUrl(url: String): Boolean

    // Insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: LinkEntity)

    // Insert multiple
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(links: List<LinkEntity>)

    // Update
    @Update
    suspend fun update(link: LinkEntity)

    // Delete (hard delete)
    @Delete
    suspend fun delete(link: LinkEntity)

    // Soft delete
    @Query("UPDATE links SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())

    // Restore from trash
    @Query("UPDATE links SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: String)

    // Toggle favorite
    @Query("UPDATE links SET isFavorite = :isFavorite, updatedAt = :timestamp WHERE id = :id")
    suspend fun toggleFavorite(id: String, isFavorite: Boolean, timestamp: Long = System.currentTimeMillis())

    // Toggle archive
    @Query("UPDATE links SET isArchived = :isArchived, updatedAt = :timestamp WHERE id = :id")
    suspend fun toggleArchive(id: String, isArchived: Boolean, timestamp: Long = System.currentTimeMillis())

    // Count all links
    @Query("SELECT COUNT(*) FROM links WHERE deletedAt IS NULL")
    suspend fun countLinks(): Int

    // Count queries with Flow for real-time updates (excluding hidden from home)
    @Query("SELECT COUNT(*) FROM links WHERE deletedAt IS NULL AND isArchived = 0 AND hideFromHome = 0")
    fun getAllLinksCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM links WHERE deletedAt IS NULL AND isArchived = 0 AND hideFromHome = 0 AND isFavorite = 1")
    fun getFavoriteLinksCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM links WHERE deletedAt IS NULL AND isArchived = 1 AND hideFromHome = 0")
    fun getArchivedLinksCount(): Flow<Int>

    // Phase 2: Sync queries
    @Query("SELECT * FROM links WHERE syncToRemote = 1")
    suspend fun getDirtyLinks(): List<LinkEntity>

    @Query("UPDATE links SET syncToRemote = 0 WHERE id = :id")
    suspend fun markSynced(id: String)
}
