package com.rejowan.linky.presentation.feature.addlink

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.usecase.folder.GetAllFoldersUseCase
import com.rejowan.linky.domain.usecase.link.GetLinkByIdUseCase
import com.rejowan.linky.domain.usecase.link.SaveLinkUseCase
import com.rejowan.linky.domain.usecase.link.UpdateLinkUseCase
import com.rejowan.linky.util.ErrorHandler
import com.rejowan.linky.util.FileStorageManager
import com.rejowan.linky.util.LinkOperation
import com.rejowan.linky.util.LinkPreviewFetcher
import com.rejowan.linky.util.Result
import com.rejowan.linky.util.ValidationResult
import com.rejowan.linky.util.Validator
import java.util.UUID
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
    private val getAllFoldersUseCase: GetAllFoldersUseCase,
    private val linkPreviewFetcher: LinkPreviewFetcher,
    private val fileStorageManager: FileStorageManager
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
                    val errorMessage = ErrorHandler.getLinkErrorMessage(e, LinkOperation.LOAD)
                    _state.update { it.copy(isLoading = false, error = errorMessage) }
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
        val currentUrl = _state.value.url.trim()

        // Validate URL before fetching
        if (currentUrl.isBlank()) {
            _state.update { it.copy(error = "Please enter a URL first") }
            return
        }

        _state.update { it.copy(isFetchingPreview = true, error = null) }

        viewModelScope.launch {
            try {
                val preview = linkPreviewFetcher.fetchPreview(currentUrl)

                if (preview != null) {
                    val currentState = _state.value
                    val ensuredLinkId = currentState.linkId ?: UUID.randomUUID().toString()

                    val localImagePath = preview.imageUrl?.let { imageUrl ->
                        fileStorageManager.savePreviewImageFromUrl(imageUrl, ensuredLinkId)
                    }

                    _state.update { latestState ->
                        latestState.copy(
                            linkId = ensuredLinkId,
                            isFetchingPreview = false,
                            // Auto-fill title if it's empty
                            title = latestState.title.ifBlank { preview.title },
                            previewUrl = preview.imageUrl,
                            previewImagePath = localImagePath ?: latestState.previewImagePath
                        )
                    }
                    Timber.d("Preview fetched successfully: ${preview.title}")
                } else {
                    _state.update {
                        it.copy(
                            isFetchingPreview = false,
                            error = "Could not fetch preview for this URL. The website may not support previews."
                        )
                    }
                }
            } catch (e: Exception) {
                val errorMessage = ErrorHandler.getLinkErrorMessage(e, LinkOperation.FETCH_PREVIEW)
                _state.update {
                    it.copy(
                        isFetchingPreview = false,
                        error = errorMessage
                    )
                }
            }
        }
    }

    private fun saveLink() {
        val currentState = _state.value

        // Comprehensive validation using Validator
        val validationResult = Validator.validateLink(
            url = currentState.url.trim(),
            title = currentState.title.trim(),
            note = currentState.note.trim().ifBlank { null }
        )

        if (validationResult is ValidationResult.Error) {
            _state.update { it.copy(error = validationResult.message) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val link = Link(
                id = currentState.linkId ?: UUID.randomUUID().toString(),
                title = currentState.title.trim(),
                url = currentState.url.trim(),
                note = currentState.note.trim().ifBlank { null },
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
                    val operation = if (currentState.isEditMode) LinkOperation.UPDATE else LinkOperation.SAVE
                    val errorMessage = ErrorHandler.getLinkErrorMessage(result.exception, operation)
                    _state.update {
                        it.copy(isLoading = false, error = errorMessage)
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
