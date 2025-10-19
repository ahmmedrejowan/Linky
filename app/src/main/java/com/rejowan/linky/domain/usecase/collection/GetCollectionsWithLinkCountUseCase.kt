package com.rejowan.linky.domain.usecase.collection

import com.rejowan.linky.domain.model.CollectionWithLinkCount
import com.rejowan.linky.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow

class GetCollectionsWithLinkCountUseCase(
    private val collectionRepository: CollectionRepository
) {
    operator fun invoke(): Flow<List<CollectionWithLinkCount>> {
        return collectionRepository.getCollectionsWithLinkCount()
    }
}
