package com.rejowan.linky.domain.usecase.vault

import com.rejowan.linky.domain.repository.VaultRepository

/**
 * Use case for locking the vault
 */
class LockVaultUseCase(
    private val vaultRepository: VaultRepository
) {
    operator fun invoke() {
        vaultRepository.lock()
    }
}
