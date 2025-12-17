package com.rejowan.linky.domain.usecase.vault

import com.rejowan.linky.domain.model.VaultLink
import com.rejowan.linky.domain.repository.VaultRepository

/**
 * Use case for adding a new link to the vault
 */
class AddVaultLinkUseCase(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(vaultLink: VaultLink): Result<Unit> {
        return vaultRepository.addVaultLink(vaultLink)
    }
}
