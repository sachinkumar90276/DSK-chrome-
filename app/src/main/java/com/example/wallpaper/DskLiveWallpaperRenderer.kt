package com.example.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class DskLiveWallpaperRenderer {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    private val wavePath = Path()
    private val emblemPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val matrix = Matrix()
    private val adjustmentPaint = Paint()

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        preset: DskLivePreset,
        timeSeconds: Float,
        emblemBitmap: Bitmap? = null,
        staticFallback: Boolean = false,
        brightness: Float = 1f,
        contrast: Float = 1f
    ) {
        if (width <= 0 || height <= 0) return
        val t = if (staticFallback) 2.2f else timeSeconds

        when (preset) {
            DskLivePreset.DSK_CHROME_FLOW -> drawChromeFlow(canvas, width.toFloat(), height.toFloat(), t)
            DskLivePreset.DSK_NEON_CYBER -> drawNeonCyber(canvas, width.toFloat(), height.toFloat(), t)
            DskLivePreset.DSK_GALAXY_MOTION -> drawGalaxyMotion(canvas, width.toFloat(), height.toFloat(), t)
            DskLivePreset.DSK_RAINY_CITY -> drawRainyCity(canvas, width.toFloat(), height.toFloat(), t)
            DskLivePreset.DSK_CHROME_SIGNATURE -> drawChromeSignature(canvas, width.toFloat(), height.toFloat(), t, emblemBitmap)
        }

        // Apply subtle brightness adjustment layer if modified
        if (brightness < 0.99f) {
            val dimAlpha = ((1f - brightness).coerceIn(0f, 0.75f) * 255).toInt()
            adjustmentPaint.color = Color.argb(dimAlpha, 0, 0, 0)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), adjustmentPaint)
        } else if (brightness > 1.01f) {
            val liftAlpha = (((brightness - 1f) * 0.45f).coerceIn(0f, 0.35f) * 255).toInt()
            adjustmentPaint.color = Color.argb(liftAlpha, 220, 240, 255)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), adjustmentPaint)
        }
        if (contrast > 1.05f) {
            val edgeAlpha = (((contrast - 1f) * 0.28f).coerceIn(0f, 0.3f) * 255).toInt()
            bgPaint.shader = RadialGradient(
                width * 0.5f,
                height * 0.5f,
                maxOf(width, height) * 0.72f,
                Color.TRANSPARENT,
                Color.argb(edgeAlpha, 0, 0, 0),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            bgPaint.shader = null
        }
    }

    private fun drawChromeFlow(canvas: Canvas, w: Float, h: Float, t: Float) {
        bgPaint.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(0xFF040812.toInt(), 0xFF0B162C.toInt(), 0xFF050A14.toInt()),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        bgPaint.shader = null

        // Moving ambient specular glow
        val glowX = w * (0.5f + 0.32f * cos(t * 0.45f))
        val glowY = h * (0.35f + 0.22f * sin(t * 0.38f))
        glowPaint.shader = RadialGradient(
            glowX, glowY, min(w, h) * 0.78f,
            intArrayOf(0x6600D4FF, 0x262979FF, 0x00000000),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, glowPaint)
        glowPaint.shader = null

        // Layered liquid metallic chrome waves
        for (layer in 0 until 4) {
            val phase = t * (0.6f + layer * 0.14f) + layer * 1.3f
            val baseY = h * (0.28f + layer * 0.16f)
            val amp = h * (0.06f + layer * 0.014f)

            wavePath.reset()
            wavePath.moveTo(0f, h)
            wavePath.lineTo(0f, baseY)
            val steps = 20
            for (i in 0..steps) {
                val frac = i / steps.toFloat()
                val x = frac * w
                val y = baseY +
                    sin(frac * 2f * PI.toFloat() + phase) * amp +
                    cos(frac * 3.5f * PI.toFloat() - phase * 0.7f) * (amp * 0.45f)
                wavePath.lineTo(x, y)
            }
            wavePath.lineTo(w, h)
            wavePath.close()

            val highlightX = w * ((t * 0.22f + layer * 0.2f) % 1.4f - 0.2f)
            ribbonPaint.shader = LinearGradient(
                highlightX, baseY - amp, highlightX + w * 0.65f, baseY + h * 0.25f,
                intArrayOf(
                    0x220A162C,
                    if (layer % 2 == 0) 0x88BFD8FF.toInt() else 0x8800D4FF.toInt(),
                    0xAAEEF5FF.toInt(),
                    0x441B3B7A,
                    0x22050B16
                ),
                floatArrayOf(0f, 0.28f, 0.5f, 0.74f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(wavePath, ribbonPaint)

            // Crisp metallic rim highlight along the wave crest
            strokePaint.strokeWidth = (2.4f - layer * 0.35f).coerceAtLeast(1f)
            strokePaint.shader = LinearGradient(
                0f, 0f, w, 0f,
                intArrayOf(0x18FFFFFF, 0xCC99E9FF.toInt(), 0xEEFFFFFF.toInt(), 0x222979FF),
                floatArrayOf(0f, 0.35f, 0.62f, 1f),
                Shader.TileMode.CLAMP
            )
            wavePath.reset()
            for (i in 0..steps) {
                val frac = i / steps.toFloat()
                val x = frac * w
                val y = baseY +
                    sin(frac * 2f * PI.toFloat() + phase) * amp +
                    cos(frac * 3.5f * PI.toFloat() - phase * 0.7f) * (amp * 0.45f)
                if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
            }
            canvas.drawPath(wavePath, strokePaint)
        }
        ribbonPaint.shader = null
        strokePaint.shader = null
    }

    private fun drawNeonCyber(canvas: Canvas, w: Float, h: Float, t: Float) {
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF05030D.toInt(), 0xFF0E0826.toInt(), 0xFF060E22.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        bgPaint.shader = null

        val horizonY = h * 0.52f
        glowPaint.shader = RadialGradient(
            w * 0.5f, horizonY, w * 0.62f,
            intArrayOf(0x889D4EDD.toInt(), 0x5500D4FF, 0x00000000),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, glowPaint)
        glowPaint.shader = null

        // Perspective cyber grid below horizon
        strokePaint.shader = null
        strokePaint.strokeWidth = 1.6f
        val vanishX = w * 0.5f
        for (i in -8..8) {
            val bottomX = vanishX + i * (w * 0.14f)
            strokePaint.color = if (i % 2 == 0) 0x5500D4FF else 0x44A855F7
            canvas.drawLine(vanishX, horizonY, bottomX, h, strokePaint)
        }
        val scroll = (t * 0.45f) % 1f
        for (j in 0 until 10) {
            val linear = ((j + scroll) / 10f).coerceIn(0.01f, 1f)
            val eased = linear * linear
            val y = horizonY + (h - horizonY) * eased
            strokePaint.color = Color.argb((eased * 165).toInt(), 0, 212, 255)
            canvas.drawLine(0f, y, w, y, strokePaint)
        }

        //Sweeping laser beams
        for (b in 0 until 3) {
            val angle = sin(t * 0.7f + b * 2.1f) * 0.35f
            val bx = w * (0.2f + b * 0.3f)
            strokePaint.strokeWidth = 3f
            strokePaint.color = if (b == 1) 0x88C084FC.toInt() else 0x8800E5FF.toInt()
            canvas.drawLine(bx, 0f, bx + sin(angle) * w * 0.5f, horizonY, strokePaint)
        }

        // Glowing neon bokeh particles rising
        for (p in 0 until 26) {
            val seed = p * 73.17f
            val px = ((sin(seed) * 0.5f + 0.5f) * w + sin(t + p) * 14f)
            val progress = ((seed * 0.13f + t * (0.09f + (p % 4) * 0.03f)) % 1f)
            val py = h * (1f - progress)
            val radius = (3f + (p % 5) * 2.2f)
            particlePaint.color = if (p % 2 == 0) 0xAA00D4FF.toInt() else 0xAAC084FC.toInt()
            canvas.drawCircle(px, py, radius, particlePaint)
            particlePaint.color = 0xEEFFFFFF.toInt()
            canvas.drawCircle(px, py, radius * 0.38f, particlePaint)
        }
    }

    private fun drawGalaxyMotion(canvas: Canvas, w: Float, h: Float, t: Float) {
        bgPaint.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(0xFF03050F.toInt(), 0xFF090E29.toInt(), 0xFF0E0822.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        bgPaint.shader = null

        // Moving cosmic nebula clouds
        val neb1X = w * (0.3f + 0.12f * cos(t * 0.25f))
        val neb1Y = h * (0.35f + 0.08f * sin(t * 0.3f))
        glowPaint.shader = RadialGradient(
            neb1X, neb1Y, min(w, h) * 0.65f,
            intArrayOf(0x775B21B6, 0x442563EB, 0x00000000),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, glowPaint)

        val neb2X = w * (0.72f - 0.1f * sin(t * 0.22f))
        val neb2Y = h * (0.65f + 0.09f * cos(t * 0.28f))
        glowPaint.shader = RadialGradient(
            neb2X, neb2Y, min(w, h) * 0.58f,
            intArrayOf(0x6600D4FF, 0x337C3AED, 0x00000000),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, glowPaint)
        glowPaint.shader = null

        // Twinkling parallax stars
        for (i in 0 until 55) {
            val sx = ((sin(i * 19.31f) * 0.5f + 0.5f) * w + t * (2f + i % 4)) % w
            val sy = ((cos(i * 41.73f) * 0.5f + 0.5f) * h)
            val twinkle = (0.35f + 0.65f * (0.5f + 0.5f * sin(t * 2.4f + i * 0.9f)))
            particlePaint.color = Color.argb((twinkle * 240).toInt(), 230, 245, 255)
            val r = if (i % 7 == 0) 3.2f else 1.6f
            canvas.drawCircle(sx, sy, r, particlePaint)
        }

        // Ringed Planet with orbiting moon
        val planetX = w * 0.5f + cos(t * 0.18f) * w * 0.04f
        val planetY = h * 0.44f + sin(t * 0.22f) * h * 0.025f
        val planetR = min(w, h) * 0.16f

        glowPaint.shader = RadialGradient(
            planetX - planetR * 0.35f,
            planetY - planetR * 0.35f,
            planetR * 1.45f,
            intArrayOf(0xFF93E5FF.toInt(), 0xFF2563EB.toInt(), 0xFF0B1330.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(planetX, planetY, planetR, glowPaint)
        glowPaint.shader = null

        canvas.save()
        canvas.rotate(-18f + sin(t * 0.2f) * 4f, planetX, planetY)
        rectF.set(planetX - planetR * 1.85f, planetY - planetR * 0.42f, planetX + planetR * 1.85f, planetY + planetR * 0.42f)
        strokePaint.strokeWidth = planetR * 0.16f
        strokePaint.shader = LinearGradient(
            rectF.left, rectF.top, rectF.right, rectF.bottom,
            intArrayOf(0x2200D4FF, 0xAA80EAFF.toInt(), 0x44A855F7, 0xCCFFFFFF.toInt(), 0x2200D4FF),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(rectF, strokePaint)
        canvas.restore()
        strokePaint.shader = null

        // Orbiting Moon
        val orbitAngle = t * 0.85f
        val moonX = planetX + cos(orbitAngle) * planetR * 2.15f
        val moonY = planetY + sin(orbitAngle) * planetR * 0.72f
        particlePaint.color = 0xFFE2F1FF.toInt()
        canvas.drawCircle(moonX, moonY, planetR * 0.16f, particlePaint)
    }

    private fun drawRainyCity(canvas: Canvas, w: Float, h: Float, t: Float) {
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF070912.toInt(), 0xFF101935.toInt(), 0xFF0A0D18.toInt()),
            floatArrayOf(0f, 0.62f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        bgPaint.shader = null

        val streetY = h * 0.74f

        // Building silhouettes & glowing windows
        val buildingCount = 9
        val bw = w / buildingCount
        for (i in 0 until buildingCount) {
            val bh = h * (0.28f + ((i * 37) % 5) * 0.065f)
            val left = i * bw - 2f
            val top = streetY - bh
            val right = left + bw + 3f
            ribbonPaint.color = if (i % 2 == 0) 0xFF0B1120.toInt() else 0xFF0F172A.toInt()
            canvas.drawRect(left, top, right, streetY, ribbonPaint)

            // Neon storefront / rooftop accent sign
            val neonColor = when (i % 3) {
                0 -> 0xCC00D4FF.toInt()
                1 -> 0xCCE83E8C.toInt()
                else -> 0xCCF59E0B.toInt()
            }
            val pulse = 0.75f + 0.25f * sin(t * 2.5f + i)
            particlePaint.color = Color.argb((pulse * 220).toInt(), Color.red(neonColor), Color.green(neonColor), Color.blue(neonColor))
            canvas.drawRect(left + bw * 0.18f, top + 14f, right - bw * 0.18f, top + 20f, particlePaint)

            // Lit windows
            for (wy in 0 until 6) {
                for (wx in 0 until 2) {
                    if ((i + wy + wx) % 3 != 0) {
                        val winAlpha = if ((i + wy) % 4 == 0) 190 else 80
                        particlePaint.color = Color.argb(winAlpha, 180, 225, 255)
                        val rx = left + bw * (0.24f + wx * 0.36f)
                        val ry = top + 34f + wy * (bh * 0.12f)
                        if (ry + 10f < streetY) {
                            canvas.drawRect(rx, ry, rx + bw * 0.18f, ry + 8f, particlePaint)
                        }
                    }
                }
            }

            // Wet asphalt vertical reflection glow
            glowPaint.shader = LinearGradient(
                left, streetY, left, h,
                intArrayOf(
                    Color.argb(110, Color.red(neonColor), Color.green(neonColor), Color.blue(neonColor)),
                    0x00000000
                ),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(left + bw * 0.12f, streetY, right - bw * 0.12f, h, glowPaint)
        }
        glowPaint.shader = null

        // Moving rain streaks
        strokePaint.shader = null
        strokePaint.strokeWidth = 1.6f
        for (r in 0 until 45) {
            val seed = r * 53.7f
            val fall = ((t * (1.2f + (r % 3) * 0.25f) + seed * 0.11f) % 1.15f)
            val rx = ((sin(seed) * 0.5f + 0.5f) * (w + 60f)) - fall * 45f
            val ry = fall * h
            val len = 18f + (r % 4) * 7f
            strokePaint.color = if (r % 4 == 0) 0x8800D4FF.toInt() else 0x66CFE8FF
            canvas.drawLine(rx, ry, rx - 6f, ry + len, strokePaint)
        }

        // Puddle ripples on wet road
        for (k in 0 until 4) {
            val rp = ((t * 0.65f + k * 0.25f) % 1f)
            val rx = w * (0.18f + k * 0.22f)
            val ry = streetY + (h - streetY) * (0.25f + (k % 2) * 0.35f)
            strokePaint.strokeWidth = 1.5f
            strokePaint.color = Color.argb(((1f - rp) * 140).toInt(), 0, 212, 255)
            rectF.set(rx - rp * 32f, ry - rp * 8f, rx + rp * 32f, ry + rp * 8f)
            canvas.drawOval(rectF, strokePaint)
        }
    }

    private fun drawChromeSignature(
        canvas: Canvas,
        w: Float,
        h: Float,
        t: Float,
        emblemBitmap: Bitmap?
    ) {
        bgPaint.shader = RadialGradient(
            w * 0.5f, h * 0.44f, maxOf(w, h) * 0.78f,
            intArrayOf(0xFF0E2147.toInt(), 0xFF070C1A.toInt(), 0xFF03050B.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        bgPaint.shader = null

        val cx = w * 0.5f
        val cy = h * 0.44f
        val radius = min(w, h) * 0.26f

        // Pulsing neon aura
        val auraScale = 1.35f + 0.12f * sin(t * 1.8f)
        glowPaint.shader = RadialGradient(
            cx, cy, radius * auraScale,
            intArrayOf(0x8800D4FF.toInt(), 0x442979FF, 0x00000000),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius * auraScale, glowPaint)
        glowPaint.shader = null

        // Rotating animated metallic chrome rings
        for (ring in 0 until 3) {
            val r = radius * (1.08f + ring * 0.18f)
            canvas.save()
            val dir = if (ring % 2 == 0) 1f else -1f
            canvas.rotate(t * dir * (18f + ring * 10f), cx, cy)
            strokePaint.strokeWidth = (4.5f - ring * 1f)
            strokePaint.shader = LinearGradient(
                cx - r, cy - r, cx + r, cy + r,
                intArrayOf(0x1100D4FF, 0xEEFFFFFF.toInt(), 0xCC00D4FF.toInt(), 0x222979FF),
                floatArrayOf(0f, 0.35f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
            rectF.set(cx - r, cy - r, cx + r, cy + r)
            canvas.drawArc(rectF, 15f, 145f, false, strokePaint)
            canvas.drawArc(rectF, 195f, 145f, false, strokePaint)
            canvas.restore()
        }
        strokePaint.shader = null

        // Draw circular emblem bitmap if available
        if (emblemBitmap != null && !emblemBitmap.isRecycled) {
            val emblemSize = radius * 1.85f
            val scale = emblemSize / min(emblemBitmap.width, emblemBitmap.height).toFloat()
            canvas.save()
            val clipPath = Path().apply { addCircle(cx, cy, radius * 0.92f, Path.Direction.CW) }
            canvas.clipPath(clipPath)
            matrix.reset()
            matrix.postScale(scale, scale)
            matrix.postTranslate(
                cx - emblemBitmap.width * scale * 0.5f,
                cy - emblemBitmap.height * scale * 0.5f
            )
            emblemPaint.alpha = 245
            canvas.drawBitmap(emblemBitmap, matrix, emblemPaint)

            // Sweeping metallic light reflection beam across the emblem
            val sweep = ((t * 0.45f) % 2f) - 0.5f
            val sx = cx - radius + sweep * radius * 2.2f
            glowPaint.shader = LinearGradient(
                sx - radius * 0.35f, cy - radius,
                sx + radius * 0.35f, cy + radius,
                intArrayOf(0x00FFFFFF, 0x55FFFFFF, 0x00FFFFFF),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, radius * 0.92f, glowPaint)
            glowPaint.shader = null
            canvas.restore()
        } else {
            // Clean chrome emblem fallback ring
            strokePaint.strokeWidth = 6f
            strokePaint.color = 0xFF00D4FF.toInt()
            canvas.drawCircle(cx, cy, radius * 0.86f, strokePaint)
        }

        // Orbiting light spark
        val spAngle = t * 1.2f
        val sx = cx + cos(spAngle) * radius * 1.26f
        val sy = cy + sin(spAngle) * radius * 1.26f
        particlePaint.color = 0xFFFFFFFF.toInt()
        canvas.drawCircle(sx, sy, 4.5f, particlePaint)
    }

    companion object {
        fun buildColorMatrixFilter(brightness: Float, contrast: Float): ColorMatrixColorFilter {
            val b = (brightness - 1f) * 255f
            val c = contrast
            val translate = (-0.5f * c + 0.5f) * 255f + b
            val cm = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, translate,
                    0f, c, 0f, 0f, translate,
                    0f, 0f, c, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            return ColorMatrixColorFilter(cm)
        }
    }
}
