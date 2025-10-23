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
    private val deleteLinkUseCase: DeleteLinkUseCase
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
            is CollectionDetailEvent.OnToggleFavorite -> {
                toggleFavorite()
            }
            is CollectionDetailEvent.OnToggleLinkFavorite -> {
                toggleLinkFavorite(event.linkId)
            }
            is CollectionDetailEvent.OnArchiveLink -> {
                archiveLink(event.linkId)
            }
            is CollectionDetailEvent.OnTrashLink -> {
                trashLink(event.linkId)
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
            is CollectionDetailEvent.OnEditClick -> {
                _state.value.collection?.let { collection ->
                    _state.update {
                        it.copy(
                            showEditDialog = true,
                            editName = collection.name,
                            editColor = collection.color,
                            editIsFavorite = collection.isFavorite
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
            is CollectionDetailEvent.OnEditIsFavoriteChange -> {
                _state.update { it.copy(editIsFavorite = event.isFavorite) }
            }
            is CollectionDetailEvent.OnEditDismiss -> {
                _state.update {
                    it.copy(
                        showEditDialog = false,
                        editName = "",
                        editColor = null,
                        editIsFavorite = false
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

    private fun toggleFavorite() {
        Timber.d("toggleFavorite: Toggling favorite status")
        val collection = _state.value.collection ?: run {
            Timber.w("toggleFavorite: No collection to toggle favorite")
            return
        }

        viewModelScope.launch {
            val updatedCollection = collection.copy(isFavorite = !collection.isFavorite)
            Timber.d("toggleFavorite: Updating collection | NewFavoriteStatus: ${updatedCollection.isFavorite}")

            when (val result = updateCollectionUseCase(updatedCollection)) {
                is Result.Success -> {
                    Timber.d("toggleFavorite: Successfully updated favorite status")
                    _state.update { it.copy(collection = updatedCollection) }
                }
                is Result.Error -> {
                    Timber.e(result.exception, "toggleFavorite: Failed to update favorite status")
                    val errorMessage = ErrorHandler.getCollectionErrorMessage(result.exception, CollectionOperation.UPDATE)
                    _state.update { it.copy(error = errorMessage) }
                }
                is Result.Loading -> {
                    // Already handling loading state
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
                color = _state.value.editColor,
                isFavorite = _state.value.editIsFavorite
            )
            Timber.d("saveEditedCollection: Updating collection | Name: $name | Color: ${_state.value.editColor} | IsFavorite: ${_state.value.editIsFavorite}")

            when (val result = updateCollectionUseCase(updatedCollection)) {
                is Result.Success -> {
                    Timber.d("saveEditedCollection: Successfully updated collection")
                    _state.update {
                        it.copy(
                            collection = updatedCollection,
                            showEditDialog = false,
                            editName = "",
                            editColor = null,
                            editIsFavorite = false
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

    private fun toggleLinkFavorite(linkId: String) {
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
                    // Emit UI event for snackbar with undo
                    _uiEvents.emit(
                        CollectionDetailUiEvent.ShowLinkFavoriteToggled(
                            linkId = linkId,
                            isFavorite = updatedLink.isFavorite
                        )
                    )
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

    private fun archiveLink(linkId: String) {
        viewModelScope.launch {
            // Find the link in current state
            val link = _state.value.links.find { it.id == linkId }
            if (link == null) {
                Timber.w("archiveLink: Link not found | LinkId: $linkId")
                return@launch
            }

            val updatedLink = link.copy(isArchived = !link.isArchived)
            Timber.d("archiveLink: Toggling archive | LinkId: $linkId | NewValue: ${updatedLink.isArchived}")

            when (val result = updateLinkUseCase(updatedLink)) {
                is Result.Success -> {
                    Timber.d("archiveLink: Successfully updated archive status")
                }
                is Result.Error -> {
                    Timber.e(result.exception, "archiveLink: Failed to update archive status")
                    val errorMessage = ErrorHandler.getLinkErrorMessage(result.exception, LinkOperation.UPDATE)
                    _uiEvents.emit(CollectionDetailUiEvent.ShowError(errorMessage))
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun trashLink(linkId: String) {
        viewModelScope.launch {
            // Find the link in current state
            val link = _state.value.links.find { it.id == linkId }
            if (link == null) {
                Timber.w("trashLink: Link not found | LinkId: $linkId")
                return@launch
            }

            val updatedLink = link.copy(deletedAt = System.currentTimeMillis())
            Timber.d("trashLink: Moving link to trash | LinkId: $linkId")

            when (val result = updateLinkUseCase(updatedLink)) {
                is Result.Success -> {
                    Timber.d("trashLink: Successfully moved link to trash")
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

    /**
     * Sort links based on the selected sort type
     * Matches HomeViewModel logic: applies sort, then prioritizes favorites at top
     */
    private fun sortLinks(links: List<com.rejowan.linky.domain.model.Link>, sortType: com.rejowan.linky.presentation.feature.home.SortType): List<com.rejowan.linky.domain.model.Link> {
        // Apply the selected sort to all links
        val sorted = when (sortType) {
            com.rejowan.linky.presentation.feature.home.SortType.DATE_ADDED_DESC -> {
                links.sortedByDescending { it.createdAt }
            }
            com.rejowan.linky.presentation.feature.home.SortType.DATE_ADDED_ASC -> {
                links.sortedBy { it.createdAt }
            }
            com.rejowan.linky.presentation.feature.home.SortType.TITLE_ASC -> {
                links.sortedBy { it.title.lowercase() }
            }
            com.rejowan.linky.presentation.feature.home.SortType.TITLE_DESC -> {
                links.sortedByDescending { it.title.lowercase() }
            }
            com.rejowan.linky.presentation.feature.home.SortType.LAST_MODIFIED -> {
                links.sortedByDescending { it.updatedAt }
            }
        }

        // Prioritize favorites at top (maintaining sort order within each group)
        val favorites = sorted.filter { it.isFavorite }
        val nonFavorites = sorted.filter { !it.isFavorite }
        return favorites + nonFavorites
    }
}

sealed class CollectionDetailEvent {
    data object OnRefresh : CollectionDetailEvent()
    data object OnToggleFavorite : CollectionDetailEvent()
    data class OnToggleLinkFavorite(val linkId: String) : CollectionDetailEvent()
    data class OnArchiveLink(val linkId: String) : CollectionDetailEvent()
    data class OnTrashLink(val linkId: String) : CollectionDetailEvent()
    data class OnSortTypeChange(val sortType: com.rejowan.linky.presentation.feature.home.SortType) : CollectionDetailEvent()
    data object OnEditClick : CollectionDetailEvent()
    data class OnEditNameChange(val name: String) : CollectionDetailEvent()
    data class OnEditColorChange(val color: String?) : CollectionDetailEvent()
    data class OnEditIsFavoriteChange(val isFavorite: Boolean) : CollectionDetailEvent()
    data object OnEditDismiss : CollectionDetailEvent()
    data object OnEditConfirm : CollectionDetailEvent()
    data object OnDeleteClick : CollectionDetailEvent()
    data class OnDeleteWithLinksChange(val deleteWithLinks: Boolean) : CollectionDetailEvent()
    data object OnDeleteDismiss : CollectionDetailEvent()
    data object OnDeleteConfirm : CollectionDetailEvent()
}

sealed class CollectionDetailUiEvent {
    data class ShowError(val message: String) : CollectionDetailUiEvent()
    data class ShowLinkFavoriteToggled(val linkId: String, val isFavorite: Boolean) : CollectionDetailUiEvent()
    data object NavigateBack : CollectionDetailUiEvent()
}
