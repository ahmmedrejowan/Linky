package com.rejowan.linky.data.repository

import com.rejowan.linky.data.local.database.dao.SnapshotDao
import com.rejowan.linky.data.mapper.SnapshotMapper.toDomain
import com.rejowan.linky.data.mapper.SnapshotMapper.toDomainList
import com.rejowan.linky.data.mapper.SnapshotMapper.toEntity
import com.rejowan.linky.domain.model.Snapshot
import com.rejowan.linky.domain.repository.SnapshotRepository
import com.rejowan.linky.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class SnapshotRepositoryImpl(
    private val snapshotDao: SnapshotDao
) : SnapshotRepository {

    override fun getSnapshotsForLink(linkId: String): Flow<List<Snapshot>> {
        return snapshotDao.getSnapshotsForLink(linkId).map { it.toDomainList() }
    }

    override fun getSnapshotById(id: String): Flow<Snapshot?> {
        return snapshotDao.getByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun getSnapshotByIdOnce(id: String): Snapshot? {
        return snapshotDao.getById(id)?.toDomain()
    }

    override suspend fun getSnapshotCount(linkId: String): Int {
        return snapshotDao.getSnapshotCount(linkId)
    }

    override suspend fun getTotalStorageUsed(): Long {
        return snapshotDao.getTotalStorageUsed() ?: 0L
    }

    override suspend fun saveSnapshot(snapshot: Snapshot): Result<Unit> {
        return try {
            val entity = snapshot.toEntity()
            snapshotDao.insert(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save snapshot")
            Result.Error(e)
        }
    }

    override suspend fun deleteSnapshot(snapshotId: String): Result<Unit> {
        return try {
            val snapshot = snapshotDao.getById(snapshotId)
            if (snapshot != null) {
                snapshotDao.delete(snapshot)
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Snapshot not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete snapshot")
            Result.Error(e)
        }
    }

    override suspend fun deleteSnapshotsForLink(linkId: String): Result<Unit> {
        return try {
            snapshotDao.deleteForLink(linkId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete snapshots for link")
            Result.Error(e)
        }
    }

    override suspend fun deleteAllSnapshots(): Result<Unit> {
        return try {
            snapshotDao.deleteAll()
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete all snapshots")
            Result.Error(e)
        }
    }
}
