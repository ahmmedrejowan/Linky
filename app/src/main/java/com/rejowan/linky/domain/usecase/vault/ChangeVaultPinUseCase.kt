package com.rejowan.linky.domain.usecase.vault

import com.rejowan.linky.domain.repository.VaultRepository

/**
 * Use case for changing the vault PIN
 */
class ChangeVaultPinUseCase(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(oldPin: String, newPin: String): Result<Unit> {
        return vaultRepository.changePin(oldPin, newPin)
    }
}
