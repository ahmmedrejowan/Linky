package com.rejowan.linky.domain.usecase.folder

import com.rejowan.linky.domain.model.Folder
import com.rejowan.linky.domain.repository.FolderRepository
import com.rejowan.linky.util.Result

class SaveFolderUseCase(
    private val folderRepository: FolderRepository
) {
    suspend operator fun invoke(folder: Folder): Result<Unit> {
        return folderRepository.saveFolder(folder)
    }
}
