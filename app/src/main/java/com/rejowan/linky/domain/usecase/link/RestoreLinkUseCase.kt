package com.rejowan.linky.domain.usecase.link

import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.util.Result

class RestoreLinkUseCase(
    private val linkRepository: LinkRepository
) {
    suspend operator fun invoke(linkId: String): Result<Unit> {
        return linkRepository.restoreLink(linkId)
    }
}
