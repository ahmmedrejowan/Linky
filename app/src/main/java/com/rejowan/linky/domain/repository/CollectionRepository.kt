package com.rejowan.linky.domain.repository

import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.model.CollectionWithLinkCount
import com.rejowan.linky.util.Result
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    // Get operations
    fun getAllCollections(): Flow<List<Collection>>
    fun getCollectionById(id: String): Flow<Collection?>
    suspend fun getCollectionByIdOnce(id: String): Collection?
    fun getCollectionsWithLinkCount(): Flow<List<CollectionWithLinkCount>>

    // Create/Update operations
    suspend fun saveCollection(collection: Collection): Result<Unit>
    suspend fun updateCollection(collection: Collection): Result<Unit>

    // Delete operations
    suspend fun deleteCollection(collectionId: String): Result<Unit>
    suspend fun deleteAllCollections(): Result<Unit>

    // Count operations
    suspend fun countCollections(): Int

    // Search operations
    fun searchCollections(query: String): Flow<List<CollectionWithLinkCount>>
}
