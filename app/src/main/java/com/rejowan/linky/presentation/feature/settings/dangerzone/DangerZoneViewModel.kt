package com.rejowan.linky.presentation.feature.settings.dangerzone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.repository.CollectionRepository
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.domain.repository.SnapshotRepository
import com.rejowan.linky.util.FileStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class DangerZoneViewModel(
    private val linkRepository: LinkRepository,
    private val collectionRepository: CollectionRepository,
    private val snapshotRepository: SnapshotRepository,
    private val fileStorageManager: FileStorageManager
) : ViewModel() {

    private val _state = MutableStateFlow(DangerZoneState())
    val state: StateFlow<DangerZoneState> = _state.asStateFlow()

    fun clearCache() {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, operationType = OperationType.CLEAR_CACHE) }
            try {
                val success = fileStorageManager.clearPreviewCache()
                _state.update {
                    it.copy(
                        isProcessing = false,
                        operationType = null,
                        resultMessage = if (success) "Cache cleared successfully" else "Some files could not be deleted",
                        isSuccess = success
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear cache")
                _state.update {
                    it.copy(
                        isProcessing = false,
                        operationType = null,
                        resultMessage = "Failed to clear cache: ${e.message}",
                        isSuccess = false
                    )
                }
            }
        }
    }

    fun deleteAllSnapshots() {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, operationType = OperationType.DELETE_SNAPSHOTS) }
            try {
                snapshotRepository.deleteAllSnapshots()
                _state.update {
                    it.copy(
                        isProcessing = false,
                        operationType = null,
                        resultMessage = "All snapshots deleted successfully",
                        isSuccess = true
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete snapshots")
                _state.update {
                    it.copy(
                        isProcessing = false,
                        operationType = null,
                        resultMessage = "Failed to delete snapshots: ${e.message}",
                        isSuccess = false
                    )
                }
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, operationType = OperationType.DELETE_ALL) }
            try {
                // Delete in order: snapshots -> links -> collections
                snapshotRepository.deleteAllSnapshots()
                linkRepository.deleteAllLinks()
                collectionRepository.deleteAllCollections()

                // Clear all cached files
                fileStorageManager.clearPreviewCache()

                _state.update {
                    it.copy(
                        isProcessing = false,
                        operationType = null,
                        resultMessage = "All data deleted successfully",
                        isSuccess = true
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete all data")
                _state.update {
                    it.copy(
                        isProcessing = false,
                        operationType = null,
                        resultMessage = "Failed to delete all data: ${e.message}",
                        isSuccess = false
                    )
                }
            }
        }
    }

    fun clearResultMessage() {
        _state.update { it.copy(resultMessage = null) }
    }
}

data class DangerZoneState(
    val isProcessing: Boolean = false,
    val operationType: OperationType? = null,
    val resultMessage: String? = null,
    val isSuccess: Boolean = false
)

enum class OperationType {
    CLEAR_CACHE,
    DELETE_SNAPSHOTS,
    DELETE_ALL
}
