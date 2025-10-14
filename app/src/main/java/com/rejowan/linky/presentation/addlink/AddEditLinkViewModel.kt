package com.rejowan.linky.presentation.addlink

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.usecase.folder.GetAllFoldersUseCase
import com.rejowan.linky.domain.usecase.link.GetLinkByIdUseCase
import com.rejowan.linky.domain.usecase.link.SaveLinkUseCase
import com.rejowan.linky.domain.usecase.link.UpdateLinkUseCase
import com.rejowan.linky.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class AddEditLinkViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val saveLinkUseCase: SaveLinkUseCase,
    private val updateLinkUseCase: UpdateLinkUseCase,
    private val getLinkByIdUseCase: GetLinkByIdUseCase,
    private val getAllFoldersUseCase: GetAllFoldersUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditLinkState())
    val state: StateFlow<AddEditLinkState> = _state.asStateFlow()

    init {
        loadFolders()

        // Check if we're editing an existing link
        savedStateHandle.get<String>("linkId")?.let { linkId ->
            loadLink(linkId)
        }
    }

    fun onEvent(event: AddEditLinkEvent) {
        when (event) {
            is AddEditLinkEvent.OnTitleChange -> {
                _state.update { it.copy(title = event.title) }
            }
            is AddEditLinkEvent.OnUrlChange -> {
                _state.update { it.copy(url = event.url) }
            }
            is AddEditLinkEvent.OnNoteChange -> {
                _state.update { it.copy(note = event.note) }
            }
            is AddEditLinkEvent.OnFolderSelect -> {
                _state.update { it.copy(selectedFolderId = event.folderId) }
            }
            is AddEditLinkEvent.OnToggleFavorite -> {
                _state.update { it.copy(isFavorite = !it.isFavorite) }
            }
            is AddEditLinkEvent.OnFetchPreview -> {
                fetchPreview()
            }
            is AddEditLinkEvent.OnSave -> {
                saveLink()
            }
        }
    }

    private fun loadFolders() {
        viewModelScope.launch {
            getAllFoldersUseCase()
                .catch { e ->
                    Timber.e(e, "Failed to load folders")
                }
                .collect { folders ->
                    _state.update { it.copy(folders = folders) }
                }
        }
    }

    private fun loadLink(linkId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isEditMode = true) }

            getLinkByIdUseCase(linkId)
                .catch { e ->
                    Timber.e(e, "Failed to load link")
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { link ->
                    link?.let {
                        _state.update { state ->
                            state.copy(
                                linkId = it.id,
                                title = it.title,
                                url = it.url,
                                note = it.note ?: "",
                                selectedFolderId = it.folderId,
                                previewImagePath = it.previewImagePath,
                                previewUrl = it.previewUrl,
                                isFavorite = it.isFavorite,
                                isLoading = false
                            )
                        }
                    } ?: run {
                        _state.update { it.copy(isLoading = false, error = "Link not found") }
                    }
                }
        }
    }

    private fun fetchPreview() {
        // TODO: Implement preview fetching with Jsoup
        // This will be implemented later with LinkPreviewFetcher utility
        _state.update { it.copy(isFetchingPreview = true) }

        viewModelScope.launch {
            // Placeholder - actual implementation will use Jsoup
            try {
                // Mock: Set a preview URL based on the URL
                _state.update {
                    it.copy(
                        isFetchingPreview = false,
                        previewUrl = state.value.url // Temporary
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch preview")
                _state.update { it.copy(isFetchingPreview = false, error = e.message) }
            }
        }
    }

    private fun saveLink() {
        val currentState = _state.value

        // Validation
        if (currentState.url.isBlank()) {
            _state.update { it.copy(error = "URL is required") }
            return
        }

        if (currentState.title.isBlank()) {
            _state.update { it.copy(error = "Title is required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val link = Link(
                id = currentState.linkId ?: java.util.UUID.randomUUID().toString(),
                title = currentState.title,
                url = currentState.url,
                note = currentState.note.ifBlank { null },
                folderId = currentState.selectedFolderId,
                previewImagePath = currentState.previewImagePath,
                previewUrl = currentState.previewUrl,
                isFavorite = currentState.isFavorite
            )

            val result = if (currentState.isEditMode) {
                updateLinkUseCase(link)
            } else {
                saveLinkUseCase(link)
            }

            when (result) {
                is Result.Success -> {
                    Timber.d("Link saved successfully")
                    _state.update { it.copy(isLoading = false, isSaved = true) }
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Failed to save link")
                    _state.update {
                        it.copy(isLoading = false, error = result.exception.message)
                    }
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }
}

sealed class AddEditLinkEvent {
    data class OnTitleChange(val title: String) : AddEditLinkEvent()
    data class OnUrlChange(val url: String) : AddEditLinkEvent()
    data class OnNoteChange(val note: String) : AddEditLinkEvent()
    data class OnFolderSelect(val folderId: String?) : AddEditLinkEvent()
    data object OnToggleFavorite : AddEditLinkEvent()
    data object OnFetchPreview : AddEditLinkEvent()
    data object OnSave : AddEditLinkEvent()
}
