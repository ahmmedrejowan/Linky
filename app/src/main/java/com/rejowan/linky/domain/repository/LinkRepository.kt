package com.rejowan.linky.domain.repository

import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.util.Result
import kotlinx.coroutines.flow.Flow

interface LinkRepository {
    // Get operations
    fun getAllActiveLinks(): Flow<List<Link>>
    fun getAllLinks(): Flow<List<Link>>
    fun getFavoriteLinks(): Flow<List<Link>>
    fun getArchivedLinks(): Flow<List<Link>>
    fun getTrashedLinks(): Flow<List<Link>>
    fun getLinksByCollection(collectionId: String): Flow<List<Link>>
    fun searchLinks(query: String): Flow<List<Link>>
    fun getLinkById(id: String): Flow<Link?>
    suspend fun getLinkByIdOnce(id: String): Link?
    suspend fun existsByUrl(url: String): Boolean

    // Create/Update operations
    suspend fun saveLink(link: Link): Result<Unit>
    suspend fun updateLink(link: Link): Result<Unit>

    // Delete operations
    suspend fun deleteLink(linkId: String): Result<Unit>
    suspend fun softDeleteLink(linkId: String): Result<Unit>
    suspend fun restoreLink(linkId: String): Result<Unit>

    // Toggle operations
    suspend fun toggleFavorite(linkId: String, isFavorite: Boolean): Result<Unit>
    suspend fun toggleArchive(linkId: String, isArchived: Boolean): Result<Unit>

    // Count operations
    suspend fun countLinks(): Int
    fun getAllLinksCount(): Flow<Int>
    fun getFavoriteLinksCount(): Flow<Int>
    fun getArchivedLinksCount(): Flow<Int>

    // Advanced filtering
    suspend fun getAllActiveUrls(): List<String>
}
