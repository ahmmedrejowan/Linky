package com.rejowan.linky.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rejowan.linky.data.local.database.entity.PendingVaultLinkEntity

/**
 * DAO for pending vault link operations.
 * Used for staging links before they are encrypted and moved to vault.
 */
@Dao
interface PendingVaultLinkDao {

    /**
     * Get all pending vault links
     */
    @Query("SELECT * FROM pending_vault_links ORDER BY queuedAt ASC")
    suspend fun getAllPendingLinks(): List<PendingVaultLinkEntity>

    /**
     * Insert a link into the pending queue
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingLink(link: PendingVaultLinkEntity)

    /**
     * Delete a pending link by ID (after it's been processed)
     */
    @Query("DELETE FROM pending_vault_links WHERE id = :id")
    suspend fun deletePendingLink(id: String)

    /**
     * Delete all pending links
     */
    @Query("DELETE FROM pending_vault_links")
    suspend fun deleteAllPendingLinks()

    /**
     * Get the count of pending links
     */
    @Query("SELECT COUNT(*) FROM pending_vault_links")
    suspend fun getPendingLinkCount(): Int
}
