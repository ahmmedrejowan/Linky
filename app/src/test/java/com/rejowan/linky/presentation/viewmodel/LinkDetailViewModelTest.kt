package com.rejowan.linky.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.model.Snapshot
import com.rejowan.linky.domain.model.SnapshotType
import com.rejowan.linky.domain.usecase.collection.GetCollectionByIdUseCase
import com.rejowan.linky.domain.usecase.link.DeleteLinkUseCase
import com.rejowan.linky.domain.usecase.link.GetLinkByIdUseCase
import com.rejowan.linky.domain.usecase.link.RestoreLinkUseCase
import com.rejowan.linky.domain.usecase.link.ToggleArchiveUseCase
import com.rejowan.linky.domain.usecase.link.ToggleFavoriteUseCase
import com.rejowan.linky.domain.usecase.snapshot.CaptureSnapshotUseCase
import com.rejowan.linky.domain.usecase.snapshot.DeleteSnapshotUseCase
import com.rejowan.linky.domain.usecase.snapshot.GetSnapshotsForLinkUseCase
import com.rejowan.linky.presentation.feature.linkdetail.LinkDetailEvent
import com.rejowan.linky.presentation.feature.linkdetail.LinkDetailUiEvent
import com.rejowan.linky.presentation.feature.linkdetail.LinkDetailViewModel
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
class LinkDetailViewModelTest {

    private lateinit var viewModel: LinkDetailViewModel
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var getLinkByIdUseCase: GetLinkByIdUseCase
    private lateinit var getSnapshotsForLinkUseCase: GetSnapshotsForLinkUseCase
    private lateinit var getCollectionByIdUseCase: GetCollectionByIdUseCase
    private lateinit var captureSnapshotUseCase: CaptureSnapshotUseCase
    private lateinit var deleteSnapshotUseCase: DeleteSnapshotUseCase
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase
    private lateinit var toggleArchiveUseCase: ToggleArchiveUseCase
    private lateinit var deleteLinkUseCase: DeleteLinkUseCase
    private lateinit var restoreLinkUseCase: RestoreLinkUseCase

    private val testDispatcher = StandardTestDispatcher()

    private val testLink = Link(
        id = "link-1",
        url = "https://example.com",
        title = "Example Site",
        description = "An example website",
        collectionId = "collection-1",
        isFavorite = false,
        isArchived = false,
        createdAt = 1000L,
        updatedAt = 2000L
    )

    private val testCollection = Collection(
        id = "collection-1",
        name = "Work",
        color = "#FF5733",
        createdAt = 1000L,
        updatedAt = 2000L
    )

    private val testSnapshot = Snapshot(
        id = "snapshot-1",
        linkId = "link-1",
        type = SnapshotType.READER_MODE,
        filePath = "/path/to/snapshot.html",
        fileSize = 1024L,
        createdAt = 1000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        savedStateHandle = SavedStateHandle(mapOf("linkId" to "link-1"))
        getLinkByIdUseCase = mockk()
        getSnapshotsForLinkUseCase = mockk()
        getCollectionByIdUseCase = mockk()
        captureSnapshotUseCase = mockk()
        deleteSnapshotUseCase = mockk()
        toggleFavoriteUseCase = mockk()
        toggleArchiveUseCase = mockk()
        deleteLinkUseCase = mockk()
        restoreLinkUseCase = mockk()

        every { getLinkByIdUseCase("link-1") } returns flowOf(testLink)
        every { getSnapshotsForLinkUseCase("link-1") } returns flowOf(listOf(testSnapshot))
        every { getCollectionByIdUseCase("collection-1") } returns flowOf(testCollection)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = LinkDetailViewModel(
            savedStateHandle,
            getLinkByIdUseCase,
            getSnapshotsForLinkUseCase,
            getCollectionByIdUseCase,
            captureSnapshotUseCase,
            deleteSnapshotUseCase,
            toggleFavoriteUseCase,
            toggleArchiveUseCase,
            deleteLinkUseCase,
            restoreLinkUseCase
        )
    }

