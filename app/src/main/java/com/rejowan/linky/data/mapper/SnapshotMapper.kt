package com.rejowan.linky.data.mapper

import com.rejowan.linky.data.local.database.entity.SnapshotEntity
import com.rejowan.linky.domain.model.Snapshot
import com.rejowan.linky.domain.model.SnapshotType

object SnapshotMapper {
    // Domain to Entity
    fun Snapshot.toEntity(): SnapshotEntity {
        return SnapshotEntity(
            id = id,
            linkId = linkId,
            type = type.name,
            filePath = filePath,
            fileSize = fileSize,
            createdAt = createdAt
        )
    }

    // Entity to Domain
    fun SnapshotEntity.toDomain(): Snapshot {
        return Snapshot(
            id = id,
            linkId = linkId,
            type = SnapshotType.fromString(type),
            filePath = filePath,
            fileSize = fileSize,
            createdAt = createdAt
        )
    }

    // List conversions
    fun List<SnapshotEntity>.toDomainList(): List<Snapshot> = map { it.toDomain() }
    fun List<Snapshot>.toEntityList(): List<SnapshotEntity> = map { it.toEntity() }
}
