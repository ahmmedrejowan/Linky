package com.rejowan.linky.domain.usecase.collection

import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow

class GetCollectionByIdUseCase(
    private val collectionRepository: CollectionRepository
) {
    operator fun invoke(collectionId: String): Flow<Collection?> {
        return collectionRepository.getCollectionById(collectionId)
    }
}
