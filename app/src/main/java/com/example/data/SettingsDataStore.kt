package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.URLEncoder

private val Context.browserDataStore: DataStore<Preferences> by preferencesDataStore(name = "dsk_chrome_settings")

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    DARK("Dark Mode"),
    LIGHT("Light Mode")
}

enum class SearchEngine(
    val displayName: String,
    val baseSearchUrl: String,
    val safeSearchParam: String
) {
    GOOGLE("Google", "https://www.google.com/search?q=", "&safe=active"),
    BING("Bing", "https://www.bing.com/search?q=", "&adlt=strict"),
    YAHOO("Yahoo", "https://search.yahoo.com/search?p=", "&vm=r"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=", "&kp=1"),
    BRAVE("Brave Search", "https://search.brave.com/search?q=", "&safesearch=strict");

    fun buildSearchUrl(query: String, safeSearchEnabled: Boolean): String {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        return buildString {
            append(baseSearchUrl)
            append(encoded)
            if (safeSearchEnabled) append(safeSearchParam)
        }
    }
}

enum class WallpaperPreset(val label: String) {
    AURORA("Cosmic Aurora"),
    PRISM("Daylight Prism"),
    OBSIDIAN("Midnight Gradient"),
    CUSTOM("Custom Image")
}

enum class ToolbarLayout(val label: String, val subtitle: String) {
    TOP_BAR(
        label = "Top (Chrome Style)",
        subtitle = "Single toolbar at the top, full menu in 3-dot"
    ),
    BOTTOM_FOCUS(
        label = "Bottom (Chrome One-Handed)",
        subtitle = "Address bar & 3-dot toolbar moved to the bottom"
    ),
    STANDARD(
        label = "Split (Top Address + Bottom Nav)",
        subtitle = "Address bar at top, navigation buttons at bottom"
    )
}

enum class StartupBehavior(val label: String) {
    RESTORE_TABS("Restore Previous Tabs"),
    OPEN_HOMEPAGE("Open DSK Homepage")
}

data class BrowserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val searchSuggestionsEnabled: Boolean = true,
    val searchHistoryEnabled: Boolean = true,
    val safeSearchEnabled: Boolean = false,
    val wallpaperPreset: WallpaperPreset = WallpaperPreset.AURORA,
    val customWallpaperPath: String = "",
    val showHomepageClock: Boolean = true,
    val showHomepagePopular: Boolean = true,
    val showHomepageRecent: Boolean = true,
    val toolbarLayout: ToolbarLayout = ToolbarLayout.TOP_BAR,
    val webFontSizePercent: Int = 100,
    val desktopSiteDefault: Boolean = false,
    val fullscreenBrowsing: Boolean = false,
    val startupBehavior: StartupBehavior = StartupBehavior.RESTORE_TABS,
    val customHomepageUrl: String = "",
    val appLanguage: String = "English",
    val blockPopups: Boolean = true,
    val safeBrowsingEnabled: Boolean = true,
    val doNotTrackEnabled: Boolean = true,
    val downloadNotificationsEnabled: Boolean = true,
    val askBeforeDownloading: Boolean = false,
    val defaultCameraDecision: PermissionDecision = PermissionDecision.ASK,
    val defaultMicDecision: PermissionDecision = PermissionDecision.ASK,
    val defaultLocationDecision: PermissionDecision = PermissionDecision.ASK,
    val defaultNotificationsDecision: PermissionDecision = PermissionDecision.ASK
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val SEARCH_SUGGESTIONS = booleanPreferencesKey("search_suggestions")
        val SEARCH_HISTORY = booleanPreferencesKey("search_history")
        val SAFE_SEARCH = booleanPreferencesKey("safe_search")
        val WALLPAPER_PRESET = stringPreferencesKey("wallpaper_preset")
        val CUSTOM_WALLPAPER_PATH = stringPreferencesKey("custom_wallpaper_path")
        val SHOW_CLOCK = booleanPreferencesKey("show_clock")
        val SHOW_POPULAR = booleanPreferencesKey("show_popular")
        val SHOW_RECENT = booleanPreferencesKey("show_recent")
        val TOOLBAR_LAYOUT = stringPreferencesKey("toolbar_layout")
        val FONT_SIZE_PERCENT = intPreferencesKey("font_size_percent")
        val DESKTOP_DEFAULT = booleanPreferencesKey("desktop_default")
        val FULLSCREEN_BROWSING = booleanPreferencesKey("fullscreen_browsing")
        val STARTUP_BEHAVIOR = stringPreferencesKey("startup_behavior")
        val CUSTOM_HOMEPAGE_URL = stringPreferencesKey("custom_homepage_url")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val BLOCK_POPUPS = booleanPreferencesKey("block_popups")
        val SAFE_BROWSING = booleanPreferencesKey("safe_browsing")
        val DO_NOT_TRACK = booleanPreferencesKey("do_not_track")
        val DOWNLOAD_NOTIFICATIONS = booleanPreferencesKey("download_notifications")
        val ASK_BEFORE_DOWNLOAD = booleanPreferencesKey("ask_before_download")
        val DEFAULT_CAM = stringPreferencesKey("default_cam")
        val DEFAULT_MIC = stringPreferencesKey("default_mic")
        val DEFAULT_LOC = stringPreferencesKey("default_loc")
        val DEFAULT_NOTIF = stringPreferencesKey("default_notif")
    }

    val settingsFlow: Flow<BrowserSettings> = context.browserDataStore.data.map { prefs ->
        BrowserSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            searchEngine = prefs[Keys.SEARCH_ENGINE]?.let { runCatching { SearchEngine.valueOf(it) }.getOrNull() }
                ?: SearchEngine.GOOGLE,
            searchSuggestionsEnabled = prefs[Keys.SEARCH_SUGGESTIONS] ?: true,
            searchHistoryEnabled = prefs[Keys.SEARCH_HISTORY] ?: true,
            safeSearchEnabled = prefs[Keys.SAFE_SEARCH] ?: false,
            wallpaperPreset = prefs[Keys.WALLPAPER_PRESET]?.let { runCatching { WallpaperPreset.valueOf(it) }.getOrNull() }
                ?: WallpaperPreset.AURORA,
            customWallpaperPath = prefs[Keys.CUSTOM_WALLPAPER_PATH] ?: "",
            showHomepageClock = prefs[Keys.SHOW_CLOCK] ?: true,
            showHomepagePopular = prefs[Keys.SHOW_POPULAR] ?: true,
            showHomepageRecent = prefs[Keys.SHOW_RECENT] ?: true,
            toolbarLayout = prefs[Keys.TOOLBAR_LAYOUT]?.let { runCatching { ToolbarLayout.valueOf(it) }.getOrNull() }
                ?: ToolbarLayout.TOP_BAR,
            webFontSizePercent = (prefs[Keys.FONT_SIZE_PERCENT] ?: 100).coerceIn(75, 160),
            desktopSiteDefault = prefs[Keys.DESKTOP_DEFAULT] ?: false,
            fullscreenBrowsing = prefs[Keys.FULLSCREEN_BROWSING] ?: false,
            startupBehavior = prefs[Keys.STARTUP_BEHAVIOR]?.let { runCatching { StartupBehavior.valueOf(it) }.getOrNull() }
                ?: StartupBehavior.RESTORE_TABS,
            customHomepageUrl = prefs[Keys.CUSTOM_HOMEPAGE_URL] ?: "",
            appLanguage = prefs[Keys.APP_LANGUAGE] ?: "English",
            blockPopups = prefs[Keys.BLOCK_POPUPS] ?: true,
            safeBrowsingEnabled = prefs[Keys.SAFE_BROWSING] ?: true,
            doNotTrackEnabled = prefs[Keys.DO_NOT_TRACK] ?: true,
            downloadNotificationsEnabled = prefs[Keys.DOWNLOAD_NOTIFICATIONS] ?: true,
            askBeforeDownloading = prefs[Keys.ASK_BEFORE_DOWNLOAD] ?: false,
            defaultCameraDecision = prefs[Keys.DEFAULT_CAM]?.let { runCatching { PermissionDecision.valueOf(it) }.getOrNull() }
                ?: PermissionDecision.ASK,
            defaultMicDecision = prefs[Keys.DEFAULT_MIC]?.let { runCatching { PermissionDecision.valueOf(it) }.getOrNull() }
                ?: PermissionDecision.ASK,
            defaultLocationDecision = prefs[Keys.DEFAULT_LOC]?.let { runCatching { PermissionDecision.valueOf(it) }.getOrNull() }
                ?: PermissionDecision.ASK,
            defaultNotificationsDecision = prefs[Keys.DEFAULT_NOTIF]?.let { runCatching { PermissionDecision.valueOf(it) }.getOrNull() }
                ?: PermissionDecision.ASK
        )
    }

    suspend fun updateThemeMode(mode: ThemeMode) = context.browserDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun updateSearchEngine(engine: SearchEngine) = context.browserDataStore.edit { it[Keys.SEARCH_ENGINE] = engine.name }
    suspend fun updateSearchSuggestions(enabled: Boolean) = context.browserDataStore.edit { it[Keys.SEARCH_SUGGESTIONS] = enabled }
    suspend fun updateSearchHistory(enabled: Boolean) = context.browserDataStore.edit { it[Keys.SEARCH_HISTORY] = enabled }
    suspend fun updateSafeSearch(enabled: Boolean) = context.browserDataStore.edit { it[Keys.SAFE_SEARCH] = enabled }
    suspend fun updateWallpaperPreset(preset: WallpaperPreset) = context.browserDataStore.edit { it[Keys.WALLPAPER_PRESET] = preset.name }
    suspend fun updateCustomWallpaperPath(path: String) = context.browserDataStore.edit {
        it[Keys.CUSTOM_WALLPAPER_PATH] = path
        it[Keys.WALLPAPER_PRESET] = WallpaperPreset.CUSTOM.name
    }
    suspend fun updateHomepageVisibility(clock: Boolean? = null, popular: Boolean? = null, recent: Boolean? = null) {
        context.browserDataStore.edit { prefs ->
            clock?.let { prefs[Keys.SHOW_CLOCK] = it }
            popular?.let { prefs[Keys.SHOW_POPULAR] = it }
            recent?.let { prefs[Keys.SHOW_RECENT] = it }
        }
    }
    suspend fun updateToolbarLayout(layout: ToolbarLayout) = context.browserDataStore.edit { it[Keys.TOOLBAR_LAYOUT] = layout.name }
    suspend fun updateWebFontSize(percent: Int) = context.browserDataStore.edit { it[Keys.FONT_SIZE_PERCENT] = percent.coerceIn(75, 160) }
    suspend fun updateDesktopDefault(enabled: Boolean) = context.browserDataStore.edit { it[Keys.DESKTOP_DEFAULT] = enabled }
    suspend fun updateFullscreenBrowsing(enabled: Boolean) = context.browserDataStore.edit { it[Keys.FULLSCREEN_BROWSING] = enabled }
    suspend fun updateStartupBehavior(behavior: StartupBehavior) = context.browserDataStore.edit { it[Keys.STARTUP_BEHAVIOR] = behavior.name }
    suspend fun updateCustomHomepageUrl(url: String) = context.browserDataStore.edit { it[Keys.CUSTOM_HOMEPAGE_URL] = url.trim() }
    suspend fun updateAppLanguage(language: String) = context.browserDataStore.edit { it[Keys.APP_LANGUAGE] = language }
    suspend fun updateBlockPopups(enabled: Boolean) = context.browserDataStore.edit { it[Keys.BLOCK_POPUPS] = enabled }
    suspend fun updateSafeBrowsing(enabled: Boolean) = context.browserDataStore.edit { it[Keys.SAFE_BROWSING] = enabled }
    suspend fun updateDoNotTrack(enabled: Boolean) = context.browserDataStore.edit { it[Keys.DO_NOT_TRACK] = enabled }
    suspend fun updateDownloadNotifications(enabled: Boolean) = context.browserDataStore.edit { it[Keys.DOWNLOAD_NOTIFICATIONS] = enabled }
    suspend fun updateAskBeforeDownloading(enabled: Boolean) = context.browserDataStore.edit { it[Keys.ASK_BEFORE_DOWNLOAD] = enabled }
    suspend fun updateDefaultPermission(type: SitePermissionType, decision: PermissionDecision) {
        context.browserDataStore.edit { prefs ->
            when (type) {
                SitePermissionType.CAMERA -> prefs[Keys.DEFAULT_CAM] = decision.name
                SitePermissionType.MICROPHONE -> prefs[Keys.DEFAULT_MIC] = decision.name
                SitePermissionType.LOCATION -> prefs[Keys.DEFAULT_LOC] = decision.name
                SitePermissionType.NOTIFICATIONS -> prefs[Keys.DEFAULT_NOTIF] = decision.name
                SitePermissionType.DOWNLOADS -> prefs[Keys.ASK_BEFORE_DOWNLOAD] = (decision == PermissionDecision.ASK)
                SitePermissionType.POPUPS -> prefs[Keys.BLOCK_POPUPS] = (decision == PermissionDecision.BLOCK)
            }
        }
    }
}
