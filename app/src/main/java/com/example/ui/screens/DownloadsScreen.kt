package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DownloadEntity
import com.example.data.DownloadStatus
import com.example.download.DownloadEngine

@Composable
fun DownloadsScreen(
    downloads: List<DownloadEntity>,
    onBack: () -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onOpen: (DownloadEntity) -> Unit,
    onShare: (DownloadEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onClearFinished: () -> Unit
) {
    BackHandler(onBack = onBack)

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .navigationBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("downloads_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("Download Manager", style = MaterialTheme.typography.titleLarge)
                }
                if (downloads.isNotEmpty()) {
                    TextButton(onClick = onClearFinished) {
                        Text("Clear Done")
                    }
                }
            }

            if (downloads.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Downloads Yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Downloaded images, PDFs, documents, and permitted media will appear here with live speed and pause/resume controls.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(downloads, key = { it.id }) { item ->
                        val status = item.statusEnum
                        val icon = when {
                            item.mimeType.startsWith("image/") -> Icons.Default.Image
                            item.mimeType.contains("pdf") -> Icons.Default.PictureAsPdf
                            item.mimeType.startsWith("video/") -> Icons.Default.VideoFile
                            else -> Icons.Default.Description
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(enabled = status == DownloadStatus.COMPLETED) { onOpen(item) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.fileName,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val sizeSummary = buildString {
                                            append(DownloadEngine.formatBytes(item.downloadedBytes))
                                            if (item.totalBytes > 0L) {
                                                append(" / ${DownloadEngine.formatBytes(item.totalBytes)}")
                                                append(" (${item.progressPercent}%)")
                                            }
                                            if (status == DownloadStatus.DOWNLOADING && item.speedBytesPerSec > 0L) {
                                                append(" • ${DownloadEngine.formatSpeed(item.speedBytesPerSec)}")
                                            }
                                        }
                                        Text(
                                            text = sizeSummary,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val statusLabel = when (status) {
                                            DownloadStatus.QUEUED -> "Queued"
                                            DownloadStatus.DOWNLOADING -> "Downloading"
                                            DownloadStatus.PAUSED -> "Paused"
                                            DownloadStatus.COMPLETED -> "Completed"
                                            DownloadStatus.FAILED -> item.errorMessage ?: "Failed"
                                            DownloadStatus.CANCELLED -> "Cancelled"
                                        }
                                        Text(
                                            text = statusLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when (status) {
                                                DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                                                DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }

                                    // Action buttons
                                    when (status) {
                                        DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                                            IconButton(onClick = { onPause(item.id) }) {
                                                Icon(Icons.Default.Pause, contentDescription = "Pause download")
                                            }
                                            IconButton(onClick = { onCancel(item.id) }) {
                                                Icon(Icons.Default.Close, contentDescription = "Cancel download")
                                            }
                                        }
                                        DownloadStatus.PAUSED, DownloadStatus.FAILED -> {
                                            IconButton(onClick = { onResume(item.id) }) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume download")
                                            }
                                            IconButton(onClick = { onDelete(item.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete download", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                        DownloadStatus.COMPLETED -> {
                                            IconButton(onClick = { onOpen(item) }) {
                                                Icon(Icons.Default.OpenInNew, contentDescription = "Open file")
                                            }
                                            IconButton(onClick = { onShare(item) }) {
                                                Icon(Icons.Default.Share, contentDescription = "Share file")
                                            }
                                            IconButton(onClick = { onDelete(item.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete download", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                        DownloadStatus.CANCELLED -> {
                                            IconButton(onClick = { onDelete(item.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete download", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }

                                if (status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (item.totalBytes > 0L) {
                                        LinearProgressIndicator(
                                            progress = { item.progressPercent / 100f },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
