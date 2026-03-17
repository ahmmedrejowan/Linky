package com.rejowan.linky.presentation.feature.settings.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.domain.usecase.link.DeleteLinkUseCase
import com.rejowan.linky.util.Result
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.URI

/**
 * ViewModel for duplicate link detection and management
 */
class DuplicateDetectionViewModel(
    private val linkRepository: LinkRepository,
    private val deleteLinkUseCase: DeleteLinkUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DuplicateDetectionState())
    val state: StateFlow<DuplicateDetectionState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<DuplicateUiEvent>()
    val uiEvents: SharedFlow<DuplicateUiEvent> = _uiEvents.asSharedFlow()

    // Removed auto-scan on init - user must click "Start Scan" button

    fun onEvent(event: DuplicateEvent) {
        when (event) {
            DuplicateEvent.OnScan -> scanForDuplicates()
            is DuplicateEvent.OnDeleteLink -> deleteLink(event.linkId)
            is DuplicateEvent.OnDeleteAllInGroup -> deleteAllInGroup(event.groupIndex, event.keepFirst)
            DuplicateEvent.OnDeleteAllDuplicates -> deleteAllDuplicates()
            is DuplicateEvent.OnExpandGroup -> toggleGroupExpansion(event.groupIndex)
        }
    }

    private fun scanForDuplicates() {
        viewModelScope.launch {
            _state.update { it.copy(isScanning = true, error = null) }

            try {
                // Get all active links
                val allLinks = linkRepository.getAllLinks().first()

                // Find duplicates by normalized URL
                val duplicateGroups = findDuplicateGroups(allLinks)

                val totalDuplicates = duplicateGroups.sumOf { it.links.size - 1 } // Count duplicates (excluding originals)

                _state.update {
                    it.copy(
                        hasScanned = true,
                        isScanning = false,
                        duplicateGroups = duplicateGroups,
                        totalDuplicatesFound = totalDuplicates,
                        expandedGroups = emptySet()
                    )
                }

                if (duplicateGroups.isEmpty()) {
                    _uiEvents.emit(DuplicateUiEvent.ShowMessage("No duplicates found"))
                } else {
                    _uiEvents.emit(DuplicateUiEvent.ShowMessage("Found $totalDuplicates duplicate links in ${duplicateGroups.size} groups"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to scan for duplicates")
                _state.update { it.copy(isScanning = false, error = "Failed to scan for duplicates") }
            }
        }
    }

    private fun findDuplicateGroups(links: List<Link>): List<DuplicateGroup> {
        // Group links by normalized URL
        val urlGroups = links
            .filter { !it.isDeleted } // Exclude trashed links
            .groupBy { normalizeUrl(it.url) }
            .filter { it.value.size > 1 } // Only keep groups with duplicates

        return urlGroups.map { (normalizedUrl, duplicateLinks) ->
            DuplicateGroup(
                normalizedUrl = normalizedUrl,
                links = duplicateLinks.sortedBy { it.createdAt } // Oldest first
            )
        }.sortedByDescending { it.links.size } // Groups with most duplicates first
    }

    /**
     * Normalize URL for comparison:
     * - Lowercase
     * - Remove protocol (http/https)
     * - Remove www. prefix
     * - Remove trailing slash
     * - Remove common tracking parameters
     */
    private fun normalizeUrl(url: String): String {
        return try {
            var normalized = url.trim().lowercase()

            // Remove protocol
            normalized = normalized
                .removePrefix("https://")
                .removePrefix("http://")

            // Remove www. prefix
            normalized = normalized.removePrefix("www.")

            // Remove trailing slash
            normalized = normalized.trimEnd('/')

            // Remove common tracking parameters
            val uri = URI.create("https://$normalized")
            val query = uri.query
            if (query != null) {
                val cleanParams = query.split("&")
                    .filterNot { param ->
                        val key = param.split("=").firstOrNull() ?: ""
                        trackingParams.any { it.equals(key, ignoreCase = true) }
                    }
                    .joinToString("&")

                val path = uri.path ?: ""
                normalized = "${uri.host}$path"
                if (cleanParams.isNotEmpty()) {
                    normalized += "?$cleanParams"
                }
            }

            normalized
        } catch (e: Exception) {
            url.trim().lowercase()
        }
    }

    private fun deleteLink(linkId: String) {
        viewModelScope.launch {
            when (val result = deleteLinkUseCase(linkId, softDelete = true)) {
                is Result.Success -> {
                    _uiEvents.emit(DuplicateUiEvent.ShowMessage("Link moved to trash"))
                    // Rescan to update the list
                    scanForDuplicates()
                }
                is Result.Error -> {
                    _uiEvents.emit(DuplicateUiEvent.ShowMessage("Failed to delete link"))
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun deleteAllInGroup(groupIndex: Int, keepFirst: Boolean) {
        val group = _state.value.duplicateGroups.getOrNull(groupIndex) ?: return
        val linksToDelete = if (keepFirst) {
            group.links.drop(1) // Keep the oldest, delete the rest
        } else {
            group.links
        }

        viewModelScope.launch {
            var successCount = 0
            linksToDelete.forEach { link ->
                when (deleteLinkUseCase(link.id, softDelete = true)) {
                    is Result.Success -> successCount++
                    else -> {}
                }
            }

            _uiEvents.emit(DuplicateUiEvent.ShowMessage("$successCount duplicates moved to trash"))
            scanForDuplicates()
        }
    }

    private fun deleteAllDuplicates() {
        val groups = _state.value.duplicateGroups

        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }

            var totalDeleted = 0
            groups.forEach { group ->
                // Keep the oldest link, delete the rest
                group.links.drop(1).forEach { link ->
                    when (deleteLinkUseCase(link.id, softDelete = true)) {
                        is Result.Success -> totalDeleted++
                        else -> {}
                    }
                }
            }

            _state.update { it.copy(isDeleting = false) }
            _uiEvents.emit(DuplicateUiEvent.ShowMessage("$totalDeleted duplicates moved to trash"))
            scanForDuplicates()
        }
    }

    private fun toggleGroupExpansion(groupIndex: Int) {
        _state.update { currentState ->
            val currentExpanded = currentState.expandedGroups
            val newExpanded = if (currentExpanded.contains(groupIndex)) {
                currentExpanded - groupIndex
            } else {
                currentExpanded + groupIndex
            }
            currentState.copy(expandedGroups = newExpanded)
        }
    }

    companion object {
        // Common tracking parameters to ignore when comparing URLs
        private val trackingParams = listOf(
            "utm_source", "utm_medium", "utm_campaign", "utm_content", "utm_term",
            "fbclid", "gclid", "ref", "source", "mc_cid", "mc_eid",
            "s_kwcid", "msclkid", "twclid", "igshid", "share"
        )
    }
}

/**
 * State for duplicate detection screen
 */
data class DuplicateDetectionState(
    val hasScanned: Boolean = false,
    val isScanning: Boolean = false,
    val isDeleting: Boolean = false,
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val totalDuplicatesFound: Int = 0,
    val expandedGroups: Set<Int> = emptySet(),
    val error: String? = null
)

/**
 * A group of links that are duplicates of each other
 */
data class DuplicateGroup(
    val normalizedUrl: String,
    val links: List<Link>
)

/**
 * Events for duplicate detection screen
 */
sealed class DuplicateEvent {
    data object OnScan : DuplicateEvent()
    data class OnDeleteLink(val linkId: String) : DuplicateEvent()
    data class OnDeleteAllInGroup(val groupIndex: Int, val keepFirst: Boolean = true) : DuplicateEvent()
    data object OnDeleteAllDuplicates : DuplicateEvent()
    data class OnExpandGroup(val groupIndex: Int) : DuplicateEvent()
}

/**
 * UI events for duplicate detection screen
 */
sealed class DuplicateUiEvent {
    data class ShowMessage(val message: String) : DuplicateUiEvent()
}
