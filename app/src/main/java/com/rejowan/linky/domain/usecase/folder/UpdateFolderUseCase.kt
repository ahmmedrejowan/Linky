package com.rejowan.linky.domain.usecase.folder

import com.rejowan.linky.domain.model.Folder
import com.rejowan.linky.domain.repository.FolderRepository
import com.rejowan.linky.util.Result
import com.rejowan.linky.util.ValidationResult
import com.rejowan.linky.util.Validator

class UpdateFolderUseCase(
    private val folderRepository: FolderRepository
) {
    suspend operator fun invoke(folder: Folder): Result<Unit> {
        // Domain-level validation for folder name
        val nameValidation = Validator.validateFolderName(folder.name)
        if (nameValidation is ValidationResult.Error) {
            return Result.Error(IllegalArgumentException(nameValidation.message))
        }

        // Validate color if provided
        if (folder.color != null) {
            val colorValidation = Validator.validateColor(folder.color)
            if (colorValidation is ValidationResult.Error) {
                return Result.Error(IllegalArgumentException(colorValidation.message))
            }
        }

        return folderRepository.updateFolder(folder)
    }
}
