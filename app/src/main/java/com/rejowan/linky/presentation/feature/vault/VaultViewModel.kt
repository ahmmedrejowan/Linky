package com.rejowan.linky.presentation.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.model.VaultLink
import com.rejowan.linky.domain.repository.VaultRepository
import com.rejowan.linky.domain.usecase.vault.AddVaultLinkUseCase
import com.rejowan.linky.domain.usecase.vault.DeleteVaultLinkUseCase
import com.rejowan.linky.domain.usecase.vault.GetAllVaultLinksUseCase
import com.rejowan.linky.domain.usecase.vault.LockVaultUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VaultState(
    val vaultLinks: List<VaultLink> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val linkToDelete: VaultLink? = null,
    val error: String? = null
)

sealed class VaultEvent {
    data object OnLock : VaultEvent()
    data object OnShowAddDialog : VaultEvent()
    data object OnDismissAddDialog : VaultEvent()
    data class OnAddLink(val url: String, val title: String, val description: String?, val notes: String?) : VaultEvent()
    data class OnShowDeleteConfirm(val link: VaultLink) : VaultEvent()
    data object OnDismissDeleteConfirm : VaultEvent()
    data object OnConfirmDelete : VaultEvent()
}

sealed class VaultUiEvent {
    data object Locked : VaultUiEvent()
    data class ShowMessage(val message: String) : VaultUiEvent()
}

class VaultViewModel(
    private val getAllVaultLinksUseCase: GetAllVaultLinksUseCase,
    private val addVaultLinkUseCase: AddVaultLinkUseCase,
    private val deleteVaultLinkUseCase: DeleteVaultLinkUseCase,
    private val lockVaultUseCase: LockVaultUseCase,
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VaultState())
    val state: StateFlow<VaultState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<VaultUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    init {
        loadVaultLinks()
        observeUnlockState()
    }

    private fun loadVaultLinks() {
        viewModelScope.launch {
            getAllVaultLinksUseCase().collect { links ->
                _state.update { it.copy(vaultLinks = links, isLoading = false) }
            }
        }
    }

    private fun observeUnlockState() {
        viewModelScope.launch {
            vaultRepository.isUnlocked.collect { isUnlocked ->
                if (!isUnlocked) {
                    _uiEvents.emit(VaultUiEvent.Locked)
                }
            }
        }
    }

    fun onEvent(event: VaultEvent) {
        when (event) {
            VaultEvent.OnLock -> lock()
            VaultEvent.OnShowAddDialog -> _state.update { it.copy(showAddDialog = true) }
            VaultEvent.OnDismissAddDialog -> _state.update { it.copy(showAddDialog = false) }
            is VaultEvent.OnAddLink -> addLink(event.url, event.title, event.description, event.notes)
            is VaultEvent.OnShowDeleteConfirm -> _state.update {
                it.copy(showDeleteConfirmDialog = true, linkToDelete = event.link)
            }
            VaultEvent.OnDismissDeleteConfirm -> _state.update {
                it.copy(showDeleteConfirmDialog = false, linkToDelete = null)
            }
            VaultEvent.OnConfirmDelete -> deleteLink()
        }
    }

    private fun lock() {
        lockVaultUseCase()
    }

    private fun addLink(url: String, title: String, description: String?, notes: String?) {
        viewModelScope.launch {
            val vaultLink = VaultLink(
                url = url,
                title = title.ifBlank { url },
                description = description,
                notes = notes
            )

            addVaultLinkUseCase(vaultLink).fold(
                onSuccess = {
                    _state.update { it.copy(showAddDialog = false) }
                    _uiEvents.emit(VaultUiEvent.ShowMessage("Link added to vault"))
                },
                onFailure = { error ->
                    _uiEvents.emit(VaultUiEvent.ShowMessage(error.message ?: "Failed to add link"))
                }
            )
        }
    }

    private fun deleteLink() {
        val link = _state.value.linkToDelete ?: return

        viewModelScope.launch {
            deleteVaultLinkUseCase(link.id).fold(
                onSuccess = {
                    _state.update { it.copy(showDeleteConfirmDialog = false, linkToDelete = null) }
                    _uiEvents.emit(VaultUiEvent.ShowMessage("Link deleted"))
                },
                onFailure = { error ->
                    _uiEvents.emit(VaultUiEvent.ShowMessage(error.message ?: "Failed to delete link"))
                }
            )
        }
    }
}
