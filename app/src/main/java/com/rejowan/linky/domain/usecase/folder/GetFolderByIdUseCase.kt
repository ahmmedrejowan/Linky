package com.rejowan.linky.domain.usecase.folder

import com.rejowan.linky.domain.model.Folder
import com.rejowan.linky.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow

class GetFolderByIdUseCase(
    private val folderRepository: FolderRepository
) {
    operator fun invoke(folderId: String): Flow<Folder?> {
        return folderRepository.getFolderById(folderId)
    }
}
