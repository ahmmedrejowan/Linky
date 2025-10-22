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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
                // Delete all links in the collection
                Timber.d("deleteCollection: Deleting ${_state.value.links.size} links in collection")
                _state.value.links.forEach { link ->
                    when (val result = deleteLinkUseCase(link.id)) {
                        is Result.Success -> {
                            Timber.d("deleteCollection: Deleted link | LinkId: ${link.id}")
                        }
                        is Result.Error -> {
                            Timber.e(result.exception, "deleteCollection: Failed to delete link | LinkId: ${link.id}")
                        }
                        is Result.Loading -> {
                            // Loading state
                        }
                    }
                }
            } else {
                // Remove collection reference from all links
                Timber.d("deleteCollection: Removing collection reference from ${_state.value.links.size} links")
                _state.value.links.forEach { link ->
                    val updatedLink = link.copy(collectionId = null)
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
                            deleteWithLinks = false,
                            error = "Collection deleted successfully"
                        )
                    }
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
