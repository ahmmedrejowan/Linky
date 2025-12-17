package com.rejowan.linky.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing encrypted vault links.
 * The actual link data (url, title, description, notes) is stored encrypted in encryptedData.
 * Only id and timestamps are stored in plaintext for indexing/sorting.
 */
@Entity(tableName = "vault_links")
data class VaultLinkEntity(
    @PrimaryKey
    val id: String,

    /**
     * AES-256-GCM encrypted JSON blob containing:
     * - url: String
     * - title: String
     * - description: String?
     * - notes: String?
     */
    @ColumnInfo(name = "encryptedData")
    val encryptedData: String,

    /**
     * Initialization vector used for encryption
     */
    @ColumnInfo(name = "iv")
    val iv: String,

    /**
     * When the vault link was created (plaintext for sorting)
     */
    @ColumnInfo(name = "createdAt")
    val createdAt: Long,

    /**
     * When the vault link was last updated (plaintext for sorting)
     */
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long
)
