package com.rejowan.linky.presentation.feature.settings.healthcheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.domain.usecase.link.DeleteLinkUseCase
import com.rejowan.linky.util.Result
import kotlinx.coroutines.Dispatchers
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
    private val deleteLinkUseCase: DeleteLinkUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LinkHealthCheckState())
    val state: StateFlow<LinkHealthCheckState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<HealthCheckUiEvent>()
    val uiEvents: SharedFlow<HealthCheckUiEvent> = _uiEvents.asSharedFlow()

    fun onEvent(event: HealthCheckEvent) {
        when (event) {
            HealthCheckEvent.OnStartHealthCheck -> startHealthCheck()
            HealthCheckEvent.OnCancelHealthCheck -> cancelHealthCheck()
            is HealthCheckEvent.OnDeleteLink -> deleteLink(event.linkId)
            HealthCheckEvent.OnDeleteAllBroken -> deleteAllBroken()
            is HealthCheckEvent.OnFilterByStatus -> filterByStatus(event.status)
        }
    }

    private fun startHealthCheck() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isChecking = true,
                    isCancelled = false,
                    progress = 0f,
                    checkedCount = 0,
                    healthResults = emptyList(),
                    error = null
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

                allLinks.forEachIndexed { index, link ->
                    // Check if cancelled
                    if (_state.value.isCancelled) {
                        _state.update { it.copy(isChecking = false) }
                        _uiEvents.emit(HealthCheckUiEvent.ShowMessage("Health check cancelled"))
                        return@launch
                    }

                    val status = checkLinkHealth(link.url)
                    results.add(LinkHealthResult(link = link, status = status))

                    _state.update {
                        it.copy(
                            progress = (index + 1).toFloat() / allLinks.size,
                            checkedCount = index + 1,
                            healthResults = results.toList()
                        )
                    }
                }

                // Count results
                val brokenCount = results.count { it.status == LinkHealthStatus.BROKEN }
                val slowCount = results.count { it.status == LinkHealthStatus.SLOW }
                val healthyCount = results.count { it.status == LinkHealthStatus.HEALTHY }

                _state.update {
                    it.copy(
                        isChecking = false,
                        brokenCount = brokenCount,
                        slowCount = slowCount,
                        healthyCount = healthyCount
                    )
                }

                val message = buildString {
                    append("Health check complete: ")
                    append("$healthyCount healthy")
                    if (slowCount > 0) append(", $slowCount slow")
                    if (brokenCount > 0) append(", $brokenCount broken")
                }
                _uiEvents.emit(HealthCheckUiEvent.ShowMessage(message))

            } catch (e: Exception) {
                Timber.e(e, "Failed to perform health check")
                _state.update {
                    it.copy(
                        isChecking = false,
                        error = "Failed to perform health check: ${e.message}"
                    )
                }
            }
        }
    }

    private fun cancelHealthCheck() {
        _state.update { it.copy(isCancelled = true) }
    }

    private suspend fun checkLinkHealth(url: String): LinkHealthStatus {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD" // Use HEAD for faster check
                connection.connectTimeout = 10000 // 10 seconds
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
                Timber.w("Unknown host for URL: $url")
                LinkHealthStatus.BROKEN
            } catch (e: java.net.SocketTimeoutException) {
                Timber.w("Timeout checking URL: $url")
                LinkHealthStatus.SLOW
            } catch (e: javax.net.ssl.SSLException) {
                Timber.w("SSL error for URL: $url")
                LinkHealthStatus.SSL_ERROR
            } catch (e: Exception) {
                Timber.w(e, "Error checking URL: $url")
                LinkHealthStatus.UNKNOWN
            }
        }
    }

    private fun deleteLink(linkId: String) {
        viewModelScope.launch {
            when (val result = deleteLinkUseCase(linkId, softDelete = true)) {
                is Result.Success -> {
                    _uiEvents.emit(HealthCheckUiEvent.ShowMessage("Link moved to trash"))
                    // Remove from results list
                    _state.update { state ->
                        val updatedResults = state.healthResults.filter { it.link.id != linkId }
                        state.copy(
                            healthResults = updatedResults,
                            brokenCount = updatedResults.count { it.status == LinkHealthStatus.BROKEN },
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
        val brokenLinks = _state.value.healthResults.filter { it.status == LinkHealthStatus.BROKEN }
        if (brokenLinks.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }

            var deletedCount = 0
            brokenLinks.forEach { result ->
                when (deleteLinkUseCase(result.link.id, softDelete = true)) {
                    is Result.Success -> {
                        deletedCount++
                        // Update UI progressively
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
                    brokenCount = state.healthResults.count { it.status == LinkHealthStatus.BROKEN },
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
    val progress: Float = 0f,
    val totalLinks: Int = 0,
    val checkedCount: Int = 0,
    val healthResults: List<LinkHealthResult> = emptyList(),
    val brokenCount: Int = 0,
    val slowCount: Int = 0,
    val healthyCount: Int = 0,
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
    val status: LinkHealthStatus
)

/**
 * Possible health statuses for a link
 */
enum class LinkHealthStatus(val displayName: String, val description: String) {
    HEALTHY("Healthy", "Link is working correctly"),
    SLOW("Slow", "Link takes too long to respond (>5s)"),
    BROKEN("Broken", "Link returns 404 or similar error"),
    UNREACHABLE("Unreachable", "Server is not responding"),
    SSL_ERROR("SSL Error", "Security certificate issue"),
    UNKNOWN("Unknown", "Could not determine status")
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
}

/**
 * UI events for health check screen
 */
sealed class HealthCheckUiEvent {
    data class ShowMessage(val message: String) : HealthCheckUiEvent()
}
