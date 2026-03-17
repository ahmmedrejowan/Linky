package com.rejowan.linky.data.repository

import android.content.Context
import com.rejowan.linky.data.local.database.dao.VaultLinkDao
import com.rejowan.linky.data.mapper.VaultLinkMapper
import com.rejowan.linky.data.security.AutoLockTimeout
import com.rejowan.linky.data.security.VaultCryptoManager
import com.rejowan.linky.data.security.VaultSessionManager
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.model.VaultLink
import com.rejowan.linky.domain.repository.VaultRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Implementation of VaultRepository.
 * Handles encryption/decryption transparently.
 */
class VaultRepositoryImpl(
    private val context: Context,
    private val vaultLinkDao: VaultLinkDao,
    private val cryptoManager: VaultCryptoManager,
    private val sessionManager: VaultSessionManager
) : VaultRepository {

    companion object {
        private const val PREFS_NAME = "vault_settings"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    init {
        // Clean up orphaned vault links if PIN was reset (e.g., after reinstall)
        // These links can't be decrypted without the original PIN
        // Using GlobalScope is appropriate here: one-time app startup cleanup
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            cleanupOrphanedVaultLinks()
        }
    }

    private suspend fun cleanupOrphanedVaultLinks() {
        if (!cryptoManager.isPinSetup()) {
            val count = vaultLinkDao.getVaultLinkCountSync()
            if (count > 0) {
                Timber.w("Clearing $count orphaned vault links after key reset")
                vaultLinkDao.deleteAllVaultLinks()
            }
        }
    }

    // ============ PIN Management ============

    override fun isPinSetup(): Boolean {
        return cryptoManager.isPinSetup()
    }

    override suspend fun setupPin(pin: String): Result<Unit> {
        return if (cryptoManager.setupPin(pin)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("PIN must be 4-6 digits"))
        }
    }

    override suspend fun verifyPin(pin: String): Boolean {
        return cryptoManager.verifyPin(pin)
    }

    override suspend fun changePin(oldPin: String, newPin: String): Result<Unit> {
        return if (cryptoManager.changePin(oldPin, newPin)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Invalid current PIN or new PIN format"))
        }
    }

    // ============ Session Management ============

    override val isUnlocked: StateFlow<Boolean>
        get() = sessionManager.isUnlocked

    override suspend fun unlock(pin: String): Boolean {
        return sessionManager.unlock(pin)
    }

    override fun lock() {
        sessionManager.lock()
    }

    override fun checkAutoLock(): Boolean {
        return sessionManager.checkAutoLock()
    }

    override fun recordActivity() {
        sessionManager.recordActivity()
    }

    override fun getAutoLockTimeout(): AutoLockTimeout {
        val ms = prefs.getLong(KEY_AUTO_LOCK_TIMEOUT, AutoLockTimeout.FIVE_MINUTES.timeoutMs)
        return AutoLockTimeout.fromMs(ms)
    }

    override fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        prefs.edit().putLong(KEY_AUTO_LOCK_TIMEOUT, timeout.timeoutMs).apply()
        sessionManager.autoLockTimeoutMs = timeout.timeoutMs
    }

    // ============ Vault Links ============

    override fun getAllVaultLinks(): Flow<List<VaultLink>> {
        return vaultLinkDao.getAllVaultLinks().map { entities ->
            if (!sessionManager.isUnlocked.value) {
                emptyList()
            } else {
                entities.mapNotNull { entity ->
                    VaultLinkMapper.toDomain(entity, sessionManager)
                }
            }
        }
    }

    override suspend fun getVaultLinkById(id: String): VaultLink? {
        if (!sessionManager.isUnlocked.value) return null

        val entity = vaultLinkDao.getVaultLinkById(id) ?: return null
        return VaultLinkMapper.toDomain(entity, sessionManager)
    }

    override suspend fun addVaultLink(vaultLink: VaultLink): Result<Unit> {
        if (!sessionManager.isUnlocked.value) {
            return Result.failure(IllegalStateException("Vault is locked"))
        }

        val entity = VaultLinkMapper.toEntity(vaultLink, sessionManager)
            ?: return Result.failure(IllegalStateException("Encryption failed"))

        vaultLinkDao.insertVaultLink(entity)
        return Result.success(Unit)
    }

    override suspend fun moveToVault(link: Link): Result<VaultLink> {
        if (!sessionManager.isUnlocked.value) {
            return Result.failure(IllegalStateException("Vault is locked"))
        }

        val vaultLink = VaultLinkMapper.fromLink(link)
        val entity = VaultLinkMapper.toEntity(vaultLink, sessionManager)
            ?: return Result.failure(IllegalStateException("Encryption failed"))

        vaultLinkDao.insertVaultLink(entity)
        return Result.success(vaultLink)
    }

    override suspend fun updateVaultLink(vaultLink: VaultLink): Result<Unit> {
        if (!sessionManager.isUnlocked.value) {
            return Result.failure(IllegalStateException("Vault is locked"))
        }

        val entity = VaultLinkMapper.toEntity(vaultLink.copy(updatedAt = System.currentTimeMillis()), sessionManager)
            ?: return Result.failure(IllegalStateException("Encryption failed"))

        vaultLinkDao.updateVaultLink(entity)
        return Result.success(Unit)
    }

    override suspend fun deleteVaultLink(id: String): Result<Unit> {
        if (!sessionManager.isUnlocked.value) {
            return Result.failure(IllegalStateException("Vault is locked"))
        }

        vaultLinkDao.deleteVaultLink(id)
        return Result.success(Unit)
    }

    override fun getVaultLinkCount(): Flow<Int> {
        return vaultLinkDao.getVaultLinkCount()
    }

    // ============ Vault Management ============

    override suspend fun clearVault(): Result<Unit> {
        return try {
            vaultLinkDao.deleteAllVaultLinks()
            cryptoManager.clearVault()
            sessionManager.lock()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
