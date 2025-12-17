package com.rejowan.linky.domain.usecase.tag

import com.rejowan.linky.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

class GetLinksByTagUseCase(
    private val tagRepository: TagRepository
) {
    /**
     * Gets all link IDs that have a specific tag
     * @param tagId The ID of the tag to filter by
     * @return Flow of link IDs that have the specified tag
     */
    operator fun invoke(tagId: String): Flow<List<String>> {
        return tagRepository.getLinkIdsForTag(tagId)
    }
}
