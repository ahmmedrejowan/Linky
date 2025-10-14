package com.rejowan.linky.data.repository

import com.rejowan.linky.data.local.database.dao.LinkDao
import com.rejowan.linky.data.mapper.LinkMapper.toDomain
import com.rejowan.linky.data.mapper.LinkMapper.toDomainList
import com.rejowan.linky.data.mapper.LinkMapper.toEntity
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class LinkRepositoryImpl(
    private val linkDao: LinkDao
) : LinkRepository {

    override fun getAllActiveLinks(): Flow<List<Link>> {
        return linkDao.getAllActiveLinks().map { it.toDomainList() }
    }

    override fun getAllLinks(): Flow<List<Link>> {
        return linkDao.getAllLinks().map { it.toDomainList() }
    }

    override fun getFavoriteLinks(): Flow<List<Link>> {
        return linkDao.getFavoriteLinks().map { it.toDomainList() }
    }

    override fun getArchivedLinks(): Flow<List<Link>> {
        return linkDao.getArchivedLinks().map { it.toDomainList() }
    }

    override fun getTrashedLinks(): Flow<List<Link>> {
        return linkDao.getTrashedLinks().map { it.toDomainList() }
    }

    override fun getLinksByFolder(folderId: String): Flow<List<Link>> {
        return linkDao.getLinksByFolder(folderId).map { it.toDomainList() }
    }

    override fun searchLinks(query: String): Flow<List<Link>> {
        return linkDao.searchLinks(query).map { it.toDomainList() }
    }

    override fun getLinkById(id: String): Flow<Link?> {
        return linkDao.getByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun getLinkByIdOnce(id: String): Link? {
        return linkDao.getById(id)?.toDomain()
    }

    override suspend fun saveLink(link: Link): Result<Unit> {
        return try {
            val entity = link.toEntity(syncToRemote = false)
            linkDao.insert(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save link")
            Result.Error(e)
        }
    }

    override suspend fun updateLink(link: Link): Result<Unit> {
        return try {
            val entity = link.copy(updatedAt = System.currentTimeMillis()).toEntity(syncToRemote = false)
            linkDao.update(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update link")
            Result.Error(e)
        }
    }

    override suspend fun deleteLink(linkId: String): Result<Unit> {
        return try {
            val link = linkDao.getById(linkId)
            if (link != null) {
                linkDao.delete(link)
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Link not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete link")
            Result.Error(e)
        }
    }

    override suspend fun softDeleteLink(linkId: String): Result<Unit> {
        return try {
            linkDao.softDelete(linkId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to soft delete link")
            Result.Error(e)
        }
    }

    override suspend fun restoreLink(linkId: String): Result<Unit> {
        return try {
            linkDao.restore(linkId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to restore link")
            Result.Error(e)
        }
    }

    override suspend fun toggleFavorite(linkId: String, isFavorite: Boolean): Result<Unit> {
        return try {
            linkDao.toggleFavorite(linkId, isFavorite)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle favorite")
            Result.Error(e)
        }
    }

    override suspend fun toggleArchive(linkId: String, isArchived: Boolean): Result<Unit> {
        return try {
            linkDao.toggleArchive(linkId, isArchived)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle archive")
            Result.Error(e)
        }
    }

    override suspend fun countLinks(): Int {
        return linkDao.countLinks()
    }
}
