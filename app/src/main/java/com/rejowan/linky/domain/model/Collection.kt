package com.rejowan.linky.domain.model

import java.util.UUID

data class Collection(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: String? = null,
    val icon: String? = null,
    val isFavorite: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
