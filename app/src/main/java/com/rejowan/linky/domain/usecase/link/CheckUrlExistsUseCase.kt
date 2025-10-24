package com.rejowan.linky.domain.usecase.link

import com.rejowan.linky.domain.repository.LinkRepository

/**
 * Use case to check if a URL already exists in the database
 * Used for duplicate detection during batch import
 */
class CheckUrlExistsUseCase(
    private val linkRepository: LinkRepository
) {
    /**
     * Check if a URL exists
     * @param url The URL to check
     * @return true if URL exists, false otherwise
     */
    suspend operator fun invoke(url: String): Boolean {
        return linkRepository.existsByUrl(url)
    }
}
