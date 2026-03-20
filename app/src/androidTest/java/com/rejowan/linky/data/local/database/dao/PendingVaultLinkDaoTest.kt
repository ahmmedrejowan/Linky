package com.rejowan.linky.data.local.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rejowan.linky.data.local.database.AppDatabase
import com.rejowan.linky.data.local.database.entity.PendingVaultLinkEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingVaultLinkDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var pendingVaultLinkDao: PendingVaultLinkDao

    private val testPendingLink = PendingVaultLinkEntity(
        id = "pending-1",
        url = "https://secret.example.com",
        title = "Secret Link",
        description = "A private link",
        notes = "Some notes",
        createdAt = System.currentTimeMillis(),
        queuedAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        pendingVaultLinkDao = database.pendingVaultLinkDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ============ Insert Tests ============

    @Test
    fun insertPendingLink_savesToDatabase() = runTest {
        pendingVaultLinkDao.insertPendingLink(testPendingLink)

        val result = pendingVaultLinkDao.getAllPendingLinks()
        assertEquals(1, result.size)
        assertEquals(testPendingLink.id, result[0].id)
        assertEquals(testPendingLink.url, result[0].url)
        assertEquals(testPendingLink.title, result[0].title)
    }

    @Test
    fun insertPendingLink_replacesOnConflict() = runTest {
        pendingVaultLinkDao.insertPendingLink(testPendingLink)

        val updatedLink = testPendingLink.copy(title = "Updated Title")
        pendingVaultLinkDao.insertPendingLink(updatedLink)

        val result = pendingVaultLinkDao.getAllPendingLinks()
        assertEquals(1, result.size)
        assertEquals("Updated Title", result[0].title)
    }

    // ============ Get Tests ============

    @Test
    fun getAllPendingLinks_returnsLinksOrderedByQueuedAt() = runTest {
        val now = System.currentTimeMillis()
        val links = listOf(
            testPendingLink.copy(id = "pending-3", queuedAt = now),
            testPendingLink.copy(id = "pending-1", queuedAt = now - 2000),
            testPendingLink.copy(id = "pending-2", queuedAt = now - 1000)
        )
        links.forEach { pendingVaultLinkDao.insertPendingLink(it) }

        val result = pendingVaultLinkDao.getAllPendingLinks()
        assertEquals(3, result.size)
        // Should be ordered by queuedAt ASC (oldest first - FIFO)
        assertEquals("pending-1", result[0].id)
        assertEquals("pending-2", result[1].id)
        assertEquals("pending-3", result[2].id)
    }

    @Test
    fun getAllPendingLinks_returnsEmptyListWhenEmpty() = runTest {
        val result = pendingVaultLinkDao.getAllPendingLinks()
        assertTrue(result.isEmpty())
    }

    // ============ Delete Tests ============

    @Test
    fun deletePendingLink_removesSpecificLink() = runTest {
        pendingVaultLinkDao.insertPendingLink(testPendingLink)
        pendingVaultLinkDao.insertPendingLink(testPendingLink.copy(id = "pending-2"))

        pendingVaultLinkDao.deletePendingLink(testPendingLink.id)

        val result = pendingVaultLinkDao.getAllPendingLinks()
        assertEquals(1, result.size)
        assertEquals("pending-2", result[0].id)
    }

    @Test
    fun deletePendingLink_doesNothingIfNotFound() = runTest {
        pendingVaultLinkDao.insertPendingLink(testPendingLink)

        pendingVaultLinkDao.deletePendingLink("non-existent")

        val result = pendingVaultLinkDao.getAllPendingLinks()
        assertEquals(1, result.size)
    }

    @Test
    fun deleteAllPendingLinks_removesAllLinks() = runTest {
        pendingVaultLinkDao.insertPendingLink(testPendingLink)
        pendingVaultLinkDao.insertPendingLink(testPendingLink.copy(id = "pending-2"))
        pendingVaultLinkDao.insertPendingLink(testPendingLink.copy(id = "pending-3"))

        pendingVaultLinkDao.deleteAllPendingLinks()

        val result = pendingVaultLinkDao.getAllPendingLinks()
        assertTrue(result.isEmpty())
    }

    // ============ Count Tests ============

    @Test
    fun getPendingLinkCount_returnsCorrectCount() = runTest {
        pendingVaultLinkDao.insertPendingLink(testPendingLink)
        pendingVaultLinkDao.insertPendingLink(testPendingLink.copy(id = "pending-2"))
        pendingVaultLinkDao.insertPendingLink(testPendingLink.copy(id = "pending-3"))

        val count = pendingVaultLinkDao.getPendingLinkCount()
        assertEquals(3, count)
    }

    @Test
    fun getPendingLinkCount_returnsZeroWhenEmpty() = runTest {
        val count = pendingVaultLinkDao.getPendingLinkCount()
        assertEquals(0, count)
    }

    // ============ Nullable Fields Tests ============

    @Test
    fun insertPendingLink_handlesNullableFields() = runTest {
        val linkWithNulls = testPendingLink.copy(
            description = null,
            notes = null
        )
        pendingVaultLinkDao.insertPendingLink(linkWithNulls)

        val result = pendingVaultLinkDao.getAllPendingLinks()
        assertEquals(1, result.size)
        assertEquals(null, result[0].description)
        assertEquals(null, result[0].notes)
    }
}
