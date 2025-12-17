package com.rejowan.linky.domain.usecase.tag

import com.rejowan.linky.domain.model.Tag
import com.rejowan.linky.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

class GetAllTagsUseCase(
    private val tagRepository: TagRepository
) {
    operator fun invoke(): Flow<List<Tag>> {
        return tagRepository.getAllTags()
    }

    suspend fun once(): List<Tag> {
        return tagRepository.getAllTagsOnce()
    }
}
