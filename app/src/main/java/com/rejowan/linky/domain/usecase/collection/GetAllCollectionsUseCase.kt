package com.rejowan.linky.domain.usecase.collection

import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow

class GetAllCollectionsUseCase(
    private val collectionRepository: CollectionRepository
) {
    operator fun invoke(): Flow<List<Collection>> {
        return collectionRepository.getAllCollections()
    }
}
