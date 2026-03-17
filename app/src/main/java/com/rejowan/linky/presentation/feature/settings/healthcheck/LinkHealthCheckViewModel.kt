package com.rejowan.linky.presentation.feature.settings.healthcheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.domain.usecase.link.DeleteLinkUseCase
import com.rejowan.linky.util.LinkPreviewFetcher
import com.rejowan.linky.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

/**
 * ViewModel for link health check functionality
 * Validates all saved links and identifies broken or problematic URLs
 */
class LinkHealthCheckViewModel(
    private val linkRepository: LinkRepository,
    private val deleteLinkUseCase: DeleteLinkUseCase,
    private val linkPreviewFetcher: LinkPreviewFetcher
) : ViewModel() {

    private val _state = MutableStateFlow(LinkHealthCheckState())
    val state: StateFlow<LinkHealthCheckState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<HealthCheckUiEvent>()
    val uiEvents: SharedFlow<HealthCheckUiEvent> = _uiEvents.asSharedFlow()

    private var healthCheckJob: Job? = null

    fun onEvent(event: HealthCheckEvent) {
        when (event) {
            HealthCheckEvent.OnStartHealthCheck -> startHealthCheck()
            HealthCheckEvent.OnCancelHealthCheck -> cancelHealthCheck()
            is HealthCheckEvent.OnDeleteLink -> deleteLink(event.linkId)
            HealthCheckEvent.OnDeleteAllBroken -> deleteAllBroken()
            is HealthCheckEvent.OnFilterByStatus -> filterByStatus(event.status)
            is HealthCheckEvent.OnRefetchMetadata -> refetchMetadata(event.linkId)
            is HealthCheckEvent.OnToggleRefetchThumbnails -> toggleRefetchThumbnails(event.enabled)
            is HealthCheckEvent.OnToggleRefetchTitles -> toggleRefetchTitles(event.enabled)
        }
    }

    private fun toggleRefetchThumbnails(enabled: Boolean) {
        _state.update { it.copy(refetchThumbnailsEnabled = enabled) }
    }

    private fun toggleRefetchTitles(enabled: Boolean) {
        _state.update { it.copy(refetchTitlesEnabled = enabled) }
    }

    private fun startHealthCheck() {
        healthCheckJob?.cancel()
        healthCheckJob = viewModelScope.launch {
            val refetchThumbnails = _state.value.refetchThumbnailsEnabled
            val refetchTitles = _state.value.refetchTitlesEnabled

            _state.update {
                it.copy(
                    isChecking = true,
                    isCancelled = false,
                    progress = 0f,
                    checkedCount = 0,
                    currentCheckingLink = null,
                    healthResults = emptyList(),
                    error = null,
                    thumbnailsUpdated = 0,
                    titlesUpdated = 0
                )
            }

            try {
                // Get all active links
                val allLinks = linkRepository.getAllLinks().first()
                    .filter { !it.isDeleted }

                _state.update { it.copy(totalLinks = allLinks.size) }

                if (allLinks.isEmpty()) {
                    _state.update { it.copy(isChecking = false) }
                    _uiEvents.emit(HealthCheckUiEvent.ShowMessage("No links to check"))
                    return@launch
                }

                val results = mutableListOf<LinkHealthResult>()
                var thumbnailsUpdated = 0
                var titlesUpdated = 0

                allLinks.forEachIndexed { index, link ->
                    // Check if cancelled
                    if (_state.value.isCancelled) {
                        _state.update { it.copy(isChecking = false, currentCheckingLink = null) }
                        _uiEvents.emit(HealthCheckUiEvent.ShowMessage("Health check cancelled"))
                        return@launch
                    }

                    // Update current checking link for real-time display
                    _state.update { it.copy(currentCheckingLink = link) }

                    val status = checkLinkHealth(link.url)
                    var updatedLink = link

                    // Refetch metadata if enabled and link is healthy
                    if (status == LinkHealthStatus.HEALTHY || status == LinkHealthStatus.SLOW) {
                        if (refetchThumbnails || refetchTitles) {
                            try {
                                val preview = linkPreviewFetcher.fetchPreview(link.url)
                                if (preview != null) {
                                    var hasUpdate = false

                                    val newTitle = if (refetchTitles && preview.title.isNotBlank() && preview.title != link.title) {
                                        titlesUpdated++
                                        hasUpdate = true
                                        preview.title
                                    } else link.title

                                    val newDescription = if (refetchTitles && !preview.description.isNullOrBlank()) {
                                        preview.description
                                    } else link.description

                                    val newThumbnail = if (refetchThumbnails && !preview.imageUrl.isNullOrBlank() && preview.imageUrl != link.previewImagePath) {
                                        thumbnailsUpdated++
                                        hasUpdate = true
                                        preview.imageUrl
                                    } else link.previewImagePath

                                    if (hasUpdate) {
                                        updatedLink = link.copy(
                                            title = newTitle,
                                            description = newDescription,
                                            previewImagePath = newThumbnail,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        linkRepository.updateLink(updatedLink)
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.w(e, "Failed to refetch metadata for ${link.url}")
                            }
                        }
                    }

                    results.add(LinkHealthResult(link = updatedLink, status = status))

                    _state.update {
                        it.copy(
                            progress = (index + 1).toFloat() / allLinks.size,
                            checkedCount = index + 1,
                            healthResults = results.toList(),
                            brokenCount = results.count { r -> r.status == LinkHealthStatus.BROKEN || r.status == LinkHealthStatus.UNREACHABLE },
                            slowCount = results.count { r -> r.status == LinkHealthStatus.SLOW },
                            healthyCount = results.count { r -> r.status == LinkHealthStatus.HEALTHY },
                            thumbnailsUpdated = thumbnailsUpdated,
                            titlesUpdated = titlesUpdated
                        )
                    }
                }

                _state.update {
                    it.copy(
                        isChecking = false,
                        currentCheckingLink = null
                    )
                }

                val brokenCount = results.count { it.status == LinkHealthStatus.BROKEN || it.status == LinkHealthStatus.UNREACHABLE }
                val message = buildString {
                    if (brokenCount > 0) {
                        append("Found $brokenCount broken links")
                    } else {
                        append("All links healthy!")
                    }
                    if (thumbnailsUpdated > 0 || titlesUpdated > 0) {
                        append(" • Updated: ")
                        if (thumbnailsUpdated > 0) append("$thumbnailsUpdated thumbnails")
                        if (thumbnailsUpdated > 0 && titlesUpdated > 0) append(", ")
                        if (titlesUpdated > 0) append("$titlesUpdated titles")
                    }
                }
                _uiEvents.emit(HealthCheckUiEvent.ShowMessage(message))

            } catch (e: Exception) {
                Timber.e(e, "Failed to perform health check")
                _state.update {
                    it.copy(
                        isChecking = false,
                        currentCheckingLink = null,
                        error = "Failed to perform health check: ${e.message}"
                    )
                }
            }
        }
    }

    private fun cancelHealthCheck() {
        _state.update { it.copy(isCancelled = true) }
        healthCheckJob?.cancel()
    }

    private suspend fun checkLinkHealth(url: String): LinkHealthStatus {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.instanceFollowRedirects = true

                try {
                    val responseCode = connection.responseCode
                    val responseTime = System.currentTimeMillis() - startTime

                    when {
                        responseCode in 200..399 -> {
                            if (responseTime > 5000) {
                                LinkHealthStatus.SLOW
                            } else {
                                LinkHealthStatus.HEALTHY
                            }
                        }
                        responseCode == 404 -> LinkHealthStatus.BROKEN
                        responseCode in 400..499 -> LinkHealthStatus.BROKEN
                        responseCode in 500..599 -> LinkHealthStatus.UNREACHABLE
                        else -> LinkHealthStatus.UNKNOWN
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: java.net.UnknownHostException) {
                LinkHealthStatus.BROKEN
            } catch (e: java.net.SocketTimeoutException) {
                LinkHealthStatus.SLOW
            } catch (e: javax.net.ssl.SSLException) {
                LinkHealthStatus.SSL_ERROR
            } catch (e: Exception) {
                LinkHealthStatus.UNKNOWN
            }
        }
    }

    private fun refetchMetadata(linkId: String) {
        viewModelScope.launch {
            val link = _state.value.healthResults.find { it.link.id == linkId }?.link ?: return@launch

            _state.update { state ->
                state.copy(
                    healthResults = state.healthResults.map {
                        if (it.link.id == linkId) it.copy(isRefetching = true) else it
                    }
                )
            }

            try {
                val preview = linkPreviewFetcher.fetchPreview(link.url)
                if (preview == null) {
                    _state.update { state ->
                        state.copy(
                            healthResults = state.healthResults.map {
                                if (it.link.id == linkId) it.copy(isRefetching = false) else it
                            }
                        )
                    }
                    _uiEvents.emit(HealthCheckUiEvent.ShowMessage("Could not fetch metadata"))
                    return@launch
                }
                val updatedLink = link.copy(
                    title = preview.title.ifBlank { link.title },
                    description = preview.description ?: link.description,
                    previewImagePath = preview.imageUrl ?: link.previewImagePath,
                    updatedAt = System.currentTimeMillis()
                )

                linkRepository.updateLink(updatedLink)

                _state.update { state ->
                    state.copy(
                        healthResults = state.healthResults.map {
                            if (it.link.id == linkId) it.copy(link = updatedLink, isRefetching = false) else it
                        }
                    )
                }
                _uiEvents.emit(HealthCheckUiEvent.ShowMessage("Metadata updated"))
            } catch (e: Exception) {
                Timber.e(e, "Failed to refetch metadata")
                _state.update { state ->
                    state.copy(
                        healthResults = state.healthResults.map {
                            if (it.link.id == linkId) it.copy(isRefetching = false) else it
                        }
                    )
                }
                _uiEvents.emit(HealthCheckUiEvent.ShowMessage("Failed to update metadata"))
            }
        }
    }

    private fun deleteLink(linkId: String) {
        viewModelScope.launch {
            when (val result = deleteLinkUseCase(linkId, softDelete = true)) {
                is Result.Success -> {
                    _uiEvents.emit(HealthCheckUiEvent.ShowMessage("Link moved to trash"))
                    _state.update { state ->
                        val updatedResults = state.healthResults.filter { it.link.id != linkId }
                        state.copy(
                            healthResults = updatedResults,
                            brokenCount = updatedResults.count { it.status == LinkHealthStatus.BROKEN || it.status == LinkHealthStatus.UNREACHABLE },
                            slowCount = updatedResults.count { it.status == LinkHealthStatus.SLOW },
                            healthyCount = updatedResults.count { it.status == LinkHealthStatus.HEALTHY }
                        )
                    }
                }
                is Result.Error -> {
                    _uiEvents.emit(HealthCheckUiEvent.ShowMessage("Failed to delete link"))
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun deleteAllBroken() {
        val brokenLinks = _state.value.healthResults.filter {
            it.status == LinkHealthStatus.BROKEN || it.status == LinkHealthStatus.UNREACHABLE
        }
        if (brokenLinks.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }

            var deletedCount = 0
            brokenLinks.forEach { result ->
                when (deleteLinkUseCase(result.link.id, softDelete = true)) {
                    is Result.Success -> {
                        deletedCount++
                        _state.update { state ->
                            val updatedResults = state.healthResults.filter { it.link.id != result.link.id }
                            state.copy(healthResults = updatedResults)
                        }
                    }
                    else -> {}
                }
            }

            _state.update { state ->
                state.copy(
                    isDeleting = false,
                    brokenCount = state.healthResults.count { it.status == LinkHealthStatus.BROKEN || it.status == LinkHealthStatus.UNREACHABLE },
                    slowCount = state.healthResults.count { it.status == LinkHealthStatus.SLOW },
                    healthyCount = state.healthResults.count { it.status == LinkHealthStatus.HEALTHY }
                )
            }
            _uiEvents.emit(HealthCheckUiEvent.ShowMessage("$deletedCount broken links moved to trash"))
        }
    }

    private fun filterByStatus(status: LinkHealthStatus?) {
        _state.update { it.copy(filterStatus = status) }
    }
}

/**
 * State for link health check screen
 */
data class LinkHealthCheckState(
    val isChecking: Boolean = false,
    val isCancelled: Boolean = false,
    val isDeleting: Boolean = false,
    val refetchThumbnailsEnabled: Boolean = false,
    val refetchTitlesEnabled: Boolean = false,
    val progress: Float = 0f,
    val totalLinks: Int = 0,
    val checkedCount: Int = 0,
    val currentCheckingLink: Link? = null,
    val healthResults: List<LinkHealthResult> = emptyList(),
    val brokenCount: Int = 0,
    val slowCount: Int = 0,
    val healthyCount: Int = 0,
    val thumbnailsUpdated: Int = 0,
    val titlesUpdated: Int = 0,
    val filterStatus: LinkHealthStatus? = null,
    val error: String? = null
) {
    val filteredResults: List<LinkHealthResult>
        get() = if (filterStatus == null) {
            healthResults
        } else {
            healthResults.filter { it.status == filterStatus }
        }
}

/**
 * Health check result for a single link
 */
data class LinkHealthResult(
    val link: Link,
    val status: LinkHealthStatus,
    val isRefetching: Boolean = false
)

/**
 * Possible health statuses for a link
 */
enum class LinkHealthStatus(val displayName: String) {
    HEALTHY("Healthy"),
    SLOW("Slow"),
    BROKEN("Broken"),
    UNREACHABLE("Unreachable"),
    SSL_ERROR("SSL Error"),
    UNKNOWN("Unknown")
}

/**
 * Events for health check screen
 */
sealed class HealthCheckEvent {
    data object OnStartHealthCheck : HealthCheckEvent()
    data object OnCancelHealthCheck : HealthCheckEvent()
    data class OnDeleteLink(val linkId: String) : HealthCheckEvent()
    data object OnDeleteAllBroken : HealthCheckEvent()
    data class OnFilterByStatus(val status: LinkHealthStatus?) : HealthCheckEvent()
    data class OnRefetchMetadata(val linkId: String) : HealthCheckEvent()
    data class OnToggleRefetchThumbnails(val enabled: Boolean) : HealthCheckEvent()
    data class OnToggleRefetchTitles(val enabled: Boolean) : HealthCheckEvent()
}

/**
 * UI events for health check screen
 */
sealed class HealthCheckUiEvent {
    data class ShowMessage(val message: String) : HealthCheckUiEvent()
}
