package com.rejowan.linky.presentation.feature.snapshotviewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.usecase.snapshot.DeleteSnapshotUseCase
import com.rejowan.linky.domain.usecase.snapshot.GetSnapshotByIdUseCase
import com.rejowan.linky.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class SnapshotViewerViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getSnapshotByIdUseCase: GetSnapshotByIdUseCase,
    private val deleteSnapshotUseCase: DeleteSnapshotUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SnapshotViewerState())
    val state: StateFlow<SnapshotViewerState> = _state.asStateFlow()

    private val snapshotId: String? = savedStateHandle["snapshotId"]

    init {
        snapshotId?.let { loadSnapshot(it) }
            ?: run {
                _state.update { it.copy(error = "Snapshot ID not found") }
            }
    }

    fun onEvent(event: SnapshotViewerEvent) {
        when (event) {
            is SnapshotViewerEvent.OnIncreaseFontSize -> increaseFontSize()
            is SnapshotViewerEvent.OnDecreaseFontSize -> decreaseFontSize()
            is SnapshotViewerEvent.OnDeleteSnapshot -> deleteSnapshot()
        }
    }

    private fun loadSnapshot(snapshotId: String) {
        Timber.d("loadSnapshot: Loading snapshot | ID: $snapshotId")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            getSnapshotByIdUseCase(snapshotId)
                .catch { e ->
                    Timber.e(e, "loadSnapshot: Failed to load snapshot")
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load snapshot") }
                }
                .collect { snapshot ->
                    if (snapshot == null) {
                        Timber.w("loadSnapshot: Snapshot not found | ID: $snapshotId")
                        _state.update { it.copy(isLoading = false, error = "Snapshot not found") }
                        return@collect
                    }

                    Timber.d("loadSnapshot: Snapshot loaded | Title: ${snapshot.title}")

                    // Load content from file
                    try {
                        val content = loadContentFromFile(snapshot.filePath)
                        _state.update {
                            it.copy(
                                snapshot = snapshot,
                                content = content,
                                isLoading = false
                            )
                        }
                        Timber.d("loadSnapshot: Content loaded | Length: ${content.length} characters")
                    } catch (e: Exception) {
                        Timber.e(e, "loadSnapshot: Failed to load content from file")
                        _state.update { it.copy(isLoading = false, error = "Failed to load content: ${e.message}") }
                    }
                }
        }
    }

    private suspend fun loadContentFromFile(filePath: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                throw Exception("Snapshot file not found")
            }
            file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.e(e, "loadContentFromFile: Failed to read file: $filePath")
            throw e
        }
    }

    private fun increaseFontSize() {
        val currentSize = _state.value.fontSize
        val newSize = when (currentSize) {
            FontSize.SMALL -> FontSize.MEDIUM
            FontSize.MEDIUM -> FontSize.LARGE
            FontSize.LARGE -> FontSize.LARGE // Already at max
        }
        Timber.d("increaseFontSize: $currentSize -> $newSize")
        _state.update { it.copy(fontSize = newSize) }
    }

    private fun decreaseFontSize() {
        val currentSize = _state.value.fontSize
        val newSize = when (currentSize) {
            FontSize.SMALL -> FontSize.SMALL // Already at min
            FontSize.MEDIUM -> FontSize.SMALL
            FontSize.LARGE -> FontSize.MEDIUM
        }
        Timber.d("decreaseFontSize: $currentSize -> $newSize")
        _state.update { it.copy(fontSize = newSize) }
    }

    private fun deleteSnapshot() {
        val snapshot = _state.value.snapshot ?: return

        Timber.d("deleteSnapshot: Deleting snapshot | ID: ${snapshot.id}")
        viewModelScope.launch {
            when (val result = deleteSnapshotUseCase(snapshot.id)) {
                is Result.Success -> {
                    Timber.d("deleteSnapshot: Snapshot deleted successfully")
                    // Navigation will be handled by the screen
                }
                is Result.Error -> {
                    Timber.e(result.exception, "deleteSnapshot: Failed to delete snapshot")
                    _state.update { it.copy(error = result.exception.message ?: "Failed to delete snapshot") }
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }
}

sealed class SnapshotViewerEvent {
    data object OnIncreaseFontSize : SnapshotViewerEvent()
    data object OnDecreaseFontSize : SnapshotViewerEvent()
    data object OnDeleteSnapshot : SnapshotViewerEvent()
}
