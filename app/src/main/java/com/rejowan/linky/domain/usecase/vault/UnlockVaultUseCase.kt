package com.rejowan.linky.domain.usecase.vault

import com.rejowan.linky.domain.repository.VaultRepository

/**
 * Use case for unlocking the vault with PIN
 */
class UnlockVaultUseCase(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(pin: String): Boolean {
        return vaultRepository.unlock(pin)
    }
}
