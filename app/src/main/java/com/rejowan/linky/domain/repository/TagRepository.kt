package com.rejowan.linky.domain.repository

import com.rejowan.linky.domain.model.Tag
import com.rejowan.linky.domain.model.TagWithCount
import com.rejowan.linky.util.Result
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    // ============ TAG CRUD ============

    fun getAllTags(): Flow<List<Tag>>

    suspend fun getAllTagsOnce(): List<Tag>

    fun getTagById(id: String): Flow<Tag?>

    suspend fun getTagByIdOnce(id: String): Tag?

    suspend fun getTagByName(name: String): Tag?

    suspend fun saveTag(tag: Tag): Result<Unit>

    suspend fun updateTag(tag: Tag): Result<Unit>

    suspend fun deleteTag(tagId: String): Result<Unit>

    suspend fun countTags(): Int

    fun countTagsFlow(): Flow<Int>

    // ============ TAG WITH COUNT ============

    fun getTagsWithLinkCount(): Flow<List<TagWithCount>>

    // ============ LINK-TAG ASSOCIATIONS ============

    fun getTagsForLink(linkId: String): Flow<List<Tag>>

    suspend fun getTagsForLinkOnce(linkId: String): List<Tag>

    fun getLinkIdsForTag(tagId: String): Flow<List<String>>

    suspend fun addTagToLink(linkId: String, tagId: String): Result<Unit>

    suspend fun removeTagFromLink(linkId: String, tagId: String): Result<Unit>

    suspend fun setTagsForLink(linkId: String, tagIds: List<String>): Result<Unit>

    suspend fun linkHasTag(linkId: String, tagId: String): Boolean
}
