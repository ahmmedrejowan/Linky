package com.rejowan.linky.domain.usecase.link

import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.util.Result

class ToggleArchiveUseCase(
    private val linkRepository: LinkRepository
) {
    suspend operator fun invoke(linkId: String, isArchived: Boolean): Result<Unit> {
        return linkRepository.toggleArchive(linkId, isArchived)
    }
}
