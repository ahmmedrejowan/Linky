package com.rejowan.linky.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table for many-to-many relationship between Links and Tags
 */
@Entity(
    tableName = "link_tags",
    primaryKeys = ["linkId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = LinkEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["linkId"]),
        Index(value = ["tagId"])
    ]
)
data class LinkTagCrossRef(
    @ColumnInfo(name = "linkId")
    val linkId: String,

    @ColumnInfo(name = "tagId")
    val tagId: String
)
