package com.rejowan.linky.presentation.viewmodel

import app.cash.turbine.test
import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.model.CollectionWithLinkCount
import com.rejowan.linky.domain.usecase.collection.DeleteCollectionUseCase
import com.rejowan.linky.domain.usecase.collection.GetCollectionsWithLinkCountUseCase
import com.rejowan.linky.domain.usecase.collection.SaveCollectionUseCase
import com.rejowan.linky.domain.usecase.collection.UpdateCollectionUseCase
import com.rejowan.linky.presentation.feature.collections.CollectionSortType
import com.rejowan.linky.presentation.feature.collections.CollectionsEvent
import com.rejowan.linky.presentation.feature.collections.CollectionsViewModel
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
class CollectionsViewModelTest {

    private lateinit var viewModel: CollectionsViewModel
    private lateinit var getCollectionsWithLinkCountUseCase: GetCollectionsWithLinkCountUseCase
    private lateinit var saveCollectionUseCase: SaveCollectionUseCase
    private lateinit var updateCollectionUseCase: UpdateCollectionUseCase
    private lateinit var deleteCollectionUseCase: DeleteCollectionUseCase

    private val testDispatcher = StandardTestDispatcher()

    private val testCollection1 = Collection(
        id = "collection-1",
        name = "Work",
        color = "#FF5733",
        isFavorite = true,
        sortOrder = 0,
        createdAt = 1000L,
        updatedAt = 2000L
    )

    private val testCollection2 = Collection(
        id = "collection-2",
        name = "Personal",
        color = "#3366FF",
        isFavorite = false,
        sortOrder = 1,
        createdAt = 3000L,
        updatedAt = 4000L
    )

    private val testCollectionWithCount1 = CollectionWithLinkCount(testCollection1, 5, listOf())
    private val testCollectionWithCount2 = CollectionWithLinkCount(testCollection2, 10, listOf())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        getCollectionsWithLinkCountUseCase = mockk()
        saveCollectionUseCase = mockk()
        updateCollectionUseCase = mockk()
        deleteCollectionUseCase = mockk()

        every { getCollectionsWithLinkCountUseCase() } returns flowOf(listOf(testCollectionWithCount1, testCollectionWithCount2))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = CollectionsViewModel(
            getCollectionsWithLinkCountUseCase,
            saveCollectionUseCase,
            updateCollectionUseCase,
            deleteCollectionUseCase
        )
    }

    @Test
    fun `initial state loads collections`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(2, state.collections.size)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }
    }

    @Test
    fun `OnCreateCollection shows create dialog`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CollectionsEvent.OnCreateCollection)

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.showCreateDialog)
        }
    }

    @Test
    fun `OnDismissCreateDialog hides dialog and clears form`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CollectionsEvent.OnCreateCollection)
        viewModel.onEvent(CollectionsEvent.OnCollectionNameChange("Test"))
        viewModel.onEvent(CollectionsEvent.OnCollectionColorChange("#FF0000"))
        viewModel.onEvent(CollectionsEvent.OnDismissCreateDialog)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showCreateDialog)
            assertEquals("", state.newCollectionName)
            assertNull(state.selectedCollectionColor)
            assertFalse(state.isNewCollectionFavorite)
        }
    }

    @Test
    fun `OnCollectionNameChange updates name`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CollectionsEvent.OnCollectionNameChange("New Collection"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("New Collection", state.newCollectionName)
        }
    }

    @Test
    fun `OnCollectionColorChange updates color`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CollectionsEvent.OnCollectionColorChange("#FF5733"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("#FF5733", state.selectedCollectionColor)
        }
    }

    @Test
    fun `OnToggleFavorite toggles favorite state`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CollectionsEvent.OnToggleFavorite)

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isNewCollectionFavorite)
        }

        viewModel.onEvent(CollectionsEvent.OnToggleFavorite)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isNewCollectionFavorite)
        }
    }

    @Test
    fun `OnSaveCollection creates collection successfully`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { saveCollectionUseCase(any()) } returns Result.Success(Unit)

        viewModel.onEvent(CollectionsEvent.OnCreateCollection)
        viewModel.onEvent(CollectionsEvent.OnCollectionNameChange("Test Collection"))
        viewModel.onEvent(CollectionsEvent.OnCollectionColorChange("#FF5733"))
        viewModel.onEvent(CollectionsEvent.OnSaveCollection)

        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showCreateDialog)
            assertEquals("", state.newCollectionName)
            assertNull(state.selectedCollectionColor)
        }

        coVerify { saveCollectionUseCase(any()) }
    }

    @Test
    fun `OnSaveCollection fails with empty name`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CollectionsEvent.OnCreateCollection)
        viewModel.onEvent(CollectionsEvent.OnCollectionNameChange(""))
        viewModel.onEvent(CollectionsEvent.OnSaveCollection)

        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `OnSaveCollection fails with invalid color`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CollectionsEvent.OnCreateCollection)
        viewModel.onEvent(CollectionsEvent.OnCollectionNameChange("Valid Name"))
        viewModel.onEvent(CollectionsEvent.OnCollectionColorChange("invalid-color"))
        viewModel.onEvent(CollectionsEvent.OnSaveCollection)

        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `OnDeleteCollection deletes collection`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { deleteCollectionUseCase("collection-1") } returns Result.Success(Unit)

        viewModel.onEvent(CollectionsEvent.OnDeleteCollection("collection-1"))
        advanceUntilIdle()

        coVerify { deleteCollectionUseCase("collection-1") }
    }

    @Test
    fun `OnSortTypeChange changes sort type and sorts collections`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CollectionsEvent.OnSortTypeChange(CollectionSortType.NAME_ASC))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(CollectionSortType.NAME_ASC, state.sortType)
        }
    }

    @Test
    fun `OnToggleCollectionFavorite toggles favorite status`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { updateCollectionUseCase(any()) } returns Result.Success(Unit)

        viewModel.onEvent(CollectionsEvent.OnToggleCollectionFavorite("collection-1"))
        advanceUntilIdle()

        coVerify { updateCollectionUseCase(match { it.id == "collection-1" }) }
    }

    @Test
    fun `collections are sorted with favorites first`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            val collections = state.collections

            // Favorite collections should be first
            val favoriteCount = collections.count { it.collection.isFavorite }
            if (favoriteCount > 0) {
                assertTrue(collections.take(favoriteCount).all { it.collection.isFavorite })
            }
        }
    }
}
