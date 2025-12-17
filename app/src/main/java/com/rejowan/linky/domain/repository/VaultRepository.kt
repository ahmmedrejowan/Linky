package com.rejowan.linky.domain.repository

import com.rejowan.linky.data.security.AutoLockTimeout
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.model.VaultLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for vault operations.
 * Handles PIN management, vault links, and session state.
 */
interface VaultRepository {

    // ============ PIN Management ============

    /**
     * Check if PIN has been set up
     */
    fun isPinSetup(): Boolean

    /**
     * Setup a new PIN for the vault
     * @param pin 4-6 digit PIN
     * @return true if setup successful
     */
    suspend fun setupPin(pin: String): Result<Unit>

    /**
     * Verify a PIN
     * @param pin PIN to verify
     * @return true if PIN is correct
     */
    suspend fun verifyPin(pin: String): Boolean

    /**
     * Change the vault PIN
     * @param oldPin Current PIN
     * @param newPin New PIN
     * @return Result indicating success or failure
     */
    suspend fun changePin(oldPin: String, newPin: String): Result<Unit>

    // ============ Session Management ============

    /**
     * Get the current unlock state as a Flow
     */
    val isUnlocked: StateFlow<Boolean>

    /**
     * Unlock the vault with PIN
     * @param pin PIN to unlock with
     * @return true if unlock successful
     */
    suspend fun unlock(pin: String): Boolean

    /**
     * Lock the vault
     */
    fun lock()

    /**
     * Check auto-lock status and lock if timeout exceeded
     * @return true if vault was auto-locked
     */
    fun checkAutoLock(): Boolean

    /**
     * Record user activity to reset auto-lock timer
     */
    fun recordActivity()

    /**
     * Get current auto-lock timeout setting
     */
    fun getAutoLockTimeout(): AutoLockTimeout

    /**
     * Set auto-lock timeout
     */
    fun setAutoLockTimeout(timeout: AutoLockTimeout)

    // ============ Vault Links ============

    /**
     * Get all vault links (decrypted)
     * Returns empty flow if vault is locked
     */
    fun getAllVaultLinks(): Flow<List<VaultLink>>

    /**
     * Get a vault link by ID (decrypted)
     * @return VaultLink or null if not found or vault is locked
     */
    suspend fun getVaultLinkById(id: String): VaultLink?

    /**
     * Add a new link to the vault
     * @param vaultLink The link to add
     * @return Result indicating success or failure
     */
    suspend fun addVaultLink(vaultLink: VaultLink): Result<Unit>

    /**
     * Move an existing link to the vault
     * @param link The regular link to move
     * @return Result containing the new VaultLink or error
     */
    suspend fun moveToVault(link: Link): Result<VaultLink>

    /**
     * Update a vault link
     * @param vaultLink The updated link
     * @return Result indicating success or failure
     */
    suspend fun updateVaultLink(vaultLink: VaultLink): Result<Unit>

    /**
     * Delete a vault link
     * @param id ID of the link to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteVaultLink(id: String): Result<Unit>

    /**
     * Get the count of vault links
     */
    fun getVaultLinkCount(): Flow<Int>

    // ============ Vault Management ============

    /**
     * Clear all vault data (links and PIN)
     * WARNING: This is irreversible
     */
    suspend fun clearVault(): Result<Unit>
}
