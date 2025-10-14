package com.rejowan.linky.data.mapper

import com.rejowan.linky.data.local.database.entity.FolderEntity
import com.rejowan.linky.domain.model.Folder

object FolderMapper {
    // Domain to Entity
    fun Folder.toEntity(syncToRemote: Boolean = false, userId: String? = null): FolderEntity {
        return FolderEntity(
            id = id,
            name = name,
            color = color,
            icon = icon,
            sortOrder = sortOrder,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncToRemote = syncToRemote,
            userId = userId
        )
    }

    // Entity to Domain
    fun FolderEntity.toDomain(): Folder {
        return Folder(
            id = id,
            name = name,
            color = color,
            icon = icon,
            sortOrder = sortOrder,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    // List conversions
    fun List<FolderEntity>.toDomainList(): List<Folder> = map { it.toDomain() }
    fun List<Folder>.toEntityList(syncToRemote: Boolean = false, userId: String? = null): List<FolderEntity> =
        map { it.toEntity(syncToRemote, userId) }
}
