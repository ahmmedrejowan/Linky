package com.rejowan.linky.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rejowan.linky.data.local.database.entity.VaultLinkEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for vault link operations.
 * Note: Data is stored encrypted, decryption happens in the repository layer.
 */
@Dao
interface VaultLinkDao {

    /**
     * Get all vault links ordered by creation date (newest first)
     */
    @Query("SELECT * FROM vault_links ORDER BY createdAt DESC")
    fun getAllVaultLinks(): Flow<List<VaultLinkEntity>>

    /**
     * Get a vault link by ID
     */
    @Query("SELECT * FROM vault_links WHERE id = :id")
    suspend fun getVaultLinkById(id: String): VaultLinkEntity?

    /**
     * Insert a new vault link
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultLink(vaultLink: VaultLinkEntity)

    /**
     * Update an existing vault link
     */
    @Update
    suspend fun updateVaultLink(vaultLink: VaultLinkEntity)

    /**
     * Delete a vault link by ID
     */
    @Query("DELETE FROM vault_links WHERE id = :id")
    suspend fun deleteVaultLink(id: String)

    /**
     * Delete all vault links
     */
    @Query("DELETE FROM vault_links")
    suspend fun deleteAllVaultLinks()

    /**
     * Get the count of vault links
     */
    @Query("SELECT COUNT(*) FROM vault_links")
    fun getVaultLinkCount(): Flow<Int>
}
