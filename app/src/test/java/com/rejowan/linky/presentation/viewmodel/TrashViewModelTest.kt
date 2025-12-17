package com.rejowan.linky.presentation.viewmodel

import app.cash.turbine.test
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.usecase.link.DeleteLinkUseCase
import com.rejowan.linky.domain.usecase.link.GetTrashedLinksUseCase
import com.rejowan.linky.domain.usecase.link.RestoreLinkUseCase
import com.rejowan.linky.presentation.feature.trash.TrashEvent
import com.rejowan.linky.presentation.feature.trash.TrashUiEvent
import com.rejowan.linky.presentation.feature.trash.TrashViewModel
import com.rejowan.linky.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class TrashViewModelTest {

    private lateinit var viewModel: TrashViewModel
    private lateinit var getTrashedLinksUseCase: GetTrashedLinksUseCase
    private lateinit var restoreLinkUseCase: RestoreLinkUseCase
    private lateinit var deleteLinkUseCase: DeleteLinkUseCase

    private val testDispatcher = StandardTestDispatcher()

    private val testLink1 = Link(
        id = "link-1",
        url = "https://deleted1.com",
        title = "Deleted Link 1",
        isFavorite = false,
        isArchived = false,
        deletedAt = 3000L,
        createdAt = 1000L,
        updatedAt = 2000L
    )

    private val testLink2 = Link(
        id = "link-2",
        url = "https://deleted2.com",
        title = "Deleted Link 2",
        isFavorite = false,
        isArchived = false,
        deletedAt = 5000L,
        createdAt = 3000L,
        updatedAt = 4000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        getTrashedLinksUseCase = mockk()
        restoreLinkUseCase = mockk()
        deleteLinkUseCase = mockk()

        every { getTrashedLinksUseCase() } returns flowOf(listOf(testLink1, testLink2))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = TrashViewModel(getTrashedLinksUseCase, restoreLinkUseCase, deleteLinkUseCase)
    }

    @Test
    fun `initial state loads trashed links`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(2, state.trashedLinks.size)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }
    }

    @Test
    fun `OnRefresh reloads trashed links`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TrashEvent.OnRefresh)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(2, state.trashedLinks.size)
        }
    }

    @Test
    fun `OnRestoreLink restores link and emits event`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { restoreLinkUseCase("link-1") } returns Result.Success(Unit)

        viewModel.uiEvents.test {
            viewModel.onEvent(TrashEvent.OnRestoreLink("link-1"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is TrashUiEvent.ShowLinkRestored)
            assertEquals("link-1", (event as TrashUiEvent.ShowLinkRestored).linkId)
        }

        coVerify { restoreLinkUseCase("link-1") }
    }

    @Test
    fun `OnRestoreLink with silent does not emit event`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { restoreLinkUseCase("link-1") } returns Result.Success(Unit)

        viewModel.onEvent(TrashEvent.OnRestoreLink("link-1", silent = true))
        advanceUntilIdle()

        coVerify { restoreLinkUseCase("link-1") }
    }

    @Test
    fun `OnRestoreLink emits error on failure`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { restoreLinkUseCase("link-1") } returns Result.Error(Exception("Restore failed"))

        viewModel.uiEvents.test {
            viewModel.onEvent(TrashEvent.OnRestoreLink("link-1"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is TrashUiEvent.ShowError)
        }
    }

    @Test
    fun `OnPermanentlyDeleteLink deletes link and emits event`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { deleteLinkUseCase("link-1", softDelete = false) } returns Result.Success(Unit)

        viewModel.uiEvents.test {
            viewModel.onEvent(TrashEvent.OnPermanentlyDeleteLink("link-1"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is TrashUiEvent.ShowLinkDeleted)
            assertEquals("link-1", (event as TrashUiEvent.ShowLinkDeleted).linkId)
        }

        coVerify { deleteLinkUseCase("link-1", softDelete = false) }
    }

    @Test
    fun `OnPermanentlyDeleteLink with silent does not emit event`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { deleteLinkUseCase("link-1", softDelete = false) } returns Result.Success(Unit)

        viewModel.onEvent(TrashEvent.OnPermanentlyDeleteLink("link-1", silent = true))
        advanceUntilIdle()

        coVerify { deleteLinkUseCase("link-1", softDelete = false) }
    }

    @Test
    fun `OnPermanentlyDeleteLink emits error on failure`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { deleteLinkUseCase("link-1", softDelete = false) } returns Result.Error(Exception("Delete failed"))

        viewModel.uiEvents.test {
            viewModel.onEvent(TrashEvent.OnPermanentlyDeleteLink("link-1"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is TrashUiEvent.ShowError)
        }
    }

    @Test
    fun `OnUndoDelete restores link without event`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { restoreLinkUseCase("link-1") } returns Result.Success(Unit)

        viewModel.onEvent(TrashEvent.OnUndoDelete("link-1"))
        advanceUntilIdle()

        coVerify { restoreLinkUseCase("link-1") }
    }

    @Test
    fun `OnUndoDelete emits error on failure`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { restoreLinkUseCase("link-1") } returns Result.Error(Exception("Undo failed"))

        viewModel.uiEvents.test {
            viewModel.onEvent(TrashEvent.OnUndoDelete("link-1"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is TrashUiEvent.ShowError)
        }
    }

    @Test
    fun `OnEmptyTrash deletes all trashed links`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { deleteLinkUseCase(any(), softDelete = false) } returns Result.Success(Unit)

        viewModel.onEvent(TrashEvent.OnEmptyTrash)
        advanceUntilIdle()

        coVerify { deleteLinkUseCase("link-1", softDelete = false) }
        coVerify { deleteLinkUseCase("link-2", softDelete = false) }
    }

    @Test
    fun `OnEmptyTrash sets error if some deletions fail`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { deleteLinkUseCase("link-1", softDelete = false) } returns Result.Success(Unit)
        coEvery { deleteLinkUseCase("link-2", softDelete = false) } returns Result.Error(Exception("Failed"))

        viewModel.onEvent(TrashEvent.OnEmptyTrash)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `empty trash completes without error when all succeed`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { deleteLinkUseCase(any(), softDelete = false) } returns Result.Success(Unit)

        viewModel.onEvent(TrashEvent.OnEmptyTrash)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.error)
            assertFalse(state.isLoading)
        }
    }
}
