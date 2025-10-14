package com.rejowan.linky.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "snapshots",
    foreignKeys = [
        ForeignKey(
            entity = LinkEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["linkId"]),
        Index(value = ["type"])
    ]
)
data class SnapshotEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "linkId")
    val linkId: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "filePath")
    val filePath: String,

    @ColumnInfo(name = "fileSize")
    val fileSize: Long,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long
)
