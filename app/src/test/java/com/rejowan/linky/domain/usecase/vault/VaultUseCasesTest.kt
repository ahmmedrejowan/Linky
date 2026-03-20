package com.rejowan.linky.domain.usecase.vault

import com.rejowan.linky.domain.model.VaultLink
import com.rejowan.linky.domain.repository.VaultRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VaultUseCasesTest {

    private lateinit var vaultRepository: VaultRepository

    private lateinit var setupVaultPinUseCase: SetupVaultPinUseCase
    private lateinit var unlockVaultUseCase: UnlockVaultUseCase
    private lateinit var lockVaultUseCase: LockVaultUseCase
    private lateinit var getAllVaultLinksUseCase: GetAllVaultLinksUseCase
    private lateinit var getVaultLinkByIdUseCase: GetVaultLinkByIdUseCase
    private lateinit var addVaultLinkUseCase: AddVaultLinkUseCase
    private lateinit var updateVaultLinkUseCase: UpdateVaultLinkUseCase
    private lateinit var deleteVaultLinkUseCase: DeleteVaultLinkUseCase
    private lateinit var changeVaultPinUseCase: ChangeVaultPinUseCase
    private lateinit var clearVaultUseCase: ClearVaultUseCase

    private val testVaultLink = VaultLink(
        id = "vault-link-1",
        url = "https://secret.example.com",
        title = "Secret Link",
        description = "A private link",
        notes = "Some notes",
        createdAt = 1000L,
        updatedAt = 2000L
    )

    private val testVaultLink2 = VaultLink(
        id = "vault-link-2",
        url = "https://private.example.com",
        title = "Private Link",
        description = "Another private link",
        notes = null,
        createdAt = 3000L,
        updatedAt = 4000L
    )

    @Before
    fun setUp() {
        vaultRepository = mockk()

        setupVaultPinUseCase = SetupVaultPinUseCase(vaultRepository)
        unlockVaultUseCase = UnlockVaultUseCase(vaultRepository)
        lockVaultUseCase = LockVaultUseCase(vaultRepository)
        getAllVaultLinksUseCase = GetAllVaultLinksUseCase(vaultRepository)
        getVaultLinkByIdUseCase = GetVaultLinkByIdUseCase(vaultRepository)
        addVaultLinkUseCase = AddVaultLinkUseCase(vaultRepository)
        updateVaultLinkUseCase = UpdateVaultLinkUseCase(vaultRepository)
        deleteVaultLinkUseCase = DeleteVaultLinkUseCase(vaultRepository)
        changeVaultPinUseCase = ChangeVaultPinUseCase(vaultRepository)
        clearVaultUseCase = ClearVaultUseCase(vaultRepository)
    }

    // SetupVaultPinUseCase Tests
    @Test
    fun `setupVaultPin succeeds with valid pin`() = runTest {
        val pin = "1234"
        coEvery { vaultRepository.setupPin(pin) } returns Result.success(Unit)
        coEvery { vaultRepository.unlock(pin) } returns true

        val result = setupVaultPinUseCase(pin)

        assertTrue(result.isSuccess)
        coVerify { vaultRepository.setupPin(pin) }
    }

    @Test
    fun `setupVaultPin fails when repository fails`() = runTest {
        val pin = "1234"
        coEvery { vaultRepository.setupPin(pin) } returns Result.failure(Exception("Setup failed"))

        val result = setupVaultPinUseCase(pin)

        assertTrue(result.isFailure)
    }

    @Test
    fun `setupVaultPin with 6 digit pin`() = runTest {
        val pin = "123456"
        coEvery { vaultRepository.setupPin(pin) } returns Result.success(Unit)
        coEvery { vaultRepository.unlock(pin) } returns true

        val result = setupVaultPinUseCase(pin)

        assertTrue(result.isSuccess)
    }

    // UnlockVaultUseCase Tests
    @Test
    fun `unlockVault returns true when pin is correct`() = runTest {
        val pin = "1234"
        coEvery { vaultRepository.unlock(pin) } returns true
        coEvery { vaultRepository.processPendingVaultLinks() } returns 0

        val result = unlockVaultUseCase(pin)

        assertTrue(result)
        coVerify { vaultRepository.unlock(pin) }
    }

    @Test
    fun `unlockVault returns false when pin is incorrect`() = runTest {
        val pin = "wrong"
        coEvery { vaultRepository.unlock(pin) } returns false

        val result = unlockVaultUseCase(pin)

        assertFalse(result)
    }

    // LockVaultUseCase Tests
    @Test
    fun `lockVault calls repository lock`() {
        every { vaultRepository.lock() } just runs

        lockVaultUseCase()

        verify { vaultRepository.lock() }
    }

    // GetAllVaultLinksUseCase Tests
    @Test
    fun `getAllVaultLinks returns all vault links`() = runTest {
        val vaultLinks = listOf(testVaultLink, testVaultLink2)
        every { vaultRepository.getAllVaultLinks() } returns flowOf(vaultLinks)

        val result = getAllVaultLinksUseCase().first()

        assertEquals(2, result.size)
        assertEquals(testVaultLink, result[0])
        assertEquals(testVaultLink2, result[1])
    }

    @Test
    fun `getAllVaultLinks returns empty list when no links`() = runTest {
        every { vaultRepository.getAllVaultLinks() } returns flowOf(emptyList())

        val result = getAllVaultLinksUseCase().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllVaultLinks returns empty list when vault is locked`() = runTest {
        every { vaultRepository.getAllVaultLinks() } returns flowOf(emptyList())

        val result = getAllVaultLinksUseCase().first()

        assertTrue(result.isEmpty())
    }

    // GetVaultLinkByIdUseCase Tests
    @Test
    fun `getVaultLinkById returns link when found`() = runTest {
        coEvery { vaultRepository.getVaultLinkById("vault-link-1") } returns testVaultLink

        val result = getVaultLinkByIdUseCase("vault-link-1")

        assertNotNull(result)
        assertEquals(testVaultLink, result)
    }

    @Test
    fun `getVaultLinkById returns null when not found`() = runTest {
        coEvery { vaultRepository.getVaultLinkById("non-existent") } returns null

        val result = getVaultLinkByIdUseCase("non-existent")

        assertNull(result)
    }

    @Test
    fun `getVaultLinkById returns null when vault is locked`() = runTest {
        coEvery { vaultRepository.getVaultLinkById(any()) } returns null

        val result = getVaultLinkByIdUseCase("vault-link-1")

        assertNull(result)
    }

    // AddVaultLinkUseCase Tests
    @Test
    fun `addVaultLink succeeds`() = runTest {
        coEvery { vaultRepository.addVaultLink(testVaultLink) } returns Result.success(Unit)

        val result = addVaultLinkUseCase(testVaultLink)

        assertTrue(result.isSuccess)
        coVerify { vaultRepository.addVaultLink(testVaultLink) }
    }

    @Test
    fun `addVaultLink fails when repository fails`() = runTest {
        coEvery { vaultRepository.addVaultLink(testVaultLink) } returns Result.failure(Exception("Add failed"))

        val result = addVaultLinkUseCase(testVaultLink)

        assertTrue(result.isFailure)
    }

    // UpdateVaultLinkUseCase Tests
    @Test
    fun `updateVaultLink succeeds`() = runTest {
        val updatedLink = testVaultLink.copy(title = "Updated Title")
        coEvery { vaultRepository.updateVaultLink(updatedLink) } returns Result.success(Unit)

        val result = updateVaultLinkUseCase(updatedLink)

        assertTrue(result.isSuccess)
        coVerify { vaultRepository.updateVaultLink(updatedLink) }
    }

    @Test
    fun `updateVaultLink fails when repository fails`() = runTest {
        coEvery { vaultRepository.updateVaultLink(testVaultLink) } returns Result.failure(Exception("Update failed"))

        val result = updateVaultLinkUseCase(testVaultLink)

        assertTrue(result.isFailure)
    }

    // DeleteVaultLinkUseCase Tests
    @Test
    fun `deleteVaultLink succeeds`() = runTest {
        coEvery { vaultRepository.deleteVaultLink("vault-link-1") } returns Result.success(Unit)

        val result = deleteVaultLinkUseCase("vault-link-1")

        assertTrue(result.isSuccess)
        coVerify { vaultRepository.deleteVaultLink("vault-link-1") }
    }

    @Test
    fun `deleteVaultLink fails when repository fails`() = runTest {
        coEvery { vaultRepository.deleteVaultLink("vault-link-1") } returns Result.failure(Exception("Delete failed"))

        val result = deleteVaultLinkUseCase("vault-link-1")

        assertTrue(result.isFailure)
    }

    // ChangeVaultPinUseCase Tests
    @Test
    fun `changeVaultPin succeeds with correct old pin`() = runTest {
        coEvery { vaultRepository.changePin("1234", "5678") } returns Result.success(Unit)

        val result = changeVaultPinUseCase("1234", "5678")

        assertTrue(result.isSuccess)
        coVerify { vaultRepository.changePin("1234", "5678") }
    }

    @Test
    fun `changeVaultPin fails with incorrect old pin`() = runTest {
        coEvery { vaultRepository.changePin("wrong", "5678") } returns Result.failure(Exception("Invalid PIN"))

        val result = changeVaultPinUseCase("wrong", "5678")

        assertTrue(result.isFailure)
    }

    // ClearVaultUseCase Tests
    @Test
    fun `clearVault succeeds`() = runTest {
        coEvery { vaultRepository.clearVault() } returns Result.success(Unit)

        val result = clearVaultUseCase()

        assertTrue(result.isSuccess)
        coVerify { vaultRepository.clearVault() }
    }

    @Test
    fun `clearVault fails when repository fails`() = runTest {
        coEvery { vaultRepository.clearVault() } returns Result.failure(Exception("Clear failed"))

        val result = clearVaultUseCase()

        assertTrue(result.isFailure)
    }
}
