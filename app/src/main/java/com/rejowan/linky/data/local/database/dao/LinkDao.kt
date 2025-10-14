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
    // Get all active links (not deleted, not archived)
    @Query("SELECT * FROM links WHERE deletedAt IS NULL AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllActiveLinks(): Flow<List<LinkEntity>>

    // Get all links (including archived, excluding deleted)
    @Query("SELECT * FROM links WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllLinks(): Flow<List<LinkEntity>>

    // Get favorites
    @Query("SELECT * FROM links WHERE deletedAt IS NULL AND isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteLinks(): Flow<List<LinkEntity>>

    // Get archived
    @Query("SELECT * FROM links WHERE deletedAt IS NULL AND isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedLinks(): Flow<List<LinkEntity>>

    // Get trashed (soft deleted)
    @Query("SELECT * FROM links WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getTrashedLinks(): Flow<List<LinkEntity>>

    // Get links by folder
    @Query("SELECT * FROM links WHERE deletedAt IS NULL AND folderId = :folderId ORDER BY updatedAt DESC")
    fun getLinksByFolder(folderId: String): Flow<List<LinkEntity>>

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

    // Phase 2: Sync queries
    @Query("SELECT * FROM links WHERE syncToRemote = 1")
    suspend fun getDirtyLinks(): List<LinkEntity>

    @Query("UPDATE links SET syncToRemote = 0 WHERE id = :id")
    suspend fun markSynced(id: String)
}
