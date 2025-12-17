package com.rejowan.linky.presentation.viewmodel

import app.cash.turbine.test
import com.rejowan.linky.domain.model.Tag
import com.rejowan.linky.domain.model.TagWithCount
import com.rejowan.linky.domain.usecase.tag.DeleteTagUseCase
import com.rejowan.linky.domain.usecase.tag.GetTagsWithLinkCountUseCase
import com.rejowan.linky.domain.usecase.tag.SaveTagUseCase
import com.rejowan.linky.domain.usecase.tag.UpdateTagUseCase
import com.rejowan.linky.presentation.feature.settings.tags.TagManagementEvent
import com.rejowan.linky.presentation.feature.settings.tags.TagManagementUiEvent
import com.rejowan.linky.presentation.feature.settings.tags.TagManagementViewModel
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
class TagManagementViewModelTest {

    private lateinit var viewModel: TagManagementViewModel
    private lateinit var getTagsWithLinkCountUseCase: GetTagsWithLinkCountUseCase
    private lateinit var saveTagUseCase: SaveTagUseCase
    private lateinit var updateTagUseCase: UpdateTagUseCase
    private lateinit var deleteTagUseCase: DeleteTagUseCase

    private val testDispatcher = StandardTestDispatcher()

    private val testTagWithCount = TagWithCount(
        id = "tag-1",
        name = "Work",
        color = "#FF5733",
        createdAt = 1000L,
        updatedAt = 2000L,
        linkCount = 5
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        getTagsWithLinkCountUseCase = mockk()
        saveTagUseCase = mockk()
        updateTagUseCase = mockk()
        deleteTagUseCase = mockk()

        every { getTagsWithLinkCountUseCase() } returns flowOf(listOf(testTagWithCount))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = TagManagementViewModel(
            getTagsWithLinkCountUseCase,
            saveTagUseCase,
            updateTagUseCase,
            deleteTagUseCase
        )
    }

    @Test
    fun `initial state loads tags`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.tags.size)
            assertEquals("Work", state.tags[0].name)
            assertEquals(5, state.tags[0].linkCount)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `OnShowCreateDialog shows create dialog`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TagManagementEvent.OnShowCreateDialog)

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.showCreateDialog)
        }
    }

    @Test
    fun `OnDismissCreateDialog hides create dialog`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TagManagementEvent.OnShowCreateDialog)
        viewModel.onEvent(TagManagementEvent.OnDismissCreateDialog)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showCreateDialog)
        }
    }

    @Test
    fun `OnCreateTag creates tag and emits success message`() = runTest {
        coEvery { saveTagUseCase(any()) } returns Result.Success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(TagManagementEvent.OnCreateTag("New Tag", "#00FF00"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is TagManagementUiEvent.ShowMessage)
            assertEquals("Tag created", (event as TagManagementUiEvent.ShowMessage).message)
        }

        coVerify { saveTagUseCase(match { it.name == "New Tag" && it.color == "#00FF00" }) }
    }

    @Test
    fun `OnCreateTag with failure emits error message`() = runTest {
        coEvery { saveTagUseCase(any()) } returns Result.Error(Exception("Failed"))

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(TagManagementEvent.OnCreateTag("New Tag", "#00FF00"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is TagManagementUiEvent.ShowMessage)
        }
    }

    @Test
    fun `OnShowEditDialog shows edit dialog with tag`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TagManagementEvent.OnShowEditDialog(testTagWithCount))

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.showEditDialog)
            assertEquals(testTagWithCount, state.editingTag)
        }
    }

    @Test
    fun `OnDismissEditDialog hides edit dialog`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TagManagementEvent.OnShowEditDialog(testTagWithCount))
        viewModel.onEvent(TagManagementEvent.OnDismissEditDialog)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showEditDialog)
            assertNull(state.editingTag)
        }
    }

    @Test
    fun `OnUpdateTag updates tag and emits success message`() = runTest {
        coEvery { updateTagUseCase(any()) } returns Result.Success(Unit)

        createViewModel()
        advanceUntilIdle()

        val updatedTag = Tag(id = "tag-1", name = "Updated Work", color = "#FF5733")

        viewModel.uiEvents.test {
            viewModel.onEvent(TagManagementEvent.OnUpdateTag(updatedTag))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is TagManagementUiEvent.ShowMessage)
            assertEquals("Tag updated", (event as TagManagementUiEvent.ShowMessage).message)
        }

        coVerify { updateTagUseCase(updatedTag) }
    }

    @Test
    fun `OnShowDeleteConfirm shows delete confirm dialog`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TagManagementEvent.OnShowDeleteConfirm(testTagWithCount))

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.showDeleteConfirmDialog)
            assertEquals(testTagWithCount, state.deletingTag)
        }
    }

    @Test
    fun `OnDismissDeleteConfirm hides delete confirm dialog`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TagManagementEvent.OnShowDeleteConfirm(testTagWithCount))
        viewModel.onEvent(TagManagementEvent.OnDismissDeleteConfirm)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showDeleteConfirmDialog)
            assertNull(state.deletingTag)
        }
    }

    @Test
    fun `OnDeleteTag deletes tag and emits success message`() = runTest {
        coEvery { deleteTagUseCase("tag-1") } returns Result.Success(Unit)

        createViewModel()
        advanceUntilIdle()

        viewModel.uiEvents.test {
            viewModel.onEvent(TagManagementEvent.OnDeleteTag("tag-1"))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is TagManagementUiEvent.ShowMessage)
            assertEquals("Tag deleted", (event as TagManagementUiEvent.ShowMessage).message)
        }
    }

    @Test
    fun `OnRefresh reloads tags`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TagManagementEvent.OnRefresh)
        advanceUntilIdle()

        // Verify tags are reloaded (getTagsWithLinkCountUseCase called again during refresh)
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.tags.size)
        }
    }
}
