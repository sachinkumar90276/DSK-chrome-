package com.example.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.R
import com.example.data.BrowserSettings
import com.example.data.WallpaperEntity
import com.example.data.WallpaperPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun BrowserWallpaperBackdrop(
    wallpaper: WallpaperEntity?,
    settings: BrowserSettings,
    isIncognito: Boolean,
    useDarkUi: Boolean,
    overrideOpacity: Float? = null,
    overrideBlurDp: Float? = null,
    overrideBrightness: Float? = null,
    modifier: Modifier = Modifier
) {
    if (isIncognito) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF151022), Color(0xFF0B0912))
                    )
                )
        )
        return
    }

    val effectiveOpacity = (overrideOpacity ?: ((wallpaper?.opacity ?: 1f) * settings.wallpaperOpacity))
        .coerceIn(0.15f, 1f)
    val effectiveBlur = (overrideBlurDp ?: (wallpaper?.blurRadiusDp ?: 0f).coerceAtLeast(settings.wallpaperBlurDp))
        .coerceIn(0f, 24f)
    val effectiveBrightness = (overrideBrightness ?: ((wallpaper?.brightness ?: 1f) * settings.wallpaperBrightness))
        .coerceIn(0.3f, 1.5f)
    val effectiveContrast = (wallpaper?.contrast ?: 1f).coerceIn(0.5f, 1.8f)

    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && effectiveBlur > 0.5f) {
        Modifier.blur(effectiveBlur.dp)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (useDarkUi) Color(0xFF050811) else Color(0xFFEEF3FB))
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(effectiveOpacity)
                .then(blurModifier)
        ) {
            if (wallpaper != null) {
                WallpaperMediaSurface(
                    wallpaper = wallpaper,
                    brightness = effectiveBrightness,
                    contrast = effectiveContrast,
                    lowPowerStatic = settings.lowPowerStaticWallpaperMode,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LegacyPresetBackdrop(
                    settings = settings,
                    useDarkUi = useDarkUi,
                    brightness = effectiveBrightness,
                    contrast = effectiveContrast,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Readability overlay (stronger if blur fallback on pre-Android 12 or lower brightness)
        val fallbackBlurScrim = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && effectiveBlur > 0.5f) {
            (effectiveBlur / 24f) * 0.25f
        } else 0f
        val baseScrimAlpha = if (useDarkUi) 0.24f else 0.16f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    (if (useDarkUi) Color(0xFF050811) else Color(0xFFF6F9FF))
                        .copy(alpha = (baseScrimAlpha + fallbackBlurScrim).coerceIn(0f, 0.72f))
                )
        )
    }
}

@Composable
fun WallpaperMediaSurface(
    wallpaper: WallpaperEntity,
    brightness: Float = wallpaper.brightness,
    contrast: Float = wallpaper.contrast,
    lowPowerStatic: Boolean = false,
    animateLive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val contentScale = remember(wallpaper.fitMode) {
        when (runCatching { WallpaperFitMode.valueOf(wallpaper.fitMode) }.getOrDefault(WallpaperFitMode.FILL)) {
            WallpaperFitMode.FILL -> ContentScale.Crop
            WallpaperFitMode.FIT -> ContentScale.Fit
            WallpaperFitMode.CENTER -> ContentScale.Inside
            WallpaperFitMode.STRETCH -> ContentScale.FillBounds
        }
    }
    val colorFilter = remember(brightness, contrast) {
        buildComposeColorFilter(brightness, contrast)
    }
    val transformModifier = Modifier.graphicsLayer {
        scaleX = wallpaper.zoomScale.coerceIn(0.5f, 3f)
        scaleY = wallpaper.zoomScale.coerceIn(0.5f, 3f)
        translationX = wallpaper.panX * size.width * 0.35f
        translationY = wallpaper.panY * size.height * 0.35f
    }

    when (runCatching { WallpaperMediaType.valueOf(wallpaper.type) }.getOrDefault(WallpaperMediaType.DSK_LIVE)) {
        WallpaperMediaType.DSK_LIVE -> {
            val preset = DskLivePreset.fromIdOrNull(wallpaper.livePresetId) ?: DskLivePreset.DSK_CHROME_FLOW
            DskLiveCanvasView(
                preset = preset,
                animate = animateLive && !lowPowerStatic,
                brightness = brightness,
                contrast = contrast,
                modifier = modifier.then(transformModifier)
            )
        }

        WallpaperMediaType.PHOTO,
        WallpaperMediaType.AI_IMAGE -> {
            val file = remember(wallpaper.sourceUriOrPath) { File(wallpaper.sourceUriOrPath) }
            if (wallpaper.sourceUriOrPath.isNotBlank() && file.exists()) {
                AsyncImage(
                    model = file,
                    contentDescription = wallpaper.title,
                    contentScale = contentScale,
                    colorFilter = colorFilter,
                    modifier = modifier
                        .fillMaxSize()
                        .then(transformModifier)
                )
            } else if (wallpaper.sourceUriOrPath.startsWith("content://")) {
                AsyncImage(
                    model = Uri.parse(wallpaper.sourceUriOrPath),
                    contentDescription = wallpaper.title,
                    contentScale = contentScale,
                    colorFilter = colorFilter,
                    modifier = modifier
                        .fillMaxSize()
                        .then(transformModifier)
                )
            } else {
                DskLiveCanvasView(
                    preset = DskLivePreset.DSK_CHROME_FLOW,
                    animate = false,
                    brightness = brightness,
                    contrast = contrast,
                    modifier = modifier
                )
            }
        }

        WallpaperMediaType.VIDEO,
        WallpaperMediaType.AI_LIVE -> {
            if (wallpaper.sourceUriOrPath.isNotBlank()) {
                VideoWallpaperSurface(
                    wallpaper = wallpaper,
                    lowPowerStatic = lowPowerStatic || !animateLive,
                    modifier = modifier.fillMaxSize()
                )
            } else {
                DskLiveCanvasView(
                    preset = DskLivePreset.DSK_NEON_CYBER,
                    animate = animateLive && !lowPowerStatic,
                    brightness = brightness,
                    contrast = contrast,
                    modifier = modifier
                )
            }
        }

        WallpaperMediaType.STATIC_PRESET -> {
            Image(
                painter = painterResource(id = R.drawable.img_wallpaper_aurora),
                contentDescription = wallpaper.title,
                contentScale = contentScale,
                colorFilter = colorFilter,
                modifier = modifier
                    .fillMaxSize()
                    .then(transformModifier)
            )
        }
    }
}

