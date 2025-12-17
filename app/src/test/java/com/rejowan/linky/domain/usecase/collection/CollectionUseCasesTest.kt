package com.rejowan.linky.domain.usecase.collection

import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.model.CollectionWithLinkCount
import com.rejowan.linky.domain.repository.CollectionRepository
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

class CollectionUseCasesTest {

    private lateinit var collectionRepository: CollectionRepository

    private lateinit var getAllCollectionsUseCase: GetAllCollectionsUseCase
    private lateinit var getCollectionByIdUseCase: GetCollectionByIdUseCase
    private lateinit var saveCollectionUseCase: SaveCollectionUseCase
    private lateinit var updateCollectionUseCase: UpdateCollectionUseCase
    private lateinit var deleteCollectionUseCase: DeleteCollectionUseCase
    private lateinit var getCollectionsWithLinkCountUseCase: GetCollectionsWithLinkCountUseCase

    private val testCollection = Collection(
        id = "collection-1",
        name = "Work Links",
        color = "#FF5733",
        icon = "work",
        isFavorite = false,
        sortOrder = 0,
        createdAt = 1000L,
        updatedAt = 2000L
    )

    private val testCollection2 = Collection(
        id = "collection-2",
        name = "Personal Links",
        color = "#3366FF",
        icon = "home",
        isFavorite = true,
        sortOrder = 1,
        createdAt = 3000L,
        updatedAt = 4000L
    )

    @Before
    fun setUp() {
        collectionRepository = mockk()

        getAllCollectionsUseCase = GetAllCollectionsUseCase(collectionRepository)
        getCollectionByIdUseCase = GetCollectionByIdUseCase(collectionRepository)
        saveCollectionUseCase = SaveCollectionUseCase(collectionRepository)
        updateCollectionUseCase = UpdateCollectionUseCase(collectionRepository)
        deleteCollectionUseCase = DeleteCollectionUseCase(collectionRepository)
        getCollectionsWithLinkCountUseCase = GetCollectionsWithLinkCountUseCase(collectionRepository)
    }

    // GetAllCollectionsUseCase Tests
    @Test
    fun `getAllCollections returns all collections from repository`() = runTest {
        val collections = listOf(testCollection, testCollection2)
        every { collectionRepository.getAllCollections() } returns flowOf(collections)

        val result = getAllCollectionsUseCase().first()

        assertEquals(2, result.size)
        assertEquals(testCollection, result[0])
        assertEquals(testCollection2, result[1])
    }

    @Test
    fun `getAllCollections returns empty list when no collections`() = runTest {
        every { collectionRepository.getAllCollections() } returns flowOf(emptyList())

        val result = getAllCollectionsUseCase().first()

        assertTrue(result.isEmpty())
    }

    // GetCollectionByIdUseCase Tests
    @Test
    fun `getCollectionById returns collection when found`() = runTest {
        every { collectionRepository.getCollectionById("collection-1") } returns flowOf(testCollection)

        val result = getCollectionByIdUseCase("collection-1").first()

        assertNotNull(result)
        assertEquals(testCollection, result)
    }

    @Test
    fun `getCollectionById returns null when not found`() = runTest {
        every { collectionRepository.getCollectionById("non-existent") } returns flowOf(null)

        val result = getCollectionByIdUseCase("non-existent").first()

        assertNull(result)
    }

    // SaveCollectionUseCase Tests
    @Test
    fun `saveCollection succeeds with valid collection`() = runTest {
        coEvery { collectionRepository.saveCollection(testCollection) } returns Result.Success(Unit)

        val result = saveCollectionUseCase(testCollection)

        assertTrue(result is Result.Success)
        coVerify { collectionRepository.saveCollection(testCollection) }
    }

