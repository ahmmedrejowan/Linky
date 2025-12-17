package com.rejowan.linky.presentation.feature.batchimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.usecase.collection.GetAllCollectionsUseCase
import com.rejowan.linky.domain.usecase.collection.SaveCollectionUseCase
import com.rejowan.linky.domain.usecase.link.BatchSaveLinksUseCase
import com.rejowan.linky.domain.usecase.link.CheckUrlExistsUseCase
import com.rejowan.linky.domain.usecase.link.SaveLinkUseCase
import com.rejowan.linky.util.LinkPreviewFetcher
import com.rejowan.linky.util.Result
import com.rejowan.linky.util.UrlExtractor
import timber.log.Timber
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

/**
 * ViewModel for Batch Import feature
 * Manages state and business logic for the multi-step import flow
 */
class BatchImportViewModel(
    private val saveLinkUseCase: SaveLinkUseCase,
    private val checkUrlExistsUseCase: CheckUrlExistsUseCase,
    private val linkPreviewFetcher: LinkPreviewFetcher,
    private val batchSaveLinksUseCase: BatchSaveLinksUseCase,
    private val getAllCollectionsUseCase: GetAllCollectionsUseCase,
    private val saveCollectionUseCase: SaveCollectionUseCase
) : ViewModel() {

    // State
    private val _state = MutableStateFlow(BatchImportState())
    val state: StateFlow<BatchImportState> = _state.asStateFlow()

    // UI Events (one-time events)
    private val _uiEvent = MutableSharedFlow<BatchImportUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        // Load collections on ViewModel creation
        loadCollections()
    }

    /**
     * Handle events from UI
     */
    fun onEvent(event: BatchImportEvent) {
        when (event) {
            // Step 1: Paste
            is BatchImportEvent.OnTextChanged -> {
                _state.update { it.copy(pastedText = event.text) }
            }

            is BatchImportEvent.OnStartScan -> {
                startScan()
            }

            // Step 2: Summary & Selection
            is BatchImportEvent.OnProceedToSelection -> {
                _state.update { it.copy(showSelectionScreen = true) }
            }

            is BatchImportEvent.OnBackToEdit -> {
                _state.update {
                    it.copy(
                        showSelectionScreen = false,
                        error = null,
                        extractedUrls = emptyList(),
                        urlStatuses = emptyList(),
                        totalUrls = 0,
                        duplicateCount = 0,
                        selectedCount = 0
                    )
                }
            }

            // Selection actions
            is BatchImportEvent.OnToggleUrlSelection -> {
                toggleUrlSelection(event.url)
            }

            is BatchImportEvent.OnSelectAll -> {
                selectAll()
            }

            is BatchImportEvent.OnDeselectAll -> {
                deselectAll()
            }

            is BatchImportEvent.OnSelectNewOnly -> {
                selectNewOnly()
            }

            // Removal actions (destructive)
            is BatchImportEvent.OnRemoveDuplicates -> {
                removeDuplicates()
            }

            is BatchImportEvent.OnRemoveUnselected -> {
                removeUnselected()
            }

            is BatchImportEvent.OnRemoveUrl -> {
                removeUrl(event.url)
            }

            // Step 5: Preview Fetch
            is BatchImportEvent.OnStartFetching -> {
                startPreviewFetching()
            }

            is BatchImportEvent.OnAutoFillTitles -> {
                autoFillTitlesWithDomains()
            }

            // Step 4: Import
            is BatchImportEvent.OnCollectionSelected -> {
                _state.update { it.copy(selectedCollectionId = event.collectionId) }
            }

            is BatchImportEvent.OnStartImport -> {
                startImport()
            }

            is BatchImportEvent.OnRetryImport -> {
                retryImport()
            }

            is BatchImportEvent.OnRetryFailed -> {
                retryFailedOnly()
            }

            // Create collection dialog events
            is BatchImportEvent.OnCreateCollectionClick -> {
                Timber.d("Create collection dialog opened")
                _state.update { it.copy(showCreateCollectionDialog = true) }
            }

            is BatchImportEvent.OnNewCollectionNameChange -> {
                _state.update { it.copy(newCollectionName = event.name) }
            }

            is BatchImportEvent.OnNewCollectionColorChange -> {
                _state.update { it.copy(newCollectionColor = event.color) }
            }

            is BatchImportEvent.OnNewCollectionToggleFavorite -> {
                val newValue = !_state.value.newCollectionIsFavorite
                _state.update { it.copy(newCollectionIsFavorite = newValue) }
            }

            is BatchImportEvent.OnCreateCollectionConfirm -> {
                Timber.d("Create collection confirm")
                createCollection()
            }

            is BatchImportEvent.OnCreateCollectionDismiss -> {
                Timber.d("Create collection dialog dismissed")
                _state.update {
                    it.copy(
                        showCreateCollectionDialog = false,
                        newCollectionName = "",
                        newCollectionColor = null,
                        newCollectionIsFavorite = false
                    )
                }
            }

            // Navigation
            is BatchImportEvent.OnBack -> {
                viewModelScope.launch {
                    _uiEvent.emit(BatchImportUiEvent.NavigateToSettings)
                }
            }

            is BatchImportEvent.OnCancel -> {
                viewModelScope.launch {
                    _uiEvent.emit(BatchImportUiEvent.NavigateToSettings)
                }
            }

            is BatchImportEvent.OnDone -> {
                viewModelScope.launch {
                    // Navigate to collection detail if a collection was selected, otherwise go to home
                    val selectedCollectionId = _state.value.selectedCollectionId
                    if (selectedCollectionId != null) {
                        _uiEvent.emit(BatchImportUiEvent.NavigateToCollection(selectedCollectionId))
                    } else {
                        _uiEvent.emit(BatchImportUiEvent.NavigateToHome)
                    }
                    // Reset state after navigation
                    resetState()
                }
            }
        }
    }

    /**
     * Start scanning for URLs in pasted text
     */
    private fun startScan() {
        viewModelScope.launch {
            _state.update { it.copy(isScanning = true, error = null) }

            try {
                // Extract URLs from text
                val urls = UrlExtractor.extractUrls(_state.value.pastedText)

                // Check if no URLs found
                if (urls.isEmpty()) {
                    _state.update {
                        it.copy(
                            isScanning = false,
                            error = BatchImportError.NoUrlsFound(
                                "No valid URLs found in the pasted text. Please check and try again."
                            )
                        )
                    }
                    return@launch
                }

                // Check for duplicates in database
                val urlStatuses = urls.map { url ->
                    val isDuplicate = checkUrlExistsUseCase(url)
                    UrlStatus(
                        url = url,
                        domain = UrlExtractor.extractDomain(url),
                        isDuplicate = isDuplicate,
                        isSelected = !isDuplicate // Auto-deselect duplicates
                    )
                }

                val duplicateCount = urlStatuses.count { it.isDuplicate }
                val selectedCount = urlStatuses.count { it.isSelected }

                _state.update {
                    it.copy(
                        isScanning = false,
                        extractedUrls = urls,
                        urlStatuses = urlStatuses,
                        totalUrls = urls.size,
                        duplicateCount = duplicateCount,
                        selectedCount = selectedCount
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isScanning = false,
                        error = BatchImportError.NoUrlsFound(
                            e.message ?: "Failed to scan for URLs"
                        )
                    )
                }
            }
        }
    }

    /**
     * Toggle selection of a URL
     */
    private fun toggleUrlSelection(url: String) {
        _state.update { state ->
            val updatedStatuses = state.urlStatuses.map { urlStatus ->
                if (urlStatus.url == url) {
                    urlStatus.copy(isSelected = !urlStatus.isSelected)
                } else {
                    urlStatus
                }
            }
            val selectedCount = updatedStatuses.count { it.isSelected }

            state.copy(
                urlStatuses = updatedStatuses,
                selectedCount = selectedCount
            )
        }
    }

    /**
     * Remove duplicates from selection
     */
    private fun removeDuplicates() {
        _state.update { state ->
            val updatedStatuses = state.urlStatuses.filterNot { it.isDuplicate }
            val selectedCount = updatedStatuses.count { it.isSelected }

            state.copy(
                urlStatuses = updatedStatuses,
                totalUrls = updatedStatuses.size,
                selectedCount = selectedCount
            )
        }
    }

    /**
     * Remove a specific URL
     */
    private fun removeUrl(url: String) {
        _state.update { state ->
            val updatedStatuses = state.urlStatuses.filterNot { it.url == url }
            val selectedCount = updatedStatuses.count { it.isSelected }
            val duplicateCount = updatedStatuses.count { it.isDuplicate }

            state.copy(
                urlStatuses = updatedStatuses,
                totalUrls = updatedStatuses.size,
                duplicateCount = duplicateCount,
                selectedCount = selectedCount
            )
        }
    }

    /**
     * Select all URLs
     */
    private fun selectAll() {
        _state.update { state ->
            val updatedStatuses = state.urlStatuses.map { it.copy(isSelected = true) }
            state.copy(
                urlStatuses = updatedStatuses,
                selectedCount = updatedStatuses.size
            )
        }
    }

    /**
     * Deselect all URLs
     */
    private fun deselectAll() {
        _state.update { state ->
            val updatedStatuses = state.urlStatuses.map { it.copy(isSelected = false) }
            state.copy(
                urlStatuses = updatedStatuses,
                selectedCount = 0
            )
        }
    }

    /**
     * Select only new (non-duplicate) URLs
     */
    private fun selectNewOnly() {
        _state.update { state ->
            val updatedStatuses = state.urlStatuses.map { urlStatus ->
                urlStatus.copy(isSelected = !urlStatus.isDuplicate)
            }
            val selectedCount = updatedStatuses.count { it.isSelected }

            state.copy(
                urlStatuses = updatedStatuses,
                selectedCount = selectedCount
            )
        }
    }

    /**
     * Remove unselected URLs from the list
     */
    private fun removeUnselected() {
        _state.update { state ->
            val updatedStatuses = state.urlStatuses.filter { it.isSelected }
            val duplicateCount = updatedStatuses.count { it.isDuplicate }

            state.copy(
                urlStatuses = updatedStatuses,
                totalUrls = updatedStatuses.size,
                duplicateCount = duplicateCount,
                selectedCount = updatedStatuses.size
            )
        }
    }

    /**
     * Start preview fetching for selected URLs
     * Fetches in parallel batches of 10 URLs at a time
     */
    private fun startPreviewFetching() {
        viewModelScope.launch {
            try {
                // Get selected URLs
                val selectedUrls = _state.value.urlStatuses
                    .filter { it.isSelected }
                    .map { it.url }

                if (selectedUrls.isEmpty()) {
                    _state.update {
                        it.copy(
                            error = BatchImportError.NoUrlsFound("No URLs selected for import")
                        )
                    }
                    return@launch
                }

                // Navigate to preview screen and start fetching
                _state.update {
                    it.copy(
                        showPreviewScreen = true,
                        isFetching = true,
                        previewResults = emptyList()
                    )
                }

                // Process URLs in chunks of 10
                val chunkSize = 10
                val chunks = selectedUrls.chunked(chunkSize)
                val allResults = mutableListOf<LinkPreviewResult>()

                chunks.forEachIndexed { chunkIndex, chunk ->
                    // Update progress
                    _state.update {
                        it.copy(
                            fetchProgress = FetchProgress(
                                current = chunkIndex * chunkSize,
                                total = selectedUrls.size,
                                currentChunk = chunkIndex + 1,
                                totalChunks = chunks.size
                            )
                        )
                    }

                    // Fetch all URLs in this chunk in parallel
                    val chunkResults = chunk.map { url ->
                        async {
                            fetchSinglePreview(url)
                        }
                    }.awaitAll()

                    allResults.addAll(chunkResults)

                    // Update state with accumulated results
                    _state.update {
                        it.copy(
                            previewResults = allResults.toList(),
                            fetchProgress = FetchProgress(
                                current = ((chunkIndex + 1) * chunkSize).coerceAtMost(selectedUrls.size),
                                total = selectedUrls.size,
                                currentChunk = chunkIndex + 1,
                                totalChunks = chunks.size
                            )
                        )
                    }
                }

                // Fetching complete
                _state.update {
                    it.copy(
                        isFetching = false,
                        fetchProgress = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isFetching = false,
                        fetchProgress = null,
                        error = BatchImportError.FetchFailed(
                            e.message ?: "Failed to fetch previews"
                        )
                    )
                }
            }
        }
    }

    /**
     * Fetch preview for a single URL with timeout handling
     */
    private suspend fun fetchSinglePreview(url: String): LinkPreviewResult {
        return try {
            // Fetch preview with 10 second timeout (handled by LinkPreviewFetcher)
            val preview = linkPreviewFetcher.fetchPreview(url, timeoutMs = 10000)

            if (preview != null) {
                LinkPreviewResult.Success(
                    url = url,
                    domain = UrlExtractor.extractDomain(url),
                    title = preview.title,
                    description = preview.description,
                    imageUrl = preview.imageUrl
                )
            } else {
                // Failed to fetch - use domain as fallback
                LinkPreviewResult.Error(
                    url = url,
                    domain = UrlExtractor.extractDomain(url),
                    error = "Failed to fetch preview"
                )
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            // Timeout - use domain as fallback
            LinkPreviewResult.Timeout(
                url = url,
                domain = UrlExtractor.extractDomain(url)
            )
        } catch (e: Exception) {
            // Error - use domain as fallback
            LinkPreviewResult.Error(
                url = url,
                domain = UrlExtractor.extractDomain(url),
                error = e.message
            )
        }
    }

    /**
     * Start importing links to database
     */
    private fun startImport() {
        viewModelScope.launch {
            try {
                // Get preview results
                val previewResults = _state.value.previewResults

                if (previewResults.isEmpty()) {
                    _state.update {
                        it.copy(
                            error = BatchImportError.ImportFailed("No links to import")
                        )
                    }
                    return@launch
                }

                // Update state to importing
                _state.update {
                    it.copy(
                        isImporting = true,
                        importProgress = ImportProgress(
                            current = 0,
                            total = previewResults.size
                        ),
                        error = null
                    )
                }

                // Wrap import operation with timeout (5 minutes max)
                withTimeout(300000) {
                    // Convert LinkPreviewResult to Link objects
                    val links = previewResults.map { result ->
                        createLinkFromPreviewResult(result, _state.value.selectedCollectionId)
                    }

                    // Save links using batch save use case
                    val saveResult = batchSaveLinksUseCase(links)

                    // Convert BatchSaveResult to BatchImportResult
                    val importResult = BatchImportResult(
                        successful = saveResult.successful,
                        failed = saveResult.failed.map { failedLink ->
                            FailedImport(
                                url = failedLink.link.url,
                                error = failedLink.error
                            )
                        }
                    )

                    // Update state with result
                    _state.update {
                        it.copy(
                            isImporting = false,
                            importProgress = null,
                            importResult = importResult
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _state.update {
                    it.copy(
                        isImporting = false,
                        importProgress = null,
                        error = BatchImportError.ImportFailed(
                            "Import operation timed out. Please try again with fewer links."
                        )
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isImporting = false,
                        importProgress = null,
                        error = BatchImportError.ImportFailed(
                            e.message ?: "Failed to import links"
                        )
                    )
                }
            }
        }
    }

    /**
     * Retry importing all links (complete retry)
     */
    private fun retryImport() {
        // Simply call startImport again
        startImport()
    }

    /**
     * Retry importing only the failed links from last attempt
     */
    private fun retryFailedOnly() {
        viewModelScope.launch {
            try {
                val previousResult = _state.value.importResult ?: return@launch

                if (previousResult.failed.isEmpty()) {
                    return@launch
                }

                // Update state to importing
                _state.update {
                    it.copy(
                        isImporting = true,
                        importProgress = ImportProgress(
                            current = 0,
                            total = previousResult.failed.size
                        ),
                        error = null
                    )
                }

                // Find the preview results for failed URLs
                val failedUrls = previousResult.failed.map { it.url }.toSet()
                val failedPreviewResults = _state.value.previewResults.filter {
                    failedUrls.contains(it.url)
                }

                // Convert to Link objects
                val links = failedPreviewResults.map { result ->
                    createLinkFromPreviewResult(result, _state.value.selectedCollectionId)
                }

                // Save links
                val saveResult = batchSaveLinksUseCase(links)

                // Merge with previous successful imports
                val mergedSuccessful = previousResult.successful + saveResult.successful
                val mergedFailed = saveResult.failed.map { failedLink ->
                    FailedImport(
                        url = failedLink.link.url,
                        error = failedLink.error
                    )
                }

                val updatedResult = BatchImportResult(
                    successful = mergedSuccessful,
                    failed = mergedFailed
                )

                // Update state with merged result
                _state.update {
                    it.copy(
                        isImporting = false,
                        importProgress = null,
                        importResult = updatedResult
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isImporting = false,
                        importProgress = null,
                        error = BatchImportError.ImportFailed(
                            e.message ?: "Failed to retry import"
                        )
                    )
                }
            }
        }
    }

    /**
     * Auto-fill titles with domain names for links that failed to fetch or have no title
     */
    private fun autoFillTitlesWithDomains() {
        _state.update { state ->
            val updatedResults = state.previewResults.map { result ->
                when (result) {
                    is LinkPreviewResult.Success -> {
                        // If title is blank, use domain
                        if (result.title.isBlank()) {
                            result.copy(title = formatDomainAsTitle(result.domain))
                        } else {
                            result
                        }
                    }
                    is LinkPreviewResult.Error -> {
                        // Convert error to success with domain as title
                        LinkPreviewResult.Success(
                            url = result.url,
                            domain = result.domain,
                            title = formatDomainAsTitle(result.domain),
                            description = null,
                            imageUrl = null
                        )
                    }
                    is LinkPreviewResult.Timeout -> {
                        // Convert timeout to success with domain as title
                        LinkPreviewResult.Success(
                            url = result.url,
                            domain = result.domain,
                            title = formatDomainAsTitle(result.domain),
                            description = null,
                            imageUrl = null
                        )
                    }
                }
            }
            state.copy(previewResults = updatedResults)
        }
    }

    /**
     * Format a domain name as a readable title
     * e.g., "example.com" -> "Example", "github.com" -> "Github"
     */
    private fun formatDomainAsTitle(domain: String): String {
        return domain
            .removePrefix("www.")
            .substringBefore(".")
            .replaceFirstChar { it.uppercase() }
    }

    /**
     * Create a Link object from LinkPreviewResult
     */
    private fun createLinkFromPreviewResult(
        result: LinkPreviewResult,
        collectionId: String?
    ): Link {
        val currentTime = System.currentTimeMillis()

        return when (result) {
            is LinkPreviewResult.Success -> Link(
                id = UUID.randomUUID().toString(),
                url = result.url,
                title = result.title,
                description = result.description,
                previewUrl = result.imageUrl,
                collectionId = collectionId,
                isFavorite = false,
                isArchived = false,
                createdAt = currentTime,
                updatedAt = currentTime
            )

            is LinkPreviewResult.Error -> Link(
                id = UUID.randomUUID().toString(),
                url = result.url,
                title = result.domain, // Fallback to domain
                description = null,
                previewUrl = null,
                collectionId = collectionId,
                isFavorite = false,
                isArchived = false,
                createdAt = currentTime,
                updatedAt = currentTime
            )

            is LinkPreviewResult.Timeout -> Link(
                id = UUID.randomUUID().toString(),
                url = result.url,
                title = result.domain, // Fallback to domain
                description = null,
                previewUrl = null,
                collectionId = collectionId,
                isFavorite = false,
                isArchived = false,
                createdAt = currentTime,
                updatedAt = currentTime
            )
        }
    }

    /**
     * Create a new collection
     */
    private fun createCollection() {
        viewModelScope.launch {
            val collectionName = _state.value.newCollectionName.trim()

            if (collectionName.isBlank()) {
                Timber.w("Collection name is blank")
                return@launch
            }

            try {
                val newCollection = Collection(
                    name = collectionName,
                    color = _state.value.newCollectionColor,
                    isFavorite = _state.value.newCollectionIsFavorite
                )

                when (val result = saveCollectionUseCase(newCollection)) {
                    is Result.Success -> {
                        Timber.d("Collection created successfully: ${newCollection.name}")
                        // Close dialog and select the new collection
                        _state.update {
                            it.copy(
                                showCreateCollectionDialog = false,
                                newCollectionName = "",
                                newCollectionColor = null,
                                newCollectionIsFavorite = false,
                                selectedCollectionId = newCollection.id
                            )
                        }
                    }
                    is Result.Error -> {
                        Timber.e(result.exception, "Failed to create collection")
                        _state.update {
                            it.copy(
                                showCreateCollectionDialog = false,
                                newCollectionName = "",
                                newCollectionColor = null,
                                newCollectionIsFavorite = false
                            )
                        }
                    }
                    is Result.Loading -> {
                        // No-op
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception creating collection")
                _state.update {
                    it.copy(
                        showCreateCollectionDialog = false,
                        newCollectionName = "",
                        newCollectionColor = null,
                        newCollectionIsFavorite = false
                    )
                }
            }
        }
    }

    /**
     * Load all collections for the collection picker
     */
    private fun loadCollections() {
        viewModelScope.launch {
            getAllCollectionsUseCase().collect { collections ->
                _state.update { it.copy(collections = collections) }
            }
        }
    }

    /**
     * Reset state (for testing or navigation)
     */
    fun resetState() {
        _state.value = BatchImportState()
    }
}