@Composable
private fun LegacyPresetBackdrop(
    settings: BrowserSettings,
    useDarkUi: Boolean,
    brightness: Float,
    contrast: Float,
    modifier: Modifier = Modifier
) {
    val colorFilter = remember(brightness, contrast) {
        buildComposeColorFilter(brightness, contrast)
    }
    when (settings.wallpaperPreset) {
        WallpaperPreset.AURORA -> {
            Image(
                painter = painterResource(id = R.drawable.img_wallpaper_aurora),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = modifier.fillMaxSize()
            )
        }
        WallpaperPreset.PRISM -> {
            Image(
                painter = painterResource(id = R.drawable.img_wallpaper_prism),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = modifier.fillMaxSize()
            )
        }
        WallpaperPreset.CUSTOM -> {
            val customFile = remember(settings.customWallpaperPath) { File(settings.customWallpaperPath) }
            if (settings.customWallpaperPath.isNotBlank() && customFile.exists()) {
                AsyncImage(
                    model = customFile,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = modifier.fillMaxSize()
                )
            } else {
                Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            }
        }
        WallpaperPreset.OBSIDIAN -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (useDarkUi) {
                                listOf(Color(0xFF0A1428), Color(0xFF070B14))
                            } else {
                                listOf(Color(0xFFEAF1FD), Color(0xFFF7F9FD))
                            }
                        )
                    )
            )
        }
    }
}

@Composable
fun DskLiveCanvasView(
    preset: DskLivePreset,
    animate: Boolean = true,
    brightness: Float = 1f,
    contrast: Float = 1f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val renderer = remember { DskLiveWallpaperRenderer() }
    var isLifecycleActive by remember { mutableStateOf(true) }
    var timeSeconds by remember { mutableFloatStateOf(2.2f) }

    val emblemBitmap = remember(preset) {
        if (preset == DskLivePreset.DSK_CHROME_SIGNATURE) {
            runCatching {
                BitmapFactory.decodeResource(context.resources, R.drawable.img_dsk_brand_emblem)
            }.getOrNull()
        } else null
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isLifecycleActive = true
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> isLifecycleActive = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(animate, isLifecycleActive, preset) {
        if (!animate || !isLifecycleActive) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val delta = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    timeSeconds += delta
                }
                lastNanos = frameNanos
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        renderer.draw(
            canvas = drawContext.canvas.nativeCanvas,
            width = size.width.toInt(),
            height = size.height.toInt(),
            preset = preset,
            timeSeconds = timeSeconds,
            emblemBitmap = emblemBitmap,
            staticFallback = !animate,
            brightness = brightness,
            contrast = contrast
        )
    }
}

