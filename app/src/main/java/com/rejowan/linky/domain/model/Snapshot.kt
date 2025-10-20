package com.rejowan.linky.domain.model

import java.util.UUID

data class Snapshot(
    val id: String = UUID.randomUUID().toString(),
    val linkId: String,
    val type: SnapshotType,
    val filePath: String,
    val fileSize: Long,
    val createdAt: Long = System.currentTimeMillis(),
    // Reader mode specific fields
    val title: String? = null,
    val author: String? = null,
    val excerpt: String? = null,
    val wordCount: Int? = null,
    val estimatedReadTime: Int? = null  // In minutes
)
