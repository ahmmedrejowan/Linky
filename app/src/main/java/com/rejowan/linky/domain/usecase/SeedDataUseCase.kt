package com.rejowan.linky.domain.usecase

import com.rejowan.linky.domain.repository.CollectionRepository
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.util.Result
import com.rejowan.linky.util.SeedData
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Use case to seed the database with sample data
 * For development and testing purposes
 *
 * USAGE: Drop this in any composable to seed data on first launch:
 *
 * ```
 * val seedDataUseCase: SeedDataUseCase = koinInject()
 * LaunchedEffect(Unit) {
 *     seedDataUseCase()
 * }
 * ```
 *
 * Or call from ViewModel:
 * ```
 * viewModelScope.launch {
 *     seedDataUseCase()
 * }
 * ```
 */
class SeedDataUseCase(
    private val collectionRepository: CollectionRepository,
    private val linkRepository: LinkRepository
) {
    suspend operator fun invoke(): Result<SeedResult> {
        return try {
            // Check if data already exists - skip if not empty
            val existingCollections = collectionRepository.getAllCollections().first()
            if (existingCollections.isNotEmpty()) {
                Timber.d("Database already has data, skipping seed")
                return Result.Success(SeedResult(0, 0, skipped = true))
            }

            var collectionsAdded = 0
            var linksAdded = 0

            // Seed collections first
            val collections = SeedData.generateCollections()
            for (collection in collections) {
                when (collectionRepository.saveCollection(collection)) {
                    is Result.Success -> collectionsAdded++
                    is Result.Error -> Timber.w("Failed to seed collection: ${collection.name}")
                    is Result.Loading -> {}
                }
            }
            Timber.d("Seeded $collectionsAdded collections")

            // Seed links
            val links = SeedData.generateLinks()
            for (link in links) {
                when (linkRepository.saveLink(link)) {
                    is Result.Success -> linksAdded++
                    is Result.Error -> Timber.w("Failed to seed link: ${link.title}")
                    is Result.Loading -> {}
                }
            }
            Timber.d("Seeded $linksAdded links")

            Result.Success(SeedResult(collectionsAdded, linksAdded))
        } catch (e: Exception) {
            Timber.e(e, "Failed to seed data")
            Result.Error(e)
        }
    }
}

data class SeedResult(
    val collectionsAdded: Int,
    val linksAdded: Int,
    val skipped: Boolean = false
)
