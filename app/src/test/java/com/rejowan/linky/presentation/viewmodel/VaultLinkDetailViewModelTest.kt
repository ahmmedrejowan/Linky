package com.rejowan.linky.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rejowan.linky.domain.model.VaultLink
import com.rejowan.linky.domain.usecase.vault.DeleteVaultLinkUseCase
import com.rejowan.linky.domain.usecase.vault.GetVaultLinkByIdUseCase
import com.rejowan.linky.domain.usecase.vault.UpdateVaultLinkUseCase
import com.rejowan.linky.presentation.feature.vault.VaultLinkDetailEvent
import com.rejowan.linky.presentation.feature.vault.VaultLinkDetailUiEvent
import com.rejowan.linky.presentation.feature.vault.VaultLinkDetailViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultLinkDetailViewModelTest {

    private lateinit var viewModel: VaultLinkDetailViewModel
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var getVaultLinkByIdUseCase: GetVaultLinkByIdUseCase
    private lateinit var updateVaultLinkUseCase: UpdateVaultLinkUseCase
    private lateinit var deleteVaultLinkUseCase: DeleteVaultLinkUseCase

    private val testDispatcher = StandardTestDispatcher()

    private val testVaultLink = VaultLink(
        id = "vault-link-1",
        url = "https://secret.example.com",
        title = "Secret Link",
        description = "A private link",
        notes = "Some notes",
        isFavorite = false,
        createdAt = 1000L,
        updatedAt = 2000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        savedStateHandle = SavedStateHandle(mapOf("linkId" to "vault-link-1"))
        getVaultLinkByIdUseCase = mockk()
        updateVaultLinkUseCase = mockk()
        deleteVaultLinkUseCase = mockk()

        coEvery { getVaultLinkByIdUseCase("vault-link-1") } returns testVaultLink
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = VaultLinkDetailViewModel(
            savedStateHandle,
            getVaultLinkByIdUseCase,
            updateVaultLinkUseCase,
            deleteVaultLinkUseCase
        )
    }

    @Test
    fun `initial state loads vault link`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.link)
            assertEquals("vault-link-1", state.link?.id)
            assertEquals("Secret Link", state.link?.title)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }
    }

    @Test
    fun `initial state shows error when link not found`() = runTest {
        coEvery { getVaultLinkByIdUseCase("vault-link-1") } returns null

        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.link)
            assertFalse(state.isLoading)
            assertEquals("Link not found", state.error)
        }
    }

    @Test
    fun `OnToggleFavorite adds to favorites and emits message`() = runTest {
        coEvery { updateVaultLinkUseCase(any()) } returns Result.success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(VaultLinkDetailEvent.OnToggleFavorite)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is VaultLinkDetailUiEvent.ShowMessage)
            assertEquals("Added to favorites", (event as VaultLinkDetailUiEvent.ShowMessage).message)
        }

        coVerify { updateVaultLinkUseCase(match { it.isFavorite }) }
    }

    @Test
    fun `OnToggleFavorite removes from favorites when already favorite`() = runTest {
        val favoriteLink = testVaultLink.copy(isFavorite = true)
        coEvery { getVaultLinkByIdUseCase("vault-link-1") } returns favoriteLink
        coEvery { updateVaultLinkUseCase(any()) } returns Result.success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(VaultLinkDetailEvent.OnToggleFavorite)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is VaultLinkDetailUiEvent.ShowMessage)
            assertEquals("Removed from favorites", (event as VaultLinkDetailUiEvent.ShowMessage).message)
        }

        coVerify { updateVaultLinkUseCase(match { !it.isFavorite }) }
    }

    @Test
    fun `OnToggleFavorite emits error on failure`() = runTest {
        coEvery { updateVaultLinkUseCase(any()) } returns Result.failure(Exception("Update failed"))

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(VaultLinkDetailEvent.OnToggleFavorite)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is VaultLinkDetailUiEvent.ShowMessage)
            assertTrue((event as VaultLinkDetailUiEvent.ShowMessage).message.contains("failed") ||
                    event.message.contains("Update failed"))
        }
    }

    @Test
    fun `OnDelete deletes link and emits LinkDeleted`() = runTest {
        coEvery { deleteVaultLinkUseCase("vault-link-1") } returns Result.success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(VaultLinkDetailEvent.OnDelete)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is VaultLinkDetailUiEvent.LinkDeleted)
        }

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isDeleted)
        }

        coVerify { deleteVaultLinkUseCase("vault-link-1") }
    }

    @Test
    fun `OnDelete emits error on failure`() = runTest {
        coEvery { deleteVaultLinkUseCase("vault-link-1") } returns Result.failure(Exception("Delete failed"))

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(VaultLinkDetailEvent.OnDelete)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is VaultLinkDetailUiEvent.ShowMessage)
            assertTrue((event as VaultLinkDetailUiEvent.ShowMessage).message.contains("failed") ||
                    event.message.contains("Delete failed"))
        }
    }

    @Test
    fun `OnRefresh reloads link`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val updatedLink = testVaultLink.copy(title = "Updated Title")
        coEvery { getVaultLinkByIdUseCase("vault-link-1") } returns updatedLink

        viewModel.onEvent(VaultLinkDetailEvent.OnRefresh)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Updated Title", state.link?.title)
        }
    }

    @Test
    fun `OnToggleFavorite does nothing when link is null`() = runTest {
        coEvery { getVaultLinkByIdUseCase("vault-link-1") } returns null

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultLinkDetailEvent.OnToggleFavorite)
        advanceUntilIdle()

        coVerify(exactly = 0) { updateVaultLinkUseCase(any()) }
    }

    @Test
    fun `OnDelete does nothing when link is null`() = runTest {
        coEvery { getVaultLinkByIdUseCase("vault-link-1") } returns null

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultLinkDetailEvent.OnDelete)
        advanceUntilIdle()

        coVerify(exactly = 0) { deleteVaultLinkUseCase(any()) }
    }
}
