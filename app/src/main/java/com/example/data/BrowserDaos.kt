package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    fun observeIsBookmarked(url: String): Flow<Boolean>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Update
    suspend fun update(bookmark: BookmarkEntity)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteByUrl(url: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY visitedAt DESC LIMIT 12")
    suspend fun searchTop(query: String): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history WHERE visitedAt >= :sinceTimestamp")
    suspend fun deleteSince(sinceTimestamp: Long)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long

    @Update
    suspend fun update(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloads WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    suspend fun clearFinished()
}

@Dao
interface SitePermissionDao {
    @Query("SELECT * FROM site_permissions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SitePermissionEntity>>

    @Query("SELECT * FROM site_permissions WHERE host = :host LIMIT 1")
    suspend fun getByHost(host: String): SitePermissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(permission: SitePermissionEntity)

    @Query("DELETE FROM site_permissions WHERE host = :host")
    suspend fun deleteByHost(host: String)

    @Query("DELETE FROM site_permissions")
    suspend fun clearAll()
}

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcuts ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<ShortcutEntity>>

    @Query("SELECT COUNT(*) FROM shortcuts")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shortcut: ShortcutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shortcuts: List<ShortcutEntity>)

    @Update
    suspend fun update(shortcut: ShortcutEntity)

    @Query("DELETE FROM shortcuts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SavedTabDao {
    @Query("SELECT * FROM saved_tabs ORDER BY sortOrder ASC")
    suspend fun getAll(): List<SavedTabEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tabs: List<SavedTabEntity>)

    @Query("DELETE FROM saved_tabs")
    suspend fun clearAll()
}

@Dao
interface WallpaperDao {
    @Query("SELECT * FROM wallpapers ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<WallpaperEntity?>

    @Query("SELECT * FROM wallpapers WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun observeFavorites(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE lastUsedAt > 0 ORDER BY lastUsedAt DESC LIMIT 20")
    fun observeRecentlyUsed(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WallpaperEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<WallpaperEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WallpaperEntity)

    @Query("UPDATE wallpapers SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE wallpapers SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun markUsed(id: String, timestamp: Long)

    @Query("DELETE FROM wallpapers WHERE id = :id")
    suspend fun deleteById(id: String)
}

