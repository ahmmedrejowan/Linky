package com.rejowan.linky.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.model.Tag
import com.rejowan.linky.domain.usecase.collection.GetAllCollectionsUseCase
import com.rejowan.linky.domain.usecase.collection.SaveCollectionUseCase
import com.rejowan.linky.domain.usecase.link.GetLinkByIdUseCase
import com.rejowan.linky.domain.usecase.link.SaveLinkUseCase
import com.rejowan.linky.domain.usecase.link.UpdateLinkUseCase
import com.rejowan.linky.domain.usecase.tag.GetAllTagsUseCase
import com.rejowan.linky.domain.usecase.tag.GetTagsForLinkUseCase
import com.rejowan.linky.domain.usecase.tag.SaveTagUseCase
import com.rejowan.linky.domain.usecase.tag.SetTagsForLinkUseCase
import com.rejowan.linky.presentation.feature.addlink.AddEditLinkEvent
import com.rejowan.linky.presentation.feature.addlink.AddEditLinkUiEvent
import com.rejowan.linky.presentation.feature.addlink.AddEditLinkViewModel
import com.rejowan.linky.util.FileStorageManager
import com.rejowan.linky.util.LinkPreviewFetcher
import com.rejowan.linky.util.PreferencesManager
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
class AddEditLinkViewModelTest {

    private lateinit var viewModel: AddEditLinkViewModel
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var saveLinkUseCase: SaveLinkUseCase
    private lateinit var updateLinkUseCase: UpdateLinkUseCase
    private lateinit var getLinkByIdUseCase: GetLinkByIdUseCase
    private lateinit var getAllCollectionsUseCase: GetAllCollectionsUseCase
    private lateinit var saveCollectionUseCase: SaveCollectionUseCase
    private lateinit var linkPreviewFetcher: LinkPreviewFetcher
    private lateinit var fileStorageManager: FileStorageManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var getAllTagsUseCase: GetAllTagsUseCase
    private lateinit var getTagsForLinkUseCase: GetTagsForLinkUseCase
    private lateinit var saveTagUseCase: SaveTagUseCase
    private lateinit var setTagsForLinkUseCase: SetTagsForLinkUseCase

    private val testDispatcher = StandardTestDispatcher()

    private val testCollection = Collection(
        id = "collection-1",
        name = "Work",
        color = "#FF5733",
        createdAt = 1000L,
        updatedAt = 2000L
    )

    private val testTag = Tag(
        id = "tag-1",
        name = "Important",
        color = "#00FF00"
    )

    private val testLink = Link(
        id = "link-1",
        url = "https://example.com",
        title = "Example",
        description = "Example description",
        collectionId = "collection-1",
        isFavorite = true,
        hideFromHome = false,
        createdAt = 1000L,
        updatedAt = 2000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        savedStateHandle = SavedStateHandle()
        saveLinkUseCase = mockk()
        updateLinkUseCase = mockk()
        getLinkByIdUseCase = mockk()
        getAllCollectionsUseCase = mockk()
        saveCollectionUseCase = mockk()
        linkPreviewFetcher = mockk()
        fileStorageManager = mockk()
        preferencesManager = mockk()
        getAllTagsUseCase = mockk()
        getTagsForLinkUseCase = mockk()
        saveTagUseCase = mockk()
        setTagsForLinkUseCase = mockk()

        every { getAllCollectionsUseCase() } returns flowOf(listOf(testCollection))
        every { getAllTagsUseCase() } returns flowOf(listOf(testTag))
        every { preferencesManager.shouldShowPreviewFetchSuggestion() } returns true
        coEvery { setTagsForLinkUseCase(any(), any()) } returns Result.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = AddEditLinkViewModel(
            savedStateHandle,
            saveLinkUseCase,
            updateLinkUseCase,
            getLinkByIdUseCase,
            getAllCollectionsUseCase,
            saveCollectionUseCase,
            linkPreviewFetcher,
            fileStorageManager,
            preferencesManager,
            getAllTagsUseCase,
            getTagsForLinkUseCase,
            saveTagUseCase,
            setTagsForLinkUseCase
        )
    }

