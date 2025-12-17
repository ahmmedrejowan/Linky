package com.rejowan.linky.presentation.viewmodel

import app.cash.turbine.test
import com.rejowan.linky.domain.model.VaultLink
import com.rejowan.linky.domain.repository.VaultRepository
import com.rejowan.linky.domain.usecase.vault.AddVaultLinkUseCase
import com.rejowan.linky.domain.usecase.vault.DeleteVaultLinkUseCase
import com.rejowan.linky.domain.usecase.vault.GetAllVaultLinksUseCase
import com.rejowan.linky.domain.usecase.vault.LockVaultUseCase
import com.rejowan.linky.presentation.feature.vault.VaultEvent
import com.rejowan.linky.presentation.feature.vault.VaultUiEvent
import com.rejowan.linky.presentation.feature.vault.VaultViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultViewModelTest {

    private lateinit var viewModel: VaultViewModel
    private lateinit var getAllVaultLinksUseCase: GetAllVaultLinksUseCase
    private lateinit var addVaultLinkUseCase: AddVaultLinkUseCase
    private lateinit var deleteVaultLinkUseCase: DeleteVaultLinkUseCase
    private lateinit var lockVaultUseCase: LockVaultUseCase
    private lateinit var vaultRepository: VaultRepository

    private val testDispatcher = StandardTestDispatcher()
    private val isUnlockedFlow = MutableStateFlow(true)

    private val testVaultLink = VaultLink(
        id = "vault-link-1",
        url = "https://secret.example.com",
        title = "Secret Link",
        description = "A private link",
        notes = "Some notes",
        createdAt = 1000L,
        updatedAt = 2000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        getAllVaultLinksUseCase = mockk()
        addVaultLinkUseCase = mockk()
        deleteVaultLinkUseCase = mockk()
        lockVaultUseCase = mockk()
        vaultRepository = mockk()

        every { getAllVaultLinksUseCase() } returns flowOf(listOf(testVaultLink))
        every { vaultRepository.isUnlocked } returns isUnlockedFlow
        every { lockVaultUseCase() } just runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = VaultViewModel(
            getAllVaultLinksUseCase,
            addVaultLinkUseCase,
            deleteVaultLinkUseCase,
            lockVaultUseCase,
            vaultRepository
        )
    }

    @Test
    fun `initial state loads vault links`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.vaultLinks.size)
            assertEquals(testVaultLink, state.vaultLinks[0])
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `OnLock locks the vault`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultEvent.OnLock)

        verify { lockVaultUseCase() }
    }

    @Test
    fun `OnShowAddDialog shows add dialog`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultEvent.OnShowAddDialog)

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.showAddDialog)
        }
    }

    @Test
    fun `OnDismissAddDialog hides add dialog`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultEvent.OnShowAddDialog)
        viewModel.onEvent(VaultEvent.OnDismissAddDialog)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showAddDialog)
        }
    }

    @Test
    fun `OnAddLink adds link and emits success message`() = runTest {
        coEvery { addVaultLinkUseCase(any()) } returns Result.success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultEvent.OnShowAddDialog)

        viewModel.uiEvents.test {
            viewModel.onEvent(VaultEvent.OnAddLink("https://test.com", "Test", "Desc", "Notes"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is VaultUiEvent.ShowMessage)
            assertEquals("Link added to vault", (event as VaultUiEvent.ShowMessage).message)
        }
    }

    @Test
    fun `OnAddLink with failure emits error message`() = runTest {
        coEvery { addVaultLinkUseCase(any()) } returns Result.failure(Exception("Add failed"))

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(VaultEvent.OnAddLink("https://test.com", "Test", null, null))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is VaultUiEvent.ShowMessage)
            assertTrue((event as VaultUiEvent.ShowMessage).message.contains("failed") || event.message.contains("Add failed"))
        }
    }

    @Test
    fun `OnShowDeleteConfirm shows delete dialog`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultEvent.OnShowDeleteConfirm(testVaultLink))

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.showDeleteConfirmDialog)
            assertEquals(testVaultLink, state.linkToDelete)
        }
    }

    @Test
    fun `OnDismissDeleteConfirm hides delete dialog`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultEvent.OnShowDeleteConfirm(testVaultLink))
        viewModel.onEvent(VaultEvent.OnDismissDeleteConfirm)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showDeleteConfirmDialog)
            assertNull(state.linkToDelete)
        }
    }

    @Test
    fun `OnConfirmDelete deletes link and emits success`() = runTest {
        coEvery { deleteVaultLinkUseCase(testVaultLink.id) } returns Result.success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultEvent.OnShowDeleteConfirm(testVaultLink))

        viewModel.uiEvents.test {
            viewModel.onEvent(VaultEvent.OnConfirmDelete)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is VaultUiEvent.ShowMessage)
            assertEquals("Link deleted", (event as VaultUiEvent.ShowMessage).message)
        }
    }

    @Test
    fun `emits Locked event when vault becomes locked`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            isUnlockedFlow.value = false
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is VaultUiEvent.Locked)
        }
    }
}
