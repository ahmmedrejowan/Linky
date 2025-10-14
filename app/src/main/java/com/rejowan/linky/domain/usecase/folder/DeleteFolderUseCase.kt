package com.rejowan.linky.domain.usecase.folder

import com.rejowan.linky.domain.repository.FolderRepository
import com.rejowan.linky.util.Result

class DeleteFolderUseCase(
    private val folderRepository: FolderRepository
) {
    suspend operator fun invoke(folderId: String): Result<Unit> {
        return folderRepository.deleteFolder(folderId)
    }
}
