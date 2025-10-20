package com.rejowan.linky.domain.usecase.snapshot

import com.rejowan.linky.domain.model.Snapshot
import com.rejowan.linky.domain.repository.SnapshotRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Use case to get a snapshot by its ID
 */
class GetSnapshotByIdUseCase(
    private val snapshotRepository: SnapshotRepository
) {
    /**
     * Gets a snapshot by ID as a Flow
     * @param snapshotId The ID of the snapshot
     * @return Flow of Snapshot or null if not found
     */
    operator fun invoke(snapshotId: String): Flow<Snapshot?> {
        Timber.d("GetSnapshotByIdUseCase: Getting snapshot | ID: $snapshotId")
        return snapshotRepository.getSnapshotById(snapshotId)
    }

    /**
     * Gets a snapshot by ID once (suspend function)
     * @param snapshotId The ID of the snapshot
     * @return Snapshot or null if not found
     */
    suspend fun getOnce(snapshotId: String): Snapshot? {
        Timber.d("GetSnapshotByIdUseCase: Getting snapshot once | ID: $snapshotId")
        return snapshotRepository.getSnapshotByIdOnce(snapshotId)
    }
}