@Composable
fun VideoWallpaperSurface(
    wallpaper: WallpaperEntity,
    lowPowerStatic: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(true) }
    var staticFrame by remember { mutableStateOf<Bitmap?>(null) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    val mediaPlayerRef = remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isResumed = true
                    runCatching {
                        mediaPlayerRef.value?.let { mp -> if (!mp.isPlaying) mp.start() }
                    }
                }
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    isResumed = false
                    runCatching {
                        mediaPlayerRef.value?.let { mp -> if (mp.isPlaying) mp.pause() }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Load a frozen thumbnail frame for lowPowerStatic mode
    LaunchedEffect(wallpaper.sourceUriOrPath, lowPowerStatic, wallpaper.videoTrimStartMs) {
        if (lowPowerStatic && wallpaper.sourceUriOrPath.isNotBlank()) {
            staticFrame = withContext(Dispatchers.IO) {
                runCatching {
                    val retriever = MediaMetadataRetriever()
                    try {
                        val src = wallpaper.sourceUriOrPath
                        if (src.startsWith("content://") || src.startsWith("file://")) {
                            retriever.setDataSource(context, Uri.parse(src))
                        } else {
                            retriever.setDataSource(src)
                        }
                        retriever.getFrameAtTime(wallpaper.videoTrimStartMs * 1000L)
                    } finally {
                        retriever.release()
                    }
                }.getOrNull()
            }
        } else {
            staticFrame = null
        }
    }

    if (lowPowerStatic) {
        val frame = staticFrame
        if (frame != null) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = wallpaper.zoomScale
                        scaleY = wallpaper.zoomScale
                        translationX = wallpaper.panX * size.width * 0.35f
                        translationY = wallpaper.panY * size.height * 0.35f
                    }
            )
        } else {
            DskLiveCanvasView(
                preset = DskLivePreset.DSK_CHROME_FLOW,
                animate = false,
                modifier = modifier
            )
        }
        return
    }

    // Apply live parameter updates (volume, speed, loop, trim)
    LaunchedEffect(
        wallpaper.videoMuted,
        wallpaper.videoLoop,
        wallpaper.videoSpeed,
        wallpaper.videoTrimStartMs,
        wallpaper.videoTrimEndMs
    ) {
        val mp = mediaPlayerRef.value
        if (mp != null) {
            runCatching {
                val vol = if (wallpaper.videoMuted) 0f else 1f
                mp.setVolume(vol, vol)
                mp.isLooping = wallpaper.videoLoop
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mp.playbackParams = PlaybackParams().setSpeed(wallpaper.videoSpeed.coerceIn(0.5f, 2.0f))
                }
            }
        }
        while (true) {
            delay(220L)
            val player = mediaPlayerRef.value ?: continue
            runCatching {
                if (player.isPlaying) {
                    val end = wallpaper.videoTrimEndMs
                    if (end > wallpaper.videoTrimStartMs && player.currentPosition >= end) {
                        player.seekTo(wallpaper.videoTrimStartMs.toInt())
                    }
                }
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        val mp = MediaPlayer()
                        mediaPlayerRef.value = mp
                        runCatching {
                            val src = wallpaper.sourceUriOrPath
                            if (src.startsWith("content://") || src.startsWith("file://")) {
                                mp.setDataSource(ctx, Uri.parse(src))
                            } else {
                                mp.setDataSource(src)
                            }
                            mp.setSurface(Surface(surfaceTexture))
                            mp.isLooping = wallpaper.videoLoop
                            val vol = if (wallpaper.videoMuted) 0f else 1f
                            mp.setVolume(vol, vol)
                            mp.setOnVideoSizeChangedListener { _, vw, vh ->
                                videoWidth = vw
                                videoHeight = vh
                                applyTextureTransform(
                                    textureView = this@apply,
                                    viewW = width,
                                    viewH = height,
                                    videoW = vw,
                                    videoH = vh,
                                    fitMode = wallpaper.fitMode,
                                    zoom = wallpaper.zoomScale,
                                    panX = wallpaper.panX,
                                    panY = wallpaper.panY
                                )
                            }
                            mp.setOnPreparedListener { prepared ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && wallpaper.videoSpeed != 1f) {
                                    runCatching {
                                        prepared.playbackParams = PlaybackParams()
                                            .setSpeed(wallpaper.videoSpeed.coerceIn(0.5f, 2.0f))
                                    }
                                }
                                if (wallpaper.videoTrimStartMs > 0L) {
                                    runCatching { prepared.seekTo(wallpaper.videoTrimStartMs.toInt()) }
                                }
                                if (isResumed) prepared.start()
                            }
                            mp.setOnCompletionListener { finished ->
                                if (wallpaper.videoLoop) {
                                    runCatching {
                                        finished.seekTo(wallpaper.videoTrimStartMs.toInt())
                                        finished.start()
                                    }
                                }
                            }
                            mp.prepareAsync()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        applyTextureTransform(
                            textureView = this@apply,
                            viewW = width,
                            viewH = height,
                            videoW = videoWidth,
                            videoH = videoHeight,
                            fitMode = wallpaper.fitMode,
                            zoom = wallpaper.zoomScale,
                            panX = wallpaper.panX,
                            panY = wallpaper.panY
                        )
                    }

                    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                        mediaPlayerRef.value?.let { mp ->
                            runCatching { mp.stop() }
                            runCatching { mp.release() }
                        }
                        mediaPlayerRef.value = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
                }
            }
        },
        update = { tv ->
            applyTextureTransform(
                textureView = tv,
                viewW = tv.width,
                viewH = tv.height,
                videoW = videoWidth,
                videoH = videoHeight,
                fitMode = wallpaper.fitMode,
                zoom = wallpaper.zoomScale,
                panX = wallpaper.panX,
                panY = wallpaper.panY
            )
        },
        modifier = modifier
    )
}

