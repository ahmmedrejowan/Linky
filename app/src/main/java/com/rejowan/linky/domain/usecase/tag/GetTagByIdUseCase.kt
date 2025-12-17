package com.rejowan.linky.domain.usecase.tag

import com.rejowan.linky.domain.model.Tag
import com.rejowan.linky.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

class GetTagByIdUseCase(
    private val tagRepository: TagRepository
) {
    operator fun invoke(tagId: String): Flow<Tag?> {
        return tagRepository.getTagById(tagId)
    }

    suspend fun once(tagId: String): Tag? {
        return tagRepository.getTagByIdOnce(tagId)
    }
}
