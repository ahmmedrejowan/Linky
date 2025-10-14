package com.rejowan.linky.domain.usecase.snapshot

import com.rejowan.linky.domain.repository.SnapshotRepository
import com.rejowan.linky.util.Result

class DeleteSnapshotUseCase(
    private val snapshotRepository: SnapshotRepository
) {
    suspend operator fun invoke(snapshotId: String): Result<Unit> {
        return snapshotRepository.deleteSnapshot(snapshotId)
    }
}
