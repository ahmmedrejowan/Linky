package com.rejowan.linky.domain.usecase.folder

import com.rejowan.linky.domain.model.Folder
import com.rejowan.linky.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow

class GetAllFoldersUseCase(
    private val folderRepository: FolderRepository
) {
    operator fun invoke(): Flow<List<Folder>> {
        return folderRepository.getAllFolders()
    }
}
