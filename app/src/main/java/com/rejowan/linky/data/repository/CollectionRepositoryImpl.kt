package com.rejowan.linky.data.repository

import com.rejowan.linky.data.local.database.dao.CollectionDao
import com.rejowan.linky.data.mapper.CollectionMapper.toDomain
import com.rejowan.linky.data.mapper.CollectionMapper.toDomainList
import com.rejowan.linky.data.mapper.CollectionMapper.toEntity
import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.model.CollectionWithLinkCount
import com.rejowan.linky.domain.repository.CollectionRepository
import com.rejowan.linky.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class CollectionRepositoryImpl(
    private val collectionDao: CollectionDao
) : CollectionRepository {

    override fun getAllCollections(): Flow<List<Collection>> {
        return collectionDao.getAllCollections().map { it.toDomainList() }
    }

    override fun getCollectionById(id: String): Flow<Collection?> {
        return collectionDao.getByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun getCollectionByIdOnce(id: String): Collection? {
        return collectionDao.getById(id)?.toDomain()
    }

    override fun getCollectionsWithLinkCount(): Flow<List<CollectionWithLinkCount>> {
        return collectionDao.getCollectionsWithCount().map { collectionWithCountList ->
            collectionWithCountList.map { collectionWithCount ->
                CollectionWithLinkCount(
                    collection = collectionWithCount.collection.toDomain(),
                    linkCount = collectionWithCount.linkCount
                )
            }
        }
    }

    override suspend fun saveCollection(collection: Collection): Result<Unit> {
        return try {
            val entity = collection.toEntity(syncToRemote = false)
            collectionDao.insert(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save collection")
            Result.Error(e)
        }
    }

    override suspend fun updateCollection(collection: Collection): Result<Unit> {
        return try {
            val entity = collection.copy(updatedAt = System.currentTimeMillis()).toEntity(syncToRemote = false)
            collectionDao.update(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update collection")
            Result.Error(e)
        }
    }

    override suspend fun deleteCollection(collectionId: String): Result<Unit> {
        return try {
            val collection = collectionDao.getById(collectionId)
            if (collection != null) {
                collectionDao.delete(collection)
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Collection not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete collection")
            Result.Error(e)
        }
    }

    override suspend fun countCollections(): Int {
        return collectionDao.countCollections()
    }
}
