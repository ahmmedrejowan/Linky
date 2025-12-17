package com.rejowan.linky.domain.usecase.tag

import com.rejowan.linky.domain.model.Tag
import com.rejowan.linky.domain.repository.TagRepository
import com.rejowan.linky.util.Result

class SaveTagUseCase(
    private val tagRepository: TagRepository
) {
    suspend operator fun invoke(tag: Tag): Result<Unit> {
        // Validate tag name
        if (tag.name.isBlank()) {
            return Result.Error(IllegalArgumentException("Tag name cannot be empty"))
        }

        // Check for duplicate name
        val existingTag = tagRepository.getTagByName(tag.name.trim())
        if (existingTag != null) {
            return Result.Error(IllegalArgumentException("A tag with this name already exists"))
        }

        val tagToSave = tag.copy(name = tag.name.trim())
        return tagRepository.saveTag(tagToSave)
    }
}
