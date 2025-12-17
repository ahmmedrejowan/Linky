package com.rejowan.linky.domain.usecase.vault

import com.rejowan.linky.domain.model.VaultLink
import com.rejowan.linky.domain.repository.VaultRepository

/**
 * Use case for getting a vault link by ID
 */
class GetVaultLinkByIdUseCase(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(id: String): VaultLink? {
        return vaultRepository.getVaultLinkById(id)
    }
}
