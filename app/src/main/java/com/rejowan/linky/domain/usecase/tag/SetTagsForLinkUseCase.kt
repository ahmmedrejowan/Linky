package com.rejowan.linky.domain.usecase.tag

import com.rejowan.linky.domain.repository.TagRepository
import com.rejowan.linky.util.Result

class SetTagsForLinkUseCase(
    private val tagRepository: TagRepository
) {
    /**
     * Sets the tags for a link, replacing any existing tags
     * @param linkId The ID of the link
     * @param tagIds The list of tag IDs to associate with the link
     */
    suspend operator fun invoke(linkId: String, tagIds: List<String>): Result<Unit> {
        return tagRepository.setTagsForLink(linkId, tagIds)
    }
}
