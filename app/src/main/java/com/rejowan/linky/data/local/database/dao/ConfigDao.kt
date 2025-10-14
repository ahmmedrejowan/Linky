package com.rejowan.linky.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rejowan.linky.data.local.database.entity.ConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT value FROM config WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM config WHERE `key` = :key")
    fun observe(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(config: ConfigEntity)

    @Query("DELETE FROM config WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM config")
    suspend fun clearAll()

    // Convenience methods
    suspend fun getString(key: String): String? = get(key)

    suspend fun getLong(key: String): Long? = get(key)?.toLongOrNull()

    suspend fun getBoolean(key: String): Boolean? = get(key)?.toBooleanStrictOrNull()

    suspend fun setString(key: String, value: String) {
        set(ConfigEntity(key, value))
    }

    suspend fun setLong(key: String, value: Long) {
        set(ConfigEntity(key, value.toString()))
    }

    suspend fun setBoolean(key: String, value: Boolean) {
        set(ConfigEntity(key, value.toString()))
    }

    fun observeString(key: String): Flow<String?> = observe(key)
}
