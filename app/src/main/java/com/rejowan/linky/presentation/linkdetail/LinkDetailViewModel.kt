package com.rejowan.linky.presentation.linkdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.usecase.link.DeleteLinkUseCase
import com.rejowan.linky.domain.usecase.link.GetLinkByIdUseCase
import com.rejowan.linky.domain.usecase.link.ToggleArchiveUseCase
import com.rejowan.linky.domain.usecase.link.ToggleFavoriteUseCase
import com.rejowan.linky.domain.usecase.snapshot.GetSnapshotsForLinkUseCase
import com.rejowan.linky.util.ErrorHandler
import com.rejowan.linky.util.LinkOperation
import com.rejowan.linky.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class LinkDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getLinkByIdUseCase: GetLinkByIdUseCase,
    private val getSnapshotsForLinkUseCase: GetSnapshotsForLinkUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val toggleArchiveUseCase: ToggleArchiveUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LinkDetailState())
    val state: StateFlow<LinkDetailState> = _state.asStateFlow()

    private val linkId: String? = savedStateHandle["linkId"]

    init {
        linkId?.let { loadLinkDetails(it) }
            ?: run {
                _state.update { it.copy(error = "Link ID not found") }
            }
    }

    fun onEvent(event: LinkDetailEvent) {
        when (event) {
            is LinkDetailEvent.OnToggleFavorite -> toggleFavorite()
            is LinkDetailEvent.OnDeleteLink -> deleteLink()
            is LinkDetailEvent.OnArchiveLink -> archiveLink()
            is LinkDetailEvent.OnRefresh -> linkId?.let { loadLinkDetails(it) }
        }
    }

    private fun loadLinkDetails(linkId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val linkFlow = getLinkByIdUseCase(linkId)
            val snapshotsFlow = getSnapshotsForLinkUseCase(linkId)

            combine(linkFlow, snapshotsFlow) { link, snapshots ->
                LinkDetailState(
                    link = link,
                    snapshots = snapshots,
                    isLoading = false,
                    error = if (link == null) "Link not found" else null
                )
            }
                .catch { e ->
                    val errorMessage = ErrorHandler.getLinkErrorMessage(e, LinkOperation.LOAD)
                    _state.update { it.copy(isLoading = false, error = errorMessage) }
                }
                .collect { newState ->
                    _state.value = newState
                }
        }
    }

    private fun toggleFavorite() {
        val link = _state.value.link ?: return

        viewModelScope.launch {
            when (val result = toggleFavoriteUseCase(link.id, !link.isFavorite)) {
                is Result.Success -> {
                    Timber.d("Toggled favorite for link: ${link.id}")
                }
                is Result.Error -> {
                    val errorMessage = ErrorHandler.getLinkErrorMessage(result.exception, LinkOperation.TOGGLE_FAVORITE)
                    _state.update { it.copy(error = errorMessage) }
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun deleteLink() {
        val link = _state.value.link ?: return

        viewModelScope.launch {
            when (val result = deleteLinkUseCase(link.id, softDelete = true)) {
                is Result.Success -> {
                    Timber.d("Deleted link: ${link.id}")
                    _state.update { it.copy(isDeleted = true) }
                }
                is Result.Error -> {
                    val errorMessage = ErrorHandler.getLinkErrorMessage(result.exception, LinkOperation.DELETE)
                    _state.update { it.copy(error = errorMessage) }
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun archiveLink() {
        val link = _state.value.link ?: return

        viewModelScope.launch {
            when (val result = toggleArchiveUseCase(link.id, !link.isArchived)) {
                is Result.Success -> {
                    Timber.d("Toggled archive for link: ${link.id}")
                }
                is Result.Error -> {
                    val errorMessage = ErrorHandler.getLinkErrorMessage(result.exception, LinkOperation.TOGGLE_ARCHIVE)
                    _state.update { it.copy(error = errorMessage) }
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }
}

sealed class LinkDetailEvent {
    data object OnToggleFavorite : LinkDetailEvent()
    data object OnDeleteLink : LinkDetailEvent()
    data object OnArchiveLink : LinkDetailEvent()
    data object OnRefresh : LinkDetailEvent()
}
