package com.rejowan.linky.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for staging links that are queued to be moved to the vault.
 * These links are stored unencrypted temporarily and will be encrypted
 * and moved to vault_links when the vault is next unlocked.
 */
@Entity(tableName = "pending_vault_links")
data class PendingVaultLinkEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "notes")
    val notes: String?,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long,

    @ColumnInfo(name = "queuedAt")
    val queuedAt: Long = System.currentTimeMillis()
)
