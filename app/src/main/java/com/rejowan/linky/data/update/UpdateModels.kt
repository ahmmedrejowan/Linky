package com.rejowan.linky.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub Release model for update checking
 */
@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String,
    @SerialName("body") val body: String,
    @SerialName("published_at") val publishedAt: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("assets") val assets: List<GithubAsset> = emptyList(),
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("draft") val draft: Boolean = false
) {
    val version: String
        get() = tagName.removePrefix("v")

    val apkAsset: GithubAsset?
        get() = assets.find { it.name.endsWith(".apk") }
}

@Serializable
data class GithubAsset(
    @SerialName("name") val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("size") val size: Long,
    @SerialName("content_type") val contentType: String
)

/**
 * Update check state
 */
sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class Available(
        val release: GithubRelease,
        val currentVersion: String
    ) : UpdateState()
    data object UpToDate : UpdateState()
    data class Error(val message: String) : UpdateState()
}

/**
 * Update check interval
 */
enum class UpdateCheckInterval(val displayName: String, val days: Int) {
    NEVER("Never", -1),
    DAILY("Daily", 1),
    EVERY_3_DAYS("Every 3 days", 3),
    WEEKLY("Weekly", 7),
    EVERY_2_WEEKS("Every 2 weeks", 14),
    MONTHLY("Monthly", 30)
}

/**
 * APK download state
 */
sealed class DownloadState {
    data object Idle : DownloadState()
    data object Starting : DownloadState()
    data class Downloading(val progress: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
    data class Completed(val filePath: String) : DownloadState()
    data class Failed(val error: String) : DownloadState()
    data object Cancelled : DownloadState()
}
