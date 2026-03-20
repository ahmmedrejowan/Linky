package com.rejowan.linky.data.repository

import android.content.Context
import com.rejowan.linky.data.local.database.dao.PendingVaultLinkDao
import com.rejowan.linky.data.local.database.dao.VaultLinkDao
import com.rejowan.linky.data.local.database.entity.PendingVaultLinkEntity
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
    private val pendingVaultLinkDao: PendingVaultLinkDao,
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
            val vaultCount = vaultLinkDao.getVaultLinkCountSync()
            val pendingCount = pendingVaultLinkDao.getPendingLinkCount()
            if (vaultCount > 0 || pendingCount > 0) {
                Timber.w("Clearing $vaultCount orphaned vault links and $pendingCount pending links after key reset")
                vaultLinkDao.deleteAllVaultLinks()
                pendingVaultLinkDao.deleteAllPendingLinks()
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

    // ============ Queue Operations ============

    override suspend fun queueForVault(link: Link): Result<Unit> {
        return try {
            val pendingEntity = PendingVaultLinkEntity(
                id = link.id,
                url = link.url,
                title = link.title,
                description = link.description,
                notes = link.note,
                createdAt = link.createdAt
            )
            pendingVaultLinkDao.insertPendingLink(pendingEntity)
            Timber.d("Link queued for vault: ${link.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to queue link for vault")
            Result.failure(e)
        }
    }

    override suspend fun processPendingVaultLinks(): Int {
        if (!sessionManager.isUnlocked.value) {
            Timber.w("Cannot process pending vault links - vault is locked")
            return 0
        }

        val pendingLinks = pendingVaultLinkDao.getAllPendingLinks()
        if (pendingLinks.isEmpty()) {
            return 0
        }

        Timber.d("Processing ${pendingLinks.size} pending vault links")
        var processedCount = 0

        pendingLinks.forEach { pending ->
            try {
                // Create VaultLink from pending data
                val vaultLink = VaultLink(
                    id = pending.id,
                    url = pending.url,
                    title = pending.title,
                    description = pending.description,
                    notes = pending.notes,
                    createdAt = pending.createdAt,
                    updatedAt = System.currentTimeMillis()
                )

                // Encrypt and insert
                val entity = VaultLinkMapper.toEntity(vaultLink, sessionManager)
                if (entity != null) {
                    vaultLinkDao.insertVaultLink(entity)
                    pendingVaultLinkDao.deletePendingLink(pending.id)
                    processedCount++
                    Timber.d("Processed pending vault link: ${pending.id}")
                } else {
                    Timber.e("Failed to encrypt pending vault link: ${pending.id}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing pending vault link: ${pending.id}")
            }
        }

        Timber.d("Processed $processedCount/${pendingLinks.size} pending vault links")
        return processedCount
    }

    // ============ Vault Management ============

    override suspend fun clearVault(): Result<Unit> {
        return try {
            vaultLinkDao.deleteAllVaultLinks()
            pendingVaultLinkDao.deleteAllPendingLinks()
            cryptoManager.clearVault()
            sessionManager.lock()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
