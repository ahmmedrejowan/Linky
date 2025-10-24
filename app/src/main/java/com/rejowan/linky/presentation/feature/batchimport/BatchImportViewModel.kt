package com.rejowan.linky.presentation.feature.batchimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.usecase.link.CheckUrlExistsUseCase
import com.rejowan.linky.domain.usecase.link.SaveLinkUseCase
import com.rejowan.linky.util.LinkPreviewFetcher
import com.rejowan.linky.util.Result
import com.rejowan.linky.util.UrlExtractor
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

/**
 * ViewModel for Batch Import feature
 * Manages state and business logic for the multi-step import flow
 */
class BatchImportViewModel(
    private val saveLinkUseCase: SaveLinkUseCase,
    private val checkUrlExistsUseCase: CheckUrlExistsUseCase,
    private val linkPreviewFetcher: LinkPreviewFetcher
) : ViewModel() {

    // State
    private val _state = MutableStateFlow(BatchImportState())
    val state: StateFlow<BatchImportState> = _state.asStateFlow()

    // UI Events (one-time events)
    private val _uiEvent = MutableSharedFlow<BatchImportUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

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
                // TODO: Implement auto-fill with domain names
            }

            // Step 4: Import
            is BatchImportEvent.OnCollectionSelected -> {
                _state.update { it.copy(selectedCollectionId = event.collectionId) }
            }

            is BatchImportEvent.OnStartImport -> {
                // TODO: Implement import
            }

            is BatchImportEvent.OnRetryImport -> {
                // TODO: Implement retry
            }

            is BatchImportEvent.OnRetryFailed -> {
                // TODO: Implement retry failed
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
                    _uiEvent.emit(BatchImportUiEvent.NavigateToHome)
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
                                current = (chunkIndex + 1) * chunkSize.coerceAtMost(selectedUrls.size),
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
            // Set timeout of 10 seconds per URL
            val preview = withTimeout(10000) {
                linkPreviewFetcher.fetchPreview(url, timeoutMs = 10000)
            }

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
     * Reset state (for testing or navigation)
     */
    fun resetState() {
        _state.value = BatchImportState()
    }
}
