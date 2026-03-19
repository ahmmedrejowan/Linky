package com.rejowan.linky.presentation.feature.vault

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.model.VaultLink
import com.rejowan.linky.domain.usecase.vault.DeleteVaultLinkUseCase
import com.rejowan.linky.domain.usecase.vault.GetVaultLinkByIdUseCase
import com.rejowan.linky.domain.usecase.vault.UpdateVaultLinkUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class VaultLinkDetailState(
    val link: VaultLink? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val error: String? = null
)

sealed class VaultLinkDetailEvent {
    data object OnToggleFavorite : VaultLinkDetailEvent()
    data object OnDelete : VaultLinkDetailEvent()
    data object OnRefresh : VaultLinkDetailEvent()
}

sealed class VaultLinkDetailUiEvent {
    data class ShowMessage(val message: String) : VaultLinkDetailUiEvent()
    data object LinkDeleted : VaultLinkDetailUiEvent()
}

class VaultLinkDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val getVaultLinkByIdUseCase: GetVaultLinkByIdUseCase,
    private val updateVaultLinkUseCase: UpdateVaultLinkUseCase,
    private val deleteVaultLinkUseCase: DeleteVaultLinkUseCase
) : ViewModel() {

    private val linkId: String = checkNotNull(savedStateHandle["linkId"])

    private val _state = MutableStateFlow(VaultLinkDetailState())
    val state: StateFlow<VaultLinkDetailState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<VaultLinkDetailUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    init {
        loadLink()
    }

    private fun loadLink() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val link = getVaultLinkByIdUseCase(linkId)
            if (link != null) {
                _state.update { it.copy(link = link, isLoading = false) }
            } else {
                _state.update { it.copy(isLoading = false, error = "Link not found") }
            }
        }
    }

    fun onEvent(event: VaultLinkDetailEvent) {
        when (event) {
            VaultLinkDetailEvent.OnToggleFavorite -> toggleFavorite()
            VaultLinkDetailEvent.OnDelete -> deleteLink()
            VaultLinkDetailEvent.OnRefresh -> loadLink()
        }
    }

    private fun toggleFavorite() {
        val currentLink = _state.value.link ?: return
        val updatedLink = currentLink.copy(
            isFavorite = !currentLink.isFavorite,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            updateVaultLinkUseCase(updatedLink).fold(
                onSuccess = {
                    _state.update { it.copy(link = updatedLink) }
                    val message = if (updatedLink.isFavorite) "Added to favorites" else "Removed from favorites"
                    _uiEvents.emit(VaultLinkDetailUiEvent.ShowMessage(message))
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to toggle favorite")
                    _uiEvents.emit(VaultLinkDetailUiEvent.ShowMessage(error.message ?: "Failed to update"))
                }
            )
        }
    }

    private fun deleteLink() {
        val currentLink = _state.value.link ?: return

        viewModelScope.launch {
            deleteVaultLinkUseCase(currentLink.id).fold(
                onSuccess = {
                    _state.update { it.copy(isDeleted = true) }
                    _uiEvents.emit(VaultLinkDetailUiEvent.LinkDeleted)
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to delete link")
                    _uiEvents.emit(VaultLinkDetailUiEvent.ShowMessage(error.message ?: "Failed to delete"))
                }
            )
        }
    }
}
