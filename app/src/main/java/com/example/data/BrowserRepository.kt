package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import com.example.wallpaper.AiAspectRatio
import com.example.wallpaper.AiWallpaperStyle
import com.example.wallpaper.DskLivePreset
import com.example.wallpaper.WallpaperMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class BrowserRepository(
    private val context: Context,
    private val database: BrowserDatabase,
    val settingsStore: SettingsDataStore
) {
    private val bookmarkDao = database.bookmarkDao()
    private val historyDao = database.historyDao()
    private val downloadDao = database.downloadDao()
    private val permissionDao = database.sitePermissionDao()
    private val shortcutDao = database.shortcutDao()
    private val savedTabDao = database.savedTabDao()
    private val wallpaperDao = database.wallpaperDao()

    private val suggestClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.observeAll()
    val history: Flow<List<HistoryEntity>> = historyDao.observeAll()
    val recentHistory: Flow<List<HistoryEntity>> = historyDao.observeRecent(8)
    val downloads: Flow<List<DownloadEntity>> = downloadDao.observeAll()
    val sitePermissions: Flow<List<SitePermissionEntity>> = permissionDao.observeAll()
    val shortcuts: Flow<List<ShortcutEntity>> = shortcutDao.observeAll()
    val wallpapers: Flow<List<WallpaperEntity>> = wallpaperDao.observeAll()
    val favoriteWallpapers: Flow<List<WallpaperEntity>> = wallpaperDao.observeFavorites()
    val recentWallpapers: Flow<List<WallpaperEntity>> = wallpaperDao.observeRecentlyUsed()
    val settings: Flow<BrowserSettings> = settingsStore.settingsFlow

    fun observeIsBookmarked(url: String): Flow<Boolean> = bookmarkDao.observeIsBookmarked(url)

    suspend fun ensureDefaultShortcuts() {
        if (shortcutDao.count() == 0) {
            shortcutDao.insertAll(
                listOf(
                    ShortcutEntity(title = "Google", url = "https://www.google.com", sortOrder = 0),
                    ShortcutEntity(title = "YouTube", url = "https://www.youtube.com", sortOrder = 1),
                    ShortcutEntity(title = "Wikipedia", url = "https://www.wikipedia.org", sortOrder = 2),
                    ShortcutEntity(title = "GitHub", url = "https://github.com", sortOrder = 3),
                    ShortcutEntity(title = "Reddit", url = "https://www.reddit.com", sortOrder = 4),
                    ShortcutEntity(title = "BBC News", url = "https://www.bbc.com/news", sortOrder = 5)
                )
            )
        }
    }

    suspend fun ensureDefaultLiveWallpapers() {
        val baseTime = 1_700_000_000_000L
        val presets = DskLivePreset.entries.mapIndexed { index, preset ->
            WallpaperEntity(
                id = preset.id,
                title = preset.title,
                type = WallpaperMediaType.DSK_LIVE.name,
                livePresetId = preset.id,
                styleTag = preset.styleTag,
                promptText = preset.subtitle,
                createdAt = baseTime + index
            )
        }
        wallpaperDao.insertIgnore(presets)
    }

    suspend fun addShortcut(title: String, url: String) {
        val normalizedUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        val nextOrder = shortcutDao.count()
        shortcutDao.insert(ShortcutEntity(title = title.trim(), url = normalizedUrl.trim(), sortOrder = nextOrder))
    }

    suspend fun deleteShortcut(id: Long) = shortcutDao.deleteById(id)

    suspend fun toggleBookmark(title: String, url: String, folder: String = "Bookmarks"): Boolean {
        val existing = bookmarkDao.getByUrl(url)
        return if (existing != null) {
            bookmarkDao.delete(existing)
            false
        } else {
            val host = runCatching { Uri.parse(url).host.orEmpty() }.getOrDefault("")
            val favicon = if (host.isNotBlank()) "https://www.google.com/s2/favicons?domain=$host&sz=64" else ""
            bookmarkDao.insert(
                BookmarkEntity(
                    title = title.ifBlank { host.ifBlank { url } },
                    url = url,
                    folder = folder.ifBlank { "Bookmarks" },
                    faviconUrl = favicon
                )
            )
            true
        }
    }

    suspend fun addOrUpdateBookmark(bookmark: BookmarkEntity) {
        val host = runCatching { Uri.parse(bookmark.url).host.orEmpty() }.getOrDefault("")
        val favicon = bookmark.faviconUrl.ifBlank {
            if (host.isNotBlank()) "https://www.google.com/s2/favicons?domain=$host&sz=64" else ""
        }
        if (bookmark.id == 0L) {
            bookmarkDao.insert(bookmark.copy(faviconUrl = favicon))
        } else {
            bookmarkDao.update(bookmark.copy(faviconUrl = favicon))
        }
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) = bookmarkDao.delete(bookmark)

    suspend fun recordVisit(title: String, url: String) {
        if (url.isBlank() || url.startsWith("about:") || url.startsWith("data:")) return
        historyDao.insert(
            HistoryEntity(
                title = title.ifBlank { url },
                url = url,
                visitedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteHistoryItem(id: Long) = historyDao.deleteById(id)

    suspend fun clearHistorySince(sinceTimestamp: Long?) {
        if (sinceTimestamp == null) {
            historyDao.clearAll()
        } else {
            historyDao.deleteSince(sinceTimestamp)
        }
    }

    suspend fun searchLocalHistory(query: String): List<HistoryEntity> {
        if (query.isBlank()) return emptyList()
        return historyDao.searchTop(query)
    }

    suspend fun fetchSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val request = Request.Builder()
                .url("https://suggestqueries.google.com/complete/search?client=firefox&q=$encoded")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            suggestClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching emptyList()
                val body = resp.body?.string().orEmpty()
                val root = JSONArray(body)
                val suggestions = root.optJSONArray(1) ?: return@runCatching emptyList()
                buildList {
                    for (i in 0 until minOf(suggestions.length(), 6)) {
                        val item = suggestions.optString(i)
                        if (item.isNotBlank()) add(item)
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun getPermissionForHost(host: String, type: SitePermissionType, defaultDecision: PermissionDecision): PermissionDecision {
        val cleanHost = host.lowercase()
        val entry = permissionDao.getByHost(cleanHost) ?: return defaultDecision
        return entry.decisionFor(type)
    }

    suspend fun setPermissionForHost(host: String, type: SitePermissionType, decision: PermissionDecision) {
        val cleanHost = host.lowercase()
        val current = permissionDao.getByHost(cleanHost) ?: SitePermissionEntity(host = cleanHost)
        permissionDao.upsert(current.withDecision(type, decision))
    }

    suspend fun deleteSitePermissions(host: String) = permissionDao.deleteByHost(host)

    suspend fun clearAllSitePermissions() = permissionDao.clearAll()

    suspend fun getSavedTabs(): List<SavedTabEntity> = savedTabDao.getAll()

    suspend fun persistTabs(tabs: List<SavedTabEntity>) {
        savedTabDao.clearAll()
        if (tabs.isNotEmpty()) {
            savedTabDao.insertAll(tabs)
        }
    }

    suspend fun saveCustomWallpaperFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "wallpapers").apply { mkdirs() }
            val dest = File(dir, "custom_wallpaper_${System.currentTimeMillis()}.jpg")
            dir.listFiles()?.forEach { if (it.name.startsWith("custom_wallpaper_")) it.delete() }
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.absolutePath
        }.getOrNull()
    }

    suspend fun importPhotoWallpaperFromUri(uri: Uri): WallpaperEntity? = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val dir = File(context.filesDir, "wallpapers").apply { mkdirs() }
            val displayName = resolveUriDisplayName(uri) ?: "Photo Wallpaper"
            val ext = displayName.substringAfterLast('.', "jpg")
                .lowercase()
                .takeIf { it in setOf("jpg", "jpeg", "png", "webp") }
                ?: "jpg"
            val dest = File(dir, "photo_$now.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            if (!dest.exists() || dest.length() == 0L) return@runCatching null

            val entity = WallpaperEntity(
                id = "photo_$now",
                title = displayName.substringBeforeLast('.').ifBlank { "My Photo" }.take(36),
                type = WallpaperMediaType.PHOTO.name,
                sourceUriOrPath = dest.absolutePath,
                styleTag = "Photo",
                createdAt = now
            )
            wallpaperDao.upsert(entity)
            entity
        }.getOrNull()
    }

    suspend fun importVideoWallpaperFromUri(uri: Uri): WallpaperEntity? = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val displayName = resolveUriDisplayName(uri) ?: "Video Wallpaper"
            val persisted = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                true
            }.getOrDefault(false)

            val sourceRef: String = if (persisted) {
                uri.toString()
            } else {
                // Only copy if under 50 MB when provider denies persistable URI grant
                val dir = File(context.filesDir, "wallpapers").apply { mkdirs() }
                val dest = File(dir, "video_$now.mp4")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var copied = 0L
                        val maxBytes = 50L * 1024L * 1024L
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            copied += read
                            if (copied > maxBytes) {
                                dest.delete()
                                return@runCatching null
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                } ?: return@runCatching null
                dest.absolutePath
            }

            val entity = WallpaperEntity(
                id = "video_$now",
                title = displayName.substringBeforeLast('.').ifBlank { "My Video" }.take(36),
                type = WallpaperMediaType.VIDEO.name,
                sourceUriOrPath = sourceRef,
                styleTag = "Video",
                videoMuted = true,
                videoLoop = true,
                createdAt = now
            )
            wallpaperDao.upsert(entity)
            entity
        }.getOrNull()
    }

    suspend fun saveAiGeneratedWallpaper(
        bitmap: Bitmap,
        prompt: String,
        style: AiWallpaperStyle,
        aspectRatio: AiAspectRatio
    ): WallpaperEntity? = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val dir = File(context.filesDir, "wallpapers").apply { mkdirs() }
            val dest = File(dir, "ai_$now.png")
            dest.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            val entity = WallpaperEntity(
                id = "ai_$now",
                title = prompt.trim().take(28).ifBlank { "AI ${style.label}" },
                type = WallpaperMediaType.AI_IMAGE.name,
                sourceUriOrPath = dest.absolutePath,
                styleTag = style.label,
                promptText = prompt.trim(),
                aspectRatio = aspectRatio.ratioValue,
                createdAt = now
            )
            wallpaperDao.upsert(entity)
            entity
        }.getOrNull()
    }

    suspend fun upsertWallpaper(wallpaper: WallpaperEntity) = wallpaperDao.upsert(wallpaper)
    suspend fun getWallpaperById(id: String): WallpaperEntity? = wallpaperDao.getById(id)
    suspend fun setWallpaperFavorite(id: String, favorite: Boolean) = wallpaperDao.setFavorite(id, favorite)
    suspend fun markWallpaperUsed(id: String) = wallpaperDao.markUsed(id, System.currentTimeMillis())

    suspend fun deleteWallpaper(wallpaper: WallpaperEntity) = withContext(Dispatchers.IO) {
        if (wallpaper.type == WallpaperMediaType.DSK_LIVE.name) return@withContext
        val path = wallpaper.sourceUriOrPath
        if (path.startsWith(context.filesDir.absolutePath)) {
            runCatching { File(path).delete() }
        }
        wallpaperDao.deleteById(wallpaper.id)
    }

    private fun resolveUriDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        }.getOrNull()
    }

    suspend fun createDownloadRecord(download: DownloadEntity): Long = downloadDao.insert(download)
    suspend fun updateDownloadRecord(download: DownloadEntity) = downloadDao.update(download)
    suspend fun getDownloadRecord(id: Long): DownloadEntity? = downloadDao.getById(id)
    suspend fun deleteDownloadRecord(id: Long) = downloadDao.deleteById(id)
    suspend fun clearFinishedDownloads() = downloadDao.clearFinished()
}
