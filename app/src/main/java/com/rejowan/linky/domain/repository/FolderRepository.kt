package com.rejowan.linky.domain.repository

import com.rejowan.linky.domain.model.Folder
import com.rejowan.linky.util.Result
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    // Get operations
    fun getAllFolders(): Flow<List<Folder>>
    fun getFolderById(id: String): Flow<Folder?>
    suspend fun getFolderByIdOnce(id: String): Folder?

    // Create/Update operations
    suspend fun saveFolder(folder: Folder): Result<Unit>
    suspend fun updateFolder(folder: Folder): Result<Unit>

    // Delete operations
    suspend fun deleteFolder(folderId: String): Result<Unit>

    // Count operations
    suspend fun countFolders(): Int
}
