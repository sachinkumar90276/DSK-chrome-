package com.example.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.example.R
import com.example.data.BrowserDatabase
import com.example.data.SettingsDataStore
import com.example.data.WallpaperEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max

object LiveWallpaperUtils {
    fun launchSystemLiveWallpaperChooser(context: Context): Boolean {
        val component = ComponentName(context, DskAndroidWallpaperService::class.java)
        val targeted = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(targeted)
            true
        }.recoverCatching {
            val fallback = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            true
        }.getOrDefault(false)
    }

    fun isDskLiveWallpaperActive(context: Context): Boolean {
        val manager = runCatching { WallpaperManager.getInstance(context) }.getOrNull() ?: return false
        val currentComponent = manager.wallpaperInfo?.component ?: return false
        return currentComponent == ComponentName(context, DskAndroidWallpaperService::class.java)
    }
}

class DskAndroidWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = DskWallpaperEngine()

    inner class DskWallpaperEngine : Engine() {

        private val mainHandler = Handler(Looper.getMainLooper())
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val renderer = DskLiveWallpaperRenderer()
        private var emblemBitmap: Bitmap? = null
        private var customBitmap: Bitmap? = null
        private var customBitmapPath: String = ""
        private var mediaPlayer: MediaPlayer? = null
        private var configObserverJob: Job? = null

        private var activeWallpaper: WallpaperEntity? = null
        private var lowPowerStatic = false
        private var isVisible = false
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var startTimeNanos = System.nanoTime()

        private val frameRunnable = object : Runnable {
            override fun run() {
                drawNextFrame()
                val isAnimatedDsk = activeWallpaper?.type != WallpaperMediaType.VIDEO.name &&
                    activeWallpaper?.type != WallpaperMediaType.PHOTO.name &&
                    activeWallpaper?.type != WallpaperMediaType.AI_IMAGE.name
                if (isVisible && isAnimatedDsk && !lowPowerStatic) {
                    mainHandler.postDelayed(this, 33L)
                }
            }
        }

        private val trimLoopRunnable = object : Runnable {
            override fun run() {
                val mp = mediaPlayer ?: return
                val wp = activeWallpaper ?: return
                if (isVisible && wp.type == WallpaperMediaType.VIDEO.name) {
                    runCatching {
                        if (mp.isPlaying) {
                            val endMs = wp.videoTrimEndMs
                            if (endMs > wp.videoTrimStartMs && mp.currentPosition >= endMs) {
                                mp.seekTo(wp.videoTrimStartMs.toInt())
                            }
                        }
                    }
                    mainHandler.postDelayed(this, 220L)
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            emblemBitmap = runCatching {
                BitmapFactory.decodeResource(resources, R.drawable.img_dsk_brand_emblem)
            }.getOrNull()

            val settingsStore = SettingsDataStore(applicationContext)
            val db = BrowserDatabase.getInstance(applicationContext)
            configObserverJob = scope.launch {
                settingsStore.settingsFlow.collect { settings ->
                    lowPowerStatic = settings.lowPowerStaticWallpaperMode
                    val sourceId = settings.androidLiveWallpaperSourceId.ifBlank {
                        DskLivePreset.DSK_CHROME_FLOW.id
                    }
                    val found = db.wallpaperDao().getById(sourceId)
                    activeWallpaper = found ?: WallpaperEntity(
                        id = DskLivePreset.DSK_CHROME_FLOW.id,
                        title = DskLivePreset.DSK_CHROME_FLOW.title,
                        type = WallpaperMediaType.DSK_LIVE.name,
                        livePresetId = DskLivePreset.DSK_CHROME_FLOW.id
                    )
                    applyModeChange()
                }
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            applyModeChange()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisible = visible
            if (visible) {
                startTimeNanos = System.nanoTime()
                resumePlaybackOrAnimation()
            } else {
                pausePlaybackAndAnimation()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            isVisible = false
            pausePlaybackAndAnimation()
            releaseMediaPlayer()
        }

        override fun onDestroy() {
            pausePlaybackAndAnimation()
            releaseMediaPlayer()
            configObserverJob?.cancel()
            scope.cancel()
            emblemBitmap?.recycle()
            emblemBitmap = null
            customBitmap?.recycle()
            customBitmap = null
            super.onDestroy()
        }

        private fun applyModeChange() {
            val wp = activeWallpaper ?: return
            if (wp.type == WallpaperMediaType.VIDEO.name && wp.sourceUriOrPath.isNotBlank() && !lowPowerStatic) {
                mainHandler.removeCallbacks(frameRunnable)
                setupVideoPlayer(wp)
            } else {
                releaseMediaPlayer()
                if (wp.type == WallpaperMediaType.PHOTO.name || wp.type == WallpaperMediaType.AI_IMAGE.name) {
                    loadCustomImageAsync(wp.sourceUriOrPath)
                }
                if (isVisible) {
                    mainHandler.removeCallbacks(frameRunnable)
                    mainHandler.post(frameRunnable)
                }
            }
        }

        private fun resumePlaybackOrAnimation() {
            val wp = activeWallpaper
            if (wp?.type == WallpaperMediaType.VIDEO.name && !lowPowerStatic) {
                if (mediaPlayer == null) {
                    setupVideoPlayer(wp)
                } else {
                    runCatching {
                        if (!mediaPlayer!!.isPlaying) {
                            mediaPlayer!!.start()
                        }
                    }
                    mainHandler.removeCallbacks(trimLoopRunnable)
                    mainHandler.postDelayed(trimLoopRunnable, 220L)
                }
            } else {
                mainHandler.removeCallbacks(frameRunnable)
                mainHandler.post(frameRunnable)
            }
        }

        private fun pausePlaybackAndAnimation() {
            mainHandler.removeCallbacks(frameRunnable)
            mainHandler.removeCallbacks(trimLoopRunnable)
            runCatching {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
            }
        }

        private fun loadCustomImageAsync(path: String) {
            if (path.isBlank() || path == customBitmapPath) return
            scope.launch(Dispatchers.IO) {
                val decoded = runCatching {
                    val file = File(path)
                    if (file.exists()) {
                        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                        BitmapFactory.decodeFile(file.absolutePath, opts)
                    } else null
                }.getOrNull()
                mainHandler.post {
                    customBitmap?.recycle()
                    customBitmap = decoded
                    customBitmapPath = path
                    if (isVisible) drawNextFrame()
                }
            }
        }

        private fun setupVideoPlayer(wp: WallpaperEntity) {
            val holder = surfaceHolder ?: return
            if (!holder.surface.isValid) return
            releaseMediaPlayer()
            runCatching {
                val mp = MediaPlayer()
                val source = wp.sourceUriOrPath
                if (source.startsWith("content://") || source.startsWith("file://")) {
                    mp.setDataSource(applicationContext, Uri.parse(source))
                } else {
                    mp.setDataSource(source)
                }
                mp.setSurface(holder.surface)
                mp.isLooping = wp.videoLoop
                mp.setVolume(0f, 0f)
                mp.setOnPreparedListener { prepared ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && wp.videoSpeed != 1f) {
                        runCatching {
                            prepared.playbackParams = PlaybackParams().setSpeed(wp.videoSpeed.coerceIn(0.5f, 2.0f))
                        }
                    }
                    if (wp.videoTrimStartMs > 0L) {
                        runCatching { prepared.seekTo(wp.videoTrimStartMs.toInt()) }
                    }
                    if (isVisible) {
                        prepared.start()
                        mainHandler.removeCallbacks(trimLoopRunnable)
                        mainHandler.postDelayed(trimLoopRunnable, 220L)
                    }
                }
                mp.setOnErrorListener { _, _, _ ->
                    // Fall back safely to DSK Chrome Flow canvas if video URI becomes inaccessible
                    releaseMediaPlayer()
                    activeWallpaper = WallpaperEntity(
                        id = DskLivePreset.DSK_CHROME_FLOW.id,
                        title = DskLivePreset.DSK_CHROME_FLOW.title,
                        type = WallpaperMediaType.DSK_LIVE.name,
                        livePresetId = DskLivePreset.DSK_CHROME_FLOW.id
                    )
                    if (isVisible) mainHandler.post(frameRunnable)
                    true
                }
                mp.prepareAsync()
                mediaPlayer = mp
            }.onFailure {
                releaseMediaPlayer()
            }
        }

        private fun releaseMediaPlayer() {
            mainHandler.removeCallbacks(trimLoopRunnable)
            mediaPlayer?.let { mp ->
                runCatching { mp.stop() }
                runCatching { mp.release() }
            }
            mediaPlayer = null
        }

        private fun drawNextFrame() {
            if (surfaceWidth <= 0 || surfaceHeight <= 0) return
            val holder = surfaceHolder ?: return
            val canvas = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }
            }.getOrNull() ?: return

            try {
                val wp = activeWallpaper
                if (wp != null && (wp.type == WallpaperMediaType.PHOTO.name || wp.type == WallpaperMediaType.AI_IMAGE.name)) {
                    val bmp = customBitmap
                    canvas.drawColor(Color.BLACK)
                    if (bmp != null && !bmp.isRecycled) {
                        val scale = max(surfaceWidth.toFloat() / bmp.width, surfaceHeight.toFloat() / bmp.height) * wp.zoomScale
                        val drawW = bmp.width * scale
                        val drawH = bmp.height * scale
                        val left = (surfaceWidth - drawW) * 0.5f + wp.panX * surfaceWidth * 0.35f
                        val top = (surfaceHeight - drawH) * 0.5f + wp.panY * surfaceHeight * 0.35f
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                            colorFilter = DskLiveWallpaperRenderer.buildColorMatrixFilter(wp.brightness, wp.contrast)
                            alpha = (wp.opacity.coerceIn(0.2f, 1f) * 255).toInt()
                        }
                        canvas.drawBitmap(bmp, null, RectF(left, top, left + drawW, top + drawH), paint)
                    } else {
                        renderer.draw(
                            canvas = canvas,
                            width = surfaceWidth,
                            height = surfaceHeight,
                            preset = DskLivePreset.DSK_CHROME_FLOW,
                            timeSeconds = 2.0f,
                            emblemBitmap = emblemBitmap,
                            staticFallback = true
                        )
                    }
                } else {
                    val elapsed = (System.nanoTime() - startTimeNanos) / 1_000_000_000f
                    val preset = DskLivePreset.fromIdOrNull(wp?.livePresetId.orEmpty())
                        ?: DskLivePreset.DSK_CHROME_FLOW
                    renderer.draw(
                        canvas = canvas,
                        width = surfaceWidth,
                        height = surfaceHeight,
                        preset = preset,
                        timeSeconds = elapsed,
                        emblemBitmap = emblemBitmap,
                        staticFallback = lowPowerStatic,
                        brightness = wp?.brightness ?: 1f,
                        contrast = wp?.contrast ?: 1f
                    )
                }
            } finally {
                runCatching { holder.unlockCanvasAndPost(canvas) }
            }
        }
    }
}
