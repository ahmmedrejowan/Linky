package com.rejowan.linky.presentation.feature.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.usecase.link.DeleteLinkUseCase
import com.rejowan.linky.domain.usecase.link.GetTrashedLinksUseCase
import com.rejowan.linky.domain.usecase.link.RestoreLinkUseCase
import com.rejowan.linky.util.ErrorHandler
import com.rejowan.linky.util.LinkOperation
import com.rejowan.linky.util.Result
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class TrashViewModel(
    private val getTrashedLinksUseCase: GetTrashedLinksUseCase,
    private val restoreLinkUseCase: RestoreLinkUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TrashState())
    val state: StateFlow<TrashState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<TrashUiEvent>()
    val uiEvents: SharedFlow<TrashUiEvent> = _uiEvents.asSharedFlow()

    init {
        loadTrashedLinks()
    }

    fun onEvent(event: TrashEvent) {
        when (event) {
            is TrashEvent.OnRefresh -> loadTrashedLinks()
            is TrashEvent.OnRestoreLink -> restoreLink(event.linkId)
            is TrashEvent.OnPermanentlyDeleteLink -> permanentlyDeleteLink(event.linkId)
            is TrashEvent.OnUndoDelete -> undoDelete(event.linkId)
            is TrashEvent.OnEmptyTrash -> emptyTrash()
        }
    }

    private fun loadTrashedLinks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            getTrashedLinksUseCase()
                .catch { e ->
                    val errorMessage = ErrorHandler.getLinkErrorMessage(e, LinkOperation.LOAD_ALL)
                    _state.update { it.copy(isLoading = false, error = errorMessage) }
                }
                .collect { trashedLinks ->
                    _state.update { it.copy(trashedLinks = trashedLinks, isLoading = false, error = null) }
                }
        }
    }

    private fun restoreLink(linkId: String) {
        viewModelScope.launch {
            when (val result = restoreLinkUseCase(linkId)) {
                is Result.Success -> {
                    Timber.d("Restored link: $linkId")
                    // Emit UI event for undo functionality
                    _uiEvents.emit(TrashUiEvent.ShowLinkRestored(linkId))
                }
                is Result.Error -> {
                    val errorMessage = ErrorHandler.getLinkErrorMessage(result.exception, LinkOperation.RESTORE)
                    _uiEvents.emit(TrashUiEvent.ShowError(errorMessage))
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun permanentlyDeleteLink(linkId: String) {
        viewModelScope.launch {
            when (val result = deleteLinkUseCase(linkId, softDelete = false)) {
                is Result.Success -> {
                    Timber.d("Permanently deleted link: $linkId")
                    // Emit UI event for undo functionality
                    _uiEvents.emit(TrashUiEvent.ShowLinkDeleted(linkId))
                }
                is Result.Error -> {
                    val errorMessage = ErrorHandler.getLinkErrorMessage(result.exception, LinkOperation.DELETE)
                    _uiEvents.emit(TrashUiEvent.ShowError(errorMessage))
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun undoDelete(linkId: String) {
        viewModelScope.launch {
            when (val result = restoreLinkUseCase(linkId)) {
                is Result.Success -> {
                    Timber.d("Undid delete for link: $linkId")
                }
                is Result.Error -> {
                    val errorMessage = ErrorHandler.getLinkErrorMessage(result.exception, LinkOperation.RESTORE)
                    _uiEvents.emit(TrashUiEvent.ShowError(errorMessage))
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun emptyTrash() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val trashedLinks = _state.value.trashedLinks
            var hasError = false

            trashedLinks.forEach { link ->
                when (val result = deleteLinkUseCase(link.id, softDelete = false)) {
                    is Result.Success -> {
                        Timber.d("Permanently deleted link: ${link.id}")
                    }
                    is Result.Error -> {
                        hasError = true
                        Timber.e(result.exception, "Failed to delete link: ${link.id}")
                    }
                    is Result.Loading -> { /* No-op */ }
                }
            }

            if (hasError) {
                _state.update { it.copy(isLoading = false, error = "Some items could not be deleted") }
            } else {
                _state.update { it.copy(isLoading = false, error = null) }
            }
        }
    }
}

sealed class TrashEvent {
    data object OnRefresh : TrashEvent()
    data class OnRestoreLink(val linkId: String) : TrashEvent()
    data class OnPermanentlyDeleteLink(val linkId: String) : TrashEvent()
    data class OnUndoDelete(val linkId: String) : TrashEvent()
    data object OnEmptyTrash : TrashEvent()
}

sealed class TrashUiEvent {
    data class ShowError(val message: String) : TrashUiEvent()
    data class ShowLinkRestored(val linkId: String) : TrashUiEvent()
    data class ShowLinkDeleted(val linkId: String) : TrashUiEvent()
}
