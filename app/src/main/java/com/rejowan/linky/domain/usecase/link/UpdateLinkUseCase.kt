package com.rejowan.linky.domain.usecase.link

import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.util.Result
import com.rejowan.linky.util.ValidationResult
import com.rejowan.linky.util.Validator

class UpdateLinkUseCase(
    private val linkRepository: LinkRepository
) {
    suspend operator fun invoke(link: Link): Result<Unit> {
        // Domain-level validation
        val validationResult = Validator.validateLink(
            url = link.url,
            title = link.title,
            note = link.note
        )

        return if (validationResult is ValidationResult.Error) {
            Result.Error(IllegalArgumentException(validationResult.message))
        } else {
            linkRepository.updateLink(link)
        }
    }
}
