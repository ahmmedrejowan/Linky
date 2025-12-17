package com.rejowan.linky.presentation.viewmodel

import app.cash.turbine.test
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.usecase.link.SearchLinksUseCase
import com.rejowan.linky.domain.usecase.link.ToggleFavoriteUseCase
import com.rejowan.linky.presentation.feature.search.SearchEvent
import com.rejowan.linky.presentation.feature.search.SearchUiEvent
import com.rejowan.linky.presentation.feature.search.SearchViewModel
import com.rejowan.linky.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private lateinit var viewModel: SearchViewModel
    private lateinit var searchLinksUseCase: SearchLinksUseCase
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase

    private val testDispatcher = StandardTestDispatcher()

    private val testLink1 = Link(
        id = "link-1",
        url = "https://example.com",
        title = "Example Site",
        description = "An example website",
        isFavorite = false,
        isArchived = false,
        createdAt = 1000L,
        updatedAt = 2000L
    )

    private val testLink2 = Link(
        id = "link-2",
        url = "https://test.com",
        title = "Test Site",
        description = "A test website",
        isFavorite = true,
        isArchived = false,
        createdAt = 3000L,
        updatedAt = 4000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        searchLinksUseCase = mockk()
        toggleFavoriteUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = SearchViewModel(searchLinksUseCase, toggleFavoriteUseCase)
    }

    @Test
    fun `initial state has empty query and results`() = runTest {
        createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("", state.searchQuery)
            assertTrue(state.searchResults.isEmpty())
            assertFalse(state.isSearching)
            assertFalse(state.hasSearched)
            assertNull(state.error)
        }
    }

    @Test
    fun `OnSearchQueryChange updates search query`() = runTest {
        every { searchLinksUseCase("test") } returns flowOf(emptyList())

        createViewModel()

        viewModel.onEvent(SearchEvent.OnSearchQueryChange("test"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("test", state.searchQuery)
        }
    }

    @Test
    fun `OnSearchQueryChange with blank clears results`() = runTest {
        createViewModel()

        every { searchLinksUseCase("test") } returns flowOf(listOf(testLink1))

        viewModel.onEvent(SearchEvent.OnSearchQueryChange("test"))
        advanceTimeBy(500)
        advanceUntilIdle()

        viewModel.onEvent(SearchEvent.OnSearchQueryChange(""))

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.searchResults.isEmpty())
            assertFalse(state.hasSearched)
        }
    }

    @Test
    fun `OnSearchQueryChange triggers debounced search`() = runTest {
        createViewModel()

        every { searchLinksUseCase("example") } returns flowOf(listOf(testLink1))

        viewModel.onEvent(SearchEvent.OnSearchQueryChange("example"))
        advanceTimeBy(500)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.searchResults.size)
            assertEquals(testLink1, state.searchResults[0])
            assertTrue(state.hasSearched)
        }
    }

    @Test
    fun `OnSearch performs immediate search`() = runTest {
        createViewModel()

        every { searchLinksUseCase("test") } returns flowOf(listOf(testLink1, testLink2))

        viewModel.onEvent(SearchEvent.OnSearchQueryChange("test"))
        viewModel.onEvent(SearchEvent.OnSearch)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(2, state.searchResults.size)
            assertTrue(state.hasSearched)
        }
    }

    @Test
    fun `OnSearch with blank query does nothing`() = runTest {
        createViewModel()

        viewModel.onEvent(SearchEvent.OnSearch)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.searchResults.isEmpty())
            assertFalse(state.hasSearched)
        }
    }

    @Test
    fun `OnClearSearch clears query and results`() = runTest {
        createViewModel()

        every { searchLinksUseCase("test") } returns flowOf(listOf(testLink1))

        viewModel.onEvent(SearchEvent.OnSearchQueryChange("test"))
        advanceTimeBy(500)
        advanceUntilIdle()

        viewModel.onEvent(SearchEvent.OnClearSearch)

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("", state.searchQuery)
            assertTrue(state.searchResults.isEmpty())
            assertFalse(state.hasSearched)
            assertNull(state.error)
        }
    }

    @Test
    fun `OnToggleFavorite toggles favorite and emits event`() = runTest {
        createViewModel()

        coEvery { toggleFavoriteUseCase("link-1", true) } returns Result.Success(Unit)

        viewModel.uiEvents.test {
            viewModel.onEvent(SearchEvent.OnToggleFavorite("link-1", true))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is SearchUiEvent.ShowFavoriteToggled)
            assertEquals("link-1", (event as SearchUiEvent.ShowFavoriteToggled).linkId)
            assertTrue(event.isFavorite)
        }

        coVerify { toggleFavoriteUseCase("link-1", true) }
    }

    @Test
    fun `OnToggleFavorite with silent does not emit event`() = runTest {
        createViewModel()

        coEvery { toggleFavoriteUseCase("link-1", true) } returns Result.Success(Unit)

        viewModel.onEvent(SearchEvent.OnToggleFavorite("link-1", true, silent = true))
        advanceUntilIdle()

        coVerify { toggleFavoriteUseCase("link-1", true) }
    }

    @Test
    fun `OnToggleFavorite emits error on failure`() = runTest {
        createViewModel()

        coEvery { toggleFavoriteUseCase("link-1", true) } returns Result.Error(Exception("Failed"))

        viewModel.uiEvents.test {
            viewModel.onEvent(SearchEvent.OnToggleFavorite("link-1", true))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is SearchUiEvent.ShowError)
        }
    }

    @Test
    fun `debounce cancels previous search`() = runTest {
        createViewModel()

        every { searchLinksUseCase("test") } returns flowOf(listOf(testLink1, testLink2))
        every { searchLinksUseCase("te") } returns flowOf(listOf(testLink1))

        viewModel.onEvent(SearchEvent.OnSearchQueryChange("te"))
        advanceTimeBy(200)
        viewModel.onEvent(SearchEvent.OnSearchQueryChange("test"))
        advanceTimeBy(500)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(2, state.searchResults.size)
        }
    }

    @Test
    fun `search sets isSearching during search`() = runTest {
        createViewModel()

        every { searchLinksUseCase("test") } returns flowOf(listOf(testLink1))

        viewModel.onEvent(SearchEvent.OnSearchQueryChange("test"))
        advanceTimeBy(450)

        viewModel.state.test {
            // During search
            advanceUntilIdle()
            val state = awaitItem()
            assertFalse(state.isSearching)
        }
    }
}
