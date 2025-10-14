package com.rejowan.linky.domain.usecase.link

import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.util.Result

class UpdateLinkUseCase(
    private val linkRepository: LinkRepository
) {
    suspend operator fun invoke(link: Link): Result<Unit> {
        return linkRepository.updateLink(link)
    }
}
