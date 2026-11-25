package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.browser.SearchSuggestionItem
import com.example.browser.UrlUtils
import com.example.data.BrowserSettings
import com.example.data.HistoryEntity
import com.example.data.ShortcutEntity
import com.example.data.ThemeMode
import com.example.data.WallpaperPreset
import com.example.ui.theme.DskCobalt
import com.example.ui.theme.DskCyan
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class PopularSite(val title: String, val url: String, val badgeColor: Color)

private val popularWebsites = listOf(
    PopularSite("Google", "https://www.google.com", Color(0xFF4285F4)),
    PopularSite("YouTube", "https://www.youtube.com", Color(0xFFFF0033)),
    PopularSite("Wikipedia", "https://www.wikipedia.org", Color(0xFF64748B)),
    PopularSite("GitHub", "https://github.com", Color(0xFF24292F)),
    PopularSite("Reddit", "https://www.reddit.com", Color(0xFFFF4500)),
    PopularSite("Stack Overflow", "https://stackoverflow.com", Color(0xFFF48024))
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    isIncognito: Boolean,
    settings: BrowserSettings,
    shortcuts: List<ShortcutEntity>,
    recentHistory: List<HistoryEntity>,
    suggestions: List<SearchSuggestionItem>,
    onQueryChange: (String) -> Unit,
    onSubmitSearchOrUrl: (String) -> Unit,
    onClearSuggestions: () -> Unit,
    onVoiceSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAiSearch: () -> Unit,
    onOpenAddShortcut: () -> Unit,
    onDeleteShortcut: (Long) -> Unit,
    onOpenUrl: (String) -> Unit,
    tabCount: Int = 1,
    showChromeTopBar: Boolean = true,
    onOpenTabs: () -> Unit = {},
    onOpenMenu: () -> Unit = {},
    onOpenNewIncognitoTab: () -> Unit = {},
    applyTopStatusInset: Boolean = true,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var searchInput by remember { mutableStateOf("") }
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(30_000L)
        }
    }

    val systemDark = isSystemInDarkTheme()
    val useDarkUi = isIncognito || settings.themeMode == ThemeMode.DARK ||
        (settings.themeMode == ThemeMode.SYSTEM && systemDark)

    val dateStr = remember(currentTimeMillis) {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(currentTimeMillis))
    }
    val timeStr = remember(currentTimeMillis) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(currentTimeMillis))
    }
    val greeting = remember(currentTimeMillis) {
        val hour = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }.get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Late night browsing"
        }
    }

    val uiAlpha = settings.uiTransparency.coerceIn(0.35f, 1f)
    val glassBorderAlpha = if (settings.glassEffectEnabled) 0.24f else 0.12f

    Box(modifier = modifier.fillMaxSize()) {
        // Content Scroll over shared BrowserWallpaperBackdrop
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 28.dp),
            modifier = Modifier
                .fillMaxSize()
                .then(if (applyTopStatusInset) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
        ) {
            // TOP BAR: Chrome-style transparent header over wallpaper (Home | Settings, Tabs, 3-Dot)
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showChromeTopBar) {
                            IconButton(
                                onClick = {},
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "DSK CHROME Home",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Image(
                            painter = painterResource(id = R.drawable.img_dsk_brand_emblem),
                            contentDescription = "DSK CHROME Brand Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        if (isIncognito) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "PRIVATE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("home_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Browser Settings",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        if (showChromeTopBar) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onOpenTabs)
                                    .testTag("nav_tabs_button")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .border(
                                            width = 1.8.dp,
                                            color = if (isIncognito) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                ) {
                                    Text(
                                        text = if (tabCount > 99) "99" else tabCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isIncognito) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }

                            IconButton(
                                onClick = onOpenMenu,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("nav_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }

            // Date & Time + Greeting Widget OR Incognito Banner
            if (isIncognito) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                RoundedCornerShape(22.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Incognito Mode",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "You're in Private Mode",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "DSK CHROME won't save your browsing history, search history, cookies, or site data once you close all Private tabs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Notice: Private mode does not make you anonymous to the websites you visit, your employer or school, or your internet service provider.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .padding(top = 18.dp, bottom = 14.dp, start = 20.dp, end = 20.dp)
                    ) {
                        Text(
                            text = "DSK CHROME",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (settings.showHomepageClock) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$timeStr  •  $greeting  •  $dateStr",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // CENTER: Main Search & URL Bar
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = uiAlpha.coerceAtLeast(0.45f)),
                        shadowElevation = if (settings.glassEffectEnabled) 4.dp else 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (settings.glassEffectEnabled) {
                                    Color.White.copy(alpha = glassBorderAlpha)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                },
                                RoundedCornerShape(28.dp)
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = searchInput,
                                    onValueChange = {
                                        searchInput = it
                                        onQueryChange(it)
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            if (searchInput.isNotBlank()) {
                                                focusManager.clearFocus()
                                                onSubmitSearchOrUrl(searchInput)
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("home_search_input"),
                                    decorationBox = { inner ->
                                        if (searchInput.isEmpty()) {
                                            Text(
                                                text = "Search ${settings.searchEngine.displayName} or type URL",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        inner()
                                    }
                                )
                            }

                            if (searchInput.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchInput = ""
                                        onClearSuggestions()
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onVoiceSearch,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("home_voice_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice search",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (searchInput.isNotBlank()) {
                                            focusManager.clearFocus()
                                            onSubmitSearchOrUrl(searchInput)
                                        } else {
                                            onOpenUrl("https://www.google.com")
                                        }
                                    }
                                    .testTag("home_search_submit_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Go",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Search Suggestions Dropdown on Homepage
                    AnimatedVisibility(visible = suggestions.isNotEmpty() && searchInput.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Column {
                                suggestions.forEachIndexed { idx, item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                focusManager.clearFocus()
                                                onSubmitSearchOrUrl(item.url ?: item.text)
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (item.isHistory) Icons.Default.History else Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = item.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (idx < suggestions.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Chrome-style Dual Pill Buttons: AI Mode + Incognito
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = uiAlpha),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White.copy(alpha = glassBorderAlpha * 0.7f), RoundedCornerShape(24.dp))
                            .clickable(onClick = onOpenAiSearch)
                            .testTag("home_ai_search_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Mode",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Mode",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = uiAlpha),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White.copy(alpha = glassBorderAlpha * 0.7f), RoundedCornerShape(24.dp))
                            .clickable(onClick = onOpenNewIncognitoTab)
                            .testTag("home_incognito_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Incognito",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Incognito",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Quick Website Shortcuts + Add Shortcut Button
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Quick Shortcuts",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Hold to remove",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val rows = (shortcuts.map { it as ShortcutEntity? } + listOf(null)).chunked(4)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows.forEach { rowItems ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowItems.forEach { shortcut ->
                                    if (shortcut == null) {
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = (uiAlpha * 0.85f).coerceIn(0.3f, 0.95f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(18.dp))
                                                .border(1.dp, Color.White.copy(alpha = glassBorderAlpha * 0.55f), RoundedCornerShape(18.dp))
                                                .clickable(onClick = onOpenAddShortcut)
                                                .testTag("add_shortcut_button")
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp)
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Add shortcut",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "Add",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = uiAlpha),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(18.dp))
                                                .border(1.dp, Color.White.copy(alpha = glassBorderAlpha * 0.6f), RoundedCornerShape(18.dp))
                                                .combinedClickable(
                                                    onClick = { onOpenUrl(shortcut.url) },
                                                    onLongClick = { onDeleteShortcut(shortcut.id) }
                                                )
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp)
                                            ) {
                                                SiteFaviconBadge(
                                                    title = shortcut.title,
                                                    url = shortcut.url,
                                                    size = 40
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = shortcut.title,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                                repeat(4 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Popular Websites
            if (settings.showHomepagePopular) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            text = "Popular Websites",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(popularWebsites) { site ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = uiAlpha),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.dp, Color.White.copy(alpha = glassBorderAlpha * 0.6f), RoundedCornerShape(16.dp))
                                        .clickable { onOpenUrl(site.url) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(site.badgeColor.copy(alpha = 0.16f))
                                        ) {
                                            Text(
                                                text = site.title.take(1),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = site.badgeColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = site.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recently Visited Websites
            if (settings.showHomepageRecent && !isIncognito && recentHistory.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "Recently Visited",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = uiAlpha),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = glassBorderAlpha * 0.6f), RoundedCornerShape(20.dp))
                        ) {
                            Column {
                                recentHistory.take(5).forEachIndexed { idx, entry ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenUrl(entry.url) }
                                            .padding(horizontal = 14.dp, vertical = 11.dp)
                                    ) {
                                        SiteFaviconBadge(
                                            title = entry.title,
                                            url = entry.url,
                                            size = 34
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = UrlUtils.extractHost(entry.url).ifBlank { entry.url },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (idx < minOf(recentHistory.size, 5) - 1) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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

@Composable
fun SiteFaviconBadge(
    title: String,
    url: String,
    size: Int = 36
) {
    val faviconUrl = remember(url) { UrlUtils.buildFaviconUrl(url) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Text(
            text = title.trim().take(1).uppercase().ifBlank { "W" },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        if (faviconUrl.isNotBlank()) {
            AsyncImage(
                model = faviconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}
