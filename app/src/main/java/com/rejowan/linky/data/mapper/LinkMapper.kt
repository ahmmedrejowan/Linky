package com.rejowan.linky.data.mapper

import com.rejowan.linky.data.local.database.entity.LinkEntity
import com.rejowan.linky.domain.model.Link

object LinkMapper {
    // Domain to Entity
    fun Link.toEntity(syncToRemote: Boolean = false, userId: String? = null): LinkEntity {
        return LinkEntity(
            id = id,
            title = title,
            description = description,
            url = url,
            note = note,
            collectionId = collectionId,
            previewImagePath = previewImagePath,
            previewUrl = previewUrl,
            isFavorite = isFavorite,
            isArchived = isArchived,
            deletedAt = deletedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncToRemote = syncToRemote,
            userId = userId
        )
    }

    // Entity to Domain
    fun LinkEntity.toDomain(): Link {
        return Link(
            id = id,
            title = title,
            description = description,
            url = url,
            note = note,
            collectionId = collectionId,
            previewImagePath = previewImagePath,
            previewUrl = previewUrl,
            isFavorite = isFavorite,
            isArchived = isArchived,
            deletedAt = deletedAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    // List conversions
    fun List<LinkEntity>.toDomainList(): List<Link> = map { it.toDomain() }
    fun List<Link>.toEntityList(syncToRemote: Boolean = false, userId: String? = null): List<LinkEntity> =
        map { it.toEntity(syncToRemote, userId) }
}
