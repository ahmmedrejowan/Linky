package com.rejowan.linky.presentation.feature.batchimport

import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.model.Link

/**
 * State for Batch Import feature
 * Manages all state across the multi-step import flow
 */
data class BatchImportState(
    // Step 1: Paste
    val pastedText: String = "",
    val isScanning: Boolean = false,
    val scanProgress: String = "",

    // Step 2: Extracted URLs
    val extractedUrls: List<String> = emptyList(),
    val urlStatuses: List<UrlStatus> = emptyList(),

    // Statistics
    val totalUrls: Int = 0,
    val duplicateCount: Int = 0,
    val selectedCount: Int = 0,

    // Navigation between steps
    val showSelectionScreen: Boolean = false,
    val showPreviewScreen: Boolean = false,

    // Step 5: Preview Fetching
    val isFetching: Boolean = false,
    val fetchProgress: FetchProgress? = null,
    val currentFetchingUrl: String? = null,
    val previewResults: List<LinkPreviewResult> = emptyList(),

    // Step 4: Import
    val isImporting: Boolean = false,
    val importProgress: ImportProgress? = null,
    val importResult: BatchImportResult? = null,

    // Collection
    val collections: List<Collection> = emptyList(),
    val selectedCollectionId: String? = null,

    // Create collection dialog state
    val showCreateCollectionDialog: Boolean = false,
    val newCollectionName: String = "",
    val newCollectionColor: String? = null,

    // Error states
    val error: BatchImportError? = null
)

/**
 * Status of a URL during processing
 */
data class UrlStatus(
    val url: String,
    val domain: String,
    val isDuplicate: Boolean,
    val isSelected: Boolean
)

/**
 * Progress during preview fetching
 */
data class FetchProgress(
    val current: Int,
    val total: Int,
    val successCount: Int = 0,
    val errorCount: Int = 0,
    val timeoutCount: Int = 0
)

/**
 * Progress during import
 */
data class ImportProgress(
    val current: Int,
    val total: Int
)

/**
 * Result of link preview fetch
 */
sealed class LinkPreviewResult {
    abstract val url: String
    abstract val domain: String

    data class Success(
        override val url: String,
        override val domain: String,
        val title: String,
        val description: String?,
        val imageUrl: String?
    ) : LinkPreviewResult()

    data class Error(
        override val url: String,
        override val domain: String,
        val error: String?
    ) : LinkPreviewResult()

    data class Timeout(
        override val url: String,
        override val domain: String
    ) : LinkPreviewResult()
}

/**
 * Result of batch import operation
 */
data class BatchImportResult(
    val successful: List<Link>,
    val failed: List<FailedImport>
) {
    val totalSuccess: Int get() = successful.size
    val totalFailed: Int get() = failed.size
    val hasFailures: Boolean get() = failed.isNotEmpty()
    val isCompleteSuccess: Boolean get() = failed.isEmpty()
    val isCompleteFailure: Boolean get() = successful.isEmpty()
}

/**
 * Failed import entry
 */
data class FailedImport(
    val url: String,
    val error: String?
)

/**
 * Error states for batch import
 */
sealed class BatchImportError {
    data class NoUrlsFound(val message: String) : BatchImportError()
    data class AllDuplicates(val message: String) : BatchImportError()
    data class FetchFailed(val message: String) : BatchImportError()
    data class ImportFailed(val message: String) : BatchImportError()
}
