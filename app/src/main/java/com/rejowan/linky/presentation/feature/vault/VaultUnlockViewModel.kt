package com.rejowan.linky.presentation.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.repository.VaultRepository
import com.rejowan.linky.domain.usecase.vault.UnlockVaultUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VaultUnlockState(
    val pin: String = "",
    val isLoading: Boolean = false,
    val failedAttempts: Int = 0,
    val error: String? = null,
    val isLocked: Boolean = false,
    val lockoutEndTime: Long = 0L
)

sealed class VaultUnlockEvent {
    data class OnDigitEntered(val digit: String) : VaultUnlockEvent()
    data object OnBackspace : VaultUnlockEvent()
    data object OnClearError : VaultUnlockEvent()
}

sealed class VaultUnlockUiEvent {
    data object UnlockSuccess : VaultUnlockUiEvent()
    data object NavigateToSetup : VaultUnlockUiEvent()
    data class ShowError(val message: String) : VaultUnlockUiEvent()
}

class VaultUnlockViewModel(
    private val unlockVaultUseCase: UnlockVaultUseCase,
    private val vaultRepository: VaultRepository
) : ViewModel() {

    companion object {
        const val PIN_LENGTH = 4
        const val MAX_ATTEMPTS_BEFORE_LOCKOUT = 3
        private val LOCKOUT_DURATIONS = listOf(30_000L, 60_000L, 300_000L) // 30s, 1m, 5m
    }

    private val _state = MutableStateFlow(VaultUnlockState())
    val state: StateFlow<VaultUnlockState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<VaultUnlockUiEvent>(replay = 1)
    val uiEvents = _uiEvents.asSharedFlow()

    init {
        checkVaultSetup()
    }

    private fun checkVaultSetup() {
        viewModelScope.launch {
            val isPinSetup = vaultRepository.isPinSetup()
            timber.log.Timber.d("VaultUnlockViewModel: isPinSetup=$isPinSetup")
            if (!isPinSetup) {
                timber.log.Timber.d("VaultUnlockViewModel: Emitting NavigateToSetup")
                _uiEvents.emit(VaultUnlockUiEvent.NavigateToSetup)
            }
        }
    }

    fun onEvent(event: VaultUnlockEvent) {
        when (event) {
            is VaultUnlockEvent.OnDigitEntered -> handleDigitEntered(event.digit)
            VaultUnlockEvent.OnBackspace -> handleBackspace()
            VaultUnlockEvent.OnClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun handleDigitEntered(digit: String) {
        val currentState = _state.value

        // Check if locked out
        if (currentState.isLocked && System.currentTimeMillis() < currentState.lockoutEndTime) {
            return
        } else if (currentState.isLocked) {
            // Lockout expired
            _state.update { it.copy(isLocked = false, lockoutEndTime = 0L) }
        }

        if (currentState.pin.length < PIN_LENGTH) {
            val newPin = currentState.pin + digit
            _state.update { it.copy(pin = newPin, error = null) }

            // Auto-verify when PIN is complete
            if (newPin.length == PIN_LENGTH) {
                verifyPin(newPin)
            }
        }
    }

    private fun handleBackspace() {
        val currentState = _state.value
        if (currentState.pin.isNotEmpty() && !currentState.isLoading) {
            _state.update { it.copy(pin = currentState.pin.dropLast(1)) }
        }
    }

    private fun verifyPin(pin: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Small delay for UX
            delay(200)

            val success = unlockVaultUseCase(pin)

            if (success) {
                _state.update { it.copy(isLoading = false, failedAttempts = 0) }
                _uiEvents.emit(VaultUnlockUiEvent.UnlockSuccess)
            } else {
                val newFailedAttempts = _state.value.failedAttempts + 1

                // Check if should lockout
                if (newFailedAttempts >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
                    val lockoutIndex = minOf(
                        (newFailedAttempts - MAX_ATTEMPTS_BEFORE_LOCKOUT),
                        LOCKOUT_DURATIONS.size - 1
                    )
                    val lockoutDuration = LOCKOUT_DURATIONS[lockoutIndex]
                    val lockoutEndTime = System.currentTimeMillis() + lockoutDuration

                    _state.update {
                        it.copy(
                            pin = "",
                            isLoading = false,
                            failedAttempts = newFailedAttempts,
                            error = "Too many attempts. Try again in ${lockoutDuration / 1000}s",
                            isLocked = true,
                            lockoutEndTime = lockoutEndTime
                        )
                    }

                    // Start countdown to unlock
                    startLockoutCountdown(lockoutDuration)
                } else {
                    _state.update {
                        it.copy(
                            pin = "",
                            isLoading = false,
                            failedAttempts = newFailedAttempts,
                            error = "Incorrect PIN. ${MAX_ATTEMPTS_BEFORE_LOCKOUT - newFailedAttempts} attempts remaining."
                        )
                    }
                }
            }
        }
    }

    private fun startLockoutCountdown(duration: Long) {
        viewModelScope.launch {
            delay(duration)
            _state.update { it.copy(isLocked = false, lockoutEndTime = 0L, error = null) }
        }
    }
}
