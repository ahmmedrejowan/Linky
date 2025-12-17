package com.rejowan.linky.domain.usecase.tag

import com.rejowan.linky.domain.model.Tag
import com.rejowan.linky.domain.model.TagWithCount
import com.rejowan.linky.domain.repository.TagRepository
import com.rejowan.linky.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TagUseCasesTest {

    private lateinit var tagRepository: TagRepository

    private lateinit var getAllTagsUseCase: GetAllTagsUseCase
    private lateinit var getTagByIdUseCase: GetTagByIdUseCase
    private lateinit var getTagsWithLinkCountUseCase: GetTagsWithLinkCountUseCase
    private lateinit var saveTagUseCase: SaveTagUseCase
    private lateinit var updateTagUseCase: UpdateTagUseCase
    private lateinit var deleteTagUseCase: DeleteTagUseCase
    private lateinit var getTagsForLinkUseCase: GetTagsForLinkUseCase
    private lateinit var setTagsForLinkUseCase: SetTagsForLinkUseCase
    private lateinit var getLinksByTagUseCase: GetLinksByTagUseCase

    private val testTag = Tag(
        id = "tag-1",
        name = "Android",
        color = "#3DDC84",
        createdAt = 1000L,
        updatedAt = 2000L
    )

    private val testTag2 = Tag(
        id = "tag-2",
        name = "Kotlin",
        color = "#7F52FF",
        createdAt = 3000L,
        updatedAt = 4000L
    )

    @Before
    fun setUp() {
        tagRepository = mockk()

        getAllTagsUseCase = GetAllTagsUseCase(tagRepository)
        getTagByIdUseCase = GetTagByIdUseCase(tagRepository)
        getTagsWithLinkCountUseCase = GetTagsWithLinkCountUseCase(tagRepository)
        saveTagUseCase = SaveTagUseCase(tagRepository)
        updateTagUseCase = UpdateTagUseCase(tagRepository)
        deleteTagUseCase = DeleteTagUseCase(tagRepository)
        getTagsForLinkUseCase = GetTagsForLinkUseCase(tagRepository)
        setTagsForLinkUseCase = SetTagsForLinkUseCase(tagRepository)
        getLinksByTagUseCase = GetLinksByTagUseCase(tagRepository)
    }

    // GetAllTagsUseCase Tests
    @Test
    fun `getAllTags returns all tags from repository`() = runTest {
        val tags = listOf(testTag, testTag2)
        every { tagRepository.getAllTags() } returns flowOf(tags)

        val result = getAllTagsUseCase().first()

        assertEquals(2, result.size)
        assertEquals(testTag, result[0])
        assertEquals(testTag2, result[1])
    }

    @Test
    fun `getAllTags returns empty list when no tags`() = runTest {
        every { tagRepository.getAllTags() } returns flowOf(emptyList())

        val result = getAllTagsUseCase().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllTagsOnce returns all tags once`() = runTest {
        val tags = listOf(testTag, testTag2)
        coEvery { tagRepository.getAllTagsOnce() } returns tags

        val result = getAllTagsUseCase.once()

        assertEquals(2, result.size)
    }

    // GetTagByIdUseCase Tests
    @Test
    fun `getTagById returns tag when found`() = runTest {
        every { tagRepository.getTagById("tag-1") } returns flowOf(testTag)

        val result = getTagByIdUseCase("tag-1").first()

        assertNotNull(result)
        assertEquals(testTag, result)
    }

    @Test
    fun `getTagById returns null when not found`() = runTest {
        every { tagRepository.getTagById("non-existent") } returns flowOf(null)

        val result = getTagByIdUseCase("non-existent").first()

        assertNull(result)
    }

    // GetTagsWithLinkCountUseCase Tests
    @Test
    fun `getTagsWithLinkCount returns tags with counts`() = runTest {
        val tagsWithCount = listOf(
            TagWithCount("tag-1", "Android", "#3DDC84", 1000L, 2000L, 5),
            TagWithCount("tag-2", "Kotlin", "#7F52FF", 3000L, 4000L, 10)
        )
        every { tagRepository.getTagsWithLinkCount() } returns flowOf(tagsWithCount)

        val result = getTagsWithLinkCountUseCase().first()

        assertEquals(2, result.size)
        assertEquals(5, result[0].linkCount)
        assertEquals(10, result[1].linkCount)
    }

    @Test
    fun `getTagsWithLinkCount returns empty list when no tags`() = runTest {
        every { tagRepository.getTagsWithLinkCount() } returns flowOf(emptyList())

        val result = getTagsWithLinkCountUseCase().first()

        assertTrue(result.isEmpty())
    }

    // SaveTagUseCase Tests
    @Test
    fun `saveTag succeeds with valid tag`() = runTest {
        coEvery { tagRepository.getTagByName("Android") } returns null
        coEvery { tagRepository.saveTag(any()) } returns Result.Success(Unit)

        val result = saveTagUseCase(testTag)

        assertTrue(result is Result.Success)
    }

    @Test
    fun `saveTag fails with empty name`() = runTest {
        val invalidTag = testTag.copy(name = "")

        val result = saveTagUseCase(invalidTag)

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception is IllegalArgumentException)
        assertTrue(error.exception.message!!.contains("empty"))
    }

    @Test
    fun `saveTag fails with blank name`() = runTest {
        val invalidTag = testTag.copy(name = "   ")

        val result = saveTagUseCase(invalidTag)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `saveTag fails when tag name already exists`() = runTest {
        coEvery { tagRepository.getTagByName("Android") } returns testTag

        val newTag = testTag.copy(id = "tag-new")
        val result = saveTagUseCase(newTag)

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message!!.contains("already exists"))
    }

    @Test
    fun `saveTag trims tag name before saving`() = runTest {
        val tagWithSpaces = testTag.copy(name = "  Android  ")
        coEvery { tagRepository.getTagByName("Android") } returns null
        coEvery { tagRepository.saveTag(any()) } returns Result.Success(Unit)

        saveTagUseCase(tagWithSpaces)

        coVerify { tagRepository.saveTag(match { it.name == "Android" }) }
    }

    // UpdateTagUseCase Tests
    @Test
    fun `updateTag succeeds with valid tag`() = runTest {
        coEvery { tagRepository.getTagByName("Android") } returns null
        coEvery { tagRepository.updateTag(any()) } returns Result.Success(Unit)

        val result = updateTagUseCase(testTag)

        assertTrue(result is Result.Success)
    }

    @Test
    fun `updateTag fails with empty name`() = runTest {
        val invalidTag = testTag.copy(name = "")

        val result = updateTagUseCase(invalidTag)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `updateTag succeeds when same tag has the name`() = runTest {
        coEvery { tagRepository.getTagByName("Android") } returns testTag
        coEvery { tagRepository.updateTag(any()) } returns Result.Success(Unit)

        val result = updateTagUseCase(testTag)

        assertTrue(result is Result.Success)
    }

    @Test
    fun `updateTag fails when different tag has the name`() = runTest {
        coEvery { tagRepository.getTagByName("Android") } returns testTag

        val differentTag = testTag2.copy(name = "Android")
        val result = updateTagUseCase(differentTag)

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message!!.contains("already exists"))
    }

    @Test
    fun `updateTag trims name and updates timestamp`() = runTest {
        val tagWithSpaces = testTag.copy(name = "  Android  ")
        coEvery { tagRepository.getTagByName("Android") } returns null
        coEvery { tagRepository.updateTag(any()) } returns Result.Success(Unit)

        updateTagUseCase(tagWithSpaces)

        coVerify { tagRepository.updateTag(match {
            it.name == "Android" && it.updatedAt > tagWithSpaces.createdAt
        }) }
    }

    // DeleteTagUseCase Tests
    @Test
    fun `deleteTag succeeds`() = runTest {
        coEvery { tagRepository.deleteTag("tag-1") } returns Result.Success(Unit)

        val result = deleteTagUseCase("tag-1")

        assertTrue(result is Result.Success)
        coVerify { tagRepository.deleteTag("tag-1") }
    }

    @Test
    fun `deleteTag returns error when repository fails`() = runTest {
        coEvery { tagRepository.deleteTag("tag-1") } returns Result.Error(Exception("Delete failed"))

        val result = deleteTagUseCase("tag-1")

        assertTrue(result is Result.Error)
    }

    // GetTagsForLinkUseCase Tests
    @Test
    fun `getTagsForLink returns tags for link`() = runTest {
        val tags = listOf(testTag, testTag2)
        every { tagRepository.getTagsForLink("link-1") } returns flowOf(tags)

        val result = getTagsForLinkUseCase("link-1").first()

        assertEquals(2, result.size)
    }

    @Test
    fun `getTagsForLink returns empty list when no tags`() = runTest {
        every { tagRepository.getTagsForLink("link-1") } returns flowOf(emptyList())

        val result = getTagsForLinkUseCase("link-1").first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getTagsForLinkOnce returns tags once`() = runTest {
        val tags = listOf(testTag, testTag2)
        coEvery { tagRepository.getTagsForLinkOnce("link-1") } returns tags

        val result = getTagsForLinkUseCase.once("link-1")

        assertEquals(2, result.size)
    }

    // SetTagsForLinkUseCase Tests
    @Test
    fun `setTagsForLink succeeds`() = runTest {
        val tagIds = listOf("tag-1", "tag-2")
        coEvery { tagRepository.setTagsForLink("link-1", tagIds) } returns Result.Success(Unit)

        val result = setTagsForLinkUseCase("link-1", tagIds)

        assertTrue(result is Result.Success)
        coVerify { tagRepository.setTagsForLink("link-1", tagIds) }
    }

    @Test
    fun `setTagsForLink with empty list clears tags`() = runTest {
        coEvery { tagRepository.setTagsForLink("link-1", emptyList()) } returns Result.Success(Unit)

        val result = setTagsForLinkUseCase("link-1", emptyList())

        assertTrue(result is Result.Success)
    }

    @Test
    fun `setTagsForLink returns error when repository fails`() = runTest {
        coEvery { tagRepository.setTagsForLink(any(), any()) } returns Result.Error(Exception("Failed"))

        val result = setTagsForLinkUseCase("link-1", listOf("tag-1"))

        assertTrue(result is Result.Error)
    }

    // GetLinksByTagUseCase Tests
    @Test
    fun `getLinksByTag returns link IDs for tag`() = runTest {
        val linkIds = listOf("link-1", "link-2", "link-3")
        every { tagRepository.getLinkIdsForTag("tag-1") } returns flowOf(linkIds)

        val result = getLinksByTagUseCase("tag-1").first()

        assertEquals(3, result.size)
        assertEquals("link-1", result[0])
    }

    @Test
    fun `getLinksByTag returns empty list when no links`() = runTest {
        every { tagRepository.getLinkIdsForTag("tag-1") } returns flowOf(emptyList())

        val result = getLinksByTagUseCase("tag-1").first()

        assertTrue(result.isEmpty())
    }
}
