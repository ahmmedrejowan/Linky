package com.rejowan.linky.domain.usecase.link

import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.repository.LinkRepository
import kotlinx.coroutines.flow.Flow

class GetLinksByFolderUseCase(
    private val linkRepository: LinkRepository
) {
    operator fun invoke(folderId: String): Flow<List<Link>> {
        return linkRepository.getLinksByFolder(folderId)
    }
}
