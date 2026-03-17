package com.rejowan.linky.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import timber.log.Timber
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import androidx.core.content.edit

/**
 * Manages encryption/decryption for the Vault feature.
 * Uses AES-256-GCM for data encryption and PBKDF2 for PIN-based key derivation.
 *
 * Storage strategy:
 * - PIN hash and salts are stored in regular SharedPreferences (hash is secure, salts are random)
 * - Vault link data is encrypted with PIN-derived keys using AES-GCM
 * - AndroidKeyStore is used for additional encryption of sensitive metadata if needed
 */
class VaultCryptoManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val VAULT_KEY_ALIAS = "linky_vault_key"
        private const val PREFS_NAME = "linky_vault_prefs"

        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_ENCRYPTION_SALT = "encryption_salt"
        private const val KEY_VERIFICATION_TOKEN = "verification_token"

        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val PBKDF2_ITERATIONS = 10000
        private const val SALT_LENGTH = 32
    }

    private val secureRandom = SecureRandom()

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val keystoreKey: SecretKey? by lazy {
        getOrCreateKeystoreKey()
    }

    init {
        // Verify keystore integrity on init - if key was regenerated (reinstall), clear vault data
        verifyKeystoreIntegrity()
    }

    /**
     * Get or create a key in AndroidKeyStore for additional encryption
     */
    private fun getOrCreateKeystoreKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            // Return existing key if available
            keyStore.getKey(VAULT_KEY_ALIAS, null)?.let {
                return it as SecretKey
            }

            // Create new key
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val keySpec = KeyGenParameterSpec.Builder(
                VAULT_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AES_KEY_SIZE)
                .build()

            keyGenerator.init(keySpec)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get/create keystore key")
            null
        }
    }

    /**
     * Verify keystore integrity - detect if key was regenerated after reinstall
     */
    private fun verifyKeystoreIntegrity() {
        val storedToken = prefs.getString(KEY_VERIFICATION_TOKEN, null)

        if (storedToken != null && keystoreKey != null) {
            // Try to decrypt the verification token
            try {
                val parts = storedToken.split(":")
                if (parts.size == 2) {
                    val iv = Base64.decode(parts[0], Base64.NO_WRAP)
                    val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)

                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, keystoreKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
                    cipher.doFinal(ciphertext)
                    // Token decrypted successfully - keystore is intact
                    return
                }
            } catch (_: Exception) {
                // Decryption failed - key was regenerated
                Timber.w("Keystore key regenerated (reinstall detected), clearing vault data")
                clearVault()
            }
        } else if (storedToken == null && keystoreKey != null) {
            // First time setup or after clear - create verification token
            createVerificationToken()
        }
    }

    /**
     * Create a verification token encrypted with keystore key
     */
    private fun createVerificationToken() {
        try {
            val key = keystoreKey ?: return
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv
            val ciphertext = cipher.doFinal("linky_vault_verify".toByteArray())

            val token = "${Base64.encodeToString(iv, Base64.NO_WRAP)}:${Base64.encodeToString(ciphertext, Base64.NO_WRAP)}"
            prefs.edit { putString(KEY_VERIFICATION_TOKEN, token) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create verification token")
        }
    }

    /**
     * Check if PIN is already set up
     */
    fun isPinSetup(): Boolean {
        return prefs.contains(KEY_PIN_HASH)
    }

    /**
     * Setup a new PIN for the vault
     * @param pin The PIN to set (4-6 digits)
     * @return true if setup was successful
     */
    fun setupPin(pin: String): Boolean {
        if (pin.length < 4 || pin.length > 6) return false

        val salt = generateSalt()
        val hash = hashPin(pin, salt)

        // Also generate and store encryption salt
        val encryptionSalt = generateSalt()

        prefs.edit {
            putString(KEY_PIN_HASH, hash)
                .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(
                    KEY_ENCRYPTION_SALT,
                    Base64.encodeToString(encryptionSalt, Base64.NO_WRAP)
                )
        }

        // Create verification token for reinstall detection
        createVerificationToken()

        return true
    }

    /**
     * Verify a PIN against the stored hash
     * @param pin The PIN to verify
     * @return true if PIN matches
     */
    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val saltString = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val salt = Base64.decode(saltString, Base64.NO_WRAP)

        val hash = hashPin(pin, salt)
        return hash == storedHash
    }

    /**
     * Change the vault PIN
     * @param oldPin Current PIN for verification
     * @param newPin New PIN to set
     * @return true if change was successful
     */
    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!verifyPin(oldPin)) return false
        if (newPin.length < 4 || newPin.length > 6) return false

        val newSalt = generateSalt()
        val newHash = hashPin(newPin, newSalt)

        prefs.edit {
            putString(KEY_PIN_HASH, newHash)
                .putString(KEY_PIN_SALT, Base64.encodeToString(newSalt, Base64.NO_WRAP))
        }

        return true
    }

    /**
     * Derive an encryption key from the PIN
     * This key is used to encrypt/decrypt vault data
     * @param pin The PIN to derive key from
     * @return The derived SecretKey or null if derivation fails
     */
    fun deriveKeyFromPin(pin: String): SecretKey? {
        val saltString = prefs.getString(KEY_ENCRYPTION_SALT, null) ?: return null
        val salt = Base64.decode(saltString, Base64.NO_WRAP)

        return try {
            val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_SIZE)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = factory.generateSecret(spec).encoded
            SecretKeySpec(keyBytes, "AES")
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Encrypt data using AES-256-GCM
     * @param plaintext The data to encrypt
     * @param key The encryption key
     * @return EncryptedData containing ciphertext and IV, or null on failure
     */
    fun encrypt(plaintext: String, key: SecretKey): EncryptedData? {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(GCM_IV_LENGTH)
            secureRandom.nextBytes(iv)

            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)

            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            EncryptedData(
                ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
                iv = Base64.encodeToString(iv, Base64.NO_WRAP)
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Decrypt data using AES-256-GCM
     * @param encryptedData The encrypted data with IV
     * @param key The decryption key
     * @return The decrypted plaintext or null on failure
     */
    fun decrypt(encryptedData: EncryptedData, key: SecretKey): String? {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = Base64.decode(encryptedData.iv, Base64.NO_WRAP)
            val ciphertext = Base64.decode(encryptedData.ciphertext, Base64.NO_WRAP)

            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val plaintext = cipher.doFinal(ciphertext)
            String(plaintext, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Clear all vault data (PIN, salts, etc.)
     * WARNING: This will make all encrypted vault links unrecoverable
     */
    fun clearVault() {
        prefs.edit { clear() }
    }

    /**
     * Generate a random salt
     */
    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Hash a PIN using PBKDF2
     */
    private fun hashPin(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
}

/**
 * Container for encrypted data
 */
data class EncryptedData(
    val ciphertext: String,
    val iv: String
)