    @Test
    fun `initial state loads collections and tags`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.collections.size)
            assertEquals(1, state.allTags.size)
        }
    }

    @Test
    fun `OnTitleChange updates title`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnTitleChange("New Title"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("New Title", state.title)
        }
    }

    @Test
    fun `OnDescriptionChange updates description`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnDescriptionChange("New Description"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("New Description", state.description)
        }
    }

    @Test
    fun `OnUrlChange updates URL`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnUrlChange("https://newurl.com"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("https://newurl.com", state.url)
        }
    }

    @Test
    fun `OnNoteChange updates note`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnNoteChange("Some notes"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Some notes", state.note)
        }
    }

    @Test
    fun `OnCollectionSelect updates selected collection`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnCollectionSelect("collection-1"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("collection-1", state.selectedCollectionId)
        }
    }

    @Test
    fun `OnCollectionSelect with null resets hideFromHome`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnCollectionSelect("collection-1"))
        viewModel.onEvent(AddEditLinkEvent.OnToggleHideFromHome)
        viewModel.onEvent(AddEditLinkEvent.OnCollectionSelect(null))

        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.selectedCollectionId)
            assertFalse(state.hideFromHome)
        }
    }

    @Test
    fun `OnToggleFavorite toggles favorite`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnToggleFavorite)

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isFavorite)
        }
    }

    @Test
    fun `OnToggleHideFromHome toggles hideFromHome`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnToggleHideFromHome)

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.hideFromHome)
        }
    }

    @Test
    fun `OnCreateCollectionClick shows dialog`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnCreateCollectionClick)

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.showCreateCollectionDialog)
        }
    }

    @Test
    fun `OnCreateCollectionDismiss hides dialog and resets form`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnCreateCollectionClick)
        viewModel.onEvent(AddEditLinkEvent.OnNewCollectionNameChange("Test"))
        viewModel.onEvent(AddEditLinkEvent.OnCreateCollectionDismiss)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showCreateCollectionDialog)
            assertEquals("", state.newCollectionName)
        }
    }

    @Test
    fun `OnNewCollectionNameChange updates name`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnNewCollectionNameChange("New Collection"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("New Collection", state.newCollectionName)
        }
    }

    @Test
    fun `OnNewCollectionColorChange updates color`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnNewCollectionColorChange("#FF0000"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("#FF0000", state.newCollectionColor)
        }
    }

    @Test
    fun `OnTagSelected adds tag to selected tags`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnTagSelected(testTag))

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.selectedTags.any { it.id == testTag.id })
        }
    }

    @Test
    fun `OnTagRemoved removes tag from selected tags`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnTagSelected(testTag))
        viewModel.onEvent(AddEditLinkEvent.OnTagRemoved(testTag))

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.selectedTags.any { it.id == testTag.id })
        }
    }

    @Test
    fun `OnSave with valid data saves link and emits LinkSaved`() = runTest {
        coEvery { saveLinkUseCase(any()) } returns Result.Success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnUrlChange("https://example.com"))
        viewModel.onEvent(AddEditLinkEvent.OnTitleChange("Test Title"))

        viewModel.uiEvents.test {
            viewModel.onEvent(AddEditLinkEvent.OnSave)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is AddEditLinkUiEvent.LinkSaved)
        }
    }

    @Test
    fun `OnSave with empty URL shows error`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnTitleChange("Test Title"))
        viewModel.onEvent(AddEditLinkEvent.OnSave)

        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `OnSave with empty title shows error`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnUrlChange("https://example.com"))
        viewModel.onEvent(AddEditLinkEvent.OnSave)

        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `edit mode loads existing link`() = runTest {
        savedStateHandle = SavedStateHandle(mapOf("linkId" to "link-1"))
        every { getLinkByIdUseCase("link-1") } returns flowOf(testLink)
        every { getTagsForLinkUseCase("link-1") } returns flowOf(listOf(testTag))

        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isEditMode)
            assertEquals("Example", state.title)
            assertEquals("https://example.com", state.url)
        }
    }

    @Test
    fun `edit mode uses updateLinkUseCase on save`() = runTest {
        savedStateHandle = SavedStateHandle(mapOf("linkId" to "link-1"))
        every { getLinkByIdUseCase("link-1") } returns flowOf(testLink)
        every { getTagsForLinkUseCase("link-1") } returns flowOf(emptyList())
        coEvery { updateLinkUseCase(any()) } returns Result.Success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(AddEditLinkEvent.OnSave)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is AddEditLinkUiEvent.LinkSaved)
        }

        coVerify { updateLinkUseCase(any()) }
    }

    @Test
    fun `prefilled URL from navigation auto-fetches preview`() = runTest {
        savedStateHandle = SavedStateHandle(mapOf("url" to "https://example.com"))
        coEvery { linkPreviewFetcher.fetchPreview(any()) } returns null

        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("https://example.com", state.url)
        }
    }

    @Test
    fun `prefilled title from share intent sets title`() = runTest {
        savedStateHandle = SavedStateHandle(mapOf("title" to "Shared Title"))

        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Shared Title", state.title)
        }
    }

    @Test
    fun `preselected collection from navigation sets collection`() = runTest {
        savedStateHandle = SavedStateHandle(mapOf("collectionId" to "collection-1"))

        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("collection-1", state.selectedCollectionId)
        }
    }

    @Test
    fun `OnDismissPreviewSuggestion updates preference and hides suggestion`() = runTest {
        every { preferencesManager.dismissPreviewFetchSuggestion() } returns Unit

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AddEditLinkEvent.OnDismissPreviewSuggestion)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showPreviewFetchSuggestion)
        }
    }
}
