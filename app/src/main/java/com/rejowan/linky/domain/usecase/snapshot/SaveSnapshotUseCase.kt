package com.rejowan.linky.domain.usecase.snapshot

import com.rejowan.linky.domain.model.Snapshot
import com.rejowan.linky.domain.repository.SnapshotRepository
import com.rejowan.linky.util.Result

class SaveSnapshotUseCase(
    private val snapshotRepository: SnapshotRepository
) {
    suspend operator fun invoke(snapshot: Snapshot): Result<Unit> {
        return snapshotRepository.saveSnapshot(snapshot)
    }
}
