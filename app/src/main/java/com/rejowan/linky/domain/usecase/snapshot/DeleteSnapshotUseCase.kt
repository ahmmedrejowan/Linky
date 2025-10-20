package com.rejowan.linky.domain.usecase.snapshot

import com.rejowan.linky.domain.repository.SnapshotRepository
import com.rejowan.linky.util.FileStorageManager
import com.rejowan.linky.util.Result
import timber.log.Timber

class DeleteSnapshotUseCase(
    private val snapshotRepository: SnapshotRepository,
    private val fileStorageManager: FileStorageManager
) {
    suspend operator fun invoke(snapshotId: String): Result<Unit> {
        return try {
            Timber.d("DeleteSnapshotUseCase: Deleting snapshot | ID: $snapshotId")

            // Step 1: Get snapshot to retrieve file path
            val snapshot = snapshotRepository.getSnapshotByIdOnce(snapshotId)

            if (snapshot == null) {
                Timber.w("DeleteSnapshotUseCase: Snapshot not found | ID: $snapshotId")
                return Result.Error(Exception("Snapshot not found"))
            }

            Timber.d("DeleteSnapshotUseCase: Snapshot found | File path: ${snapshot.filePath}")

            // Step 2: Delete file
            Timber.d("DeleteSnapshotUseCase: Deleting file...")
            val fileDeleted = fileStorageManager.deleteSnapshot(snapshot.filePath)

            if (!fileDeleted) {
                Timber.w("DeleteSnapshotUseCase: Failed to delete file, but will continue with database deletion")
            } else {
                Timber.d("DeleteSnapshotUseCase: File deleted successfully")
            }

            // Step 3: Delete from database
            Timber.d("DeleteSnapshotUseCase: Deleting from database...")
            val result = snapshotRepository.deleteSnapshot(snapshotId)

            when (result) {
                is Result.Success -> {
                    Timber.d("DeleteSnapshotUseCase: Snapshot deleted successfully")
                }
                is Result.Error -> {
                    Timber.e(result.exception, "DeleteSnapshotUseCase: Failed to delete from database")
                }
                is Result.Loading -> {}
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "DeleteSnapshotUseCase: Unexpected error during deletion")
            Result.Error(e)
        }
    }
}
