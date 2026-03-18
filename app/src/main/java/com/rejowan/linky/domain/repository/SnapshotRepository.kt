package com.rejowan.linky.domain.repository

import com.rejowan.linky.domain.model.Snapshot
import com.rejowan.linky.util.Result
import kotlinx.coroutines.flow.Flow

interface SnapshotRepository {
    // Get operations
    fun getSnapshotsForLink(linkId: String): Flow<List<Snapshot>>
    fun getSnapshotById(id: String): Flow<Snapshot?>
    suspend fun getSnapshotByIdOnce(id: String): Snapshot?
    suspend fun getSnapshotCount(linkId: String): Int
    suspend fun getTotalStorageUsed(): Long

    // Create operations
    suspend fun saveSnapshot(snapshot: Snapshot): Result<Unit>

    // Delete operations
    suspend fun deleteSnapshot(snapshotId: String): Result<Unit>
    suspend fun deleteSnapshotsForLink(linkId: String): Result<Unit>
    suspend fun deleteAllSnapshots(): Result<Unit>
}
