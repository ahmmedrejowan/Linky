package com.rejowan.linky.domain.usecase.collection

import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.repository.CollectionRepository
import com.rejowan.linky.util.Result
import com.rejowan.linky.util.ValidationResult
import com.rejowan.linky.util.Validator

class SaveCollectionUseCase(
    private val collectionRepository: CollectionRepository
) {
    suspend operator fun invoke(collection: Collection): Result<Unit> {
        // Domain-level validation for collection name
        val nameValidation = Validator.validateCollectionName(collection.name)
        if (nameValidation is ValidationResult.Error) {
            return Result.Error(IllegalArgumentException(nameValidation.message))
        }

        // Validate color if provided
        if (collection.color != null) {
            val colorValidation = Validator.validateColor(collection.color)
            if (colorValidation is ValidationResult.Error) {
                return Result.Error(IllegalArgumentException(colorValidation.message))
            }
        }

        return collectionRepository.saveCollection(collection)
    }
}
