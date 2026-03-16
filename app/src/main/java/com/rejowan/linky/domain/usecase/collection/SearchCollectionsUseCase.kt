package com.rejowan.linky.domain.usecase.collection

import com.rejowan.linky.domain.model.CollectionWithLinkCount
import com.rejowan.linky.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow

class SearchCollectionsUseCase(
    private val collectionRepository: CollectionRepository
) {
    operator fun invoke(query: String): Flow<List<CollectionWithLinkCount>> {
        return collectionRepository.searchCollections(query)
    }
}