    @Test
    fun `saveCollection fails with empty name`() = runTest {
        val invalidCollection = testCollection.copy(name = "")

        val result = saveCollectionUseCase(invalidCollection)

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception is IllegalArgumentException)
    }

    @Test
    fun `saveCollection fails with blank name`() = runTest {
        val invalidCollection = testCollection.copy(name = "   ")

        val result = saveCollectionUseCase(invalidCollection)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `saveCollection fails with name exceeding max length`() = runTest {
        val longName = "A".repeat(101)
        val invalidCollection = testCollection.copy(name = longName)

        val result = saveCollectionUseCase(invalidCollection)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `saveCollection fails with invalid color format`() = runTest {
        val invalidCollection = testCollection.copy(color = "not-a-color")

        val result = saveCollectionUseCase(invalidCollection)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `saveCollection succeeds with null color`() = runTest {
        val collectionWithNullColor = testCollection.copy(color = null)
        coEvery { collectionRepository.saveCollection(collectionWithNullColor) } returns Result.Success(Unit)

        val result = saveCollectionUseCase(collectionWithNullColor)

        assertTrue(result is Result.Success)
    }

    @Test
    fun `saveCollection succeeds with valid hex color`() = runTest {
        val collection = testCollection.copy(color = "#AABBCC")
        coEvery { collectionRepository.saveCollection(collection) } returns Result.Success(Unit)

        val result = saveCollectionUseCase(collection)

        assertTrue(result is Result.Success)
    }

    // UpdateCollectionUseCase Tests
    @Test
    fun `updateCollection succeeds with valid collection`() = runTest {
        coEvery { collectionRepository.updateCollection(testCollection) } returns Result.Success(Unit)

        val result = updateCollectionUseCase(testCollection)

        assertTrue(result is Result.Success)
        coVerify { collectionRepository.updateCollection(testCollection) }
    }

    @Test
    fun `updateCollection fails with empty name`() = runTest {
        val invalidCollection = testCollection.copy(name = "")

        val result = updateCollectionUseCase(invalidCollection)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `updateCollection fails with invalid color`() = runTest {
        val invalidCollection = testCollection.copy(color = "red")

        val result = updateCollectionUseCase(invalidCollection)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `updateCollection succeeds with null color`() = runTest {
        val collection = testCollection.copy(color = null)
        coEvery { collectionRepository.updateCollection(collection) } returns Result.Success(Unit)

        val result = updateCollectionUseCase(collection)

        assertTrue(result is Result.Success)
    }

    // DeleteCollectionUseCase Tests
    @Test
    fun `deleteCollection succeeds`() = runTest {
        coEvery { collectionRepository.deleteCollection("collection-1") } returns Result.Success(Unit)

        val result = deleteCollectionUseCase("collection-1")

        assertTrue(result is Result.Success)
        coVerify { collectionRepository.deleteCollection("collection-1") }
    }

    @Test
    fun `deleteCollection returns error when repository fails`() = runTest {
        coEvery { collectionRepository.deleteCollection("collection-1") } returns Result.Error(Exception("Delete failed"))

        val result = deleteCollectionUseCase("collection-1")

        assertTrue(result is Result.Error)
    }

    // GetCollectionsWithLinkCountUseCase Tests
    @Test
    fun `getCollectionsWithLinkCount returns collections with counts`() = runTest {
        val collectionsWithCount = listOf(
            CollectionWithLinkCount(testCollection, 5, listOf("preview1.png", "preview2.png")),
            CollectionWithLinkCount(testCollection2, 10, listOf("preview3.png"))
        )
        every { collectionRepository.getCollectionsWithLinkCount() } returns flowOf(collectionsWithCount)

        val result = getCollectionsWithLinkCountUseCase().first()

        assertEquals(2, result.size)
        assertEquals(5, result[0].linkCount)
        assertEquals(10, result[1].linkCount)
    }

    @Test
    fun `getCollectionsWithLinkCount returns empty list when no collections`() = runTest {
        every { collectionRepository.getCollectionsWithLinkCount() } returns flowOf(emptyList())

        val result = getCollectionsWithLinkCountUseCase().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getCollectionsWithLinkCount includes link previews`() = runTest {
        val previews = listOf("img1.png", "img2.png", "img3.png")
        val collectionWithPreviews = CollectionWithLinkCount(testCollection, 3, previews)
        every { collectionRepository.getCollectionsWithLinkCount() } returns flowOf(listOf(collectionWithPreviews))

        val result = getCollectionsWithLinkCountUseCase().first()

        assertEquals(3, result[0].linkPreviews.size)
        assertEquals("img1.png", result[0].linkPreviews[0])
    }
}
