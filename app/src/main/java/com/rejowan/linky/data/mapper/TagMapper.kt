package com.rejowan.linky.data.mapper

import com.rejowan.linky.data.local.database.dao.TagWithLinkCount
import com.rejowan.linky.data.local.database.entity.TagEntity
import com.rejowan.linky.domain.model.Tag
import com.rejowan.linky.domain.model.TagWithCount

object TagMapper {

    fun TagEntity.toDomain(): Tag {
        return Tag(
            id = id,
            name = name,
            color = color,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun Tag.toEntity(): TagEntity {
        return TagEntity(
            id = id,
            name = name,
            color = color,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun List<TagEntity>.toDomainList(): List<Tag> {
        return map { it.toDomain() }
    }

    fun List<Tag>.toEntityList(): List<TagEntity> {
        return map { it.toEntity() }
    }

    fun TagWithLinkCount.toDomain(): TagWithCount {
        return TagWithCount(
            id = id,
            name = name,
            color = color,
            createdAt = createdAt,
            updatedAt = updatedAt,
            linkCount = linkCount
        )
    }

    fun List<TagWithLinkCount>.toDomainWithCountList(): List<TagWithCount> {
        return map { it.toDomain() }
    }
}
