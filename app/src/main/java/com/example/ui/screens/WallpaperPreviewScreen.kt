package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BrowserSettings
import com.example.data.WallpaperEntity
import com.example.wallpaper.BrowserWallpaperBackdrop
import com.example.wallpaper.WallpaperFitMode
import com.example.wallpaper.WallpaperMediaType
import kotlin.math.roundToInt

private enum class EditorTab(val label: String) {
    CROP_FIT("Crop & Fit"),
    ADJUST("Light & Blur"),
    VIDEO("Video"),
    BROWSER_UI("Browser UI")
}

@Composable
fun WallpaperPreviewScreen(
    wallpaper: WallpaperEntity,
    settings: BrowserSettings,
    isSystemLiveWallpaperActive: Boolean,
    onBack: () -> Unit,
    onSaveEdits: (WallpaperEntity) -> Unit,
    onSetBrowserWallpaper: (WallpaperEntity) -> Unit,
    onSetNewTabWallpaper: (WallpaperEntity) -> Unit,
    onSetAndroidLiveWallpaper: (WallpaperEntity) -> Unit,
    onToggleFavorite: (WallpaperEntity) -> Unit,
    onDeleteWallpaper: (WallpaperEntity) -> Unit,
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

    var draft by remember(wallpaper.id) { mutableStateOf(wallpaper) }
    var isLandscapeFrame by remember { mutableStateOf(false) }
    var showSimulatedBrowserUi by remember { mutableStateOf(true) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val isVideo = draft.type == WallpaperMediaType.VIDEO.name
    val isBuiltInLive = draft.type == WallpaperMediaType.DSK_LIVE.name
    val editorTabs = remember(isVideo) {
        if (isVideo) {
            listOf(EditorTab.CROP_FIT, EditorTab.ADJUST, EditorTab.VIDEO, EditorTab.BROWSER_UI)
        } else {
            listOf(EditorTab.CROP_FIT, EditorTab.ADJUST, EditorTab.BROWSER_UI)
        }
    }
    val activeTab = editorTabs.getOrElse(selectedTabIndex) { EditorTab.CROP_FIT }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060911))
    ) {
        // Full Preview Viewport (with portrait/landscape orientation preview mode + pan drag gesture)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val w = size.width.coerceAtLeast(1)
                        val h = size.height.coerceAtLeast(1)
                        draft = draft.copy(
                            panX = (draft.panX + dragAmount.x / w).coerceIn(-1f, 1f),
                            panY = (draft.panY + dragAmount.y / h).coerceIn(-1f, 1f)
                        )
                    }
                }
        ) {
            val frameModifier = if (isLandscapeFrame) {
                Modifier
                    .fillMaxWidth(0.94f)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(22.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
            } else {
                Modifier.fillMaxSize()
            }

            Box(modifier = frameModifier) {
                BrowserWallpaperBackdrop(
                    wallpaper = draft,
                    settings = settings,
                    isIncognito = false,
                    useDarkUi = true,
                    overrideOpacity = (draft.opacity * settings.wallpaperOpacity).coerceIn(0.15f, 1f),
                    overrideBlurDp = maxOf(draft.blurRadiusDp, settings.wallpaperBlurDp),
                    overrideBrightness = (draft.brightness * settings.wallpaperBrightness).coerceIn(0.3f, 1.5f),
                    modifier = Modifier.fillMaxSize()
                )

                // Live Preview Mock Browser Search Bar & Glass Card so user can preview transparency
                if (showSimulatedBrowserUi) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = settings.uiTransparency),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = if (isLandscapeFrame) 18.dp else 94.dp, start = 20.dp, end = 20.dp)
                            .fillMaxWidth()
                            .then(
                                if (settings.glassEffectEnabled) {
                                    Modifier.border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                                } else Modifier
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "DSK CHROME Search or URL",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Top Translucent Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("wallpaper_preview_back")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = draft.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isLandscapeFrame) "Landscape Preview (Drag to pan)" else "Portrait Preview (Drag to pan)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { isLandscapeFrame = !isLandscapeFrame },
                    modifier = Modifier.testTag("wallpaper_preview_rotate")
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = "Toggle portrait/landscape preview",
                        tint = if (isLandscapeFrame) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = { showSimulatedBrowserUi = !showSimulatedBrowserUi },
                    modifier = Modifier.testTag("wallpaper_preview_ui_overlay")
                ) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = "Toggle browser UI preview overlay",
                        tint = if (showSimulatedBrowserUi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = {
                        draft = draft.copy(isFavorite = !draft.isFavorite)
                        onToggleFavorite(draft)
                    },
                    modifier = Modifier.testTag("wallpaper_preview_favorite")
                ) {
                    Icon(
                        imageVector = if (draft.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (draft.isFavorite) Color(0xFFFF4D6D) else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (!isBuiltInLive) {
                    IconButton(
                        onClick = { onDeleteWallpaper(draft) },
                        modifier = Modifier.testTag("wallpaper_preview_delete")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete wallpaper",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Bottom Editor & Apply Panel
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Editor Section Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(editorTabs.size) { index ->
                        val tab = editorTabs[index]
                        FilterChip(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            label = { Text(tab.label) }
                        )
                    }
                }

                // Controls Body (scrollable compact area)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (activeTab) {
                        EditorTab.CROP_FIT -> {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(WallpaperFitMode.entries) { mode ->
                                    FilterChip(
                                        selected = draft.fitMode == mode.name,
                                        onClick = { draft = draft.copy(fitMode = mode.name) },
                                        label = { Text(mode.label) }
                                    )
                                }
                            }
                            PreviewSlider("Zoom", draft.zoomScale, 0.5f..3f, "${(draft.zoomScale * 100).roundToInt()}%") {
                                draft = draft.copy(zoomScale = it)
                            }
                            PreviewSlider("Pan X", draft.panX, -1f..1f, "${(draft.panX * 100).roundToInt()}%") {
                                draft = draft.copy(panX = it)
                            }
                            PreviewSlider("Pan Y", draft.panY, -1f..1f, "${(draft.panY * 100).roundToInt()}%") {
                                draft = draft.copy(panY = it)
                            }
                            TextButton(onClick = {
                                draft = draft.copy(zoomScale = 1f, panX = 0f, panY = 0f, fitMode = WallpaperFitMode.FILL.name)
                            }) {
                                Text("Reset Crop & Position")
                            }
                        }

                        EditorTab.ADJUST -> {
                            PreviewSlider("Brightness", draft.brightness, 0.3f..1.6f, "${(draft.brightness * 100).roundToInt()}%") {
                                draft = draft.copy(brightness = it)
                            }
                            PreviewSlider("Contrast", draft.contrast, 0.5f..1.8f, "${(draft.contrast * 100).roundToInt()}%") {
                                draft = draft.copy(contrast = it)
                            }
                            PreviewSlider("Blur", draft.blurRadiusDp, 0f..24f, "${draft.blurRadiusDp.roundToInt()} dp") {
                                draft = draft.copy(blurRadiusDp = it)
                            }
                            PreviewSlider("Opacity", draft.opacity, 0.2f..1f, "${(draft.opacity * 100).roundToInt()}%") {
                                draft = draft.copy(opacity = it)
                            }
                        }

                        EditorTab.VIDEO -> {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Loop Playback", style = MaterialTheme.typography.bodySmall)
                                Switch(
                                    checked = draft.videoLoop,
                                    onCheckedChange = { draft = draft.copy(videoLoop = it) }
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Mute Audio", style = MaterialTheme.typography.bodySmall)
                                Switch(
                                    checked = draft.videoMuted,
                                    onCheckedChange = { draft = draft.copy(videoMuted = it) }
                                )
                            }
                            PreviewSlider(
                                label = "Playback Speed",
                                value = draft.videoSpeed,
                                range = 0.5f..2.0f,
                                valueText = "${((draft.videoSpeed * 10).roundToInt() / 10f)}x"
                            ) {
                                draft = draft.copy(videoSpeed = it)
                            }
                            PreviewSlider(
                                label = "Trim Start (sec)",
                                value = draft.videoTrimStartMs / 1000f,
                                range = 0f..30f,
                                valueText = "${(draft.videoTrimStartMs / 1000f).roundToInt()}s"
                            ) { sec ->
                                val startMs = (sec * 1000L).toLong()
                                val endMs = if (draft.videoTrimEndMs in 1..startMs) startMs + 2000L else draft.videoTrimEndMs
                                draft = draft.copy(videoTrimStartMs = startMs, videoTrimEndMs = endMs)
                            }
                            PreviewSlider(
                                label = "Trim End (0 = full)",
                                value = draft.videoTrimEndMs / 1000f,
                                range = 0f..60f,
                                valueText = if (draft.videoTrimEndMs == 0L) "Full" else "${(draft.videoTrimEndMs / 1000f).roundToInt()}s"
                            ) { sec ->
                                val endMs = (sec * 1000L).toLong()
                                val validEnd = if (endMs == 0L || endMs > draft.videoTrimStartMs) endMs else draft.videoTrimStartMs + 1000L
                                draft = draft.copy(videoTrimEndMs = validEnd)
                            }
                        }

                        EditorTab.BROWSER_UI -> {
                            PreviewSlider(
                                label = "UI Panel Opacity",
                                value = settings.uiTransparency,
                                range = 0.35f..1f,
                                valueText = "${(settings.uiTransparency * 100).roundToInt()}%"
                            ) {
                                onUpdateAppearance(null, null, null, it, null, null, null, null)
                            }
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Glassmorphism Effect", style = MaterialTheme.typography.bodySmall)
                                Switch(
                                    checked = settings.glassEffectEnabled,
                                    onCheckedChange = { onUpdateAppearance(null, null, null, null, it, null, null, null) }
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Transparent WebView Mode", style = MaterialTheme.typography.bodySmall)
                                Switch(
                                    checked = settings.transparentWebViewEnabled,
                                    onCheckedChange = { onUpdateAppearance(null, null, null, null, null, it, null, null) }
                                )
                            }
                        }
                    }
                }

                // Primary Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onSetBrowserWallpaper(draft) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("set_browser_wallpaper_button")
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SET BROWSER WALLPAPER", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onSetNewTabWallpaper(draft) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("set_new_tab_wallpaper_button")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SET NEW TAB WALLPAPER", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { onSetAndroidLiveWallpaper(draft) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("set_android_live_wallpaper_button")
                    ) {
                        Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSystemLiveWallpaperActive) "UPDATE ANDROID LIVE WALLPAPER" else "SET ANDROID LIVE WALLPAPER",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                    OutlinedButton(
                        onClick = { onSaveEdits(draft) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("save_wallpaper_edits_button")
                    ) {
                        Text("Save", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewSlider(
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
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = valueText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}
