package com.rejowan.linky.domain.usecase.link

import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.util.Result

/**
 * Use case to save multiple links in batch
 * Used during batch import operation
 */
class BatchSaveLinksUseCase(
    private val linkRepository: LinkRepository
) {
    /**
     * Save multiple links
     * @param links List of links to save
     * @return Result with list of successfully saved links and failures
     */
    suspend operator fun invoke(links: List<Link>): BatchSaveResult {
        val successful = mutableListOf<Link>()
        val failed = mutableListOf<FailedLink>()

        links.forEach { link ->
            when (val result = linkRepository.saveLink(link)) {
                is Result.Success -> {
                    successful.add(link)
                }
                is Result.Error -> {
                    failed.add(
                        FailedLink(
                            link = link,
                            error = result.exception.message ?: "Unknown error"
                        )
                    )
                }
                is Result.Loading -> {
                    // Should not happen during save
                }
            }
        }

        return BatchSaveResult(
            successful = successful,
            failed = failed
        )
    }
}

/**
 * Result of batch save operation
 */
data class BatchSaveResult(
    val successful: List<Link>,
    val failed: List<FailedLink>
) {
    val totalSuccess: Int get() = successful.size
    val totalFailed: Int get() = failed.size
    val hasFailures: Boolean get() = failed.isNotEmpty()
    val isCompleteSuccess: Boolean get() = failed.isEmpty()
}

/**
 * Failed link save entry
 */
data class FailedLink(
    val link: Link,
    val error: String
)
