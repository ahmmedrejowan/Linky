package com.rejowan.linky.presentation.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.usecase.vault.SetupVaultPinUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VaultSetupState(
    val step: SetupStep = SetupStep.CREATE_PIN,
    val pin: String = "",
    val confirmPin: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class SetupStep {
    CREATE_PIN,
    CONFIRM_PIN
}

sealed class VaultSetupEvent {
    data class OnDigitEntered(val digit: String) : VaultSetupEvent()
    data object OnBackspace : VaultSetupEvent()
    data object OnClearError : VaultSetupEvent()
}

sealed class VaultSetupUiEvent {
    data object SetupComplete : VaultSetupUiEvent()
    data class ShowError(val message: String) : VaultSetupUiEvent()
}

class VaultSetupViewModel(
    private val setupVaultPinUseCase: SetupVaultPinUseCase
) : ViewModel() {

    companion object {
        const val PIN_LENGTH = 4
    }

    private val _state = MutableStateFlow(VaultSetupState())
    val state: StateFlow<VaultSetupState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<VaultSetupUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    fun onEvent(event: VaultSetupEvent) {
        when (event) {
            is VaultSetupEvent.OnDigitEntered -> handleDigitEntered(event.digit)
            VaultSetupEvent.OnBackspace -> handleBackspace()
            VaultSetupEvent.OnClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun handleDigitEntered(digit: String) {
        val currentState = _state.value

        when (currentState.step) {
            SetupStep.CREATE_PIN -> {
                if (currentState.pin.length < PIN_LENGTH) {
                    val newPin = currentState.pin + digit
                    _state.update { it.copy(pin = newPin, error = null) }

                    // Auto-advance to confirm step when PIN is complete
                    if (newPin.length == PIN_LENGTH) {
                        _state.update { it.copy(step = SetupStep.CONFIRM_PIN) }
                    }
                }
            }

            SetupStep.CONFIRM_PIN -> {
                if (currentState.confirmPin.length < PIN_LENGTH) {
                    val newConfirmPin = currentState.confirmPin + digit
                    _state.update { it.copy(confirmPin = newConfirmPin, error = null) }

                    // Verify when confirm PIN is complete
                    if (newConfirmPin.length == PIN_LENGTH) {
                        verifyAndSetupPin(currentState.pin, newConfirmPin)
                    }
                }
            }
        }
    }

    private fun handleBackspace() {
        val currentState = _state.value

        when (currentState.step) {
            SetupStep.CREATE_PIN -> {
                if (currentState.pin.isNotEmpty()) {
                    _state.update { it.copy(pin = currentState.pin.dropLast(1)) }
                }
            }

            SetupStep.CONFIRM_PIN -> {
                if (currentState.confirmPin.isNotEmpty()) {
                    _state.update { it.copy(confirmPin = currentState.confirmPin.dropLast(1)) }
                } else {
                    // Go back to create step if confirm is empty
                    _state.update { it.copy(step = SetupStep.CREATE_PIN, pin = "") }
                }
            }
        }
    }

    private fun verifyAndSetupPin(pin: String, confirmPin: String) {
        if (pin != confirmPin) {
            _state.update {
                it.copy(
                    confirmPin = "",
                    error = "PINs don't match. Try again."
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val result = setupVaultPinUseCase(pin)

            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _uiEvents.emit(VaultSetupUiEvent.SetupComplete)
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            confirmPin = "",
                            error = error.message ?: "Failed to setup PIN"
                        )
                    }
                }
            )
        }
    }
}
