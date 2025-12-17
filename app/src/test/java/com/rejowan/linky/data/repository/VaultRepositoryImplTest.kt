package com.rejowan.linky.data.repository

import android.content.Context
import android.content.SharedPreferences
import app.cash.turbine.test
import com.rejowan.linky.data.local.database.dao.VaultLinkDao
import com.rejowan.linky.data.local.database.entity.VaultLinkEntity
import com.rejowan.linky.data.security.AutoLockTimeout
import com.rejowan.linky.data.security.VaultCryptoManager
import com.rejowan.linky.data.security.VaultSessionManager
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.model.VaultLink
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class VaultRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var vaultLinkDao: VaultLinkDao
    private lateinit var cryptoManager: VaultCryptoManager
    private lateinit var sessionManager: VaultSessionManager
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var repository: VaultRepositoryImpl

    private val isUnlockedFlow = MutableStateFlow(false)

    private val testVaultLinkEntity = VaultLinkEntity(
        id = "vault-link-1",
        encryptedData = "encrypted-json-data",
        iv = "initialization-vector",
        createdAt = 1000L,
        updatedAt = 2000L
    )

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        vaultLinkDao = mockk(relaxed = true)
        cryptoManager = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.getLong(any(), any()) } returns AutoLockTimeout.FIVE_MINUTES.timeoutMs
        every { sessionManager.isUnlocked } returns isUnlockedFlow

        repository = VaultRepositoryImpl(context, vaultLinkDao, cryptoManager, sessionManager)
    }

    // ============ PIN Management Tests ============

    @Test
    fun `isPinSetup returns true when pin is setup`() {
        every { cryptoManager.isPinSetup() } returns true

        val result = repository.isPinSetup()

        assertTrue(result)
        verify { cryptoManager.isPinSetup() }
    }

    @Test
    fun `isPinSetup returns false when pin is not setup`() {
        every { cryptoManager.isPinSetup() } returns false

        val result = repository.isPinSetup()

        assertFalse(result)
    }

    @Test
    fun `setupPin returns success when valid pin`() = runTest {
        every { cryptoManager.setupPin("1234") } returns true

        val result = repository.setupPin("1234")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `setupPin returns failure when invalid pin`() = runTest {
        every { cryptoManager.setupPin("123") } returns false

        val result = repository.setupPin("123")

        assertTrue(result.isFailure)
    }

    @Test
    fun `verifyPin returns true when correct`() = runTest {
        coEvery { cryptoManager.verifyPin("1234") } returns true

        val result = repository.verifyPin("1234")

        assertTrue(result)
    }

    @Test
    fun `verifyPin returns false when incorrect`() = runTest {
        coEvery { cryptoManager.verifyPin("wrong") } returns false

        val result = repository.verifyPin("wrong")

        assertFalse(result)
    }

    @Test
    fun `changePin returns success when valid`() = runTest {
        every { cryptoManager.changePin("1234", "5678") } returns true

        val result = repository.changePin("1234", "5678")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `changePin returns failure when invalid`() = runTest {
        every { cryptoManager.changePin("wrong", "5678") } returns false

        val result = repository.changePin("wrong", "5678")

        assertTrue(result.isFailure)
    }

    // ============ Session Management Tests ============

    @Test
    fun `isUnlocked returns session state`() = runTest {
        isUnlockedFlow.value = true

        repository.isUnlocked.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unlock returns true when successful`() = runTest {
        coEvery { sessionManager.unlock("1234") } returns true

        val result = repository.unlock("1234")

        assertTrue(result)
        coVerify { sessionManager.unlock("1234") }
    }

    @Test
    fun `unlock returns false when failed`() = runTest {
        coEvery { sessionManager.unlock("wrong") } returns false

        val result = repository.unlock("wrong")

        assertFalse(result)
    }

    @Test
    fun `lock calls session manager`() {
        repository.lock()

        verify { sessionManager.lock() }
    }

    @Test
    fun `checkAutoLock returns session state`() {
        every { sessionManager.checkAutoLock() } returns true

        val result = repository.checkAutoLock()

        assertTrue(result)
        verify { sessionManager.checkAutoLock() }
    }

    @Test
    fun `recordActivity calls session manager`() {
        repository.recordActivity()

        verify { sessionManager.recordActivity() }
    }

    @Test
    fun `getAutoLockTimeout returns saved timeout`() {
        every { sharedPreferences.getLong(any(), any()) } returns AutoLockTimeout.FIFTEEN_MINUTES.timeoutMs

        val result = repository.getAutoLockTimeout()

        assertEquals(AutoLockTimeout.FIFTEEN_MINUTES, result)
    }

    @Test
    fun `setAutoLockTimeout saves and updates session`() {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { sharedPreferences.edit() } returns editor
        every { editor.putLong(any(), any()) } returns editor

        repository.setAutoLockTimeout(AutoLockTimeout.THIRTY_MINUTES)

        verify { editor.putLong(any(), AutoLockTimeout.THIRTY_MINUTES.timeoutMs) }
        verify { sessionManager.autoLockTimeoutMs = AutoLockTimeout.THIRTY_MINUTES.timeoutMs }
    }

    // ============ Vault Links Tests ============

    @Test
    fun `getAllVaultLinks returns empty when locked`() = runTest {
        isUnlockedFlow.value = false
        every { vaultLinkDao.getAllVaultLinks() } returns flowOf(listOf(testVaultLinkEntity))

        repository.getAllVaultLinks().test {
            val result = awaitItem()
            assertTrue(result.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getVaultLinkById returns null when locked`() = runTest {
        isUnlockedFlow.value = false

        val result = repository.getVaultLinkById("vault-link-1")

        assertNull(result)
    }

    @Test
    fun `addVaultLink returns failure when locked`() = runTest {
        isUnlockedFlow.value = false
        val vaultLink = VaultLink(
            id = "vault-link-1",
            title = "Test",
            url = "https://example.com",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val result = repository.addVaultLink(vaultLink)

        assertTrue(result.isFailure)
    }

    @Test
    fun `moveToVault returns failure when locked`() = runTest {
        isUnlockedFlow.value = false
        val link = Link(
            id = "link-1",
            title = "Test",
            url = "https://example.com"
        )

        val result = repository.moveToVault(link)

        assertTrue(result.isFailure)
    }

    @Test
    fun `updateVaultLink returns failure when locked`() = runTest {
        isUnlockedFlow.value = false
        val vaultLink = VaultLink(
            id = "vault-link-1",
            title = "Test",
            url = "https://example.com",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val result = repository.updateVaultLink(vaultLink)

        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteVaultLink returns failure when locked`() = runTest {
        isUnlockedFlow.value = false

        val result = repository.deleteVaultLink("vault-link-1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getVaultLinkCount returns count`() = runTest {
        every { vaultLinkDao.getVaultLinkCount() } returns flowOf(5)

        repository.getVaultLinkCount().test {
            assertEquals(5, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ Vault Management Tests ============

    @Test
    fun `clearVault clears all data`() = runTest {
        coEvery { vaultLinkDao.deleteAllVaultLinks() } just Runs
        every { cryptoManager.clearVault() } just Runs
        every { sessionManager.lock() } just Runs

        val result = repository.clearVault()

        assertTrue(result.isSuccess)
        coVerify { vaultLinkDao.deleteAllVaultLinks() }
        verify { cryptoManager.clearVault() }
        verify { sessionManager.lock() }
    }

    @Test
    fun `clearVault returns failure on error`() = runTest {
        coEvery { vaultLinkDao.deleteAllVaultLinks() } throws RuntimeException("Error")

        val result = repository.clearVault()

        assertTrue(result.isFailure)
    }
}
