package com.rejowan.linky.data.local.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.rejowan.linky.data.local.database.AppDatabase
import com.rejowan.linky.data.local.database.entity.VaultLinkEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultLinkDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var vaultLinkDao: VaultLinkDao

    private val testVaultLink = VaultLinkEntity(
        id = "vault-link-1",
        encryptedData = "encrypted-json-data",
        iv = "initialization-vector",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        vaultLinkDao = database.vaultLinkDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ============ Insert Tests ============

    @Test
    fun insertVaultLink_savesToDatabase() = runTest {
        vaultLinkDao.insertVaultLink(testVaultLink)

        val result = vaultLinkDao.getVaultLinkById(testVaultLink.id)
        assertNotNull(result)
        assertEquals(testVaultLink.id, result?.id)
        assertEquals(testVaultLink.encryptedData, result?.encryptedData)
        assertEquals(testVaultLink.iv, result?.iv)
    }

    @Test
    fun insertVaultLink_replacesOnConflict() = runTest {
        vaultLinkDao.insertVaultLink(testVaultLink)

        val updatedLink = testVaultLink.copy(encryptedData = "new-encrypted-data")
        vaultLinkDao.insertVaultLink(updatedLink)

        val result = vaultLinkDao.getVaultLinkById(testVaultLink.id)
        assertEquals("new-encrypted-data", result?.encryptedData)
    }

    // ============ Get Tests ============

    @Test
    fun getVaultLinkById_returnsNullForNonExistent() = runTest {
        val result = vaultLinkDao.getVaultLinkById("non-existent")
        assertNull(result)
    }

    @Test
    fun getAllVaultLinks_returnsLinksOrderedByCreatedAtDesc() = runTest {
        val now = System.currentTimeMillis()
        val links = listOf(
            testVaultLink.copy(id = "vault-link-1", createdAt = now - 2000),
            testVaultLink.copy(id = "vault-link-2", createdAt = now - 1000),
            testVaultLink.copy(id = "vault-link-3", createdAt = now)
        )
        links.forEach { vaultLinkDao.insertVaultLink(it) }

        vaultLinkDao.getAllVaultLinks().test {
            val result = awaitItem()
            assertEquals(3, result.size)
            // Should be ordered by createdAt DESC (newest first)
            assertEquals("vault-link-3", result[0].id)
            assertEquals("vault-link-2", result[1].id)
            assertEquals("vault-link-1", result[2].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllVaultLinks_emitsUpdates() = runTest {
        vaultLinkDao.getAllVaultLinks().test {
            assertEquals(0, awaitItem().size)

            vaultLinkDao.insertVaultLink(testVaultLink)
            assertEquals(1, awaitItem().size)

            vaultLinkDao.insertVaultLink(testVaultLink.copy(id = "vault-link-2"))
            assertEquals(2, awaitItem().size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ Update Tests ============

    @Test
    fun updateVaultLink_modifiesExisting() = runTest {
        vaultLinkDao.insertVaultLink(testVaultLink)

        val updated = testVaultLink.copy(
            encryptedData = "updated-encrypted-data",
            updatedAt = System.currentTimeMillis() + 1000
        )
        vaultLinkDao.updateVaultLink(updated)

        val result = vaultLinkDao.getVaultLinkById(testVaultLink.id)
        assertEquals("updated-encrypted-data", result?.encryptedData)
    }

    // ============ Delete Tests ============

    @Test
    fun deleteVaultLink_removesFromDatabase() = runTest {
        vaultLinkDao.insertVaultLink(testVaultLink)

        vaultLinkDao.deleteVaultLink(testVaultLink.id)

        val result = vaultLinkDao.getVaultLinkById(testVaultLink.id)
        assertNull(result)
    }

    @Test
    fun deleteAllVaultLinks_removesAllLinks() = runTest {
        vaultLinkDao.insertVaultLink(testVaultLink)
        vaultLinkDao.insertVaultLink(testVaultLink.copy(id = "vault-link-2"))
        vaultLinkDao.insertVaultLink(testVaultLink.copy(id = "vault-link-3"))

        vaultLinkDao.deleteAllVaultLinks()

        val count = vaultLinkDao.getVaultLinkCountSync()
        assertEquals(0, count)
    }

    // ============ Count Tests ============

    @Test
    fun getVaultLinkCount_returnsCorrectCount() = runTest {
        vaultLinkDao.insertVaultLink(testVaultLink)
        vaultLinkDao.insertVaultLink(testVaultLink.copy(id = "vault-link-2"))
        vaultLinkDao.insertVaultLink(testVaultLink.copy(id = "vault-link-3"))

        vaultLinkDao.getVaultLinkCount().test {
            val count = awaitItem()
            assertEquals(3, count)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getVaultLinkCount_emitsUpdates() = runTest {
        vaultLinkDao.getVaultLinkCount().test {
            assertEquals(0, awaitItem())

            vaultLinkDao.insertVaultLink(testVaultLink)
            assertEquals(1, awaitItem())

            vaultLinkDao.deleteVaultLink(testVaultLink.id)
            assertEquals(0, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getVaultLinkCountSync_returnsCorrectCount() = runTest {
        vaultLinkDao.insertVaultLink(testVaultLink)
        vaultLinkDao.insertVaultLink(testVaultLink.copy(id = "vault-link-2"))

        val count = vaultLinkDao.getVaultLinkCountSync()
        assertEquals(2, count)
    }

    @Test
    fun getVaultLinkCountSync_returnsZeroWhenEmpty() = runTest {
        val count = vaultLinkDao.getVaultLinkCountSync()
        assertEquals(0, count)
    }
}
