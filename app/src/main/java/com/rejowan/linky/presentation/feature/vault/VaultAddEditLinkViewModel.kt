package com.rejowan.linky.presentation.feature.vault

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.model.VaultLink
import com.rejowan.linky.domain.usecase.vault.AddVaultLinkUseCase
import com.rejowan.linky.domain.usecase.vault.GetVaultLinkByIdUseCase
import com.rejowan.linky.domain.usecase.vault.UpdateVaultLinkUseCase
import com.rejowan.linky.util.LinkPreviewFetcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class VaultAddEditState(
    val isEditMode: Boolean = false,
    val linkId: String? = null,
    val url: String = "",
    val title: String = "",
    val description: String = "",
    val note: String = "",
    val isFavorite: Boolean = false,
    val previewUrl: String? = null,
    val previewImagePath: String? = null,
    val isLoading: Boolean = false,
    val isFetchingPreview: Boolean = false,
    val error: String? = null
)

sealed class VaultAddEditEvent {
    data class OnUrlChange(val url: String) : VaultAddEditEvent()
    data class OnTitleChange(val title: String) : VaultAddEditEvent()
    data class OnDescriptionChange(val description: String) : VaultAddEditEvent()
    data class OnNoteChange(val note: String) : VaultAddEditEvent()
    data object OnToggleFavorite : VaultAddEditEvent()
    data object OnFetchPreview : VaultAddEditEvent()
    data object OnSave : VaultAddEditEvent()
}

sealed class VaultAddEditUiEvent {
    data object LinkSaved : VaultAddEditUiEvent()
}

class VaultAddEditLinkViewModel(
    savedStateHandle: SavedStateHandle,
    private val addVaultLinkUseCase: AddVaultLinkUseCase,
    private val updateVaultLinkUseCase: UpdateVaultLinkUseCase,
    private val getVaultLinkByIdUseCase: GetVaultLinkByIdUseCase,
    private val linkPreviewFetcher: LinkPreviewFetcher
) : ViewModel() {

    private val _state = MutableStateFlow(VaultAddEditState())
    val state: StateFlow<VaultAddEditState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<VaultAddEditUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    init {
        // Check for linkId (edit mode) or prefill URL
        val linkId: String? = savedStateHandle["linkId"]
        val prefillUrl: String? = savedStateHandle["url"]

        if (linkId != null) {
            loadLink(linkId)
        } else if (!prefillUrl.isNullOrBlank()) {
            _state.update { it.copy(url = prefillUrl) }
        }
    }

    private fun loadLink(linkId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val link = getVaultLinkByIdUseCase(linkId)
            if (link != null) {
                _state.update {
                    it.copy(
                        isEditMode = true,
                        linkId = link.id,
                        url = link.url,
                        title = link.title,
                        description = link.description ?: "",
                        note = link.notes ?: "",
                        isFavorite = link.isFavorite,
                        previewUrl = link.previewUrl,
                        previewImagePath = link.previewImagePath,
                        isLoading = false
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false, error = "Link not found") }
            }
        }
    }

    fun onEvent(event: VaultAddEditEvent) {
        when (event) {
            is VaultAddEditEvent.OnUrlChange -> _state.update { it.copy(url = event.url) }
            is VaultAddEditEvent.OnTitleChange -> _state.update { it.copy(title = event.title) }
            is VaultAddEditEvent.OnDescriptionChange -> _state.update { it.copy(description = event.description) }
            is VaultAddEditEvent.OnNoteChange -> _state.update { it.copy(note = event.note) }
            VaultAddEditEvent.OnToggleFavorite -> _state.update { it.copy(isFavorite = !it.isFavorite) }
            VaultAddEditEvent.OnFetchPreview -> fetchPreview()
            VaultAddEditEvent.OnSave -> saveLink()
        }
    }

    private fun fetchPreview() {
        val url = _state.value.url.trim()
        if (url.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isFetchingPreview = true) }

            val preview = linkPreviewFetcher.fetchPreview(url)
            if (preview != null) {
                Timber.d("Preview fetched: ${preview.title}")
                _state.update {
                    it.copy(
                        title = if (it.title.isBlank()) preview.title else it.title,
                        description = if (it.description.isBlank()) (preview.description ?: "") else it.description,
                        previewUrl = preview.imageUrl,
                        previewImagePath = null, // Will be downloaded when saving if needed
                        isFetchingPreview = false
                    )
                }
            } else {
                Timber.e("Failed to fetch preview for: $url")
                _state.update {
                    it.copy(
                        isFetchingPreview = false,
                        // Set a default title from URL if blank
                        title = if (it.title.isBlank()) url else it.title
                    )
                }
            }
        }
    }

    private fun saveLink() {
        val currentState = _state.value

        // Validate URL
        if (currentState.url.isBlank()) {
            _state.update { it.copy(error = "URL is required") }
            return
        }

        // Validate title
        if (currentState.title.isBlank()) {
            _state.update { it.copy(error = "Title is required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val vaultLink = VaultLink(
                id = currentState.linkId ?: java.util.UUID.randomUUID().toString(),
                url = currentState.url.trim(),
                title = currentState.title.trim(),
                description = currentState.description.trim().ifBlank { null },
                notes = currentState.note.trim().ifBlank { null },
                previewUrl = currentState.previewUrl,
                previewImagePath = currentState.previewImagePath,
                isFavorite = currentState.isFavorite,
                createdAt = if (currentState.isEditMode) System.currentTimeMillis() else System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val result = if (currentState.isEditMode) {
                updateVaultLinkUseCase(vaultLink)
            } else {
                addVaultLinkUseCase(vaultLink)
            }

            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _uiEvents.emit(VaultAddEditUiEvent.LinkSaved)
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to save link"
                        )
                    }
                }
            )
        }
    }
}
