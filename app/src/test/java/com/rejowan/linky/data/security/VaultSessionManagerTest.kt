package com.rejowan.linky.data.security

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.crypto.SecretKey

class VaultSessionManagerTest {

    private lateinit var cryptoManager: VaultCryptoManager
    private lateinit var sessionManager: VaultSessionManager
    private lateinit var mockKey: SecretKey

    @Before
    fun setUp() {
        cryptoManager = mockk(relaxed = true)
        mockKey = mockk()
        sessionManager = VaultSessionManager(cryptoManager)
    }

    // ============ Unlock Tests ============

    @Test
    fun `unlock returns true when pin is correct`() {
        every { cryptoManager.verifyPin("1234") } returns true
        every { cryptoManager.deriveKeyFromPin("1234") } returns mockKey

        val result = sessionManager.unlock("1234")

        assertTrue(result)
        assertTrue(sessionManager.isUnlocked.value)
    }

    @Test
    fun `unlock returns false when pin is incorrect`() {
        every { cryptoManager.verifyPin("wrong") } returns false

        val result = sessionManager.unlock("wrong")

        assertFalse(result)
        assertFalse(sessionManager.isUnlocked.value)
    }

    @Test
    fun `unlock returns false when key derivation fails`() {
        every { cryptoManager.verifyPin("1234") } returns true
        every { cryptoManager.deriveKeyFromPin("1234") } returns null

        val result = sessionManager.unlock("1234")

        assertFalse(result)
        assertFalse(sessionManager.isUnlocked.value)
    }

    @Test
    fun `unlock updates isUnlocked flow`() = runTest {
        every { cryptoManager.verifyPin("1234") } returns true
        every { cryptoManager.deriveKeyFromPin("1234") } returns mockKey

        sessionManager.isUnlocked.test {
            assertEquals(false, awaitItem()) // Initial state

            sessionManager.unlock("1234")
            assertEquals(true, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ Lock Tests ============

    @Test
    fun `lock clears unlocked state`() {
        every { cryptoManager.verifyPin("1234") } returns true
        every { cryptoManager.deriveKeyFromPin("1234") } returns mockKey

        sessionManager.unlock("1234")
        assertTrue(sessionManager.isUnlocked.value)

        sessionManager.lock()
        assertFalse(sessionManager.isUnlocked.value)
    }

    @Test
    fun `lock clears derived key`() {
        every { cryptoManager.verifyPin("1234") } returns true
        every { cryptoManager.deriveKeyFromPin("1234") } returns mockKey

        sessionManager.unlock("1234")
        assertNotNull(sessionManager.getKey())

        sessionManager.lock()
        assertNull(sessionManager.getKey())
    }

    // ============ getKey Tests ============

    @Test
    fun `getKey returns null when locked`() {
        val result = sessionManager.getKey()
        assertNull(result)
    }

    @Test
    fun `getKey returns key when unlocked`() {
        every { cryptoManager.verifyPin("1234") } returns true
        every { cryptoManager.deriveKeyFromPin("1234") } returns mockKey

        sessionManager.unlock("1234")

        val result = sessionManager.getKey()
        assertEquals(mockKey, result)
    }

    // ============ Auto-lock Tests ============

    @Test
    fun `checkAutoLock returns false when locked`() {
        val result = sessionManager.checkAutoLock()
        assertFalse(result)
    }

    @Test
    fun `checkAutoLock returns false when timeout is NEVER`() {
        every { cryptoManager.verifyPin("1234") } returns true
        every { cryptoManager.deriveKeyFromPin("1234") } returns mockKey

        sessionManager.unlock("1234")
        sessionManager.autoLockTimeoutMs = Long.MAX_VALUE

        val result = sessionManager.checkAutoLock()
        assertFalse(result)
        assertTrue(sessionManager.isUnlocked.value)
    }

    @Test
    fun `checkAutoLock returns false when within timeout`() {
        every { cryptoManager.verifyPin("1234") } returns true
        every { cryptoManager.deriveKeyFromPin("1234") } returns mockKey

        sessionManager.unlock("1234")
        sessionManager.autoLockTimeoutMs = 5 * 60 * 1000L // 5 minutes

        // Activity was just recorded, so should not auto-lock
        val result = sessionManager.checkAutoLock()
        assertFalse(result)
        assertTrue(sessionManager.isUnlocked.value)
    }

    @Test
    fun `checkAutoLock locks when timeout exceeded`() {
        every { cryptoManager.verifyPin("1234") } returns true
        every { cryptoManager.deriveKeyFromPin("1234") } returns mockKey

        sessionManager.unlock("1234")
        sessionManager.autoLockTimeoutMs = 0L // Immediate timeout

        // Wait a tiny bit for the timeout to be exceeded
        Thread.sleep(10)

        val result = sessionManager.checkAutoLock()
        assertTrue(result)
        assertFalse(sessionManager.isUnlocked.value)
    }

    // ============ Encrypt/Decrypt Tests ============

    @Test
    fun `encrypt returns null when locked`() {
        val result = sessionManager.encrypt("test")
        assertNull(result)
    }

    @Test
    fun `encrypt delegates to cryptoManager when unlocked`() {
        val encryptedData = EncryptedData("encrypted", "iv")
        every { cryptoManager.verifyPin("1234") } returns true
        every { cryptoManager.deriveKeyFromPin("1234") } returns mockKey
        every { cryptoManager.encrypt("test", mockKey) } returns encryptedData

        sessionManager.unlock("1234")
        val result = sessionManager.encrypt("test")

        assertEquals(encryptedData, result)
        verify { cryptoManager.encrypt("test", mockKey) }
    }

    @Test
    fun `decrypt returns null when locked`() {
        val encryptedData = EncryptedData("encrypted", "iv")
        val result = sessionManager.decrypt(encryptedData)
        assertNull(result)
    }

    @Test
    fun `decrypt delegates to cryptoManager when unlocked`() {
        val encryptedData = EncryptedData("encrypted", "iv")
        every { cryptoManager.verifyPin("1234") } returns true
        every { cryptoManager.deriveKeyFromPin("1234") } returns mockKey
        every { cryptoManager.decrypt(encryptedData, mockKey) } returns "decrypted"

        sessionManager.unlock("1234")
        val result = sessionManager.decrypt(encryptedData)

        assertEquals("decrypted", result)
        verify { cryptoManager.decrypt(encryptedData, mockKey) }
    }

    // ============ AutoLockTimeout Enum Tests ============

    @Test
    fun `AutoLockTimeout fromMs returns correct value`() {
        assertEquals(AutoLockTimeout.IMMEDIATE, AutoLockTimeout.fromMs(0L))
        assertEquals(AutoLockTimeout.ONE_MINUTE, AutoLockTimeout.fromMs(60_000L))
        assertEquals(AutoLockTimeout.FIVE_MINUTES, AutoLockTimeout.fromMs(5 * 60_000L))
        assertEquals(AutoLockTimeout.FIFTEEN_MINUTES, AutoLockTimeout.fromMs(15 * 60_000L))
        assertEquals(AutoLockTimeout.THIRTY_MINUTES, AutoLockTimeout.fromMs(30 * 60_000L))
        assertEquals(AutoLockTimeout.NEVER, AutoLockTimeout.fromMs(Long.MAX_VALUE))
    }

    @Test
    fun `AutoLockTimeout fromMs returns default for unknown value`() {
        assertEquals(AutoLockTimeout.FIVE_MINUTES, AutoLockTimeout.fromMs(12345L))
    }
}
