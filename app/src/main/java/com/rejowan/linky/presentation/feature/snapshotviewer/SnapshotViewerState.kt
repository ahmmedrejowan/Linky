package com.rejowan.linky.presentation.feature.snapshotviewer

import com.rejowan.linky.domain.model.Snapshot

data class SnapshotViewerState(
    val snapshot: Snapshot? = null,
    val content: String? = null,
    val fontSize: FontSize = FontSize.MEDIUM,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class FontSize(val sp: Int) {
    SMALL(14),
    MEDIUM(16),
    LARGE(18)
}
