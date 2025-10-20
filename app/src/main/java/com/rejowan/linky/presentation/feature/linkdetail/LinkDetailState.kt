package com.rejowan.linky.presentation.feature.linkdetail

import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.model.Snapshot

data class LinkDetailState(
    val link: Link? = null,
    val snapshots: List<Snapshot> = emptyList(),
    val isLoading: Boolean = false,
    val isCapturingSnapshot: Boolean = false,
    val error: String? = null,
    val isDeleted: Boolean = false,
    val snapshotCaptured: Boolean = false
)
