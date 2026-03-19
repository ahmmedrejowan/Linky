package com.rejowan.linky.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Domain model for a vault link (decrypted form).
 * This is the decrypted representation used in the UI.
 */
data class VaultLink(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val description: String? = null,
    val notes: String? = null,
    val previewUrl: String? = null,
    val previewImagePath: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * JSON structure for encrypted vault link data
 */
@Serializable
data class VaultLinkData(
    val url: String,
    val title: String,
    val description: String?,
    val notes: String?,
    val previewUrl: String? = null,
    val previewImagePath: String? = null,
    val isFavorite: Boolean = false
)
