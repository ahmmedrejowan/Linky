package com.rejowan.linky.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for UpdateRepositoryImpl.
 *
 * Note: HTTP-related tests are integration tests and should be run separately.
 * These tests focus on DataStore operations and version comparison logic.
 */
class UpdateRepositoryImplTest {

    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        dataStore = mockk(relaxed = true)
        every { dataStore.data } returns flowOf(emptyPreferences())
    }

    // ============ DataStore Operations Tests ============

    @Test
    fun `getLastCheckTime returns stored value`() = runTest {
        val prefs = preferencesOf(longPreferencesKey("update_last_check_time") to 12345L)
        every { dataStore.data } returns flowOf(prefs)

        val httpClient = mockk<io.ktor.client.HttpClient>(relaxed = true)
        val repository = UpdateRepositoryImpl(httpClient, dataStore)

        val result = repository.getLastCheckTime()

        assertEquals(12345L, result)
    }

    @Test
    fun `getLastCheckTime returns 0 when not set`() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())

        val httpClient = mockk<io.ktor.client.HttpClient>(relaxed = true)
        val repository = UpdateRepositoryImpl(httpClient, dataStore)

        val result = repository.getLastCheckTime()

        assertEquals(0L, result)
    }

    @Test
    fun `shouldSkipVersion returns true for skipped version`() = runTest {
        val prefs = preferencesOf(stringSetPreferencesKey("update_skipped_versions") to setOf("2.0.0"))
        every { dataStore.data } returns flowOf(prefs)

        val httpClient = mockk<io.ktor.client.HttpClient>(relaxed = true)
        val repository = UpdateRepositoryImpl(httpClient, dataStore)

        val result = repository.shouldSkipVersion("2.0.0")

        assertTrue(result)
    }

    @Test
    fun `shouldSkipVersion returns false for non-skipped version`() = runTest {
        val prefs = preferencesOf(stringSetPreferencesKey("update_skipped_versions") to setOf("1.5.0"))
        every { dataStore.data } returns flowOf(prefs)

        val httpClient = mockk<io.ktor.client.HttpClient>(relaxed = true)
        val repository = UpdateRepositoryImpl(httpClient, dataStore)

        val result = repository.shouldSkipVersion("2.0.0")

        assertFalse(result)
    }

    @Test
    fun `shouldSkipVersion returns false when no versions skipped`() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())

        val httpClient = mockk<io.ktor.client.HttpClient>(relaxed = true)
        val repository = UpdateRepositoryImpl(httpClient, dataStore)

        val result = repository.shouldSkipVersion("2.0.0")

        assertFalse(result)
    }

    @Test
    fun `setLastCheckTime does not throw`() = runTest {
        val httpClient = mockk<io.ktor.client.HttpClient>(relaxed = true)
        val repository = UpdateRepositoryImpl(httpClient, dataStore)

        // Should not throw
        repository.setLastCheckTime(99999L)
    }

    @Test
    fun `skipVersion does not throw`() = runTest {
        val httpClient = mockk<io.ktor.client.HttpClient>(relaxed = true)
        val repository = UpdateRepositoryImpl(httpClient, dataStore)

        // Should not throw
        repository.skipVersion("2.0.0")
    }

    @Test
    fun `clearSkippedVersions does not throw`() = runTest {
        val httpClient = mockk<io.ktor.client.HttpClient>(relaxed = true)
        val repository = UpdateRepositoryImpl(httpClient, dataStore)

        // Should not throw
        repository.clearSkippedVersions()
    }
}
