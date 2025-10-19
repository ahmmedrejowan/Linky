package com.rejowan.linky.domain.usecase.folder

import com.rejowan.linky.domain.model.FolderWithLinkCount
import com.rejowan.linky.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow

class GetFoldersWithLinkCountUseCase(
    private val folderRepository: FolderRepository
) {
    operator fun invoke(): Flow<List<FolderWithLinkCount>> {
        return folderRepository.getFoldersWithLinkCount()
    }
}