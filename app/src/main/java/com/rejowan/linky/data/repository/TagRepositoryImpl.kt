package com.rejowan.linky.data.repository

import com.rejowan.linky.data.local.database.dao.TagDao
import com.rejowan.linky.data.local.database.entity.LinkTagCrossRef
import com.rejowan.linky.data.mapper.TagMapper.toDomain
import com.rejowan.linky.data.mapper.TagMapper.toDomainList
import com.rejowan.linky.data.mapper.TagMapper.toDomainWithCountList
import com.rejowan.linky.data.mapper.TagMapper.toEntity
import com.rejowan.linky.domain.model.Tag
import com.rejowan.linky.domain.model.TagWithCount
import com.rejowan.linky.domain.repository.TagRepository
import com.rejowan.linky.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class TagRepositoryImpl(
    private val tagDao: TagDao
) : TagRepository {

    // ============ TAG CRUD ============

    override fun getAllTags(): Flow<List<Tag>> {
        return tagDao.getAllTags().map { it.toDomainList() }
    }

    override suspend fun getAllTagsOnce(): List<Tag> {
        return tagDao.getAllTagsOnce().toDomainList()
    }

    override fun getTagById(id: String): Flow<Tag?> {
        return tagDao.getByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun getTagByIdOnce(id: String): Tag? {
        return tagDao.getById(id)?.toDomain()
    }

    override suspend fun getTagByName(name: String): Tag? {
        return tagDao.getByName(name)?.toDomain()
    }

    override suspend fun saveTag(tag: Tag): Result<Unit> {
        return try {
            tagDao.insert(tag.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save tag")
            Result.Error(e)
        }
    }

    override suspend fun updateTag(tag: Tag): Result<Unit> {
        return try {
            val entity = tag.copy(updatedAt = System.currentTimeMillis()).toEntity()
            tagDao.update(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update tag")
            Result.Error(e)
        }
    }

    override suspend fun deleteTag(tagId: String): Result<Unit> {
        return try {
            tagDao.deleteById(tagId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete tag")
            Result.Error(e)
        }
    }

    override suspend fun countTags(): Int {
        return tagDao.countTags()
    }

    override fun countTagsFlow(): Flow<Int> {
        return tagDao.countTagsFlow()
    }

    // ============ TAG WITH COUNT ============

    override fun getTagsWithLinkCount(): Flow<List<TagWithCount>> {
        return tagDao.getTagsWithLinkCount().map { it.toDomainWithCountList() }
    }

    // ============ LINK-TAG ASSOCIATIONS ============

    override fun getTagsForLink(linkId: String): Flow<List<Tag>> {
        return tagDao.getTagsForLink(linkId).map { it.toDomainList() }
    }

    override suspend fun getTagsForLinkOnce(linkId: String): List<Tag> {
        return tagDao.getTagsForLinkOnce(linkId).toDomainList()
    }

    override fun getLinkIdsForTag(tagId: String): Flow<List<String>> {
        return tagDao.getLinkIdsForTag(tagId)
    }

    override suspend fun addTagToLink(linkId: String, tagId: String): Result<Unit> {
        return try {
            tagDao.addTagToLink(LinkTagCrossRef(linkId, tagId))
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add tag to link")
            Result.Error(e)
        }
    }

    override suspend fun removeTagFromLink(linkId: String, tagId: String): Result<Unit> {
        return try {
            tagDao.removeTagFromLink(linkId, tagId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove tag from link")
            Result.Error(e)
        }
    }

    override suspend fun setTagsForLink(linkId: String, tagIds: List<String>): Result<Unit> {
        return try {
            tagDao.setTagsForLink(linkId, tagIds)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set tags for link")
            Result.Error(e)
        }
    }

    override suspend fun linkHasTag(linkId: String, tagId: String): Boolean {
        return tagDao.linkHasTag(linkId, tagId)
    }
}
