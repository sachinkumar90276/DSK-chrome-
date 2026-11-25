package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.AiWallpaperGenState
import com.example.data.BrowserSettings
import com.example.data.WallpaperEntity
import com.example.wallpaper.AiAspectRatio
import com.example.wallpaper.AiImageQuality
import com.example.wallpaper.AiWallpaperStyle
import com.example.wallpaper.AiWallpaperSuggestion
import com.example.wallpaper.DskLiveCanvasView
import com.example.wallpaper.DskLivePreset
import com.example.wallpaper.WallpaperMediaType
import com.example.wallpaper.WallpaperStudioSection
import com.example.wallpaper.WallpaperThumbnail
import kotlin.math.roundToInt

@Composable
fun WallpaperStudioScreen(
    settings: BrowserSettings,
    wallpapers: List<WallpaperEntity>,
    favoriteWallpapers: List<WallpaperEntity>,
    recentWallpapers: List<WallpaperEntity>,
    suggestions: List<AiWallpaperSuggestion>,
    aiGenState: AiWallpaperGenState,
    isSystemLiveWallpaperActive: Boolean,
    onBack: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickVideo: () -> Unit,
    onOpenPreview: (WallpaperEntity) -> Unit,
    onToggleFavorite: (WallpaperEntity) -> Unit,
    onApplyBrowserWallpaper: (WallpaperEntity) -> Unit,
    onApplyNewTabWallpaper: (WallpaperEntity) -> Unit,
    onSetAndroidLiveWallpaper: (WallpaperEntity) -> Unit,
    onClearActiveWallpapers: () -> Unit,
    onGenerateAiWallpaper: (prompt: String, style: AiWallpaperStyle, aspectRatio: AiAspectRatio, quality: AiImageQuality) -> Unit,
    onResetAiState: () -> Unit,
    onUpdateAppearance: (
        opacity: Float?,
        blurDp: Float?,
        brightness: Float?,
        uiTransparency: Float?,
        glassEffect: Boolean?,
        transparentWebView: Boolean?,
        aiSuggestions: Boolean?,
        lowPowerStatic: Boolean?
    ) -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedSection by remember { mutableStateOf(WallpaperStudioSection.MY_WALLPAPERS) }
    var showAppearanceControls by remember { mutableStateOf(false) }

    var aiPrompt by remember { mutableStateOf("") }
    var aiStyle by remember { mutableStateOf(AiWallpaperStyle.CYBERPUNK) }
    var aiAspectRatio by remember { mutableStateOf(AiAspectRatio.PORTRAIT_9_16) }
    var aiQuality by remember { mutableStateOf(AiImageQuality.FAST) }

    var aiLivePrompt by remember { mutableStateOf("") }
    var aiLiveLoop by remember { mutableStateOf(true) }
    var aiLiveMute by remember { mutableStateOf(true) }
    var aiLivePreviewPreset by remember { mutableStateOf(DskLivePreset.DSK_CHROME_FLOW) }

    val filteredWallpapers = remember(selectedSection, wallpapers, favoriteWallpapers, recentWallpapers) {
        when (selectedSection) {
            WallpaperStudioSection.MY_WALLPAPERS -> wallpapers.filter {
                it.type != WallpaperMediaType.DSK_LIVE.name || it.isFavorite || it.lastUsedAt > 0L
            }.ifEmpty { wallpapers }
            WallpaperStudioSection.PHOTO_WALLPAPERS -> wallpapers.filter { it.type == WallpaperMediaType.PHOTO.name }
            WallpaperStudioSection.VIDEO_WALLPAPERS -> wallpapers.filter {
                it.type == WallpaperMediaType.VIDEO.name || it.type == WallpaperMediaType.AI_LIVE.name
            }
            WallpaperStudioSection.AI_WALLPAPERS -> wallpapers.filter { it.type == WallpaperMediaType.AI_IMAGE.name }
            WallpaperStudioSection.AI_LIVE_WALLPAPERS -> wallpapers.filter { it.type == WallpaperMediaType.AI_LIVE.name }
            WallpaperStudioSection.DSK_LIVE_WALLPAPERS -> wallpapers.filter { it.type == WallpaperMediaType.DSK_LIVE.name }
            WallpaperStudioSection.FAVORITES -> favoriteWallpapers
            WallpaperStudioSection.RECENTLY_USED -> recentWallpapers
        }
    }

    val activeBrowserTitle = remember(settings.activeBrowserWallpaperId, wallpapers) {
        wallpapers.firstOrNull { it.id == settings.activeBrowserWallpaperId }?.title ?: settings.wallpaperPreset.label
    }
    val activeNewTabTitle = remember(settings.activeNewTabWallpaperId, settings.activeBrowserWallpaperId, wallpapers) {
        val targetId = settings.activeNewTabWallpaperId.ifBlank { settings.activeBrowserWallpaperId }
        wallpapers.firstOrNull { it.id == targetId }?.title ?: settings.wallpaperPreset.label
    }

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
            // Top Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("wallpaper_studio_back")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DSK WALLPAPER STUDIO",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Browser, New Tab & Live Wallpapers",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showAppearanceControls = !showAppearanceControls },
                    modifier = Modifier.testTag("studio_appearance_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Browser wallpaper display controls",
                        tint = if (showAppearanceControls) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onClearActiveWallpapers,
                    modifier = Modifier.testTag("studio_reset_wallpaper")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset to default wallpaper",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Active Summary Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Browser: $activeBrowserTitle",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "New Tab: $activeNewTabTitle",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSystemLiveWallpaperActive) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ) {
                        Text(
                            text = if (isSystemLiveWallpaperActive) "Phone Live: Active" else "Phone Live: Standby",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSystemLiveWallpaperActive) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Expandable Global Browser UI / Transparency / Glass Controls
            AnimatedVisibility(visible = showAppearanceControls) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "Browser & WebView Presentation",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        StudioSliderRow(
                            label = "Wallpaper Opacity",
                            value = settings.wallpaperOpacity,
                            range = 0.15f..1f,
                            valueText = "${(settings.wallpaperOpacity * 100).roundToInt()}%",
                            onValueChange = { onUpdateAppearance(it, null, null, null, null, null, null, null) }
                        )
                        StudioSliderRow(
                            label = "Background Blur",
                            value = settings.wallpaperBlurDp,
                            range = 0f..24f,
                            valueText = "${settings.wallpaperBlurDp.roundToInt()} dp",
                            onValueChange = { onUpdateAppearance(null, it, null, null, null, null, null, null) }
                        )
                        StudioSliderRow(
                            label = "Brightness",
                            value = settings.wallpaperBrightness,
                            range = 0.3f..1.5f,
                            valueText = "${(settings.wallpaperBrightness * 100).roundToInt()}%",
                            onValueChange = { onUpdateAppearance(null, null, it, null, null, null, null, null) }
                        )
                        StudioSliderRow(
                            label = "Browser UI Opacity (Glass)",
                            value = settings.uiTransparency,
                            range = 0.35f..1f,
                            valueText = "${(settings.uiTransparency * 100).roundToInt()}%",
                            onValueChange = { onUpdateAppearance(null, null, null, it, null, null, null, null) }
                        )
                        StudioSwitchLine(
                            title = "Glassmorphism Cards & Bars",
                            checked = settings.glassEffectEnabled,
                            onCheckedChange = { onUpdateAppearance(null, null, null, null, it, null, null, null) }
                        )
                        StudioSwitchLine(
                            title = "Transparent WebView Canvas",
                            checked = settings.transparentWebViewEnabled,
                            onCheckedChange = { onUpdateAppearance(null, null, null, null, null, it, null, null) }
                        )
                        Text(
                            text = "Transparent WebView shows your wallpaper behind webpages that do not define an opaque background color.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StudioSwitchLine(
                            title = "Low-Power Static Mode",
                            checked = settings.lowPowerStaticWallpaperMode,
                            onCheckedChange = { onUpdateAppearance(null, null, null, null, null, null, null, it) }
                        )
                        StudioSwitchLine(
                            title = "AI Wallpaper Suggestions",
                            checked = settings.aiWallpaperSuggestionsEnabled,
                            onCheckedChange = { onUpdateAppearance(null, null, null, null, null, null, it, null) }
                        )
                    }
                }
            }

            // 8 Category Sections Strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(WallpaperStudioSection.entries) { section ->
                    FilterChip(
                        selected = selectedSection == section,
                        onClick = { selectedSection = section },
                        label = { Text(section.label) },
                        modifier = Modifier.testTag("studio_tab_${section.name.lowercase()}")
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Quick Import Buttons (Photo & Video Pickers)
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onPickPhoto,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("studio_pick_photo_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Photo")
                        }
                        OutlinedButton(
                            onClick = onPickVideo,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("studio_pick_video_button")
                        ) {
                            Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Video")
                        }
                    }
                }

                // AI Suggestions Carousel (when enabled)
                if (settings.aiWallpaperSuggestionsEnabled &&
                    (selectedSection == WallpaperStudioSection.MY_WALLPAPERS || selectedSection == WallpaperStudioSection.AI_WALLPAPERS)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("AI Wallpaper Suggestions", style = MaterialTheme.typography.titleSmall)
                                }
                                TextButton(onClick = {
                                    onUpdateAppearance(null, null, null, null, null, null, false, null)
                                }) {
                                    Text("Hide")
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(suggestions, key = { it.id }) { suggestion ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                        modifier = Modifier
                                            .width(240.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                selectedSection = WallpaperStudioSection.AI_WALLPAPERS
                                                aiPrompt = suggestion.prompt
                                                aiStyle = suggestion.style
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = suggestion.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = suggestion.reason,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = suggestion.prompt,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                TextButton(
                                                    onClick = {
                                                        selectedSection = WallpaperStudioSection.AI_WALLPAPERS
                                                        aiPrompt = suggestion.prompt
                                                        aiStyle = suggestion.style
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Use Prompt")
                                                }
                                                suggestion.linkedLivePreset?.let { linked ->
                                                    val liveEntity = wallpapers.firstOrNull { it.livePresetId == linked.id }
                                                    if (liveEntity != null) {
                                                        TextButton(
                                                            onClick = { onOpenPreview(liveEntity) },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("Live Preview")
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
                }

                // DSK AI WALLPAPER GENERATOR (shown in AI Wallpapers tab)
                if (selectedSection == WallpaperStudioSection.AI_WALLPAPERS) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("DSK AI WALLPAPER", style = MaterialTheme.typography.titleMedium)
                                }

                                OutlinedTextField(
                                    value = aiPrompt,
                                    onValueChange = { aiPrompt = it },
                                    label = { Text("Describe your wallpaper") },
                                    placeholder = { Text("e.g. Liquid chrome waves under neon aurora sky") },
                                    minLines = 2,
                                    maxLines = 4,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_wallpaper_prompt_input")
                                )

                                Text("Style", style = MaterialTheme.typography.labelLarge)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(AiWallpaperStyle.entries) { style ->
                                        FilterChip(
                                            selected = aiStyle == style,
                                            onClick = { aiStyle = style },
                                            label = { Text(style.label) }
                                        )
                                    }
                                }

                                Text("Aspect Ratio & Quality", style = MaterialTheme.typography.labelLarge)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(AiAspectRatio.entries) { ratio ->
                                        FilterChip(
                                            selected = aiAspectRatio == ratio,
                                            onClick = { aiAspectRatio = ratio },
                                            label = { Text(ratio.label) }
                                        )
                                    }
                                    items(AiImageQuality.entries) { q ->
                                        FilterChip(
                                            selected = aiQuality == q,
                                            onClick = { aiQuality = q },
                                            label = { Text(q.label) }
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onGenerateAiWallpaper(aiPrompt, aiStyle, aiAspectRatio, aiQuality) },
                                    enabled = aiGenState !is AiWallpaperGenState.Loading && aiPrompt.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_wallpaper_generate_button")
                                ) {
                                    Text(if (aiGenState is AiWallpaperGenState.Loading) "Generating (30-60s)..." else "Generate AI Wallpaper")
                                }

                                when (aiGenState) {
                                    is AiWallpaperGenState.Loading -> {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                            MaterialTheme.colorScheme.surface
                                                        )
                                                    )
                                                )
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator()
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = "Synthesizing AI artwork via Gemini...",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                    is AiWallpaperGenState.Success -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Image(
                                                bitmap = aiGenState.bitmap.asImageBitmap(),
                                                contentDescription = aiGenState.wallpaper.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(220.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { onApplyBrowserWallpaper(aiGenState.wallpaper) },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Set Browser")
                                                }
                                                OutlinedButton(
                                                    onClick = { onOpenPreview(aiGenState.wallpaper) },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Edit & Apply")
                                                }
                                            }
                                        }
                                    }
                                    is AiWallpaperGenState.Error -> {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.errorContainer
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Text(
                                                    text = aiGenState.message,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                TextButton(onClick = onResetAiState) {
                                                    Text("Dismiss")
                                                }
                                            }
                                        }
                                    }
                                    AiWallpaperGenState.Idle -> Unit
                                }
                            }
                        }
                    }
                }

                // AI LIVE VIDEO GENERATOR UI (Honest configuration status + DSK Live / Video import bridge)
                if (selectedSection == WallpaperStudioSection.AI_LIVE_WALLPAPERS) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI LIVE VIDEO WALLPAPERS", style = MaterialTheme.typography.titleMedium)
                                }
                                OutlinedTextField(
                                    value = aiLivePrompt,
                                    onValueChange = { aiLivePrompt = it },
                                    label = { Text("Animated scene prompt") },
                                    placeholder = { Text("Neon cyber city with falling rain reflections") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(DskLivePreset.entries) { preset ->
                                        FilterChip(
                                            selected = aiLivePreviewPreset == preset,
                                            onClick = {
                                                aiLivePreviewPreset = preset
                                                aiLivePrompt = preset.subtitle
                                            },
                                            label = { Text(preset.title) }
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                ) {
                                    DskLiveCanvasView(
                                        preset = aiLivePreviewPreset,
                                        animate = aiLiveLoop && !settings.lowPowerStaticWallpaperMode,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(selected = aiLiveLoop, onClick = { aiLiveLoop = !aiLiveLoop }, label = { Text("Loop") })
                                    FilterChip(selected = aiLiveMute, onClick = { aiLiveMute = !aiLiveMute }, label = { Text("Mute") })
                                    Spacer(modifier = Modifier.weight(1f))
                                    val linkedEntity = wallpapers.firstOrNull { it.livePresetId == aiLivePreviewPreset.id }
                                    if (linkedEntity != null) {
                                        Button(onClick = { onOpenPreview(linkedEntity) }) {
                                            Text("Apply Scene")
                                        }
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Cloud text-to-video synthesis requires a configured video generation backend (such as Vertex AI Veo). Meanwhile, you can apply our real-time procedural DSK Live scenes above or import any local AI video file.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Wallpaper Cards Grid (2 columns)
                val rows = filteredWallpapers.chunked(2)
                if (rows.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wallpaper,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No wallpapers in ${selectedSection.label} yet",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Pick a photo or video from your phone, or generate a new AI wallpaper.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(rows) { pair ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            pair.forEach { wallpaper ->
                                WallpaperCardItem(
                                    wallpaper = wallpaper,
                                    isBrowserActive = settings.activeBrowserWallpaperId == wallpaper.id,
                                    isNewTabActive = settings.activeNewTabWallpaperId == wallpaper.id,
                                    onOpenPreview = { onOpenPreview(wallpaper) },
                                    onToggleFavorite = { onToggleFavorite(wallpaper) },
                                    onQuickApplyBrowser = { onApplyBrowserWallpaper(wallpaper) },
                                    onQuickSetAndroidLive = { onSetAndroidLiveWallpaper(wallpaper) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperCardItem(
    wallpaper: WallpaperEntity,
    isBrowserActive: Boolean,
    isNewTabActive: Boolean,
    onOpenPreview: () -> Unit,
    onToggleFavorite: () -> Unit,
    onQuickApplyBrowser: () -> Unit,
    onQuickSetAndroidLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (isBrowserActive || isNewTabActive) 2.dp else 1.dp,
                color = if (isBrowserActive || isNewTabActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onOpenPreview)
            .testTag("wallpaper_card_${wallpaper.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            ) {
                WallpaperThumbnail(
                    wallpaper = wallpaper,
                    modifier = Modifier.fillMaxSize()
                )

                // Type badge
                val typeLabel = runCatching { WallpaperMediaType.valueOf(wallpaper.type).label }.getOrDefault(wallpaper.type)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.62f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Favorite button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.48f))
                ) {
                    Icon(
                        imageVector = if (wallpaper.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (wallpaper.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (wallpaper.isFavorite) Color(0xFFFF4D6D) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isBrowserActive || isNewTabActive) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    isBrowserActive && isNewTabActive -> "Browser + Tab"
                                    isBrowserActive -> "Browser"
                                    else -> "New Tab"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = wallpaper.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onQuickApplyBrowser,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Text("Apply", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onOpenPreview,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Text("Edit", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall)
            Text(text = valueText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}

@Composable
private fun StudioSwitchLine(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
