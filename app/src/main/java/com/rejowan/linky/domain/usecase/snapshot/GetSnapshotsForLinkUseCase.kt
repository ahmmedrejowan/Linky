package com.rejowan.linky.domain.usecase.snapshot

import com.rejowan.linky.domain.model.Snapshot
import com.rejowan.linky.domain.repository.SnapshotRepository
import kotlinx.coroutines.flow.Flow

class GetSnapshotsForLinkUseCase(
    private val snapshotRepository: SnapshotRepository
) {
    operator fun invoke(linkId: String): Flow<List<Snapshot>> {
        return snapshotRepository.getSnapshotsForLink(linkId)
    }
}
