package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class PermissionDecision(val label: String) {
    ALLOW("Allow"),
    BLOCK("Block"),
    ASK("Ask every time")
}

enum class SitePermissionType(val label: String) {
    CAMERA("Camera"),
    MICROPHONE("Microphone"),
    LOCATION("Location"),
    NOTIFICATIONS("Notifications"),
    DOWNLOADS("Downloads"),
    POPUPS("Pop-ups")
}

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val folder: String = "Bookmarks",
    val faviconUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val url: String,
    val mimeType: String = "application/octet-stream",
    val localPath: String = "",
    val contentUri: String = "",
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val status: String = DownloadStatus.QUEUED.name,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val statusEnum: DownloadStatus
        get() = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.FAILED)

    val progressPercent: Int
        get() = if (totalBytes > 0L) {
            ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
        } else if (statusEnum == DownloadStatus.COMPLETED) {
            100
        } else {
            0
        }
}

@Entity(tableName = "site_permissions")
data class SitePermissionEntity(
    @PrimaryKey val host: String,
    val camera: String = PermissionDecision.ASK.name,
    val microphone: String = PermissionDecision.ASK.name,
    val location: String = PermissionDecision.ASK.name,
    val notifications: String = PermissionDecision.ASK.name,
    val downloads: String = PermissionDecision.ALLOW.name,
    val popups: String = PermissionDecision.BLOCK.name,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun decisionFor(type: SitePermissionType): PermissionDecision {
        val raw = when (type) {
            SitePermissionType.CAMERA -> camera
            SitePermissionType.MICROPHONE -> microphone
            SitePermissionType.LOCATION -> location
            SitePermissionType.NOTIFICATIONS -> notifications
            SitePermissionType.DOWNLOADS -> downloads
            SitePermissionType.POPUPS -> popups
        }
        return runCatching { PermissionDecision.valueOf(raw) }.getOrDefault(PermissionDecision.ASK)
    }

    fun withDecision(type: SitePermissionType, decision: PermissionDecision): SitePermissionEntity {
        val now = System.currentTimeMillis()
        return when (type) {
            SitePermissionType.CAMERA -> copy(camera = decision.name, updatedAt = now)
            SitePermissionType.MICROPHONE -> copy(microphone = decision.name, updatedAt = now)
            SitePermissionType.LOCATION -> copy(location = decision.name, updatedAt = now)
            SitePermissionType.NOTIFICATIONS -> copy(notifications = decision.name, updatedAt = now)
            SitePermissionType.DOWNLOADS -> copy(downloads = decision.name, updatedAt = now)
            SitePermissionType.POPUPS -> copy(popups = decision.name, updatedAt = now)
        }
    }
}

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val sortOrder: Int = 0
)

@Entity(tableName = "saved_tabs")
data class SavedTabEntity(
    @PrimaryKey val tabId: String,
    val title: String,
    val url: String,
    val isHomepage: Boolean,
    val isDesktopMode: Boolean,
    val sortOrder: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallpapers")
data class WallpaperEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String, // WallpaperMediaType.name
    val sourceUriOrPath: String = "",
    val livePresetId: String = "",
    val styleTag: String = "",
    val promptText: String = "",
    val aspectRatio: String = "9:16",
    val fitMode: String = "FILL", // WallpaperFitMode.name
    val zoomScale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val brightness: Float = 1f,
    val contrast: Float = 1f,
    val blurRadiusDp: Float = 0f,
    val opacity: Float = 1f,
    val videoMuted: Boolean = true,
    val videoLoop: Boolean = true,
    val videoSpeed: Float = 1f,
    val videoTrimStartMs: Long = 0L,
    val videoTrimEndMs: Long = 0L,
    val isFavorite: Boolean = false,
    val lastUsedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

