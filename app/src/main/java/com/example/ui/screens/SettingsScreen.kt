package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BrowserSettings
import com.example.data.SearchEngine
import com.example.data.StartupBehavior
import com.example.data.ThemeMode
import com.example.data.ToolbarLayout
import com.example.data.WallpaperPreset
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: BrowserSettings,
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenAbout: () -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateSearchEngine: (SearchEngine) -> Unit,
    onUpdateSearchSuggestions: (Boolean) -> Unit,
    onUpdateSearchHistory: (Boolean) -> Unit,
    onUpdateSafeSearch: (Boolean) -> Unit,
    onUpdateWallpaperPreset: (WallpaperPreset) -> Unit,
    onUploadWallpaper: () -> Unit,
    onUpdateHomepageVisibility: (clock: Boolean?, popular: Boolean?, recent: Boolean?) -> Unit,
    onUpdateToolbarLayout: (ToolbarLayout) -> Unit,
    onUpdateFontSize: (Int) -> Unit,
    onUpdateDesktopDefault: (Boolean) -> Unit,
    onUpdateFullscreenBrowsing: (Boolean) -> Unit,
    onUpdateStartupBehavior: (StartupBehavior) -> Unit,
    onUpdateCustomHomepageUrl: (String) -> Unit,
    onUpdateLanguage: (String) -> Unit,
    onUpdateBlockPopups: (Boolean) -> Unit,
    onUpdateSafeBrowsing: (Boolean) -> Unit,
    onUpdateDoNotTrack: (Boolean) -> Unit,
    onUpdateDownloadNotifications: (Boolean) -> Unit,
    onUpdateAskBeforeDownloading: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onClearCookies: () -> Unit,
    onClearWebsiteData: () -> Unit,
    onClearHistory: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showSearchEngineDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showToolbarDialog by remember { mutableStateOf(false) }
    var showStartupDialog by remember { mutableStateOf(false) }
    var showHomepageDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("DSK CHROME Settings", style = MaterialTheme.typography.titleLarge)
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // GENERAL
                item {
                    SettingsSectionCard(title = "GENERAL") {
                        SettingsActionRow(
                            title = "Homepage",
                            subtitle = settings.customHomepageUrl.ifBlank { "DSK CHROME Home" },
                            onClick = { showHomepageDialog = true }
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            title = "Default Search Engine",
                            subtitle = settings.searchEngine.displayName,
                            testTag = "settings_search_engine",
                            onClick = { showSearchEngineDialog = true }
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = "Search Suggestions",
                            subtitle = "Show live search & history suggestions while typing",
                            checked = settings.searchSuggestionsEnabled,
                            onCheckedChange = onUpdateSearchSuggestions
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = "Safe Search",
                            subtitle = "Filter explicit results when supported by search engine",
                            checked = settings.safeSearchEnabled,
                            onCheckedChange = onUpdateSafeSearch
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            title = "Startup Behavior",
                            subtitle = settings.startupBehavior.label,
                            onClick = { showStartupDialog = true }
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            title = "Language",
                            subtitle = settings.appLanguage,
                            onClick = { showLanguageDialog = true }
                        )
                    }
                }

                // APPEARANCE
                item {
                    SettingsSectionCard(title = "APPEARANCE") {
                        SettingsActionRow(
                            title = "Theme",
                            subtitle = settings.themeMode.label,
                            testTag = "settings_theme_mode",
                            onClick = { showThemeDialog = true }
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            title = "Wallpaper",
                            subtitle = settings.wallpaperPreset.label,
                            testTag = "settings_wallpaper",
                            onClick = { showWallpaperDialog = true }
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            title = "Toolbar & Nav Position",
                            subtitle = settings.toolbarLayout.label,
                            testTag = "settings_toolbar_position",
                            onClick = { showToolbarDialog = true }
                        )
                        SettingsDivider()
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Webpage Font Size", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${settings.webFontSizePercent}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = settings.webFontSizePercent.toFloat(),
                                onValueChange = { onUpdateFontSize(it.roundToInt()) },
                                valueRange = 75f..160f,
                                steps = 16
                            )
                        }
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = "Desktop Site by Default",
                            subtitle = "Request desktop versions of websites in new tabs",
                            checked = settings.desktopSiteDefault,
                            onCheckedChange = onUpdateDesktopDefault
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = "Full-Screen Browsing",
                            subtitle = "Hide browser toolbars while viewing webpages",
                            checked = settings.fullscreenBrowsing,
                            onCheckedChange = onUpdateFullscreenBrowsing
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = "Homepage Clock & Greeting",
                            subtitle = "Show live clock and greeting on DSK Home",
                            checked = settings.showHomepageClock,
                            onCheckedChange = { onUpdateHomepageVisibility(it, null, null) }
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = "Homepage Popular Websites",
                            subtitle = "Show popular site suggestions on DSK Home",
                            checked = settings.showHomepagePopular,
                            onCheckedChange = { onUpdateHomepageVisibility(null, it, null) }
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = "Homepage Recently Visited",
                            subtitle = "Show recently visited sites on DSK Home",
                            checked = settings.showHomepageRecent,
                            onCheckedChange = { onUpdateHomepageVisibility(null, null, it) }
                        )
                    }
                }

                // PRIVACY
                item {
                    SettingsSectionCard(title = "PRIVACY") {
                        SettingsSwitchRow(
                            title = "Save Browsing & Search History",
                            subtitle = "Private Incognito tabs never save history",
                            checked = settings.searchHistoryEnabled,
                            onCheckedChange = onUpdateSearchHistory
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = "Send Do Not Track (DNT)",
                            subtitle = "Include DNT: 1 header with webpage requests",
                            checked = settings.doNotTrackEnabled,
                            onCheckedChange = onUpdateDoNotTrack
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            title = "Clear Cache",
                            subtitle = "Free temporary webpage files",
                            onClick = onClearCache
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            title = "Clear Cookies",
                            subtitle = "Sign out of websites and reset session cookies",
                            onClick = onClearCookies
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            title = "Clear Website Data",
                            subtitle = "Remove local WebStorage & form data",
                            onClick = onClearWebsiteData
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            title = "Clear Browsing History",
                            subtitle = "Remove all visited website records",
                            onClick = onClearHistory
                        )
                    }
                }

                // DOWNLOADS
                item {
                    SettingsSectionCard(title = "DOWNLOADS") {
                        SettingsActionRow(
                            title = "Download Location",
                            subtitle = "Downloads / DSK Chrome",
                            onClick = {}
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = "Download Notifications",
                            subtitle = "Notify when file downloads finish",
                            checked = settings.downloadNotificationsEnabled,
                            onCheckedChange = onUpdateDownloadNotifications
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = "Ask Before Downloading",
                            subtitle = "Confirm file name and size before downloading",
                            checked = settings.askBeforeDownloading,
                            onCheckedChange = onUpdateAskBeforeDownloading
                        )
                    }
                }

                // SECURITY
                item {
                    SettingsSectionCard(title = "SECURITY") {
                        SettingsActionRow(
                            title = "Website Permissions",
                            subtitle = "Manage Camera, Mic, Location, Pop-ups & Downloads",
                            testTag = "settings_permissions_row",
                            onClick = onOpenPermissions
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = "Safe Browsing Warnings",
                            subtitle = "Warn before visiting suspicious or deceptive URLs",
                            checked = settings.safeBrowsingEnabled,
                            onCheckedChange = onUpdateSafeBrowsing
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = "Block Pop-ups",
                            subtitle = "Prevent sites from opening unwanted pop-up windows",
                            checked = settings.blockPopups,
                            onCheckedChange = onUpdateBlockPopups
                        )
                    }
                }

                // ABOUT
                item {
                    SettingsSectionCard(title = "ABOUT") {
                        SettingsActionRow(
                            title = "DSK CHROME",
                            subtitle = "Version 1.0.0 • Developer: DSK OFFICIAL",
                            testTag = "settings_about_row",
                            onClick = onOpenAbout
                        )
                    }
                }
            }
        }
    }

    if (showSearchEngineDialog) {
        SingleChoiceSettingsDialog(
            title = "Default Search Engine",
            options = SearchEngine.entries.map { it to it.displayName },
            selected = settings.searchEngine,
            onSelect = {
                onUpdateSearchEngine(it)
                showSearchEngineDialog = false
            },
            onDismiss = { showSearchEngineDialog = false }
        )
    }

    if (showThemeDialog) {
        SingleChoiceSettingsDialog(
            title = "Appearance Theme",
            options = ThemeMode.entries.map { it to it.label },
            selected = settings.themeMode,
            onSelect = {
                onUpdateThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = { Text("Homepage Wallpaper") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        WallpaperPreset.AURORA,
                        WallpaperPreset.PRISM,
                        WallpaperPreset.OBSIDIAN
                    ).forEach { preset ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onUpdateWallpaperPreset(preset)
                                    showWallpaperDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = settings.wallpaperPreset == preset,
                                onClick = {
                                    onUpdateWallpaperPreset(preset)
                                    showWallpaperDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(preset.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            showWallpaperDialog = false
                            onUploadWallpaper()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Background Image")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWallpaperDialog = false }) { Text("Close") }
            }
        )
    }

    if (showToolbarDialog) {
        AlertDialog(
            onDismissRequest = { showToolbarDialog = false },
            title = { Text("Toolbar & Nav Position") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToolbarLayout.entries.forEach { layout ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onUpdateToolbarLayout(layout)
                                    showToolbarDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .testTag("toolbar_option_${layout.name.lowercase()}")
                        ) {
                            RadioButton(
                                selected = layout == settings.toolbarLayout,
                                onClick = {
                                    onUpdateToolbarLayout(layout)
                                    showToolbarDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = layout.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = layout.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showToolbarDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showStartupDialog) {
        SingleChoiceSettingsDialog(
            title = "Startup Behavior",
            options = StartupBehavior.entries.map { it to it.label },
            selected = settings.startupBehavior,
            onSelect = {
                onUpdateStartupBehavior(it)
                showStartupDialog = false
            },
            onDismiss = { showStartupDialog = false }
        )
    }

    if (showLanguageDialog) {
        val languages = listOf("English", "Hindi", "Spanish", "French", "German", "Arabic", "Japanese", "Portuguese")
        SingleChoiceSettingsDialog(
            title = "Browser Language",
            options = languages.map { it to it },
            selected = settings.appLanguage,
            onSelect = {
                onUpdateLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showHomepageDialog) {
        var urlInput by remember(settings.customHomepageUrl) { mutableStateOf(settings.customHomepageUrl) }
        AlertDialog(
            onDismissRequest = { showHomepageDialog = false },
            title = { Text("Homepage URL") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Leave blank to use the built-in DSK CHROME Homepage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Custom Homepage URL") },
                        placeholder = { Text("https://www.google.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateCustomHomepageUrl(urlInput)
                    showHomepageDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onUpdateCustomHomepageUrl("")
                    showHomepageDialog = false
                }) { Text("Use Default") }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    testTag: String? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> SingleChoiceSettingsDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(value) }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        RadioButton(selected = value == selected, onClick = { onSelect(value) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
