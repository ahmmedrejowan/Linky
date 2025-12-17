package com.rejowan.linky.domain.usecase.tag

import com.rejowan.linky.domain.repository.TagRepository
import com.rejowan.linky.util.Result

class DeleteTagUseCase(
    private val tagRepository: TagRepository
) {
    suspend operator fun invoke(tagId: String): Result<Unit> {
        return tagRepository.deleteTag(tagId)
    }
}
