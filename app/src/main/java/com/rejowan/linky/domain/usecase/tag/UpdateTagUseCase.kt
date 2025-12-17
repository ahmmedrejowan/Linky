package com.rejowan.linky.domain.usecase.tag

import com.rejowan.linky.domain.model.Tag
import com.rejowan.linky.domain.repository.TagRepository
import com.rejowan.linky.util.Result

class UpdateTagUseCase(
    private val tagRepository: TagRepository
) {
    suspend operator fun invoke(tag: Tag): Result<Unit> {
        // Validate tag name
        if (tag.name.isBlank()) {
            return Result.Error(IllegalArgumentException("Tag name cannot be empty"))
        }

        // Check for duplicate name (excluding current tag)
        val existingTag = tagRepository.getTagByName(tag.name.trim())
        if (existingTag != null && existingTag.id != tag.id) {
            return Result.Error(IllegalArgumentException("A tag with this name already exists"))
        }

        val tagToUpdate = tag.copy(
            name = tag.name.trim(),
            updatedAt = System.currentTimeMillis()
        )
        return tagRepository.updateTag(tagToUpdate)
    }
}
