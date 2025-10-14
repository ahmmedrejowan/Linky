package com.rejowan.linky.domain.usecase.link

import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.repository.LinkRepository
import kotlinx.coroutines.flow.Flow

class GetFavoriteLinksUseCase(
    private val linkRepository: LinkRepository
) {
    operator fun invoke(): Flow<List<Link>> {
        return linkRepository.getFavoriteLinks()
    }
}
