package com.rejowan.linky.data.export

import android.content.Context
import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Handles the .linky backup file format.
 *
 * The .linky format is a ZIP archive containing:
 * - manifest.json: Version info, checksums, metadata
 * - data.json: The actual backup data (links, collections)
 *
 * This format is future-proof and can be extended to include
 * images, favicons, and other assets without breaking compatibility.
 */
object LinkyBackupFormat {

    const val FILE_EXTENSION = "linky"
    const val MIME_TYPE = "application/vnd.linky.backup"

    private const val MANIFEST_FILE = "manifest.json"
    private const val DATA_FILE = "data.json"

    // Magic bytes to identify .linky files (ZIP signature + our marker)
    private const val FORMAT_IDENTIFIER = "LINKY_BACKUP"

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /**
     * Write backup data to a .linky file
     */
    fun writeBackup(
        outputStream: OutputStream,
        exportData: ExportData,
        onProgress: (Int) -> Unit = {}
    ) {
        onProgress(60)

        // Serialize data
        val dataJson = json.encodeToString(exportData)
        val dataChecksum = calculateChecksum(dataJson.toByteArray())

        onProgress(70)

        // Create manifest
        val manifest = BackupManifest(
            formatVersion = 1,
            appVersion = exportData.version,
            exportDate = exportData.exportDate,
            dataChecksum = dataChecksum,
            linksCount = exportData.data.links.size,
            collectionsCount = exportData.data.collections.size
        )
        val manifestJson = json.encodeToString(manifest)

        onProgress(80)

        // Write ZIP archive
        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
            // Write manifest first
            zipOut.putNextEntry(ZipEntry(MANIFEST_FILE))
            zipOut.write(manifestJson.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // Write data
            zipOut.putNextEntry(ZipEntry(DATA_FILE))
            zipOut.write(dataJson.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // Write format identifier as comment
            zipOut.setComment(FORMAT_IDENTIFIER)
        }

        onProgress(100)
    }

    /**
     * Read backup data from a .linky file
     */
    fun readBackup(
        context: Context,
        uri: Uri,
        onProgress: (Int) -> Unit = {}
    ): BackupReadResult {
        onProgress(5)

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not open file")

        return inputStream.use { stream ->
            val bufferedStream = stream.buffered()
            bufferedStream.mark(4)

            val header = ByteArray(4)
            bufferedStream.read(header)
            bufferedStream.reset()

            // ZIP files start with PK (0x50 0x4B)
            val isZip = header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()

            if (!isZip) {
                throw IllegalStateException("Invalid file format. Please select a .linky backup file.")
            }

            readLinkyFormat(bufferedStream, onProgress)
        }
    }

    private fun readLinkyFormat(
        inputStream: InputStream,
        onProgress: (Int) -> Unit
    ): BackupReadResult {
        onProgress(10)

        var manifest: BackupManifest? = null
        var dataJson: String? = null

        ZipInputStream(inputStream).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                when (entry.name) {
                    MANIFEST_FILE -> {
                        val content = zipIn.readBytes().toString(Charsets.UTF_8)
                        manifest = json.decodeFromString<BackupManifest>(content)
                        onProgress(30)
                    }
                    DATA_FILE -> {
                        dataJson = zipIn.readBytes().toString(Charsets.UTF_8)
                        onProgress(60)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        if (manifest == null) {
            throw IllegalStateException("Invalid .linky file: missing manifest")
        }
        if (dataJson == null) {
            throw IllegalStateException("Invalid .linky file: missing data")
        }

        // Verify checksum
        val actualChecksum = calculateChecksum(dataJson!!.toByteArray())
        if (actualChecksum != manifest!!.dataChecksum) {
            Timber.w("Checksum mismatch - file may be corrupted")
        }

        onProgress(80)

        val exportData = json.decodeFromString<ExportData>(dataJson!!)

        onProgress(100)

        return BackupReadResult(
            exportData = exportData,
            manifest = manifest
        )
    }

    /**
     * Quick preview of a .linky backup file without fully parsing data
     */
    fun previewBackup(
        context: Context,
        uri: Uri
    ): BackupPreview {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not open file")

        return inputStream.use { stream ->
            val bufferedStream = stream.buffered()
            bufferedStream.mark(4)

            val header = ByteArray(4)
            bufferedStream.read(header)
            bufferedStream.reset()

            val isZip = header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()

            if (!isZip) {
                throw IllegalStateException("Invalid file format. Please select a .linky backup file.")
            }

            ZipInputStream(bufferedStream).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    if (entry.name == MANIFEST_FILE) {
                        val content = zipIn.readBytes().toString(Charsets.UTF_8)
                        val manifest = json.decodeFromString<BackupManifest>(content)
                        return@use BackupPreview(
                            version = manifest.appVersion,
                            exportDate = manifest.exportDate,
                            linksCount = manifest.linksCount,
                            collectionsCount = manifest.collectionsCount
                        )
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
                throw IllegalStateException("Invalid .linky file: missing manifest")
            }
        }
    }

    private fun calculateChecksum(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Manifest file stored in .linky archive
 */
@Serializable
data class BackupManifest(
    val formatVersion: Int,
    val appVersion: Int,
    val exportDate: String,
    val dataChecksum: String,
    val linksCount: Int,
    val collectionsCount: Int,
    val formatIdentifier: String = "LINKY_BACKUP"
)

/**
 * Result of reading a backup file
 */
data class BackupReadResult(
    val exportData: ExportData,
    val manifest: BackupManifest?
)

/**
 * Quick preview info without full parsing
 */
data class BackupPreview(
    val version: Int,
    val exportDate: String,
    val linksCount: Int,
    val collectionsCount: Int
)
