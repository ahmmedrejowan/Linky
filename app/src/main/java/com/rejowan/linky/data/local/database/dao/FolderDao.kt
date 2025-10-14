package com.rejowan.linky.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rejowan.linky.data.local.database.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY sortOrder ASC, name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getById(id: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE id = :id")
    fun getByIdFlow(id: String): Flow<FolderEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(folders: List<FolderEntity>)

    @Update
    suspend fun update(folder: FolderEntity)

    @Delete
    suspend fun delete(folder: FolderEntity)

    // Get folder with link count
    @Query("""
        SELECT folders.*, COUNT(links.id) as linkCount
        FROM folders
        LEFT JOIN links ON folders.id = links.folderId AND links.deletedAt IS NULL
        GROUP BY folders.id
        ORDER BY folders.sortOrder ASC
    """)
    fun getFoldersWithCount(): Flow<List<FolderWithCount>>

    // Count folders
    @Query("SELECT COUNT(*) FROM folders")
    suspend fun countFolders(): Int

    // Phase 2: Sync queries
    @Query("SELECT * FROM folders WHERE syncToRemote = 1")
    suspend fun getDirtyFolders(): List<FolderEntity>

    @Query("UPDATE folders SET syncToRemote = 0 WHERE id = :id")
    suspend fun markSynced(id: String)
}

data class FolderWithCount(
    @Embedded val folder: FolderEntity,
    val linkCount: Int
)
