package com.rejowan.linky.util

import com.rejowan.linky.domain.repository.CollectionRepository
import com.rejowan.linky.domain.repository.LinkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Preloads frequently accessed data at app startup to improve perceived performance.
 * This warms up Room's query cache so subsequent screen loads are instant.
 */
class DataPreloader(
    private val collectionRepository: CollectionRepository,
    private val linkRepository: LinkRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Start preloading data in the background.
     * Call this during app initialization (e.g., in LinkyApp.onCreate or MainActivity.onCreate).
     */
    fun preload() {
        scope.launch {
            try {
                // Preload collections with link count (this warms up the cache)
                val collections = collectionRepository.getCollectionsWithLinkCount().first()
                Timber.d("Preloaded ${collections.size} collections")
            } catch (e: Exception) {
                Timber.e(e, "Failed to preload collections")
            }
        }

        scope.launch {
            try {
                // Preload all links (for home screen)
                val links = linkRepository.getAllLinks().first()
                Timber.d("Preloaded ${links.size} links")
            } catch (e: Exception) {
                Timber.e(e, "Failed to preload links")
            }
        }
    }
}
