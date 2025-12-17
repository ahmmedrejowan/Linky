package com.rejowan.linky.domain.usecase.vault

import com.rejowan.linky.domain.repository.VaultRepository

/**
 * Use case for clearing all vault data
 * WARNING: This is irreversible
 */
class ClearVaultUseCase(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return vaultRepository.clearVault()
    }
}
