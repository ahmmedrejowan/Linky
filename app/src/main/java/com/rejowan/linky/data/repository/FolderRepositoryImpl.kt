package com.rejowan.linky.data.repository

import com.rejowan.linky.data.local.database.dao.FolderDao
import com.rejowan.linky.data.mapper.FolderMapper.toDomain
import com.rejowan.linky.data.mapper.FolderMapper.toDomainList
import com.rejowan.linky.data.mapper.FolderMapper.toEntity
import com.rejowan.linky.domain.model.Folder
import com.rejowan.linky.domain.repository.FolderRepository
import com.rejowan.linky.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class FolderRepositoryImpl(
    private val folderDao: FolderDao
) : FolderRepository {

    override fun getAllFolders(): Flow<List<Folder>> {
        return folderDao.getAllFolders().map { it.toDomainList() }
    }

    override fun getFolderById(id: String): Flow<Folder?> {
        return folderDao.getByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun getFolderByIdOnce(id: String): Folder? {
        return folderDao.getById(id)?.toDomain()
    }

    override suspend fun saveFolder(folder: Folder): Result<Unit> {
        return try {
            val entity = folder.toEntity(syncToRemote = false)
            folderDao.insert(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save folder")
            Result.Error(e)
        }
    }

    override suspend fun updateFolder(folder: Folder): Result<Unit> {
        return try {
            val entity = folder.copy(updatedAt = System.currentTimeMillis()).toEntity(syncToRemote = false)
            folderDao.update(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update folder")
            Result.Error(e)
        }
    }

    override suspend fun deleteFolder(folderId: String): Result<Unit> {
        return try {
            val folder = folderDao.getById(folderId)
            if (folder != null) {
                folderDao.delete(folder)
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Folder not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete folder")
            Result.Error(e)
        }
    }

    override suspend fun countFolders(): Int {
        return folderDao.countFolders()
    }
}
