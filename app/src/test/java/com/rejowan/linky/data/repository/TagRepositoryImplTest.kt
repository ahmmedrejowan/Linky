package com.rejowan.linky.data.repository

import app.cash.turbine.test
import com.rejowan.linky.data.local.database.dao.TagDao
import com.rejowan.linky.data.local.database.dao.TagWithLinkCount
import com.rejowan.linky.data.local.database.entity.TagEntity
import com.rejowan.linky.domain.model.Tag
import com.rejowan.linky.util.Result
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TagRepositoryImplTest {

    private lateinit var tagDao: TagDao
    private lateinit var repository: TagRepositoryImpl

    private val testTagEntity = TagEntity(
        id = "tag-1",
        name = "Important",
        color = "#FF0000",
        createdAt = 1000L,
        updatedAt = 2000L
    )

    private val testTag = Tag(
        id = "tag-1",
        name = "Important",
        color = "#FF0000",
        createdAt = 1000L,
        updatedAt = 2000L
    )

    @Before
    fun setup() {
        tagDao = mockk(relaxed = true)
        repository = TagRepositoryImpl(tagDao)
    }

    // ============ TAG CRUD Tests ============

    @Test
    fun `getAllTags returns mapped domain models`() = runTest {
        every { tagDao.getAllTags() } returns flowOf(listOf(testTagEntity))

        repository.getAllTags().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(testTag.id, result[0].id)
            assertEquals(testTag.name, result[0].name)
            cancelAndIgnoreRemainingEvents()
        }

        verify { tagDao.getAllTags() }
    }

    @Test
    fun `getAllTagsOnce returns list of tags`() = runTest {
        coEvery { tagDao.getAllTagsOnce() } returns listOf(testTagEntity)

        val result = repository.getAllTagsOnce()

        assertEquals(1, result.size)
        assertEquals(testTag.id, result[0].id)
    }

    @Test
    fun `getTagById returns tag when exists`() = runTest {
        every { tagDao.getByIdFlow("tag-1") } returns flowOf(testTagEntity)

        repository.getTagById("tag-1").test {
            val result = awaitItem()
            assertEquals(testTag.id, result?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getTagById returns null when not exists`() = runTest {
        every { tagDao.getByIdFlow("non-existent") } returns flowOf(null)

        repository.getTagById("non-existent").test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getTagByIdOnce returns tag when exists`() = runTest {
        coEvery { tagDao.getById("tag-1") } returns testTagEntity

        val result = repository.getTagByIdOnce("tag-1")

        assertEquals(testTag.id, result?.id)
    }

    @Test
    fun `getTagByName returns tag when exists`() = runTest {
        coEvery { tagDao.getByName("Important") } returns testTagEntity

        val result = repository.getTagByName("Important")

        assertEquals(testTag.id, result?.id)
        assertEquals("Important", result?.name)
    }

    @Test
    fun `saveTag returns Success when insert succeeds`() = runTest {
        coEvery { tagDao.insert(any()) } just Runs

        val result = repository.saveTag(testTag)

        assertTrue(result is Result.Success)
        coVerify { tagDao.insert(any()) }
    }

    @Test
    fun `saveTag returns Error when insert fails`() = runTest {
        coEvery { tagDao.insert(any()) } throws RuntimeException("Database error")

        val result = repository.saveTag(testTag)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `updateTag returns Success when update succeeds`() = runTest {
        coEvery { tagDao.update(any()) } just Runs

        val result = repository.updateTag(testTag)

        assertTrue(result is Result.Success)
        coVerify { tagDao.update(any()) }
    }

    @Test
    fun `updateTag returns Error when update fails`() = runTest {
        coEvery { tagDao.update(any()) } throws RuntimeException("Database error")

        val result = repository.updateTag(testTag)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `deleteTag returns Success when delete succeeds`() = runTest {
        coEvery { tagDao.deleteById(any()) } just Runs

        val result = repository.deleteTag("tag-1")

        assertTrue(result is Result.Success)
        coVerify { tagDao.deleteById("tag-1") }
    }

    @Test
    fun `deleteTag returns Error when delete fails`() = runTest {
        coEvery { tagDao.deleteById(any()) } throws RuntimeException("Database error")

        val result = repository.deleteTag("tag-1")

        assertTrue(result is Result.Error)
    }

    @Test
    fun `countTags returns correct count`() = runTest {
        coEvery { tagDao.countTags() } returns 10

        val result = repository.countTags()

        assertEquals(10, result)
    }

    @Test
    fun `countTagsFlow emits count updates`() = runTest {
        every { tagDao.countTagsFlow() } returns flowOf(5)

        repository.countTagsFlow().test {
            assertEquals(5, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ TAG WITH COUNT Tests ============

    @Test
    fun `getTagsWithLinkCount returns tags with counts`() = runTest {
        val tagWithCount = TagWithLinkCount(
            id = "tag-1",
            name = "Important",
            color = "#FF0000",
            createdAt = 1000L,
            updatedAt = 2000L,
            linkCount = 5
        )
        every { tagDao.getTagsWithLinkCount() } returns flowOf(listOf(tagWithCount))

        repository.getTagsWithLinkCount().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("tag-1", result[0].id)
            assertEquals(5, result[0].linkCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ LINK-TAG ASSOCIATIONS Tests ============

    @Test
    fun `getTagsForLink returns tags for a link`() = runTest {
        every { tagDao.getTagsForLink("link-1") } returns flowOf(listOf(testTagEntity))

        repository.getTagsForLink("link-1").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(testTag.id, result[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getTagsForLinkOnce returns tags for a link`() = runTest {
        coEvery { tagDao.getTagsForLinkOnce("link-1") } returns listOf(testTagEntity)

        val result = repository.getTagsForLinkOnce("link-1")

        assertEquals(1, result.size)
        assertEquals(testTag.id, result[0].id)
    }

    @Test
    fun `getLinkIdsForTag returns link ids`() = runTest {
        every { tagDao.getLinkIdsForTag("tag-1") } returns flowOf(listOf("link-1", "link-2"))

        repository.getLinkIdsForTag("tag-1").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.contains("link-1"))
            assertTrue(result.contains("link-2"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addTagToLink returns Success when succeeds`() = runTest {
        coEvery { tagDao.addTagToLink(any()) } just Runs

        val result = repository.addTagToLink("link-1", "tag-1")

        assertTrue(result is Result.Success)
        coVerify { tagDao.addTagToLink(any()) }
    }

    @Test
    fun `addTagToLink returns Error when fails`() = runTest {
        coEvery { tagDao.addTagToLink(any()) } throws RuntimeException("Database error")

        val result = repository.addTagToLink("link-1", "tag-1")

        assertTrue(result is Result.Error)
    }

    @Test
    fun `removeTagFromLink returns Success when succeeds`() = runTest {
        coEvery { tagDao.removeTagFromLink(any(), any()) } just Runs

        val result = repository.removeTagFromLink("link-1", "tag-1")

        assertTrue(result is Result.Success)
        coVerify { tagDao.removeTagFromLink("link-1", "tag-1") }
    }

    @Test
    fun `removeTagFromLink returns Error when fails`() = runTest {
        coEvery { tagDao.removeTagFromLink(any(), any()) } throws RuntimeException("Database error")

        val result = repository.removeTagFromLink("link-1", "tag-1")

        assertTrue(result is Result.Error)
    }

    @Test
    fun `setTagsForLink returns Success when succeeds`() = runTest {
        coEvery { tagDao.setTagsForLink(any(), any()) } just Runs

        val result = repository.setTagsForLink("link-1", listOf("tag-1", "tag-2"))

        assertTrue(result is Result.Success)
        coVerify { tagDao.setTagsForLink("link-1", listOf("tag-1", "tag-2")) }
    }

    @Test
    fun `setTagsForLink returns Error when fails`() = runTest {
        coEvery { tagDao.setTagsForLink(any(), any()) } throws RuntimeException("Database error")

        val result = repository.setTagsForLink("link-1", listOf("tag-1", "tag-2"))

        assertTrue(result is Result.Error)
    }

    @Test
    fun `linkHasTag returns true when association exists`() = runTest {
        coEvery { tagDao.linkHasTag("link-1", "tag-1") } returns true

        val result = repository.linkHasTag("link-1", "tag-1")

        assertTrue(result)
    }

    @Test
    fun `linkHasTag returns false when association not exists`() = runTest {
        coEvery { tagDao.linkHasTag("link-1", "tag-1") } returns false

        val result = repository.linkHasTag("link-1", "tag-1")

        assertFalse(result)
    }
}
