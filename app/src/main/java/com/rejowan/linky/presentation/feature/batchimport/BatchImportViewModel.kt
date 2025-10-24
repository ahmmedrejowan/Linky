package com.rejowan.linky.presentation.feature.batchimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.usecase.link.CheckUrlExistsUseCase
import com.rejowan.linky.domain.usecase.link.SaveLinkUseCase
import com.rejowan.linky.util.Result
import com.rejowan.linky.util.UrlExtractor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Batch Import feature
 * Manages state and business logic for the multi-step import flow
 */
class BatchImportViewModel(
    private val saveLinkUseCase: SaveLinkUseCase,
    private val checkUrlExistsUseCase: CheckUrlExistsUseCase
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

            // Step 3: Preview Fetch
            is BatchImportEvent.OnStartFetching -> {
                // TODO: Implement preview fetching
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
     * Reset state (for testing or navigation)
     */
    fun resetState() {
        _state.value = BatchImportState()
    }
}
