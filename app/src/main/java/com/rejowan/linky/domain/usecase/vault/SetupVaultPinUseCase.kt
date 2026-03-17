package com.rejowan.linky.domain.usecase.vault

import com.rejowan.linky.domain.repository.VaultRepository

/**
 * Use case for setting up a new vault PIN.
 * After successful setup, the vault is automatically unlocked.
 */
class SetupVaultPinUseCase(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(pin: String): Result<Unit> {
        val result = vaultRepository.setupPin(pin)
        if (result.isSuccess) {
            // Auto-unlock vault after successful PIN setup
            vaultRepository.unlock(pin)
        }
        return result
    }
}
