package com.rejowan.linky.presentation.feature.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.data.local.preferences.ThemePreferences
import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.usecase.collection.DeleteCollectionUseCase
import com.rejowan.linky.domain.usecase.collection.GetCollectionsWithLinkCountUseCase
import com.rejowan.linky.domain.usecase.collection.SaveCollectionUseCase
import com.rejowan.linky.util.ErrorHandler
import com.rejowan.linky.util.CollectionOperation
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

class CollectionsViewModel(
    private val getCollectionsWithLinkCountUseCase: GetCollectionsWithLinkCountUseCase,
    private val saveCollectionUseCase: SaveCollectionUseCase,
    private val updateCollectionUseCase: com.rejowan.linky.domain.usecase.collection.UpdateCollectionUseCase,
    private val deleteCollectionUseCase: DeleteCollectionUseCase,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _state = MutableStateFlow(CollectionsState())
    val state: StateFlow<CollectionsState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<CollectionsUiEvent>()
    val uiEvents: SharedFlow<CollectionsUiEvent> = _uiEvents.asSharedFlow()

    init {
        loadCollections()
        observeCollectionViewMode()
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
                        selectedCollectionColor = null
                    )
                }
            }
            is CollectionsEvent.OnCollectionNameChange -> {
                _state.update { it.copy(newCollectionName = event.name) }
            }
            is CollectionsEvent.OnCollectionColorChange -> {
                _state.update { it.copy(selectedCollectionColor = event.color) }
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
            is CollectionsEvent.OnViewModeChange -> {
                _state.update { it.copy(viewMode = event.viewMode) }
                viewModelScope.launch {
                    themePreferences.setCollectionViewMode(event.viewMode.name)
                }
            }
            is CollectionsEvent.OnEditCollection -> {
                _state.update {
                    it.copy(
                        showEditDialog = true,
                        editingCollection = event.collection,
                        editCollectionName = event.collection.collection.name,
                        editCollectionColor = event.collection.collection.color
                    )
                }
            }
            is CollectionsEvent.OnDismissEditDialog -> {
                _state.update {
                    it.copy(
                        showEditDialog = false,
                        editingCollection = null,
                        editCollectionName = "",
                        editCollectionColor = null
                    )
                }
            }
            is CollectionsEvent.OnEditCollectionNameChange -> {
                _state.update { it.copy(editCollectionName = event.name) }
            }
            is CollectionsEvent.OnEditCollectionColorChange -> {
                _state.update { it.copy(editCollectionColor = event.color) }
            }
            is CollectionsEvent.OnSaveEditedCollection -> {
                saveEditedCollection()
            }
            is CollectionsEvent.OnShowDeleteDialog -> {
                _state.update {
                    it.copy(
                        showDeleteDialog = true,
                        deletingCollection = event.collection
                    )
                }
            }
            is CollectionsEvent.OnDismissDeleteDialog -> {
                _state.update {
                    it.copy(
                        showDeleteDialog = false,
                        deletingCollection = null
                    )
                }
            }
            is CollectionsEvent.OnConfirmDelete -> {
                _state.value.deletingCollection?.let { collection ->
                    deleteCollection(collection.collection.id)
                    _state.update {
                        it.copy(
                            showDeleteDialog = false,
                            deletingCollection = null
                        )
                    }
                }
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

    private fun saveEditedCollection() {
        val currentState = _state.value
        val editingCollection = currentState.editingCollection ?: return

        // Validate collection name
        val nameValidation = Validator.validateCollectionName(currentState.editCollectionName.trim())
        if (nameValidation is ValidationResult.Error) {
            _state.update { it.copy(error = nameValidation.message) }
            return
        }

        // Validate color if provided
        if (currentState.editCollectionColor != null) {
            val colorValidation = Validator.validateColor(currentState.editCollectionColor)
            if (colorValidation is ValidationResult.Error) {
                _state.update { it.copy(error = colorValidation.message) }
                return
            }
        }

        viewModelScope.launch {
            val updatedCollection = editingCollection.collection.copy(
                name = currentState.editCollectionName.trim(),
                color = currentState.editCollectionColor,
                updatedAt = System.currentTimeMillis()
            )

            when (val result = updateCollectionUseCase(updatedCollection)) {
                is Result.Success -> {
                    Timber.d("Collection updated successfully")
                    _state.update {
                        it.copy(
                            showEditDialog = false,
                            editingCollection = null,
                            editCollectionName = "",
                            editCollectionColor = null,
                            error = null
                        )
                    }
                }
                is Result.Error -> {
                    val errorMessage = ErrorHandler.getCollectionErrorMessage(result.exception, CollectionOperation.UPDATE)
                    _state.update { it.copy(error = errorMessage) }
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun observeCollectionViewMode() {
        viewModelScope.launch {
            themePreferences.getCollectionViewMode()
                .catch { e ->
                    Timber.e(e, "Failed to observe collection view mode")
                }
                .collect { viewModeName ->
                    val viewMode = try {
                        com.rejowan.linky.presentation.feature.home.ViewMode.valueOf(viewModeName)
                    } catch (e: IllegalArgumentException) {
                        com.rejowan.linky.presentation.feature.home.ViewMode.LIST
                    }
                    _state.update { it.copy(viewMode = viewMode) }
                }
        }
    }

}

sealed class CollectionsEvent {
    data object OnCreateCollection : CollectionsEvent()
    data object OnDismissCreateDialog : CollectionsEvent()
    data class OnCollectionNameChange(val name: String) : CollectionsEvent()
    data class OnCollectionColorChange(val color: String?) : CollectionsEvent()
    data object OnSaveCollection : CollectionsEvent()
    data class OnDeleteCollection(val collectionId: String) : CollectionsEvent()
    data object OnRefresh : CollectionsEvent()
    data class OnSortTypeChange(val sortType: CollectionSortType) : CollectionsEvent()
    data class OnViewModeChange(val viewMode: com.rejowan.linky.presentation.feature.home.ViewMode) : CollectionsEvent()
    // Edit events
    data class OnEditCollection(val collection: com.rejowan.linky.domain.model.CollectionWithLinkCount) : CollectionsEvent()
    data object OnDismissEditDialog : CollectionsEvent()
    data class OnEditCollectionNameChange(val name: String) : CollectionsEvent()
    data class OnEditCollectionColorChange(val color: String?) : CollectionsEvent()
    data object OnSaveEditedCollection : CollectionsEvent()
    // Delete confirmation events
    data class OnShowDeleteDialog(val collection: com.rejowan.linky.domain.model.CollectionWithLinkCount) : CollectionsEvent()
    data object OnDismissDeleteDialog : CollectionsEvent()
    data object OnConfirmDelete : CollectionsEvent()
}

sealed class CollectionsUiEvent {
    data class ShowError(val message: String) : CollectionsUiEvent()
}
