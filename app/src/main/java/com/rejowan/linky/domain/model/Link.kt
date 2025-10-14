package com.rejowan.linky.domain.model

import java.util.UUID

data class Link(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val note: String? = null,
    val folderId: String? = null,
    val previewImagePath: String? = null,
    val previewUrl: String? = null,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isDeleted: Boolean get() = deletedAt != null
    val isActive: Boolean get() = !isDeleted && !isArchived
}
