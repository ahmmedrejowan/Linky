package com.rejowan.linky.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.usecase.collection.DeleteCollectionUseCase
import com.rejowan.linky.domain.usecase.collection.GetCollectionByIdUseCase
import com.rejowan.linky.domain.usecase.collection.UpdateCollectionUseCase
import com.rejowan.linky.domain.usecase.link.DeleteLinkUseCase
import com.rejowan.linky.domain.usecase.link.GetLinksByCollectionUseCase
import com.rejowan.linky.domain.usecase.link.RestoreLinkUseCase
import com.rejowan.linky.domain.usecase.link.ToggleArchiveUseCase
import com.rejowan.linky.domain.usecase.link.UpdateLinkUseCase
import com.rejowan.linky.presentation.feature.collectiondetail.CollectionDetailEvent
import com.rejowan.linky.presentation.feature.collectiondetail.CollectionDetailUiEvent
import com.rejowan.linky.presentation.feature.collectiondetail.CollectionDetailViewModel
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
class CollectionDetailViewModelTest {

    private lateinit var viewModel: CollectionDetailViewModel
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var getCollectionByIdUseCase: GetCollectionByIdUseCase
    private lateinit var getLinksByCollectionUseCase: GetLinksByCollectionUseCase
    private lateinit var updateCollectionUseCase: UpdateCollectionUseCase
    private lateinit var deleteCollectionUseCase: DeleteCollectionUseCase
    private lateinit var updateLinkUseCase: UpdateLinkUseCase
    private lateinit var deleteLinkUseCase: DeleteLinkUseCase
    private lateinit var toggleArchiveUseCase: ToggleArchiveUseCase
    private lateinit var restoreLinkUseCase: RestoreLinkUseCase

    private val testDispatcher = StandardTestDispatcher()

    private val testCollection = Collection(
        id = "collection-1",
        name = "Work",
        color = "#FF5733",
        isFavorite = false,
        createdAt = 1000L,
        updatedAt = 2000L
    )

    private val testLink = Link(
        id = "link-1",
        url = "https://example.com",
        title = "Example",
        collectionId = "collection-1",
        isFavorite = false,
        isArchived = false,
        createdAt = 1000L,
        updatedAt = 2000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        savedStateHandle = SavedStateHandle(mapOf("collectionId" to "collection-1"))
        getCollectionByIdUseCase = mockk()
        getLinksByCollectionUseCase = mockk()
        updateCollectionUseCase = mockk()
        deleteCollectionUseCase = mockk()
        updateLinkUseCase = mockk()
        deleteLinkUseCase = mockk()
        toggleArchiveUseCase = mockk()
        restoreLinkUseCase = mockk()

        every { getCollectionByIdUseCase("collection-1") } returns flowOf(testCollection)
        every { getLinksByCollectionUseCase("collection-1") } returns flowOf(listOf(testLink))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = CollectionDetailViewModel(
            savedStateHandle,
            getCollectionByIdUseCase,
            getLinksByCollectionUseCase,
            updateCollectionUseCase,
            deleteCollectionUseCase,
            updateLinkUseCase,
            deleteLinkUseCase,
            toggleArchiveUseCase,
            restoreLinkUseCase
        )
    }

    @Test
    fun `initial state loads collection and links`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.collection)
            assertEquals("Work", state.collection?.name)
            assertEquals(1, state.links.size)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `OnToggleFavorite toggles collection favorite status`() = runTest {
        coEvery { updateCollectionUseCase(any()) } returns Result.Success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CollectionDetailEvent.OnToggleFavorite)
        advanceUntilIdle()

        coVerify { updateCollectionUseCase(match { it.isFavorite == true }) }
    }

    @Test
    fun `OnEditClick shows edit dialog with current values`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CollectionDetailEvent.OnEditClick)

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.showEditDialog)
            assertEquals("Work", state.editName)
            assertEquals("#FF5733", state.editColor)
        }
    }

    @Test
    fun `OnEditDismiss hides edit dialog`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CollectionDetailEvent.OnEditClick)
        viewModel.onEvent(CollectionDetailEvent.OnEditDismiss)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showEditDialog)
        }
    }

    @Test
    fun `OnEditNameChange updates edit name`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CollectionDetailEvent.OnEditClick)
        viewModel.onEvent(CollectionDetailEvent.OnEditNameChange("New Name"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("New Name", state.editName)
        }
    }

    @Test
    fun `OnEditConfirm saves edited collection`() = runTest {
        coEvery { updateCollectionUseCase(any()) } returns Result.Success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CollectionDetailEvent.OnEditClick)
        viewModel.onEvent(CollectionDetailEvent.OnEditNameChange("Updated Name"))
        viewModel.onEvent(CollectionDetailEvent.OnEditConfirm)
        advanceUntilIdle()

        coVerify { updateCollectionUseCase(match { it.name == "Updated Name" }) }
    }

    @Test
    fun `OnDeleteClick shows delete dialog`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CollectionDetailEvent.OnDeleteClick)

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.showDeleteDialog)
        }
    }

    @Test
    fun `OnDeleteDismiss hides delete dialog`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CollectionDetailEvent.OnDeleteClick)
        viewModel.onEvent(CollectionDetailEvent.OnDeleteDismiss)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showDeleteDialog)
        }
    }

    @Test
    fun `OnDeleteConfirm deletes collection and emits navigate back`() = runTest {
        coEvery { updateLinkUseCase(any()) } returns Result.Success(Unit)
        coEvery { deleteCollectionUseCase("collection-1") } returns Result.Success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(CollectionDetailEvent.OnDeleteClick)
            viewModel.onEvent(CollectionDetailEvent.OnDeleteConfirm)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is CollectionDetailUiEvent.NavigateBack)
        }
    }

    @Test
    fun `OnToggleLinkFavorite toggles link favorite and emits event`() = runTest {
        coEvery { updateLinkUseCase(any()) } returns Result.Success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(CollectionDetailEvent.OnToggleLinkFavorite("link-1"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is CollectionDetailUiEvent.ShowLinkFavoriteToggled)
        }
    }

    @Test
    fun `OnToggleLinkFavorite with silent does not emit event`() = runTest {
        coEvery { updateLinkUseCase(any()) } returns Result.Success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CollectionDetailEvent.OnToggleLinkFavorite("link-1", silent = true))
        advanceUntilIdle()

        coVerify { updateLinkUseCase(any()) }
    }

    @Test
    fun `OnArchiveLink toggles archive and emits event`() = runTest {
        coEvery { toggleArchiveUseCase("link-1", true) } returns Result.Success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(CollectionDetailEvent.OnArchiveLink("link-1"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is CollectionDetailUiEvent.ShowArchiveToggled)
        }
    }

    @Test
    fun `OnTrashLink trashes link and emits event`() = runTest {
        coEvery { deleteLinkUseCase("link-1", softDelete = true) } returns Result.Success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(CollectionDetailEvent.OnTrashLink("link-1"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is CollectionDetailUiEvent.ShowLinkTrashed)
        }
    }

    @Test
    fun `OnRestoreLink restores link`() = runTest {
        coEvery { restoreLinkUseCase("link-1") } returns Result.Success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CollectionDetailEvent.OnRestoreLink("link-1"))
        advanceUntilIdle()

        coVerify { restoreLinkUseCase("link-1") }
    }

    @Test
    fun `error state when collection not found`() = runTest {
        every { getCollectionByIdUseCase("collection-1") } returns flowOf(null)
        every { getLinksByCollectionUseCase("collection-1") } returns flowOf(emptyList())

        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }
}
