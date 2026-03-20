package com.rejowan.linky.domain.usecase.vault

import com.rejowan.linky.domain.repository.VaultRepository
import timber.log.Timber

/**
 * Use case for unlocking the vault with PIN.
 * Also processes any pending vault links after successful unlock.
 */
class UnlockVaultUseCase(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(pin: String): Boolean {
        val unlocked = vaultRepository.unlock(pin)
        if (unlocked) {
            // Process any links that were queued while vault was locked
            val processed = vaultRepository.processPendingVaultLinks()
            if (processed > 0) {
                Timber.d("Processed $processed pending vault links after unlock")
            }
        }
        return unlocked
    }
}
