package com.rejowan.linky.domain.usecase.link

import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.repository.LinkRepository
import kotlinx.coroutines.flow.Flow

class SearchLinksUseCase(
    private val linkRepository: LinkRepository
) {
    operator fun invoke(query: String): Flow<List<Link>> {
        return linkRepository.searchLinks(query)
    }
}
