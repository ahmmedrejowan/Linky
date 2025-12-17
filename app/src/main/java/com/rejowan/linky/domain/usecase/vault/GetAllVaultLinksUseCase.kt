package com.rejowan.linky.domain.usecase.vault

import com.rejowan.linky.domain.model.VaultLink
import com.rejowan.linky.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for getting all vault links
 */
class GetAllVaultLinksUseCase(
    private val vaultRepository: VaultRepository
) {
    operator fun invoke(): Flow<List<VaultLink>> {
        return vaultRepository.getAllVaultLinks()
    }
}
