package com.rejowan.linky.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "links",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["isFavorite"]),
        Index(value = ["isArchived"]),
        Index(value = ["deletedAt"]),
        Index(value = ["updatedAt"])
    ]
)
data class LinkEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "folderId")
    val folderId: String? = null,

    @ColumnInfo(name = "previewImagePath")
    val previewImagePath: String? = null,

    @ColumnInfo(name = "previewUrl")
    val previewUrl: String? = null,

    @ColumnInfo(name = "isFavorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "isArchived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "deletedAt")
    val deletedAt: Long? = null,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long,

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long,

    // Phase 2: Sync fields
    @ColumnInfo(name = "syncToRemote", defaultValue = "0")
    val syncToRemote: Boolean = false,

    @ColumnInfo(name = "userId")
    val userId: String? = null
)
