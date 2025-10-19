package com.rejowan.linky.domain.usecase.collection

import com.rejowan.linky.domain.repository.CollectionRepository
import com.rejowan.linky.util.Result

class DeleteCollectionUseCase(
    private val collectionRepository: CollectionRepository
) {
    suspend operator fun invoke(collectionId: String): Result<Unit> {
        return collectionRepository.deleteCollection(collectionId)
    }
}
