package com.rejowan.linky.domain.usecase.tag

import com.rejowan.linky.domain.model.TagWithCount
import com.rejowan.linky.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

class GetTagsWithLinkCountUseCase(
    private val tagRepository: TagRepository
) {
    operator fun invoke(): Flow<List<TagWithCount>> {
        return tagRepository.getTagsWithLinkCount()
    }
}
