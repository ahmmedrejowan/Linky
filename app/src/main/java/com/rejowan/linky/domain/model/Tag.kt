package com.rejowan.linky.domain.model

import java.util.UUID

data class Tag(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Tag with associated link count for display purposes
 */
data class TagWithCount(
    val id: String,
    val name: String,
    val color: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val linkCount: Int
)
