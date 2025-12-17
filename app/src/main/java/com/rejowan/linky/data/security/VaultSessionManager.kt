package com.rejowan.linky.data.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.crypto.SecretKey

/**
 * Manages the vault unlock session state.
 * Tracks whether the vault is unlocked and handles auto-lock timeout.
 */
class VaultSessionManager(
    private val cryptoManager: VaultCryptoManager
) {
    private var derivedKey: SecretKey? = null
    private var lastActivityTime: Long = 0L

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // Auto-lock timeout in milliseconds (default: 5 minutes)
    var autoLockTimeoutMs: Long = 5 * 60 * 1000L

    /**
     * Attempt to unlock the vault with the given PIN
     * @param pin The PIN to verify
     * @return true if unlock was successful
     */
    fun unlock(pin: String): Boolean {
        if (!cryptoManager.verifyPin(pin)) {
            return false
        }

        derivedKey = cryptoManager.deriveKeyFromPin(pin)
        if (derivedKey != null) {
            _isUnlocked.value = true
            recordActivity()
            return true
        }
        return false
    }

    /**
     * Lock the vault and clear the derived key
     */
    fun lock() {
        derivedKey = null
        _isUnlocked.value = false
        lastActivityTime = 0L
    }

    /**
     * Get the current encryption key (only available when unlocked)
     * @return The derived encryption key or null if locked
     */
    fun getKey(): SecretKey? {
        checkAutoLock()
        return derivedKey
    }

    /**
     * Record user activity to reset the auto-lock timer
     */
    fun recordActivity() {
        lastActivityTime = System.currentTimeMillis()
    }

    /**
     * Check if auto-lock should trigger based on inactivity
     * @return true if vault was auto-locked
     */
    fun checkAutoLock(): Boolean {
        if (!_isUnlocked.value) return false
        if (autoLockTimeoutMs == Long.MAX_VALUE) return false // Never auto-lock

        val now = System.currentTimeMillis()
        if (lastActivityTime > 0 && (now - lastActivityTime) > autoLockTimeoutMs) {
            lock()
            return true
        }
        return false
    }

    /**
     * Encrypt data using the current session key
     * @param plaintext Data to encrypt
     * @return Encrypted data or null if vault is locked
     */
    fun encrypt(plaintext: String): EncryptedData? {
        val key = getKey() ?: return null
        recordActivity()
        return cryptoManager.encrypt(plaintext, key)
    }

    /**
     * Decrypt data using the current session key
     * @param encryptedData Data to decrypt
     * @return Decrypted string or null if vault is locked or decryption fails
     */
    fun decrypt(encryptedData: EncryptedData): String? {
        val key = getKey() ?: return null
        recordActivity()
        return cryptoManager.decrypt(encryptedData, key)
    }
}

/**
 * Auto-lock timeout options
 */
enum class AutoLockTimeout(val displayName: String, val timeoutMs: Long) {
    IMMEDIATE("Immediately", 0L),
    ONE_MINUTE("1 minute", 60_000L),
    FIVE_MINUTES("5 minutes", 5 * 60_000L),
    FIFTEEN_MINUTES("15 minutes", 15 * 60_000L),
    THIRTY_MINUTES("30 minutes", 30 * 60_000L),
    NEVER("Never", Long.MAX_VALUE);

    companion object {
        fun fromMs(ms: Long): AutoLockTimeout {
            return entries.find { it.timeoutMs == ms } ?: FIVE_MINUTES
        }
    }
}
