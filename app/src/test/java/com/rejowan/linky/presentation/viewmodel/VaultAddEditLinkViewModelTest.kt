package com.rejowan.linky.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rejowan.linky.domain.model.VaultLink
import com.rejowan.linky.domain.usecase.vault.AddVaultLinkUseCase
import com.rejowan.linky.domain.usecase.vault.GetVaultLinkByIdUseCase
import com.rejowan.linky.domain.usecase.vault.UpdateVaultLinkUseCase
import com.rejowan.linky.presentation.feature.vault.VaultAddEditEvent
import com.rejowan.linky.presentation.feature.vault.VaultAddEditLinkViewModel
import com.rejowan.linky.presentation.feature.vault.VaultAddEditUiEvent
import com.rejowan.linky.util.LinkPreview
import com.rejowan.linky.util.LinkPreviewFetcher
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultAddEditLinkViewModelTest {

    private lateinit var viewModel: VaultAddEditLinkViewModel
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var addVaultLinkUseCase: AddVaultLinkUseCase
    private lateinit var updateVaultLinkUseCase: UpdateVaultLinkUseCase
    private lateinit var getVaultLinkByIdUseCase: GetVaultLinkByIdUseCase
    private lateinit var linkPreviewFetcher: LinkPreviewFetcher

    private val testDispatcher = StandardTestDispatcher()

    private val testVaultLink = VaultLink(
        id = "vault-link-1",
        url = "https://example.com",
        title = "Example Site",
        description = "An example description",
        notes = "Some notes",
        isFavorite = false,
        previewUrl = "https://example.com/image.png",
        previewImagePath = null,
        createdAt = 1000L,
        updatedAt = 2000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        addVaultLinkUseCase = mockk()
        updateVaultLinkUseCase = mockk()
        getVaultLinkByIdUseCase = mockk()
        linkPreviewFetcher = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(linkId: String? = null, prefillUrl: String? = null) {
        val params = mutableMapOf<String, Any?>()
        if (linkId != null) params["linkId"] = linkId
        if (prefillUrl != null) params["url"] = prefillUrl

        savedStateHandle = SavedStateHandle(params)
        viewModel = VaultAddEditLinkViewModel(
            savedStateHandle,
            addVaultLinkUseCase,
            updateVaultLinkUseCase,
            getVaultLinkByIdUseCase,
            linkPreviewFetcher
        )
    }

    // ============ Initialization Tests ============

    @Test
    fun `initial state is add mode with empty fields`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isEditMode)
            assertNull(state.linkId)
            assertEquals("", state.url)
            assertEquals("", state.title)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `initial state with prefill URL sets url`() = runTest {
        createViewModel(prefillUrl = "https://prefilled.com")
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("https://prefilled.com", state.url)
            assertFalse(state.isEditMode)
        }
    }

    @Test
    fun `initial state in edit mode loads existing link`() = runTest {
        coEvery { getVaultLinkByIdUseCase("vault-link-1") } returns testVaultLink

        createViewModel(linkId = "vault-link-1")
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isEditMode)
            assertEquals("vault-link-1", state.linkId)
            assertEquals("https://example.com", state.url)
            assertEquals("Example Site", state.title)
            assertEquals("An example description", state.description)
            assertEquals("Some notes", state.note)
        }
    }

    @Test
    fun `edit mode shows error when link not found`() = runTest {
        coEvery { getVaultLinkByIdUseCase("non-existent") } returns null

        createViewModel(linkId = "non-existent")
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Link not found", state.error)
            assertFalse(state.isLoading)
        }
    }

    // ============ Event Handling Tests ============

    @Test
    fun `OnUrlChange updates url`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultAddEditEvent.OnUrlChange("https://new-url.com"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("https://new-url.com", state.url)
        }
    }

    @Test
    fun `OnTitleChange updates title`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultAddEditEvent.OnTitleChange("New Title"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("New Title", state.title)
        }
    }

    @Test
    fun `OnDescriptionChange updates description`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultAddEditEvent.OnDescriptionChange("New Description"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("New Description", state.description)
        }
    }

    @Test
    fun `OnNoteChange updates note`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultAddEditEvent.OnNoteChange("New Note"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("New Note", state.note)
        }
    }

    @Test
    fun `OnToggleFavorite toggles favorite state`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultAddEditEvent.OnToggleFavorite)

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isFavorite)
        }

        viewModel.onEvent(VaultAddEditEvent.OnToggleFavorite)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isFavorite)
        }
    }

    // ============ Fetch Preview Tests ============

    @Test
    fun `OnFetchPreview fetches and updates preview data`() = runTest {
        val preview = LinkPreview(
            title = "Fetched Title",
            description = "Fetched Description",
            imageUrl = "https://example.com/preview.png",
            url = "https://example.com"
        )
        coEvery { linkPreviewFetcher.fetchPreview(any()) } returns preview

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultAddEditEvent.OnUrlChange("https://example.com"))
        viewModel.onEvent(VaultAddEditEvent.OnFetchPreview)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Fetched Title", state.title)
            assertEquals("Fetched Description", state.description)
            assertEquals("https://example.com/preview.png", state.previewUrl)
            assertFalse(state.isFetchingPreview)
        }
    }

    @Test
    fun `OnFetchPreview does not overwrite existing title`() = runTest {
        val preview = LinkPreview(
            title = "Fetched Title",
            description = "Fetched Description",
            imageUrl = null,
            url = "https://example.com"
        )
        coEvery { linkPreviewFetcher.fetchPreview(any()) } returns preview

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultAddEditEvent.OnUrlChange("https://example.com"))
        viewModel.onEvent(VaultAddEditEvent.OnTitleChange("My Custom Title"))
        viewModel.onEvent(VaultAddEditEvent.OnFetchPreview)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("My Custom Title", state.title)
        }
    }

    @Test
    fun `OnFetchPreview handles null preview gracefully`() = runTest {
        coEvery { linkPreviewFetcher.fetchPreview(any()) } returns null

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultAddEditEvent.OnUrlChange("https://example.com"))
        viewModel.onEvent(VaultAddEditEvent.OnFetchPreview)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("https://example.com", state.title)
            assertFalse(state.isFetchingPreview)
        }
    }

    @Test
    fun `OnFetchPreview does nothing with blank url`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultAddEditEvent.OnFetchPreview)
        advanceUntilIdle()

        coVerify(exactly = 0) { linkPreviewFetcher.fetchPreview(any()) }
    }

    // ============ Save Tests ============

    @Test
    fun `OnSave adds new link and emits LinkSaved`() = runTest {
        coEvery { addVaultLinkUseCase(any()) } returns Result.success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultAddEditEvent.OnUrlChange("https://example.com"))
        viewModel.onEvent(VaultAddEditEvent.OnTitleChange("Example Title"))

        viewModel.uiEvents.test {
            viewModel.onEvent(VaultAddEditEvent.OnSave)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is VaultAddEditUiEvent.LinkSaved)
        }

        coVerify { addVaultLinkUseCase(any()) }
    }

    @Test
    fun `OnSave updates existing link in edit mode`() = runTest {
        coEvery { getVaultLinkByIdUseCase("vault-link-1") } returns testVaultLink
        coEvery { updateVaultLinkUseCase(any()) } returns Result.success(Unit)

        createViewModel(linkId = "vault-link-1")
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(VaultAddEditEvent.OnSave)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is VaultAddEditUiEvent.LinkSaved)
        }

        coVerify { updateVaultLinkUseCase(any()) }
    }

    @Test
    fun `OnSave shows error when URL is blank`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultAddEditEvent.OnTitleChange("Title"))
        viewModel.onEvent(VaultAddEditEvent.OnSave)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("URL is required", state.error)
        }

        coVerify(exactly = 0) { addVaultLinkUseCase(any()) }
    }

    @Test
    fun `OnSave shows error when title is blank`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultAddEditEvent.OnUrlChange("https://example.com"))
        viewModel.onEvent(VaultAddEditEvent.OnSave)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Title is required", state.error)
        }

        coVerify(exactly = 0) { addVaultLinkUseCase(any()) }
    }

    @Test
    fun `OnSave shows error when save fails`() = runTest {
        coEvery { addVaultLinkUseCase(any()) } returns Result.failure(Exception("Save failed"))

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultAddEditEvent.OnUrlChange("https://example.com"))
        viewModel.onEvent(VaultAddEditEvent.OnTitleChange("Title"))
        viewModel.onEvent(VaultAddEditEvent.OnSave)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.error?.contains("failed") == true || state.error?.contains("Save failed") == true)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `OnSave trims whitespace from fields`() = runTest {
        coEvery { addVaultLinkUseCase(any()) } returns Result.success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultAddEditEvent.OnUrlChange("  https://example.com  "))
        viewModel.onEvent(VaultAddEditEvent.OnTitleChange("  Title  "))
        viewModel.onEvent(VaultAddEditEvent.OnDescriptionChange("  Description  "))
        viewModel.onEvent(VaultAddEditEvent.OnSave)
        advanceUntilIdle()

        coVerify {
            addVaultLinkUseCase(match {
                it.url == "https://example.com" &&
                        it.title == "Title" &&
                        it.description == "Description"
            })
        }
    }

    @Test
    fun `OnSave sets description and notes to null when blank`() = runTest {
        coEvery { addVaultLinkUseCase(any()) } returns Result.success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(VaultAddEditEvent.OnUrlChange("https://example.com"))
        viewModel.onEvent(VaultAddEditEvent.OnTitleChange("Title"))
        viewModel.onEvent(VaultAddEditEvent.OnDescriptionChange("   "))
        viewModel.onEvent(VaultAddEditEvent.OnNoteChange("   "))
        viewModel.onEvent(VaultAddEditEvent.OnSave)
        advanceUntilIdle()

        coVerify {
            addVaultLinkUseCase(match {
                it.description == null && it.notes == null
            })
        }
    }
}
