package com.rejowan.linky.domain.usecase.vault

import com.rejowan.linky.domain.repository.VaultRepository

/**
 * Use case for deleting a vault link
 */
class DeleteVaultLinkUseCase(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return vaultRepository.deleteVaultLink(id)
    }
}
