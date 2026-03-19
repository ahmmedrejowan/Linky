package com.rejowan.linky.presentation.feature.collectiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.usecase.collection.DeleteCollectionUseCase
import com.rejowan.linky.domain.usecase.collection.GetCollectionByIdUseCase
import com.rejowan.linky.domain.usecase.collection.UpdateCollectionUseCase
import com.rejowan.linky.domain.usecase.link.DeleteLinkUseCase
import com.rejowan.linky.domain.usecase.link.GetLinksByCollectionUseCase
import com.rejowan.linky.domain.usecase.link.UpdateLinkUseCase
import com.rejowan.linky.util.ErrorHandler
import com.rejowan.linky.util.CollectionOperation
import com.rejowan.linky.util.LinkOperation
import com.rejowan.linky.util.Result
import com.rejowan.linky.util.ValidationResult
import com.rejowan.linky.util.Validator
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

class CollectionDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getCollectionByIdUseCase: GetCollectionByIdUseCase,
    private val getLinksByCollectionUseCase: GetLinksByCollectionUseCase,
    private val updateCollectionUseCase: UpdateCollectionUseCase,
    private val deleteCollectionUseCase: DeleteCollectionUseCase,
    private val updateLinkUseCase: UpdateLinkUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase,
    private val toggleArchiveUseCase: com.rejowan.linky.domain.usecase.link.ToggleArchiveUseCase,
    private val restoreLinkUseCase: com.rejowan.linky.domain.usecase.link.RestoreLinkUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CollectionDetailState())
    val state: StateFlow<CollectionDetailState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<CollectionDetailUiEvent>()
    val uiEvents: SharedFlow<CollectionDetailUiEvent> = _uiEvents.asSharedFlow()

    init {
        val collectionId = savedStateHandle.get<String>("collectionId")
        Timber.d("CollectionDetailViewModel initialized | CollectionId: $collectionId")

        collectionId?.let {
            loadCollectionDetails(it)
        } ?: run {
            Timber.e("CollectionDetailViewModel: collectionId is null")
            _state.update { it.copy(error = "Collection not found") }
        }
    }

    fun onEvent(event: CollectionDetailEvent) {
        Timber.d("onEvent: $event")
        when (event) {
            is CollectionDetailEvent.OnRefresh -> {
                savedStateHandle.get<String>("collectionId")?.let { collectionId ->
                    loadCollectionDetails(collectionId)
                }
            }
            is CollectionDetailEvent.OnToggleLinkFavorite -> {
                toggleLinkFavorite(event.linkId, event.silent)
            }
            is CollectionDetailEvent.OnArchiveLink -> {
                archiveLink(event.linkId, event.silent)
            }
            is CollectionDetailEvent.OnTrashLink -> {
                trashLink(event.linkId)
            }
            is CollectionDetailEvent.OnRestoreLink -> {
                restoreLink(event.linkId)
            }
            is CollectionDetailEvent.OnSortTypeChange -> {
                Timber.d("onEvent: Changing sort type to ${event.sortType}")
                _state.update {
                    it.copy(
                        sortType = event.sortType,
                        links = sortLinks(it.links, event.sortType)
                    )
                }
            }
            is CollectionDetailEvent.OnViewModeChange -> {
                Timber.d("onEvent: Changing view mode to ${event.viewMode}")
                _state.update { it.copy(viewMode = event.viewMode) }
            }
            is CollectionDetailEvent.OnEditClick -> {
                _state.value.collection?.let { collection ->
                    _state.update {
                        it.copy(
                            showEditDialog = true,
                            editName = collection.name,
                            editColor = collection.color
                        )
                    }
                }
            }
            is CollectionDetailEvent.OnEditNameChange -> {
                _state.update { it.copy(editName = event.name) }
            }
            is CollectionDetailEvent.OnEditColorChange -> {
                _state.update { it.copy(editColor = event.color) }
            }
            is CollectionDetailEvent.OnEditDismiss -> {
                _state.update {
                    it.copy(
                        showEditDialog = false,
                        editName = "",
                        editColor = null
                    )
                }
            }
            is CollectionDetailEvent.OnEditConfirm -> {
                saveEditedCollection()
            }
            is CollectionDetailEvent.OnDeleteClick -> {
                _state.update { it.copy(showDeleteDialog = true) }
            }
            is CollectionDetailEvent.OnDeleteWithLinksChange -> {
                _state.update { it.copy(deleteWithLinks = event.deleteWithLinks) }
            }
            is CollectionDetailEvent.OnDeleteDismiss -> {
                _state.update { it.copy(showDeleteDialog = false, deleteWithLinks = false) }
            }
            is CollectionDetailEvent.OnDeleteConfirm -> {
                deleteCollection()
            }
            // Bulk selection events
            CollectionDetailEvent.OnEnterSelectionMode -> {
                _state.update { it.copy(isSelectionMode = true, selectedLinkIds = emptySet()) }
            }
            CollectionDetailEvent.OnExitSelectionMode -> {
                _state.update { it.copy(isSelectionMode = false, selectedLinkIds = emptySet()) }
            }
            is CollectionDetailEvent.OnToggleLinkSelection -> {
                val currentSelection = _state.value.selectedLinkIds
                val newSelection = if (currentSelection.contains(event.linkId)) {
                    currentSelection - event.linkId
                } else {
                    currentSelection + event.linkId
                }
                _state.update { it.copy(selectedLinkIds = newSelection) }
            }
            CollectionDetailEvent.OnSelectAll -> {
                val allLinkIds = _state.value.links.map { it.id }.toSet()
                _state.update { it.copy(selectedLinkIds = allLinkIds) }
            }
            CollectionDetailEvent.OnDeselectAll -> {
                _state.update { it.copy(selectedLinkIds = emptySet()) }
            }
            CollectionDetailEvent.OnBulkDelete -> bulkDelete()
            CollectionDetailEvent.OnBulkFavorite -> bulkFavorite(true)
            CollectionDetailEvent.OnBulkUnfavorite -> bulkFavorite(false)
        }
    }

    private fun loadCollectionDetails(collectionId: String) {
        Timber.d("loadCollectionDetails: Loading collection | CollectionId: $collectionId")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Load collection info
            launch {
                getCollectionByIdUseCase(collectionId)
                    .catch { e ->
                        Timber.e(e, "loadCollectionDetails: Failed to load collection - ${e.message}")
                        val errorMessage = ErrorHandler.getCollectionErrorMessage(e, CollectionOperation.LOAD)
                        _state.update { it.copy(error = errorMessage, isLoading = false) }
                    }
                    .collect { collection ->
                        collection?.let {
                            Timber.d("loadCollectionDetails: Collection loaded | Name: ${it.name}")
                            _state.update { state -> state.copy(collection = it) }
                        } ?: run {
                            Timber.w("loadCollectionDetails: Collection not found")
                            _state.update { it.copy(error = "Collection not found", isLoading = false) }
                        }
                    }
            }

            // Load links in this collection
            launch {
                getLinksByCollectionUseCase(collectionId)
                    .catch { e ->
                        Timber.e(e, "loadCollectionDetails: Failed to load links - ${e.message}")
                        val errorMessage = ErrorHandler.getLinkErrorMessage(e, LinkOperation.LOAD_ALL)
                        _state.update { it.copy(error = errorMessage, isLoading = false) }
                    }
                    .collect { links ->
                        Timber.d("loadCollectionDetails: Loaded ${links.size} links")
                        val sortedLinks = sortLinks(links, _state.value.sortType)
                        _state.update { it.copy(links = sortedLinks, isLoading = false) }
                    }
            }
        }
    }

    private fun saveEditedCollection() {
        Timber.d("saveEditedCollection: Saving edited collection")
        val collection = _state.value.collection ?: run {
            Timber.w("saveEditedCollection: No collection to edit")
            return
        }

        val name = _state.value.editName.trim()

        // Validate name
        when (val validationResult = Validator.validateCollectionName(name)) {
            is ValidationResult.Success -> {
                // Validation passed, continue
            }
            is ValidationResult.Error -> {
                Timber.w("saveEditedCollection: Invalid collection name - ${validationResult.message}")
                _state.update { it.copy(error = validationResult.message) }
                return
            }
        }

        viewModelScope.launch {
            val updatedCollection = collection.copy(
                name = name,
                color = _state.value.editColor
            )
            Timber.d("saveEditedCollection: Updating collection | Name: $name | Color: ${_state.value.editColor}")

            when (val result = updateCollectionUseCase(updatedCollection)) {
                is Result.Success -> {
                    Timber.d("saveEditedCollection: Successfully updated collection")
                    _state.update {
                        it.copy(
                            collection = updatedCollection,
                            showEditDialog = false,
                            editName = "",
                            editColor = null
                        )
                    }
                }
                is Result.Error -> {
                    Timber.e(result.exception, "saveEditedCollection: Failed to update collection")
                    val errorMessage = ErrorHandler.getCollectionErrorMessage(result.exception, CollectionOperation.UPDATE)
                    _state.update { it.copy(error = errorMessage) }
                }
                is Result.Loading -> {
                    // Already handling loading state
                }
            }
        }
    }

    private fun deleteCollection() {
        Timber.d("deleteCollection: Deleting collection | DeleteWithLinks: ${_state.value.deleteWithLinks}")
        val collection = _state.value.collection ?: run {
            Timber.w("deleteCollection: No collection to delete")
            return
        }

        viewModelScope.launch {
            if (_state.value.deleteWithLinks) {
                // Move all links to trash (not delete immediately)
                Timber.d("deleteCollection: Moving ${_state.value.links.size} links to trash")
                _state.value.links.forEach { link ->
                    val updatedLink = link.copy(
                        deletedAt = System.currentTimeMillis(), // Move to trash
                        collectionId = null, // Remove collection reference
                        hideFromHome = false // Reset hideFromHome for potential restore
                    )
                    when (val result = updateLinkUseCase(updatedLink)) {
                        is Result.Success -> {
                            Timber.d("deleteCollection: Moved link to trash | LinkId: ${link.id}")
                        }
                        is Result.Error -> {
                            Timber.e(result.exception, "deleteCollection: Failed to move link to trash | LinkId: ${link.id}")
                        }
                        is Result.Loading -> {
                            // Loading state
                        }
                    }
                }
            } else {
                // Remove collection reference from all links and set hideFromHome to false
                Timber.d("deleteCollection: Removing collection reference from ${_state.value.links.size} links")
                _state.value.links.forEach { link ->
                    val updatedLink = link.copy(
                        collectionId = null,
                        hideFromHome = false // Reset hideFromHome since it only applies when in a collection
                    )
                    when (val result = updateLinkUseCase(updatedLink)) {
                        is Result.Success -> {
                            Timber.d("deleteCollection: Removed collection reference | LinkId: ${link.id}")
                        }
                        is Result.Error -> {
                            Timber.e(result.exception, "deleteCollection: Failed to remove collection reference | LinkId: ${link.id}")
                        }
                        is Result.Loading -> {
                            // Loading state
                        }
                    }
                }
            }

            // Delete the collection
            Timber.d("deleteCollection: Deleting collection | CollectionId: ${collection.id}")
            when (val result = deleteCollectionUseCase(collection.id)) {
                is Result.Success -> {
                    Timber.d("deleteCollection: Successfully deleted collection")
                    _state.update {
                        it.copy(
                            showDeleteDialog = false,
                            deleteWithLinks = false
                        )
                    }
                    // Navigate back after successful deletion
                    _uiEvents.emit(CollectionDetailUiEvent.NavigateBack)
                }
                is Result.Error -> {
                    Timber.e(result.exception, "deleteCollection: Failed to delete collection")
                    val errorMessage = ErrorHandler.getCollectionErrorMessage(result.exception, CollectionOperation.DELETE)
                    _state.update { it.copy(error = errorMessage) }
                }
                is Result.Loading -> {
                    // Already handling loading state
                }
            }
        }
    }

    private fun toggleLinkFavorite(linkId: String, silent: Boolean = false) {
        viewModelScope.launch {
            // Find the link in current state
            val link = _state.value.links.find { it.id == linkId }
            if (link == null) {
                Timber.w("toggleLinkFavorite: Link not found | LinkId: $linkId")
                return@launch
            }

            val updatedLink = link.copy(isFavorite = !link.isFavorite)
            Timber.d("toggleLinkFavorite: Toggling favorite | LinkId: $linkId | NewValue: ${updatedLink.isFavorite}")

            when (val result = updateLinkUseCase(updatedLink)) {
                is Result.Success -> {
                    Timber.d("toggleLinkFavorite: Successfully updated favorite status")
                    // Only emit snackbar event if not silent (not an undo action)
                    if (!silent) {
                        _uiEvents.emit(
                            CollectionDetailUiEvent.ShowLinkFavoriteToggled(
                                linkId = linkId,
                                isFavorite = updatedLink.isFavorite
                            )
                        )
                    }
                }
                is Result.Error -> {
                    Timber.e(result.exception, "toggleLinkFavorite: Failed to update favorite status")
                    val errorMessage = ErrorHandler.getLinkErrorMessage(result.exception, LinkOperation.TOGGLE_FAVORITE)
                    _uiEvents.emit(CollectionDetailUiEvent.ShowError(errorMessage))
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun archiveLink(linkId: String, silent: Boolean = false) {
        viewModelScope.launch {
            // Find the link in current state
            val link = _state.value.links.find { it.id == linkId }
            if (link == null) {
                Timber.w("archiveLink: Link not found | LinkId: $linkId")
                return@launch
            }

            val isArchiving = !link.isArchived
            Timber.d("archiveLink: Toggling archive | LinkId: $linkId | NewValue: $isArchiving")

            when (val result = toggleArchiveUseCase(linkId, isArchiving)) {
                is Result.Success -> {
                    Timber.d("archiveLink: Successfully updated archive status")
                    // Only emit snackbar event if not silent (not an undo action)
                    if (!silent) {
                        _uiEvents.emit(CollectionDetailUiEvent.ShowArchiveToggled(linkId, isArchiving))
                    }
                }
                is Result.Error -> {
                    Timber.e(result.exception, "archiveLink: Failed to update archive status")
                    val errorMessage = ErrorHandler.getLinkErrorMessage(result.exception, LinkOperation.TOGGLE_ARCHIVE)
                    _uiEvents.emit(CollectionDetailUiEvent.ShowError(errorMessage))
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun trashLink(linkId: String) {
        viewModelScope.launch {
            when (val result = deleteLinkUseCase(linkId, softDelete = true)) {
                is Result.Success -> {
                    Timber.d("trashLink: Successfully moved link to trash | LinkId: $linkId")
                    // Emit UI event for undo functionality
                    _uiEvents.emit(CollectionDetailUiEvent.ShowLinkTrashed(linkId))
                }
                is Result.Error -> {
                    Timber.e(result.exception, "trashLink: Failed to move link to trash")
                    val errorMessage = ErrorHandler.getLinkErrorMessage(result.exception, LinkOperation.DELETE)
                    _uiEvents.emit(CollectionDetailUiEvent.ShowError(errorMessage))
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun restoreLink(linkId: String) {
        viewModelScope.launch {
            when (val result = restoreLinkUseCase(linkId)) {
                is Result.Success -> {
                    Timber.d("restoreLink: Successfully restored link | LinkId: $linkId")
                }
                is Result.Error -> {
                    Timber.e(result.exception, "restoreLink: Failed to restore link")
                    val errorMessage = ErrorHandler.getLinkErrorMessage(result.exception, LinkOperation.RESTORE)
                    _uiEvents.emit(CollectionDetailUiEvent.ShowError(errorMessage))
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    /**
     * Sort links based on the selected sort type
     * Matches HomeViewModel logic: applies sort, then prioritizes favorites at top
     */
    private fun sortLinks(links: List<com.rejowan.linky.domain.model.Link>, sortType: com.rejowan.linky.presentation.feature.home.SortType): List<com.rejowan.linky.domain.model.Link> {
        // Apply the selected sort to all links (use updatedAt for date sorting)
        val sorted = when (sortType) {
            com.rejowan.linky.presentation.feature.home.SortType.DATE_DESC -> {
                links.sortedByDescending { it.updatedAt }
            }
            com.rejowan.linky.presentation.feature.home.SortType.DATE_ASC -> {
                links.sortedBy { it.updatedAt }
            }
            com.rejowan.linky.presentation.feature.home.SortType.NAME_ASC -> {
                links.sortedBy { it.title.lowercase() }
            }
            com.rejowan.linky.presentation.feature.home.SortType.NAME_DESC -> {
                links.sortedByDescending { it.title.lowercase() }
            }
        }

        // Prioritize favorites at top (maintaining sort order within each group)
        val favorites = sorted.filter { it.isFavorite }
        val nonFavorites = sorted.filter { !it.isFavorite }
        return favorites + nonFavorites
    }

    private fun bulkDelete() {
        val selectedIds = _state.value.selectedLinkIds.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            var successCount = 0
            selectedIds.forEach { linkId ->
                when (deleteLinkUseCase(linkId, softDelete = true)) {
                    is Result.Success -> successCount++
                    else -> { /* Continue with others */ }
                }
            }
            _state.update { it.copy(isSelectionMode = false, selectedLinkIds = emptySet()) }
            _uiEvents.emit(CollectionDetailUiEvent.ShowBulkOperationResult("$successCount links moved to trash"))
        }
    }

    private fun bulkFavorite(favorite: Boolean) {
        val selectedIds = _state.value.selectedLinkIds.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            var successCount = 0
            selectedIds.forEach { linkId ->
                val link = _state.value.links.find { it.id == linkId }
                if (link != null) {
                    val updatedLink = link.copy(isFavorite = favorite)
                    when (updateLinkUseCase(updatedLink)) {
                        is Result.Success -> successCount++
                        else -> { /* Continue with others */ }
                    }
                }
            }
            _state.update { it.copy(isSelectionMode = false, selectedLinkIds = emptySet()) }
            val action = if (favorite) "added to favorites" else "removed from favorites"
            _uiEvents.emit(CollectionDetailUiEvent.ShowBulkOperationResult("$successCount links $action"))
        }
    }
}

sealed class CollectionDetailEvent {
    data object OnRefresh : CollectionDetailEvent()
    data class OnToggleLinkFavorite(val linkId: String, val silent: Boolean = false) : CollectionDetailEvent()
    data class OnArchiveLink(val linkId: String, val silent: Boolean = false) : CollectionDetailEvent()
    data class OnTrashLink(val linkId: String) : CollectionDetailEvent()
    data class OnRestoreLink(val linkId: String) : CollectionDetailEvent()
    data class OnSortTypeChange(val sortType: com.rejowan.linky.presentation.feature.home.SortType) : CollectionDetailEvent()
    data class OnViewModeChange(val viewMode: com.rejowan.linky.presentation.feature.home.ViewMode) : CollectionDetailEvent()
    data object OnEditClick : CollectionDetailEvent()
    data class OnEditNameChange(val name: String) : CollectionDetailEvent()
    data class OnEditColorChange(val color: String?) : CollectionDetailEvent()
    data object OnEditDismiss : CollectionDetailEvent()
    data object OnEditConfirm : CollectionDetailEvent()
    data object OnDeleteClick : CollectionDetailEvent()
    data class OnDeleteWithLinksChange(val deleteWithLinks: Boolean) : CollectionDetailEvent()
    data object OnDeleteDismiss : CollectionDetailEvent()
    data object OnDeleteConfirm : CollectionDetailEvent()
    // Bulk selection events
    data object OnEnterSelectionMode : CollectionDetailEvent()
    data object OnExitSelectionMode : CollectionDetailEvent()
    data class OnToggleLinkSelection(val linkId: String) : CollectionDetailEvent()
    data object OnSelectAll : CollectionDetailEvent()
    data object OnDeselectAll : CollectionDetailEvent()
    data object OnBulkDelete : CollectionDetailEvent()
    data object OnBulkFavorite : CollectionDetailEvent()
    data object OnBulkUnfavorite : CollectionDetailEvent()
}

sealed class CollectionDetailUiEvent {
    data class ShowError(val message: String) : CollectionDetailUiEvent()
    data class ShowLinkFavoriteToggled(val linkId: String, val isFavorite: Boolean) : CollectionDetailUiEvent()
    data class ShowArchiveToggled(val linkId: String, val isArchived: Boolean) : CollectionDetailUiEvent()
    data class ShowLinkTrashed(val linkId: String) : CollectionDetailUiEvent()
    data object NavigateBack : CollectionDetailUiEvent()
    data class ShowBulkOperationResult(val message: String) : CollectionDetailUiEvent()
}
