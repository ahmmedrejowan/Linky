package com.rejowan.linky.domain.model

enum class SnapshotType {
    READER_MODE,
    PDF,
    SCREENSHOT;

    companion object {
        fun fromString(value: String): SnapshotType {
            return valueOf(value.uppercase())
        }
    }
}
