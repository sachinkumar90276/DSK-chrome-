package com.example.download

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.BrowserRepository
import com.example.data.DownloadEntity
import com.example.data.DownloadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadEngine(
    private val context: Context,
    private val repository: BrowserRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val downloadDir: File
        get() = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "DSK_Chrome").apply {
            if (!exists()) mkdirs()
        }

    fun startDownload(
        url: String,
        fileName: String,
        mimeType: String,
        userAgent: String = "",
        cookies: String? = null,
        notifyOnComplete: Boolean = true
    ) {
        scope.launch {
            val safeFileName = ensureUniqueFileName(sanitizeFileName(fileName))
            val targetFile = File(downloadDir, safeFileName)
            val resolvedMime = mimeType.ifBlank { guessMimeType(safeFileName) }

            val recordId = repository.createDownloadRecord(
                DownloadEntity(
                    fileName = safeFileName,
                    url = url,
                    mimeType = resolvedMime,
                    localPath = targetFile.absolutePath,
                    status = DownloadStatus.QUEUED.name
                )
            )
            launchDownloadJob(recordId, url, targetFile, resolvedMime, userAgent, cookies, notifyOnComplete, isResume = false)
        }
    }

    fun pauseDownload(downloadId: Long) {
        activeJobs.remove(downloadId)?.cancel()
        scope.launch {
            val item = repository.getDownloadRecord(downloadId) ?: return@launch
            if (item.statusEnum == DownloadStatus.DOWNLOADING || item.statusEnum == DownloadStatus.QUEUED) {
                repository.updateDownloadRecord(
                    item.copy(status = DownloadStatus.PAUSED.name, speedBytesPerSec = 0L)
                )
            }
        }
    }

    fun resumeDownload(downloadId: Long, notifyOnComplete: Boolean = true) {
        scope.launch {
            val item = repository.getDownloadRecord(downloadId) ?: return@launch
            if (item.statusEnum == DownloadStatus.DOWNLOADING) return@launch
            val file = File(item.localPath.ifBlank { File(downloadDir, item.fileName).absolutePath })
            launchDownloadJob(
                recordId = item.id,
                url = item.url,
                targetFile = file,
                mimeType = item.mimeType,
                userAgent = "",
                cookies = null,
                notifyOnComplete = notifyOnComplete,
                isResume = file.exists() && file.length() > 0L
            )
        }
    }

    fun cancelDownload(downloadId: Long) {
        activeJobs.remove(downloadId)?.cancel()
        scope.launch {
            val item = repository.getDownloadRecord(downloadId) ?: return@launch
            runCatching { File(item.localPath).delete() }
            repository.updateDownloadRecord(
                item.copy(
                    status = DownloadStatus.CANCELLED.name,
                    speedBytesPerSec = 0L
                )
            )
        }
    }

    fun deleteDownload(downloadId: Long) {
        activeJobs.remove(downloadId)?.cancel()
        scope.launch {
            val item = repository.getDownloadRecord(downloadId)
            if (item != null) {
                runCatching {
                    if (item.localPath.isNotBlank()) File(item.localPath).delete()
                }
                repository.deleteDownloadRecord(downloadId)
            }
        }
    }

    fun clearFinished() {
        scope.launch {
            repository.clearFinishedDownloads()
        }
    }

    private fun launchDownloadJob(
        recordId: Long,
        url: String,
        targetFile: File,
        mimeType: String,
        userAgent: String,
        cookies: String?,
        notifyOnComplete: Boolean,
        isResume: Boolean
    ) {
        activeJobs[recordId]?.cancel()
        val job = scope.launch {
            try {
                if (url.startsWith("data:")) {
                    saveDataUri(recordId, url, targetFile, mimeType, notifyOnComplete)
                    return@launch
                }

                val existingBytes = if (isResume && targetFile.exists()) targetFile.length() else 0L
                val reqBuilder = Request.Builder().url(url)
                if (userAgent.isNotBlank()) reqBuilder.header("User-Agent", userAgent)
                if (!cookies.isNullOrBlank()) reqBuilder.header("Cookie", cookies)
                if (existingBytes > 0L) {
                    reqBuilder.header("Range", "bytes=$existingBytes-")
                }

                client.newCall(reqBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful && response.code != 206) {
                        val current = repository.getDownloadRecord(recordId) ?: return@launch
                        repository.updateDownloadRecord(
                            current.copy(
                                status = DownloadStatus.FAILED.name,
                                speedBytesPerSec = 0L,
                                errorMessage = "HTTP ${response.code}: ${response.message}"
                            )
                        )
                        return@launch
                    }

                    val body = response.body ?: throw IllegalStateException("Empty response body")
                    val supportsResume = response.code == 206 && existingBytes > 0L
                    val contentLength = body.contentLength()
                    val totalBytes = if (supportsResume && contentLength > 0L) {
                        existingBytes + contentLength
                    } else if (contentLength > 0L) {
                        contentLength
                    } else {
                        -1L
                    }

                    val actualMime = response.header("Content-Type")
                        ?.substringBefore(';')
                        ?.trim()
                        ?.takeIf { it.isNotBlank() && it != "application/octet-stream" }
                        ?: mimeType

                    var downloadedBytes = if (supportsResume) existingBytes else 0L
                    var currentRecord = repository.getDownloadRecord(recordId) ?: return@launch
                    currentRecord = currentRecord.copy(
                        totalBytes = totalBytes,
                        downloadedBytes = downloadedBytes,
                        mimeType = actualMime,
                        status = DownloadStatus.DOWNLOADING.name,
                        errorMessage = null
                    )
                    repository.updateDownloadRecord(currentRecord)

                    targetFile.parentFile?.mkdirs()
                    val buffer = ByteArray(16 * 1024)
                    var lastTickTime = System.currentTimeMillis()
                    var bytesSinceTick = 0L

                    body.byteStream().use { input ->
                        if (supportsResume) {
                            RandomAccessFile(targetFile, "rw").use { raf ->
                                raf.seek(existingBytes)
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read == -1) break
                                    raf.write(buffer, 0, read)
                                    downloadedBytes += read
                                    bytesSinceTick += read
                                    val now = System.currentTimeMillis()
                                    if (now - lastTickTime >= 350L) {
                                        val elapsedMs = (now - lastTickTime).coerceAtLeast(1L)
                                        val speed = (bytesSinceTick * 1000L) / elapsedMs
                                        lastTickTime = now
                                        bytesSinceTick = 0L
                                        repository.updateDownloadRecord(
                                            currentRecord.copy(
                                                downloadedBytes = downloadedBytes,
                                                speedBytesPerSec = speed,
                                                status = DownloadStatus.DOWNLOADING.name
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            FileOutputStream(targetFile, false).use { output ->
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read == -1) break
                                    output.write(buffer, 0, read)
                                    downloadedBytes += read
                                    bytesSinceTick += read
                                    val now = System.currentTimeMillis()
                                    if (now - lastTickTime >= 350L) {
                                        val elapsedMs = (now - lastTickTime).coerceAtLeast(1L)
                                        val speed = (bytesSinceTick * 1000L) / elapsedMs
                                        lastTickTime = now
                                        bytesSinceTick = 0L
                                        repository.updateDownloadRecord(
                                            currentRecord.copy(
                                                downloadedBytes = downloadedBytes,
                                                speedBytesPerSec = speed,
                                                status = DownloadStatus.DOWNLOADING.name
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val exportedUri = exportToMediaStoreDownloads(targetFile, actualMime)
                    val latest = repository.getDownloadRecord(recordId) ?: currentRecord
                    repository.updateDownloadRecord(
                        latest.copy(
                            totalBytes = if (totalBytes > 0L) totalBytes else downloadedBytes,
                            downloadedBytes = downloadedBytes,
                            speedBytesPerSec = 0L,
                            contentUri = exportedUri.orEmpty(),
                            status = DownloadStatus.COMPLETED.name,
                            errorMessage = null
                        )
                    )
                    if (notifyOnComplete) {
                        postCompletionNotification(latest.fileName, targetFile, actualMime)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val current = repository.getDownloadRecord(recordId)
                if (current != null && current.statusEnum != DownloadStatus.PAUSED && current.statusEnum != DownloadStatus.CANCELLED) {
                    repository.updateDownloadRecord(
                        current.copy(
                            status = DownloadStatus.FAILED.name,
                            speedBytesPerSec = 0L,
                            errorMessage = e.localizedMessage ?: "Download failed"
                        )
                    )
                }
            } finally {
                activeJobs.remove(recordId)
            }
        }
        activeJobs[recordId] = job
    }

    private suspend fun saveDataUri(
        recordId: Long,
        dataUri: String,
        targetFile: File,
        fallbackMime: String,
        notifyOnComplete: Boolean
    ) {
        val commaIndex = dataUri.indexOf(',')
        if (commaIndex == -1) throw IllegalArgumentException("Malformed data URI")
        val header = dataUri.substring(5, commaIndex)
        val payload = dataUri.substring(commaIndex + 1)
        val mime = header.substringBefore(';').ifBlank { fallbackMime }
        val bytes = if (header.contains(";base64")) {
            Base64.decode(payload, Base64.DEFAULT)
        } else {
            payload.toByteArray()
        }
        targetFile.parentFile?.mkdirs()
        targetFile.writeBytes(bytes)
        val exported = exportToMediaStoreDownloads(targetFile, mime)
        val record = repository.getDownloadRecord(recordId) ?: return
        repository.updateDownloadRecord(
            record.copy(
                mimeType = mime,
                totalBytes = bytes.size.toLong(),
                downloadedBytes = bytes.size.toLong(),
                speedBytesPerSec = 0L,
                contentUri = exported.orEmpty(),
                status = DownloadStatus.COMPLETED.name
            )
        )
        if (notifyOnComplete) {
            postCompletionNotification(record.fileName, targetFile, mime)
        }
    }

    private fun exportToMediaStoreDownloads(file: File, mimeType: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/DSK Chrome")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { inp -> inp.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri.toString()
        }.getOrNull()
    }

    fun openDownloadedFile(item: DownloadEntity): Boolean {
        val uri = resolveFileUri(item) ?: return false
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, item.mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(Intent.createChooser(intent, "Open with").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    fun shareDownloadedFile(item: DownloadEntity): Boolean {
        val uri = resolveFileUri(item) ?: return false
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType.ifBlank { "*/*" }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(Intent.createChooser(intent, "Share ${item.fileName}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    private fun resolveFileUri(item: DownloadEntity): Uri? {
        val file = File(item.localPath)
        if (file.exists()) {
            return runCatching {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }.getOrNull()
        }
        if (item.contentUri.isNotBlank()) {
            return runCatching { Uri.parse(item.contentUri) }.getOrNull()
        }
        return null
    }

    private fun postCompletionNotification(fileName: String, file: File, mimeType: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "dsk_chrome_downloads"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "DSK Chrome Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Download complete")
            .setContentText(fileName)
            .setAutoCancel(true)
            .build()
        manager.notify(fileName.hashCode(), notification)
    }

    private fun sanitizeFileName(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(120)
        return cleaned.ifBlank { "download_${System.currentTimeMillis()}" }
    }

    private fun ensureUniqueFileName(name: String): String {
        var candidate = File(downloadDir, name)
        if (!candidate.exists()) return name
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var counter = 1
        while (candidate.exists()) {
            candidate = File(downloadDir, "${base}_($counter)$ext")
            counter++
        }
        return candidate.name
    }

    private fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            var value = bytes.toDouble()
            var idx = 0
            while (value >= 1024.0 && idx < units.lastIndex) {
                value /= 1024.0
                idx++
            }
            return if (idx == 0) "${value.toInt()} B" else String.format("%.1f %s", value, units[idx])
        }

        fun formatSpeed(bytesPerSec: Long): String {
            if (bytesPerSec <= 0L) return ""
            return "${formatBytes(bytesPerSec)}/s"
        }
    }
}
