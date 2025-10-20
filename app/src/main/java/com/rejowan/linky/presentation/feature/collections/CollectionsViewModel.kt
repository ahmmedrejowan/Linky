package com.rejowan.linky.presentation.feature.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.usecase.collection.DeleteCollectionUseCase
import com.rejowan.linky.domain.usecase.collection.GetCollectionsWithLinkCountUseCase
import com.rejowan.linky.domain.usecase.collection.SaveCollectionUseCase
import com.rejowan.linky.util.ErrorHandler
import com.rejowan.linky.util.CollectionOperation
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

class CollectionsViewModel(
    private val getCollectionsWithLinkCountUseCase: GetCollectionsWithLinkCountUseCase,
    private val saveCollectionUseCase: SaveCollectionUseCase,
    private val deleteCollectionUseCase: DeleteCollectionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CollectionsState())
    val state: StateFlow<CollectionsState> = _state.asStateFlow()

    init {
        loadCollections()
    }

    fun onEvent(event: CollectionsEvent) {
        when (event) {
            is CollectionsEvent.OnCreateCollection -> {
                _state.update { it.copy(showCreateDialog = true) }
            }
            is CollectionsEvent.OnDismissCreateDialog -> {
                _state.update {
                    it.copy(
                        showCreateDialog = false,
                        newCollectionName = "",
                        selectedCollectionColor = null,
                        isNewCollectionFavorite = false
                    )
                }
            }
            is CollectionsEvent.OnCollectionNameChange -> {
                _state.update { it.copy(newCollectionName = event.name) }
            }
            is CollectionsEvent.OnCollectionColorChange -> {
                _state.update { it.copy(selectedCollectionColor = event.color) }
            }
            is CollectionsEvent.OnToggleFavorite -> {
                _state.update { it.copy(isNewCollectionFavorite = !it.isNewCollectionFavorite) }
            }
            is CollectionsEvent.OnSaveCollection -> {
                saveCollection()
            }
            is CollectionsEvent.OnDeleteCollection -> {
                deleteCollection(event.collectionId)
            }
            is CollectionsEvent.OnRefresh -> {
                loadCollections()
            }
            is CollectionsEvent.OnSortTypeChange -> {
                _state.update { it.copy(sortType = event.sortType) }
                applySorting()
            }
        }
    }

    private fun loadCollections() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            getCollectionsWithLinkCountUseCase()
                .catch { e ->
                    val errorMessage = ErrorHandler.getCollectionErrorMessage(e, CollectionOperation.LOAD_ALL)
                    _state.update { it.copy(isLoading = false, error = errorMessage) }
                }
                .collect { collections ->
                    val sortedCollections = sortCollections(collections, _state.value.sortType)
                    _state.update { it.copy(collections = sortedCollections, isLoading = false, error = null) }
                }
        }
    }

    /**
     * Apply sorting to current collections in state
     */
    private fun applySorting() {
        val currentCollections = _state.value.collections
        val sortedCollections = sortCollections(currentCollections, _state.value.sortType)
        _state.update { it.copy(collections = sortedCollections) }
        Timber.d("Collections sorted by ${_state.value.sortType.displayName}")
    }

    /**
     * Sort collections based on sort type
     */
    private fun sortCollections(
        collections: List<com.rejowan.linky.domain.model.CollectionWithLinkCount>,
        sortType: CollectionSortType
    ): List<com.rejowan.linky.domain.model.CollectionWithLinkCount> {
        return when (sortType) {
            CollectionSortType.DATE_CREATED_DESC -> collections.sortedByDescending { it.collection.createdAt }
            CollectionSortType.DATE_CREATED_ASC -> collections.sortedBy { it.collection.createdAt }
            CollectionSortType.NAME_ASC -> collections.sortedBy { it.collection.name.lowercase() }
            CollectionSortType.NAME_DESC -> collections.sortedByDescending { it.collection.name.lowercase() }
            CollectionSortType.LAST_MODIFIED -> collections.sortedByDescending { it.collection.updatedAt }
            CollectionSortType.MOST_LINKS -> collections.sortedByDescending { it.linkCount }
            CollectionSortType.LEAST_LINKS -> collections.sortedBy { it.linkCount }
        }
    }

    private fun saveCollection() {
        val currentState = _state.value

        // Validate collection name
        val nameValidation = Validator.validateCollectionName(currentState.newCollectionName.trim())
        if (nameValidation is ValidationResult.Error) {
            _state.update { it.copy(error = nameValidation.message) }
            return
        }

        // Validate color if provided
        if (currentState.selectedCollectionColor != null) {
            val colorValidation = Validator.validateColor(currentState.selectedCollectionColor)
            if (colorValidation is ValidationResult.Error) {
                _state.update { it.copy(error = colorValidation.message) }
                return
            }
        }

        viewModelScope.launch {
            val collection = Collection(
                name = currentState.newCollectionName.trim(),
                color = currentState.selectedCollectionColor,
                isFavorite = currentState.isNewCollectionFavorite,
                sortOrder = currentState.collections.size
            )

            when (val result = saveCollectionUseCase(collection)) {
                is Result.Success -> {
                    Timber.d("Collection created successfully")
                    _state.update {
                        it.copy(
                            showCreateDialog = false,
                            newCollectionName = "",
                            selectedCollectionColor = null,
                            isNewCollectionFavorite = false,
                            error = null
                        )
                    }
                }
                is Result.Error -> {
                    val errorMessage = ErrorHandler.getCollectionErrorMessage(result.exception, CollectionOperation.SAVE)
                    _state.update { it.copy(error = errorMessage) }
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            when (val result = deleteCollectionUseCase(collectionId)) {
                is Result.Success -> {
                    Timber.d("Collection deleted successfully")
                }
                is Result.Error -> {
                    val errorMessage = ErrorHandler.getCollectionErrorMessage(result.exception, CollectionOperation.DELETE)
                    _state.update { it.copy(error = errorMessage) }
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }
}

sealed class CollectionsEvent {
    data object OnCreateCollection : CollectionsEvent()
    data object OnDismissCreateDialog : CollectionsEvent()
    data class OnCollectionNameChange(val name: String) : CollectionsEvent()
    data class OnCollectionColorChange(val color: String?) : CollectionsEvent()
    data object OnToggleFavorite : CollectionsEvent()
    data object OnSaveCollection : CollectionsEvent()
    data class OnDeleteCollection(val collectionId: String) : CollectionsEvent()
    data object OnRefresh : CollectionsEvent()
    data class OnSortTypeChange(val sortType: CollectionSortType) : CollectionsEvent()
}
