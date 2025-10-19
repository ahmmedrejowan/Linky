package com.rejowan.linky.domain.usecase.link

import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.repository.LinkRepository
import kotlinx.coroutines.flow.Flow

class GetLinksByCollectionUseCase(
    private val linkRepository: LinkRepository
) {
    operator fun invoke(collectionId: String): Flow<List<Link>> {
        return linkRepository.getLinksByCollection(collectionId)
    }
}
