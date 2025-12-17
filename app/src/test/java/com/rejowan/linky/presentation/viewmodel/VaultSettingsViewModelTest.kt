package com.rejowan.linky.presentation.viewmodel

import app.cash.turbine.test
import com.rejowan.linky.data.security.AutoLockTimeout
import com.rejowan.linky.domain.repository.VaultRepository
import com.rejowan.linky.domain.usecase.vault.ChangeVaultPinUseCase
import com.rejowan.linky.domain.usecase.vault.ClearVaultUseCase
import com.rejowan.linky.presentation.feature.vault.VaultSettingsEvent
import com.rejowan.linky.presentation.feature.vault.VaultSettingsUiEvent
import com.rejowan.linky.presentation.feature.vault.VaultSettingsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class VaultSettingsViewModelTest {

    private lateinit var viewModel: VaultSettingsViewModel
    private lateinit var vaultRepository: VaultRepository
    private lateinit var changeVaultPinUseCase: ChangeVaultPinUseCase
    private lateinit var clearVaultUseCase: ClearVaultUseCase

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        vaultRepository = mockk()
        changeVaultPinUseCase = mockk()
        clearVaultUseCase = mockk()

        every { vaultRepository.getAutoLockTimeout() } returns AutoLockTimeout.FIVE_MINUTES
        every { vaultRepository.setAutoLockTimeout(any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = VaultSettingsViewModel(
            vaultRepository,
            changeVaultPinUseCase,
            clearVaultUseCase
        )
    }

    @Test
    fun `initial state loads auto lock timeout`() = runTest {
        createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(AutoLockTimeout.FIVE_MINUTES, state.autoLockTimeout)
        }
    }

    @Test
    fun `OnAutoLockTimeoutChanged updates timeout`() = runTest {
        createViewModel()

        viewModel.onEvent(VaultSettingsEvent.OnAutoLockTimeoutChanged(AutoLockTimeout.ONE_MINUTE))

        verify { vaultRepository.setAutoLockTimeout(AutoLockTimeout.ONE_MINUTE) }

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(AutoLockTimeout.ONE_MINUTE, state.autoLockTimeout)
        }
    }

    @Test
    fun `OnShowChangePinDialog shows dialog`() = runTest {
        createViewModel()

        viewModel.onEvent(VaultSettingsEvent.OnShowChangePinDialog)

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.showChangePinDialog)
        }
    }

    @Test
    fun `OnDismissChangePinDialog hides dialog`() = runTest {
        createViewModel()

        viewModel.onEvent(VaultSettingsEvent.OnShowChangePinDialog)
        viewModel.onEvent(VaultSettingsEvent.OnDismissChangePinDialog)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showChangePinDialog)
        }
    }

    @Test
    fun `OnChangePinConfirm success shows message and closes dialog`() = runTest {
        coEvery { changeVaultPinUseCase("1234", "5678") } returns kotlin.Result.success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultSettingsEvent.OnShowChangePinDialog)

        viewModel.uiEvents.test {
            viewModel.onEvent(VaultSettingsEvent.OnChangePinConfirm("1234", "5678"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is VaultSettingsUiEvent.ShowMessage)
            assertEquals("PIN changed successfully", (event as VaultSettingsUiEvent.ShowMessage).message)
        }

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showChangePinDialog)
        }
    }

    @Test
    fun `OnChangePinConfirm failure shows error message`() = runTest {
        coEvery { changeVaultPinUseCase("1234", "5678") } returns kotlin.Result.failure(Exception("Invalid PIN"))

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(VaultSettingsEvent.OnChangePinConfirm("1234", "5678"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is VaultSettingsUiEvent.ShowMessage)
            assertTrue((event as VaultSettingsUiEvent.ShowMessage).message.contains("Invalid PIN"))
        }
    }

    @Test
    fun `OnShowClearVaultDialog shows dialog`() = runTest {
        createViewModel()

        viewModel.onEvent(VaultSettingsEvent.OnShowClearVaultDialog)

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.showClearVaultDialog)
        }
    }

    @Test
    fun `OnDismissClearVaultDialog hides dialog`() = runTest {
        createViewModel()

        viewModel.onEvent(VaultSettingsEvent.OnShowClearVaultDialog)
        viewModel.onEvent(VaultSettingsEvent.OnDismissClearVaultDialog)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showClearVaultDialog)
        }
    }

    @Test
    fun `OnClearVaultConfirm success emits VaultCleared and closes dialog`() = runTest {
        coEvery { clearVaultUseCase() } returns kotlin.Result.success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultSettingsEvent.OnShowClearVaultDialog)

        viewModel.uiEvents.test {
            viewModel.onEvent(VaultSettingsEvent.OnClearVaultConfirm)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is VaultSettingsUiEvent.VaultCleared)
        }

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showClearVaultDialog)
        }

        coVerify { clearVaultUseCase() }
    }

    @Test
    fun `OnClearVaultConfirm failure shows error message`() = runTest {
        coEvery { clearVaultUseCase() } returns kotlin.Result.failure(Exception("Clear failed"))

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(VaultSettingsEvent.OnClearVaultConfirm)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is VaultSettingsUiEvent.ShowMessage)
            assertTrue((event as VaultSettingsUiEvent.ShowMessage).message.contains("Clear failed"))
        }
    }

    @Test
    fun `isLoading state during change pin operation`() = runTest {
        coEvery { changeVaultPinUseCase("1234", "5678") } returns kotlin.Result.success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultSettingsEvent.OnChangePinConfirm("1234", "5678"))

        // State should show loading during the operation
        viewModel.state.test {
            val state = awaitItem()
            // After completion, loading should be false
            assertFalse(state.isLoading)
        }
    }
}
