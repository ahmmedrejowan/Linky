package com.rejowan.linky.domain.usecase.vault

import com.rejowan.linky.domain.model.VaultLink
import com.rejowan.linky.domain.repository.VaultRepository

/**
 * Use case for updating a vault link
 */
class UpdateVaultLinkUseCase(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(vaultLink: VaultLink): Result<Unit> {
        return vaultRepository.updateVaultLink(vaultLink)
    }
}
