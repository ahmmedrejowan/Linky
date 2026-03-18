package com.rejowan.linky.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rejowan.linky.data.local.database.entity.SnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM snapshots WHERE linkId = :linkId ORDER BY createdAt DESC")
    fun getSnapshotsForLink(linkId: String): Flow<List<SnapshotEntity>>

    // Get all snapshots synchronously (for export)
    @Query("SELECT * FROM snapshots ORDER BY createdAt DESC")
    suspend fun getAllSnapshotsSync(): List<SnapshotEntity>

    @Query("SELECT * FROM snapshots WHERE id = :id")
    suspend fun getById(id: String): SnapshotEntity?

    @Query("SELECT * FROM snapshots WHERE id = :id")
    fun getByIdFlow(id: String): Flow<SnapshotEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: SnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snapshots: List<SnapshotEntity>)

    @Delete
    suspend fun delete(snapshot: SnapshotEntity)

    // Delete all snapshots for a link (when link is deleted)
    @Query("DELETE FROM snapshots WHERE linkId = :linkId")
    suspend fun deleteForLink(linkId: String)

    // Get total storage used by snapshots
    @Query("SELECT SUM(fileSize) FROM snapshots")
    suspend fun getTotalStorageUsed(): Long?

    // Get snapshot count for a link
    @Query("SELECT COUNT(*) FROM snapshots WHERE linkId = :linkId")
    suspend fun getSnapshotCount(linkId: String): Int

    // Delete all snapshots
    @Query("DELETE FROM snapshots")
    suspend fun deleteAll()
}
