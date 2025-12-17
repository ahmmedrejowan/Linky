package com.rejowan.linky.domain.usecase.vault

import com.rejowan.linky.domain.repository.VaultRepository

/**
 * Use case for setting up a new vault PIN
 */
class SetupVaultPinUseCase(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(pin: String): Result<Unit> {
        return vaultRepository.setupPin(pin)
    }
}
