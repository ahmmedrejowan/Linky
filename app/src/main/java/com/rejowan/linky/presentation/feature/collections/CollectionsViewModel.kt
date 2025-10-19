package com.rejowan.linky.presentation.feature.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.model.Folder
import com.rejowan.linky.domain.usecase.folder.DeleteFolderUseCase
import com.rejowan.linky.domain.usecase.folder.GetAllFoldersUseCase
import com.rejowan.linky.domain.usecase.folder.SaveFolderUseCase
import com.rejowan.linky.util.ErrorHandler
import com.rejowan.linky.util.FolderOperation
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
    private val getAllFoldersUseCase: GetAllFoldersUseCase,
    private val saveFolderUseCase: SaveFolderUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CollectionsState())
    val state: StateFlow<CollectionsState> = _state.asStateFlow()

    init {
        loadFolders()
    }

    fun onEvent(event: CollectionsEvent) {
        when (event) {
            is CollectionsEvent.OnCreateFolder -> {
                _state.update { it.copy(showCreateDialog = true) }
            }
            is CollectionsEvent.OnDismissCreateDialog -> {
                _state.update {
                    it.copy(
                        showCreateDialog = false,
                        newFolderName = "",
                        selectedFolderColor = null
                    )
                }
            }
            is CollectionsEvent.OnFolderNameChange -> {
                _state.update { it.copy(newFolderName = event.name) }
            }
            is CollectionsEvent.OnFolderColorChange -> {
                _state.update { it.copy(selectedFolderColor = event.color) }
            }
            is CollectionsEvent.OnSaveFolder -> {
                saveFolder()
            }
            is CollectionsEvent.OnDeleteFolder -> {
                deleteFolder(event.folderId)
            }
            is CollectionsEvent.OnRefresh -> {
                loadFolders()
            }
        }
    }

    private fun loadFolders() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            getAllFoldersUseCase()
                .catch { e ->
                    val errorMessage = ErrorHandler.getFolderErrorMessage(e, FolderOperation.LOAD_ALL)
                    _state.update { it.copy(isLoading = false, error = errorMessage) }
                }
                .collect { folders ->
                    _state.update { it.copy(folders = folders, isLoading = false, error = null) }
                }
        }
    }

    private fun saveFolder() {
        val currentState = _state.value

        // Validate folder name
        val nameValidation = Validator.validateFolderName(currentState.newFolderName.trim())
        if (nameValidation is ValidationResult.Error) {
            _state.update { it.copy(error = nameValidation.message) }
            return
        }

        // Validate color if provided
        if (currentState.selectedFolderColor != null) {
            val colorValidation = Validator.validateColor(currentState.selectedFolderColor)
            if (colorValidation is ValidationResult.Error) {
                _state.update { it.copy(error = colorValidation.message) }
                return
            }
        }

        viewModelScope.launch {
            val folder = Folder(
                name = currentState.newFolderName.trim(),
                color = currentState.selectedFolderColor,
                sortOrder = currentState.folders.size
            )

            when (val result = saveFolderUseCase(folder)) {
                is Result.Success -> {
                    Timber.d("Folder created successfully")
                    _state.update {
                        it.copy(
                            showCreateDialog = false,
                            newFolderName = "",
                            selectedFolderColor = null,
                            error = null
                        )
                    }
                }
                is Result.Error -> {
                    val errorMessage = ErrorHandler.getFolderErrorMessage(result.exception, FolderOperation.SAVE)
                    _state.update { it.copy(error = errorMessage) }
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            when (val result = deleteFolderUseCase(folderId)) {
                is Result.Success -> {
                    Timber.d("Folder deleted successfully")
                }
                is Result.Error -> {
                    val errorMessage = ErrorHandler.getFolderErrorMessage(result.exception, FolderOperation.DELETE)
                    _state.update { it.copy(error = errorMessage) }
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }
}

sealed class CollectionsEvent {
    data object OnCreateFolder : CollectionsEvent()
    data object OnDismissCreateDialog : CollectionsEvent()
    data class OnFolderNameChange(val name: String) : CollectionsEvent()
    data class OnFolderColorChange(val color: String) : CollectionsEvent()
    data object OnSaveFolder : CollectionsEvent()
    data class OnDeleteFolder(val folderId: String) : CollectionsEvent()
    data object OnRefresh : CollectionsEvent()
}
