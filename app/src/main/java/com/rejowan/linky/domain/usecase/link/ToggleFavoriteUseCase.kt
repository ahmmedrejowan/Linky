package com.rejowan.linky.domain.usecase.link

import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.util.Result

class ToggleFavoriteUseCase(
    private val linkRepository: LinkRepository
) {
    suspend operator fun invoke(linkId: String, isFavorite: Boolean): Result<Unit> {
        return linkRepository.toggleFavorite(linkId, isFavorite)
    }
}
