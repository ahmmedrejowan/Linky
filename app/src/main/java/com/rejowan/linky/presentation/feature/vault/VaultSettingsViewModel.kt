package com.rejowan.linky.presentation.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.data.security.AutoLockTimeout
import com.rejowan.linky.domain.repository.VaultRepository
import com.rejowan.linky.domain.usecase.vault.ChangeVaultPinUseCase
import com.rejowan.linky.domain.usecase.vault.ClearVaultUseCase
import com.rejowan.linky.domain.usecase.vault.GetAllVaultLinksUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VaultSettingsState(
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.FIVE_MINUTES,
    val showChangePinDialog: Boolean = false,
    val showClearVaultDialog: Boolean = false,
    val isLoading: Boolean = false,
    val vaultLinkCount: Int = 0
)

sealed class VaultSettingsEvent {
    data class OnAutoLockTimeoutChanged(val timeout: AutoLockTimeout) : VaultSettingsEvent()
    data object OnShowChangePinDialog : VaultSettingsEvent()
    data object OnDismissChangePinDialog : VaultSettingsEvent()
    data class OnChangePinConfirm(val oldPin: String, val newPin: String) : VaultSettingsEvent()
    data object OnShowClearVaultDialog : VaultSettingsEvent()
    data object OnDismissClearVaultDialog : VaultSettingsEvent()
    data object OnClearVaultConfirm : VaultSettingsEvent()
}

sealed class VaultSettingsUiEvent {
    data class ShowMessage(val message: String) : VaultSettingsUiEvent()
    data object VaultCleared : VaultSettingsUiEvent()
}

class VaultSettingsViewModel(
    private val vaultRepository: VaultRepository,
    private val changeVaultPinUseCase: ChangeVaultPinUseCase,
    private val clearVaultUseCase: ClearVaultUseCase,
    private val getAllVaultLinksUseCase: GetAllVaultLinksUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(VaultSettingsState())
    val state: StateFlow<VaultSettingsState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<VaultSettingsUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    init {
        loadSettings()
        loadVaultLinkCount()
    }

    private fun loadSettings() {
        _state.update {
            it.copy(autoLockTimeout = vaultRepository.getAutoLockTimeout())
        }
    }

    private fun loadVaultLinkCount() {
        viewModelScope.launch {
            getAllVaultLinksUseCase().collect { links ->
                _state.update { it.copy(vaultLinkCount = links.size) }
            }
        }
    }

    fun onEvent(event: VaultSettingsEvent) {
        when (event) {
            is VaultSettingsEvent.OnAutoLockTimeoutChanged -> {
                vaultRepository.setAutoLockTimeout(event.timeout)
                _state.update { it.copy(autoLockTimeout = event.timeout) }
            }

            VaultSettingsEvent.OnShowChangePinDialog -> {
                _state.update { it.copy(showChangePinDialog = true) }
            }

            VaultSettingsEvent.OnDismissChangePinDialog -> {
                _state.update { it.copy(showChangePinDialog = false) }
            }

            is VaultSettingsEvent.OnChangePinConfirm -> {
                changePin(event.oldPin, event.newPin)
            }

            VaultSettingsEvent.OnShowClearVaultDialog -> {
                _state.update { it.copy(showClearVaultDialog = true) }
            }

            VaultSettingsEvent.OnDismissClearVaultDialog -> {
                _state.update { it.copy(showClearVaultDialog = false) }
            }

            VaultSettingsEvent.OnClearVaultConfirm -> {
                clearVault()
            }
        }
    }

    private fun changePin(oldPin: String, newPin: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            changeVaultPinUseCase(oldPin, newPin).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, showChangePinDialog = false) }
                    _uiEvents.emit(VaultSettingsUiEvent.ShowMessage("PIN changed successfully"))
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false) }
                    _uiEvents.emit(VaultSettingsUiEvent.ShowMessage(error.message ?: "Failed to change PIN"))
                }
            )
        }
    }

    private fun clearVault() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            clearVaultUseCase().fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, showClearVaultDialog = false) }
                    _uiEvents.emit(VaultSettingsUiEvent.VaultCleared)
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false) }
                    _uiEvents.emit(VaultSettingsUiEvent.ShowMessage(error.message ?: "Failed to clear vault"))
                }
            )
        }
    }
}
