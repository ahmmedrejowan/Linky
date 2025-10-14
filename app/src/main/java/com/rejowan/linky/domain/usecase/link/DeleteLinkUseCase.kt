package com.rejowan.linky.domain.usecase.link

import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.util.Result

class DeleteLinkUseCase(
    private val linkRepository: LinkRepository
) {
    suspend operator fun invoke(linkId: String, softDelete: Boolean = true): Result<Unit> {
        return if (softDelete) {
            linkRepository.softDeleteLink(linkId)
        } else {
            linkRepository.deleteLink(linkId)
        }
    }
}
