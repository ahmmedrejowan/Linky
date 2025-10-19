package com.rejowan.linky.data.mapper

import com.rejowan.linky.data.local.database.entity.CollectionEntity
import com.rejowan.linky.domain.model.Collection

object CollectionMapper {
    // Domain to Entity
    fun Collection.toEntity(syncToRemote: Boolean = false, userId: String? = null): CollectionEntity {
        return CollectionEntity(
            id = id,
            name = name,
            color = color,
            icon = icon,
            isFavorite = isFavorite,
            sortOrder = sortOrder,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncToRemote = syncToRemote,
            userId = userId
        )
    }

    // Entity to Domain
    fun CollectionEntity.toDomain(): Collection {
        return Collection(
            id = id,
            name = name,
            color = color,
            icon = icon,
            isFavorite = isFavorite,
            sortOrder = sortOrder,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    // List conversions
    fun List<CollectionEntity>.toDomainList(): List<Collection> = map { it.toDomain() }
    fun List<Collection>.toEntityList(syncToRemote: Boolean = false, userId: String? = null): List<CollectionEntity> =
        map { it.toEntity(syncToRemote, userId) }
}
