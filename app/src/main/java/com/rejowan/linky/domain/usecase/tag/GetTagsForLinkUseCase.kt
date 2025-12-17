package com.rejowan.linky.domain.usecase.tag

import com.rejowan.linky.domain.model.Tag
import com.rejowan.linky.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

class GetTagsForLinkUseCase(
    private val tagRepository: TagRepository
) {
    operator fun invoke(linkId: String): Flow<List<Tag>> {
        return tagRepository.getTagsForLink(linkId)
    }

    suspend fun once(linkId: String): List<Tag> {
        return tagRepository.getTagsForLinkOnce(linkId)
    }
}