private fun applyTextureTransform(
    textureView: TextureView,
    viewW: Int,
    viewH: Int,
    videoW: Int,
    videoH: Int,
    fitMode: String,
    zoom: Float,
    panX: Float,
    panY: Float
) {
    if (viewW <= 0 || viewH <= 0 || videoW <= 0 || videoH <= 0) return
    val mode = runCatching { WallpaperFitMode.valueOf(fitMode) }.getOrDefault(WallpaperFitMode.FILL)
    val matrix = Matrix()
    val viewRatio = viewW.toFloat() / viewH.toFloat()
    val videoRatio = videoW.toFloat() / videoH.toFloat()
    var sx = 1f
    var sy = 1f

    when (mode) {
        WallpaperFitMode.FILL -> {
            if (videoRatio > viewRatio) {
                sx = videoRatio / viewRatio
            } else {
                sy = viewRatio / videoRatio
            }
        }
        WallpaperFitMode.FIT,
        WallpaperFitMode.CENTER -> {
            if (videoRatio > viewRatio) {
                sy = viewRatio / videoRatio
            } else {
                sx = videoRatio / viewRatio
            }
        }
        WallpaperFitMode.STRETCH -> {
            sx = 1f
            sy = 1f
        }
    }

    sx *= zoom.coerceIn(0.5f, 3f)
    sy *= zoom.coerceIn(0.5f, 3f)
    matrix.setScale(sx, sy, viewW / 2f, viewH / 2f)
    matrix.postTranslate(panX * viewW * 0.35f, panY * viewH * 0.35f)
    textureView.setTransform(matrix)
}

private fun buildComposeColorFilter(brightness: Float, contrast: Float): ColorFilter {
    val b = (brightness - 1f) * 255f
    val c = contrast
    val translate = (-0.5f * c + 0.5f) * 255f + b
    val matrix = ColorMatrix(
        floatArrayOf(
            c, 0f, 0f, 0f, translate,
            0f, c, 0f, 0f, translate,
            0f, 0f, c, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
    )
    return ColorFilter.colorMatrix(matrix)
}

@Composable
fun WallpaperThumbnail(
    wallpaper: WallpaperEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    when (runCatching { WallpaperMediaType.valueOf(wallpaper.type) }.getOrDefault(WallpaperMediaType.DSK_LIVE)) {
        WallpaperMediaType.VIDEO -> {
            var thumb by remember(wallpaper.sourceUriOrPath) { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(wallpaper.sourceUriOrPath) {
                thumb = withContext(Dispatchers.IO) {
                    runCatching {
                        val retriever = MediaMetadataRetriever()
                        try {
                            val src = wallpaper.sourceUriOrPath
                            if (src.startsWith("content://") || src.startsWith("file://")) {
                                retriever.setDataSource(context, Uri.parse(src))
                            } else {
                                retriever.setDataSource(src)
                            }
                            retriever.getFrameAtTime(wallpaper.videoTrimStartMs * 1000L)
                        } finally {
                            retriever.release()
                        }
                    }.getOrNull()
                }
            }
            if (thumb != null) {
                Image(
                    bitmap = thumb!!.asImageBitmap(),
                    contentDescription = wallpaper.title,
                    contentScale = ContentScale.Crop,
                    modifier = modifier.fillMaxSize()
                )
            } else {
                DskLiveCanvasView(
                    preset = DskLivePreset.DSK_CHROME_FLOW,
                    animate = false,
                    modifier = modifier
                )
            }
        }
        WallpaperMediaType.DSK_LIVE -> {
            val preset = DskLivePreset.fromIdOrNull(wallpaper.livePresetId) ?: DskLivePreset.DSK_CHROME_FLOW
            DskLiveCanvasView(
                preset = preset,
                animate = false,
                modifier = modifier
            )
        }
        else -> {
            WallpaperMediaSurface(
                wallpaper = wallpaper,
                animateLive = false,
                modifier = modifier
            )
        }
    }
}
