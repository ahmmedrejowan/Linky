package com.rejowan.linky.data.repository

import app.cash.turbine.test
import com.rejowan.linky.data.local.database.dao.CollectionDao
import com.rejowan.linky.data.local.database.dao.CollectionWithCount
import com.rejowan.linky.data.local.database.entity.CollectionEntity
import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.util.Result
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionRepositoryImplTest {

    private lateinit var collectionDao: CollectionDao
    private lateinit var repository: CollectionRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    private val testCollectionEntity = CollectionEntity(
        id = "collection-1",
        name = "Work Links",
        color = "#FF5733",
        icon = "work",
        isFavorite = false,
        sortOrder = 0,
        createdAt = 1000L,
        updatedAt = 2000L
    )

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

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        collectionDao = mockk(relaxed = true)
        repository = CollectionRepositoryImpl(collectionDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============ Get Operations Tests ============

    @Test
    fun `getAllCollections returns mapped domain models`() = runTest {
        val entities = listOf(testCollectionEntity)
        every { collectionDao.getAllCollections() } returns flowOf(entities)

        repository.getAllCollections().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(testCollection.id, result[0].id)
            assertEquals(testCollection.name, result[0].name)
            cancelAndIgnoreRemainingEvents()
        }

        verify { collectionDao.getAllCollections() }
    }

    @Test
    fun `getAllCollections returns empty list when no collections`() = runTest {
        every { collectionDao.getAllCollections() } returns flowOf(emptyList())

        repository.getAllCollections().test {
            val result = awaitItem()
            assertTrue(result.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getCollectionById returns collection when exists`() = runTest {
        every { collectionDao.getByIdFlow("collection-1") } returns flowOf(testCollectionEntity)

        repository.getCollectionById("collection-1").test {
            val result = awaitItem()
            assertEquals(testCollection.id, result?.id)
            assertEquals(testCollection.name, result?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getCollectionById returns null when not exists`() = runTest {
        every { collectionDao.getByIdFlow("non-existent") } returns flowOf(null)

        repository.getCollectionById("non-existent").test {
            val result = awaitItem()
            assertNull(result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getCollectionByIdOnce returns collection when exists`() = runTest {
        coEvery { collectionDao.getById("collection-1") } returns testCollectionEntity

        val result = repository.getCollectionByIdOnce("collection-1")

        assertEquals(testCollection.id, result?.id)
        coVerify { collectionDao.getById("collection-1") }
    }

    @Test
    fun `getCollectionByIdOnce returns null when not exists`() = runTest {
        coEvery { collectionDao.getById("non-existent") } returns null

        val result = repository.getCollectionByIdOnce("non-existent")

        assertNull(result)
    }

    @Test
    fun `getCollectionsWithLinkCount returns collections with counts`() = runTest {
        val collectionWithCount = CollectionWithCount(
            collection = testCollectionEntity,
            linkCount = 5
        )
        every { collectionDao.getCollectionsWithCount() } returns flowOf(listOf(collectionWithCount))
        coEvery { collectionDao.getPreviewsForCollection(any()) } returns emptyList()

        repository.getCollectionsWithLinkCount().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(testCollection.id, result[0].collection.id)
            assertEquals(5, result[0].linkCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ Save Operations Tests ============

    @Test
    fun `saveCollection returns Success when insert succeeds`() = runTest {
        coEvery { collectionDao.insert(any()) } just Runs

        val result = repository.saveCollection(testCollection)

        assertTrue(result is Result.Success)
        coVerify { collectionDao.insert(any()) }
    }

    @Test
    fun `saveCollection returns Error when insert fails`() = runTest {
        coEvery { collectionDao.insert(any()) } throws RuntimeException("Database error")

        val result = repository.saveCollection(testCollection)

        assertTrue(result is Result.Error)
    }

    // ============ Update Operations Tests ============

    @Test
    fun `updateCollection returns Success when update succeeds`() = runTest {
        coEvery { collectionDao.update(any()) } just Runs

        val result = repository.updateCollection(testCollection)

        assertTrue(result is Result.Success)
        coVerify { collectionDao.update(any()) }
    }

    @Test
    fun `updateCollection returns Error when update fails`() = runTest {
        coEvery { collectionDao.update(any()) } throws RuntimeException("Database error")

        val result = repository.updateCollection(testCollection)

        assertTrue(result is Result.Error)
    }

    // ============ Delete Operations Tests ============

    @Test
    fun `deleteCollection returns Success when collection exists`() = runTest {
        coEvery { collectionDao.getById("collection-1") } returns testCollectionEntity
        coEvery { collectionDao.delete(any()) } just Runs

        val result = repository.deleteCollection("collection-1")

        assertTrue(result is Result.Success)
        coVerify { collectionDao.delete(testCollectionEntity) }
    }

    @Test
    fun `deleteCollection returns Error when collection not found`() = runTest {
        coEvery { collectionDao.getById("non-existent") } returns null

        val result = repository.deleteCollection("non-existent")

        assertTrue(result is Result.Error)
        assertEquals("Collection not found", (result as Result.Error).exception.message)
    }

    @Test
    fun `deleteCollection returns Error when delete fails`() = runTest {
        coEvery { collectionDao.getById("collection-1") } returns testCollectionEntity
        coEvery { collectionDao.delete(any()) } throws RuntimeException("Delete failed")

        val result = repository.deleteCollection("collection-1")

        assertTrue(result is Result.Error)
    }

    // ============ Count Operations Tests ============

    @Test
    fun `countCollections returns correct count`() = runTest {
        coEvery { collectionDao.countCollections() } returns 5

        val result = repository.countCollections()

        assertEquals(5, result)
    }
}