    @Test
    fun `initial state loads link details`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.link)
            assertEquals("link-1", state.link?.id)
            assertNotNull(state.collection)
            assertEquals(1, state.snapshots.size)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `OnToggleFavorite toggles favorite and emits event`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { toggleFavoriteUseCase("link-1", true) } returns Result.Success(Unit)

        viewModel.uiEvents.test {
            viewModel.onEvent(LinkDetailEvent.OnToggleFavorite())
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is LinkDetailUiEvent.ShowFavoriteToggled)
        }

        coVerify { toggleFavoriteUseCase("link-1", true) }
    }

    @Test
    fun `OnToggleFavorite with silent does not emit event`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { toggleFavoriteUseCase("link-1", true) } returns Result.Success(Unit)

        viewModel.onEvent(LinkDetailEvent.OnToggleFavorite(silent = true))
        advanceUntilIdle()

        coVerify { toggleFavoriteUseCase("link-1", true) }
    }

    @Test
    fun `OnDeleteLink soft deletes link and emits event`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { deleteLinkUseCase("link-1", softDelete = true) } returns Result.Success(Unit)

        viewModel.uiEvents.test {
            viewModel.onEvent(LinkDetailEvent.OnDeleteLink())
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is LinkDetailUiEvent.ShowLinkTrashed)
        }

        coVerify { deleteLinkUseCase("link-1", softDelete = true) }
    }

    @Test
    fun `OnArchiveLink toggles archive and emits event`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { toggleArchiveUseCase("link-1", true) } returns Result.Success(Unit)

        viewModel.uiEvents.test {
            viewModel.onEvent(LinkDetailEvent.OnArchiveLink())
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is LinkDetailUiEvent.ShowArchiveToggled)
        }

        coVerify { toggleArchiveUseCase("link-1", true) }
    }

    @Test
    fun `OnRestoreLink restores link and emits event`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { restoreLinkUseCase("link-1") } returns Result.Success(Unit)

        viewModel.uiEvents.test {
            viewModel.onEvent(LinkDetailEvent.OnRestoreLink())
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is LinkDetailUiEvent.ShowLinkRestored)
        }

        coVerify { restoreLinkUseCase("link-1") }
    }

    @Test
    fun `OnPermanentlyDeleteLink permanently deletes link`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { deleteLinkUseCase("link-1", softDelete = false) } returns Result.Success(Unit)

        viewModel.uiEvents.test {
            viewModel.onEvent(LinkDetailEvent.OnPermanentlyDeleteLink())
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is LinkDetailUiEvent.ShowLinkDeleted)
        }

        coVerify { deleteLinkUseCase("link-1", softDelete = false) }
    }

    @Test
    fun `OnCreateSnapshot captures snapshot and emits success`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { captureSnapshotUseCase(testLink.url, testLink.id) } returns Result.Success(testSnapshot)

        viewModel.uiEvents.test {
            viewModel.onEvent(LinkDetailEvent.OnCreateSnapshot)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is LinkDetailUiEvent.ShowSuccess)
            assertEquals("Snapshot captured successfully", (event as LinkDetailUiEvent.ShowSuccess).message)
        }
    }

    @Test
    fun `OnCreateSnapshot emits error on failure`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { captureSnapshotUseCase(testLink.url, testLink.id) } returns Result.Error(Exception("Capture failed"))

        viewModel.uiEvents.test {
            viewModel.onEvent(LinkDetailEvent.OnCreateSnapshot)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is LinkDetailUiEvent.ShowError)
        }
    }

    @Test
    fun `OnDeleteSnapshot deletes snapshot and emits success`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { deleteSnapshotUseCase("snapshot-1") } returns Result.Success(Unit)

        viewModel.uiEvents.test {
            viewModel.onEvent(LinkDetailEvent.OnDeleteSnapshot("snapshot-1"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is LinkDetailUiEvent.ShowSuccess)
            assertEquals("Snapshot deleted", (event as LinkDetailUiEvent.ShowSuccess).message)
        }
    }

    @Test
    fun `OnRefresh reloads link details`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(LinkDetailEvent.OnRefresh)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.link)
        }
    }

    @Test
    fun `error is emitted when link not found`() = runTest {
        every { getLinkByIdUseCase("link-1") } returns flowOf(null)
        every { getSnapshotsForLinkUseCase("link-1") } returns flowOf(emptyList())

        createViewModel()

        viewModel.uiEvents.test {
            advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is LinkDetailUiEvent.ShowError)
        }
    }
}
