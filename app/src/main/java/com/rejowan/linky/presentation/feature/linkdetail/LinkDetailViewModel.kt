package com.rejowan.linky.presentation.feature.linkdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.usecase.collection.GetCollectionByIdUseCase
import com.rejowan.linky.domain.usecase.link.DeleteLinkUseCase
import com.rejowan.linky.domain.usecase.link.GetLinkByIdUseCase
import com.rejowan.linky.domain.usecase.link.ToggleArchiveUseCase
import com.rejowan.linky.domain.usecase.link.ToggleFavoriteUseCase
import com.rejowan.linky.domain.usecase.snapshot.CaptureSnapshotUseCase
import com.rejowan.linky.domain.usecase.snapshot.DeleteSnapshotUseCase
import com.rejowan.linky.domain.usecase.snapshot.GetSnapshotsForLinkUseCase
import com.rejowan.linky.util.ErrorHandler
import com.rejowan.linky.util.LinkOperation
import com.rejowan.linky.util.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class LinkDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getLinkByIdUseCase: GetLinkByIdUseCase,
    private val getSnapshotsForLinkUseCase: GetSnapshotsForLinkUseCase,
    private val getCollectionByIdUseCase: GetCollectionByIdUseCase,
    private val captureSnapshotUseCase: CaptureSnapshotUseCase,
    private val deleteSnapshotUseCase: DeleteSnapshotUseCase,
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
            is LinkDetailEvent.OnCreateSnapshot -> captureSnapshot()
            is LinkDetailEvent.OnDeleteSnapshot -> deleteSnapshot(event.snapshotId)
        }
    }

    private fun loadLinkDetails(linkId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            getLinkByIdUseCase(linkId)
                .flatMapLatest { link ->
                    val collectionFlow = link?.collectionId?.let { collectionId ->
                        getCollectionByIdUseCase(collectionId).catch { emit(null) }
                    } ?: flowOf(null)

                    val snapshotsFlow = getSnapshotsForLinkUseCase(linkId)

                    combine(flowOf(link), collectionFlow, snapshotsFlow) { l, c, s ->
                        Triple(l, c, s)
                    }
                }
                .catch { e ->
                    val errorMessage = ErrorHandler.getLinkErrorMessage(e, LinkOperation.LOAD)
                    _state.update { it.copy(isLoading = false, error = errorMessage) }
                }
                .collect { (link, collection, snapshots) ->
                    _state.value = LinkDetailState(
                        link = link,
                        collection = collection,
                        snapshots = snapshots,
                        isLoading = false,
                        error = if (link == null) "Link not found" else null
                    )
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
                    // Add small delay to allow database changes to propagate through Flows
                    kotlinx.coroutines.delay(150)
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

    private fun captureSnapshot() {
        val link = _state.value.link ?: return

        Timber.d("captureSnapshot: Starting snapshot capture for link: ${link.id}")
        _state.update { it.copy(isCapturingSnapshot = true, error = null, snapshotCaptured = false) }

        viewModelScope.launch {
            when (val result = captureSnapshotUseCase(link.url, link.id)) {
                is Result.Success -> {
                    Timber.d("captureSnapshot: Snapshot captured successfully | ID: ${result.data.id}")
                    _state.update { it.copy(isCapturingSnapshot = false, snapshotCaptured = true) }
                }
                is Result.Error -> {
                    Timber.e(result.exception, "captureSnapshot: Failed to capture snapshot")
                    val errorMessage = result.exception.message ?: "Failed to capture snapshot"
                    _state.update { it.copy(isCapturingSnapshot = false, error = errorMessage) }
                }
                is Result.Loading -> {
                    Timber.d("captureSnapshot: Capture in progress...")
                }
            }
        }
    }

    private fun deleteSnapshot(snapshotId: String) {
        Timber.d("deleteSnapshot: Deleting snapshot | ID: $snapshotId")

        viewModelScope.launch {
            when (val result = deleteSnapshotUseCase(snapshotId)) {
                is Result.Success -> {
                    Timber.d("deleteSnapshot: Snapshot deleted successfully")
                }
                is Result.Error -> {
                    Timber.e(result.exception, "deleteSnapshot: Failed to delete snapshot")
                    val errorMessage = result.exception.message ?: "Failed to delete snapshot"
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
    data object OnCreateSnapshot : LinkDetailEvent()
    data class OnDeleteSnapshot(val snapshotId: String) : LinkDetailEvent()
}
