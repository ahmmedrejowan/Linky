package com.rejowan.linky.data.local.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.rejowan.linky.data.local.database.AppDatabase
import com.rejowan.linky.data.local.database.entity.ConfigEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var configDao: ConfigDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        configDao = database.configDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ============ Basic Get/Set Tests ============

    @Test
    fun set_savesToDatabase() = runTest {
        val config = ConfigEntity(key = "test_key", value = "test_value")
        configDao.set(config)

        val result = configDao.get("test_key")
        assertEquals("test_value", result)
    }

    @Test
    fun get_returnsNullForNonExistent() = runTest {
        val result = configDao.get("non_existent_key")
        assertNull(result)
    }

    @Test
    fun set_replacesOnConflict() = runTest {
        configDao.set(ConfigEntity(key = "test_key", value = "original_value"))
        configDao.set(ConfigEntity(key = "test_key", value = "new_value"))

        val result = configDao.get("test_key")
        assertEquals("new_value", result)
    }

    // ============ Observe Tests ============

    @Test
    fun observe_emitsUpdates() = runTest {
        configDao.observe("test_key").test {
            assertNull(awaitItem())

            configDao.set(ConfigEntity(key = "test_key", value = "value1"))
            assertEquals("value1", awaitItem())

            configDao.set(ConfigEntity(key = "test_key", value = "value2"))
            assertEquals("value2", awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observe_emitsNullWhenDeleted() = runTest {
        configDao.set(ConfigEntity(key = "test_key", value = "test_value"))

        configDao.observe("test_key").test {
            assertEquals("test_value", awaitItem())

            configDao.delete("test_key")
            assertNull(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============ Delete Tests ============

    @Test
    fun delete_removesSpecificKey() = runTest {
        configDao.set(ConfigEntity(key = "key1", value = "value1"))
        configDao.set(ConfigEntity(key = "key2", value = "value2"))

        configDao.delete("key1")

        assertNull(configDao.get("key1"))
        assertEquals("value2", configDao.get("key2"))
    }

    @Test
    fun delete_doesNothingIfNotFound() = runTest {
        configDao.set(ConfigEntity(key = "test_key", value = "test_value"))

        configDao.delete("non_existent")

        assertEquals("test_value", configDao.get("test_key"))
    }

    @Test
    fun clearAll_removesAllConfigs() = runTest {
        configDao.set(ConfigEntity(key = "key1", value = "value1"))
        configDao.set(ConfigEntity(key = "key2", value = "value2"))
        configDao.set(ConfigEntity(key = "key3", value = "value3"))

        configDao.clearAll()

        assertNull(configDao.get("key1"))
        assertNull(configDao.get("key2"))
        assertNull(configDao.get("key3"))
    }

    // ============ String Convenience Methods ============

    @Test
    fun getString_returnsStoredValue() = runTest {
        configDao.setString("string_key", "string_value")

        val result = configDao.getString("string_key")
        assertEquals("string_value", result)
    }

    @Test
    fun setString_savesValue() = runTest {
        configDao.setString("key", "value")

        val result = configDao.get("key")
        assertEquals("value", result)
    }

    // ============ Long Convenience Methods ============

    @Test
    fun getLong_returnsStoredValue() = runTest {
        configDao.setLong("long_key", 12345L)

        val result = configDao.getLong("long_key")
        assertEquals(12345L, result)
    }

    @Test
    fun getLong_returnsNullForInvalidFormat() = runTest {
        configDao.set(ConfigEntity(key = "invalid_long", value = "not_a_number"))

        val result = configDao.getLong("invalid_long")
        assertNull(result)
    }

    @Test
    fun getLong_returnsNullForNonExistent() = runTest {
        val result = configDao.getLong("non_existent")
        assertNull(result)
    }

    @Test
    fun setLong_savesValueAsString() = runTest {
        configDao.setLong("key", 99999L)

        val result = configDao.get("key")
        assertEquals("99999", result)
    }

    // ============ Boolean Convenience Methods ============

    @Test
    fun getBoolean_returnsTrueForTrueValue() = runTest {
        configDao.setBoolean("bool_key", true)

        val result = configDao.getBoolean("bool_key")
        assertTrue(result == true)
    }

    @Test
    fun getBoolean_returnsFalseForFalseValue() = runTest {
        configDao.setBoolean("bool_key", false)

        val result = configDao.getBoolean("bool_key")
        assertFalse(result == true)
    }

    @Test
    fun getBoolean_returnsNullForInvalidFormat() = runTest {
        configDao.set(ConfigEntity(key = "invalid_bool", value = "not_a_boolean"))

        val result = configDao.getBoolean("invalid_bool")
        assertNull(result)
    }

    @Test
    fun getBoolean_returnsNullForNonExistent() = runTest {
        val result = configDao.getBoolean("non_existent")
        assertNull(result)
    }

    @Test
    fun setBoolean_savesValueAsString() = runTest {
        configDao.setBoolean("key", true)

        val result = configDao.get("key")
        assertEquals("true", result)
    }

    // ============ ObserveString Tests ============

    @Test
    fun observeString_emitsUpdates() = runTest {
        configDao.observeString("test_key").test {
            assertNull(awaitItem())

            configDao.setString("test_key", "hello")
            assertEquals("hello", awaitItem())

            configDao.setString("test_key", "world")
            assertEquals("world", awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
