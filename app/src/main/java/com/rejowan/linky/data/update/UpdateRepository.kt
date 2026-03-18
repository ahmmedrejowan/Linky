package com.rejowan.linky.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Repository for checking and downloading app updates from GitHub
 */
class UpdateRepository(
    private val context: Context
) {
    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos/ahmmedrejowan/Linky/releases/latest"
        private const val APK_DIR = "updates"
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var currentDownloadJob: kotlinx.coroutines.Job? = null

    /**
     * Check for available updates
     */
    suspend fun checkForUpdates(currentVersion: String): Result<GithubRelease?> = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("HTTP $responseCode"))
            }

            val response = connection.inputStream.bufferedReader().readText()
            val release = json.decodeFromString<GithubRelease>(response)

            // Skip prereleases and drafts
            if (release.prerelease || release.draft) {
                return@withContext Result.success(null)
            }

            // Compare versions
            if (isNewerVersion(release.version, currentVersion)) {
                Result.success(release)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check for updates")
            Result.failure(e)
        }
    }

    /**
     * Download APK from the given URL
     */
    suspend fun downloadApk(
        downloadUrl: String,
        fileName: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            _downloadState.value = DownloadState.Starting

            val apkDir = File(context.filesDir, APK_DIR)
            if (!apkDir.exists()) apkDir.mkdirs()

            // Clean old APKs
            apkDir.listFiles()?.forEach { it.delete() }

            val apkFile = File(apkDir, fileName)

            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 30000
                readTimeout = 30000
            }

            val totalBytes = connection.contentLength.toLong()
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt()
                        } else 0

                        _downloadState.value = DownloadState.Downloading(
                            progress = progress,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes
                        )
                    }
                }
            }

            _downloadState.value = DownloadState.Completed(apkFile.absolutePath)
            Result.success(apkFile)
        } catch (e: Exception) {
            Timber.e(e, "Failed to download APK")
            _downloadState.value = DownloadState.Failed(e.message ?: "Download failed")
            Result.failure(e)
        }
    }

    /**
     * Cancel ongoing download
     */
    fun cancelDownload() {
        currentDownloadJob?.cancel()
        _downloadState.value = DownloadState.Cancelled
    }

    /**
     * Reset download state
     */
    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }

    /**
     * Check if app can install APKs
     */
    fun canInstallApks(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Get intent to open install permission settings
     */
    fun getInstallPermissionIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else null
    }

    /**
     * Install APK from file
     */
    fun installApk(apkFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    /**
     * Get pending APK if exists
     */
    fun getPendingApk(): File? {
        val apkDir = File(context.filesDir, APK_DIR)
        return apkDir.listFiles()?.firstOrNull { it.extension == "apk" }
    }

    /**
     * Compare version strings
     */
    private fun isNewerVersion(newVersion: String, currentVersion: String): Boolean {
        try {
            val newParts = newVersion.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }

            val maxLength = maxOf(newParts.size, currentParts.size)

            for (i in 0 until maxLength) {
                val newPart = newParts.getOrElse(i) { 0 }
                val currentPart = currentParts.getOrElse(i) { 0 }

                if (newPart > currentPart) return true
                if (newPart < currentPart) return false
            }
            return false
        } catch (e: Exception) {
            Timber.e(e, "Version comparison failed")
            return false
        }
    }
}
