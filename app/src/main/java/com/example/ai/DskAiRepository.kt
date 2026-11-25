package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.WallpaperEntity
import com.example.wallpaper.AiAspectRatio
import com.example.wallpaper.AiImageQuality
import com.example.wallpaper.AiWallpaperStyle
import com.example.wallpaper.AiWallpaperSuggestion
import com.example.wallpaper.DskLivePreset
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "DskAiRepository"
private const val AI_MODEL_NAME = "gemini-3.8-flash"
private const val AI_TIMEOUT_MS = 60_000L

sealed interface AiStreamEvent {
    data object Loading : AiStreamEvent
    data class Streaming(val text: String) : AiStreamEvent
    data class Completed(val text: String) : AiStreamEvent
    data class Error(val message: String) : AiStreamEvent
}

sealed interface AiWallpaperGenState {
    data object Idle : AiWallpaperGenState
    data class Loading(val prompt: String) : AiWallpaperGenState
    data class Success(val wallpaper: WallpaperEntity, val bitmap: Bitmap) : AiWallpaperGenState
    data class Error(val message: String) : AiWallpaperGenState
}

enum class AiSearchMode(val label: String, val systemInstruction: String) {
    ASK(
        label = "Ask AI",
        systemInstruction = "You are DSK AI Search inside the DSK CHROME mobile web browser. Give clear, accurate, concise, well-structured answers."
    ),
    SUMMARIZE_PAGE(
        label = "Summarize Page",
        systemInstruction = "You are DSK AI Search. Summarize the provided webpage content clearly into a brief overview followed by key bullet points."
    ),
    EXPLAIN(
        label = "Explain Topic",
        systemInstruction = "You are DSK AI Search. Explain complex topics clearly using plain language, structured steps, and intuitive analogies."
    ),
    GENERATE(
        label = "Generate Text",
        systemInstruction = "You are DSK AI Search. Generate helpful, polished, ready-to-use text matching the user's request."
    ),
    TRANSLATE(
        label = "Translate",
        systemInstruction = "You are DSK AI Search Translator. Translate the provided text accurately and naturally, preserving tone and meaning."
    )
}

class DskAiRepository {

    private val model by lazy {
        Firebase.ai.generativeModel(AI_MODEL_NAME)
    }

    suspend fun generateWallpaperBitmap(
        prompt: String,
        style: AiWallpaperStyle,
        aspectRatio: AiAspectRatio,
        quality: AiImageQuality
    ): Result<Bitmap> {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a prompt to generate a wallpaper."))
        }
        val mappedRatio = when (aspectRatio) {
            AiAspectRatio.PORTRAIT_9_16 -> AspectRatio.PORTRAIT_9x16
            AiAspectRatio.LANDSCAPE_16_9 -> AspectRatio.LANDSCAPE_16x9
            AiAspectRatio.SQUARE_1_1 -> AspectRatio.SQUARE_1x1
            AiAspectRatio.PORTRAIT_3_4 -> AspectRatio.PORTRAIT_3x4
        }
        val mappedSize = when (quality) {
            AiImageQuality.FAST -> ImageSize.SIZE_1K
            AiImageQuality.STUDIO -> ImageSize.SIZE_2K
        }
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(mappedRatio)
                .setImageSize(mappedSize)
                .build()
        }
        val imageModel = Firebase.ai.generativeModel(
            modelName = quality.modelName,
            generationConfig = config
        )
        val composedPrompt = "Mobile wallpaper artwork, ${style.promptEnhancer}. Subject: $cleanPrompt. Clean edge-to-edge composition with balanced visual contrast for browser UI icons, no watermarks or text."
        return try {
            val bitmap = withTimeout(AI_TIMEOUT_MS) {
                val response = imageModel.generateContent(composedPrompt)
                val imagePart = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                imagePart?.image
            }
            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("Gemini did not return an image. Please refine your prompt."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI wallpaper generation timed out", e)
            Result.failure(IllegalStateException("Generation timed out after 60 seconds. Check your connection and try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI wallpaper generation failed", e)
            Result.failure(
                IllegalStateException(
                    e.localizedMessage
                        ?: "AI image generation failed. Ensure Firebase AI Logic / Blaze billing and network access are active."
                )
            )
        }
    }

    fun buildWallpaperSuggestions(
        wallpapers: List<WallpaperEntity>,
        isDarkMode: Boolean,
        hourOfDay: Int
    ): List<AiWallpaperSuggestion> {
        val favoriteStyles = wallpapers
            .filter { it.isFavorite && it.styleTag.isNotBlank() }
            .groupingBy { it.styleTag }
            .eachCount()
        val recentStyles = wallpapers
            .filter { it.lastUsedAt > 0L && it.styleTag.isNotBlank() }
            .sortedByDescending { it.lastUsedAt }
            .take(5)
            .map { it.styleTag }

        val timeBand = when (hourOfDay) {
            in 5..11 -> "Morning Clarity"
            in 12..16 -> "Afternoon Focus"
            in 17..20 -> "Evening Glow"
            else -> "Late Night Mode"
        }

        val suggestions = mutableListOf<AiWallpaperSuggestion>()

        if (isDarkMode || hourOfDay >= 19 || hourOfDay < 6) {
            suggestions += AiWallpaperSuggestion(
                id = "sug_night_chrome",
                title = "Liquid Chrome Abyss",
                reason = "$timeBand • Matches dark browser UI",
                style = AiWallpaperStyle.ABSTRACT,
                prompt = "Sleek sculpted liquid chrome waves over deep obsidian glass with subtle electric cyan rim lighting",
                linkedLivePreset = DskLivePreset.DSK_CHROME_FLOW
            )
            suggestions += AiWallpaperSuggestion(
                id = "sug_rain_city",
                title = "Neon Rain Skyline",
                reason = "$timeBand • High contrast behind search bar",
                style = AiWallpaperStyle.CYBERPUNK,
                prompt = "Atmospheric cyberpunk skyline at night with soft neon cyan reflections and gentle rain haze",
                linkedLivePreset = DskLivePreset.DSK_RAINY_CITY
            )
        } else {
            suggestions += AiWallpaperSuggestion(
                id = "sug_day_prism",
                title = "Iridescent Glass Horizon",
                reason = "$timeBand • Clean bright workspace",
                style = AiWallpaperStyle.MINIMAL,
                prompt = "Architectural frosted glass curves catching soft morning sapphire light and clean sky gradient",
                linkedLivePreset = DskLivePreset.DSK_CHROME_SIGNATURE
            )
            suggestions += AiWallpaperSuggestion(
                id = "sug_day_nature",
                title = "Alpine Emerald Mist",
                reason = "$timeBand • Crisp daylight balance",
                style = AiWallpaperStyle.NATURE,
                prompt = "Minimal misty mountain ridge at sunrise with deep cobalt shadows and crisp golden peaks"
            )
        }

        val topFavStyle = favoriteStyles.maxByOrNull { it.value }?.key
            ?: recentStyles.firstOrNull()
        when (topFavStyle?.lowercase()) {
            "space" -> suggestions += AiWallpaperSuggestion(
                id = "sug_fav_space",
                title = "Deep Nebula Drift",
                reason = "Based on your Space preference",
                style = AiWallpaperStyle.SPACE,
                prompt = "Vibrant sapphire and violet cosmic nebula dust around a distant ringed exoplanet",
                linkedLivePreset = DskLivePreset.DSK_GALAXY_MOTION
            )
            "cyberpunk", "neon" -> suggestions += AiWallpaperSuggestion(
                id = "sug_fav_neon",
                title = "Cyber Pulse Grid",
                reason = "Based on your Neon/Cyberpunk picks",
                style = AiWallpaperStyle.NEON,
                prompt = "Futuristic dark cyber horizon with luminous electric blue and purple geometric light beams",
                linkedLivePreset = DskLivePreset.DSK_NEON_CYBER
            )
            "anime" -> suggestions += AiWallpaperSuggestion(
                id = "sug_fav_anime",
                title = "Twilight Rooftop Sky",
                reason = "Based on your Anime favorites",
                style = AiWallpaperStyle.ANIME,
                prompt = "Detailed anime twilight sky over a quiet futuristic city with drifting violet clouds"
            )
            else -> suggestions += AiWallpaperSuggestion(
                id = "sug_dsk_cosmic",
                title = "Orbital Ring Horizon",
                reason = "Curated for DSK Chrome",
                style = AiWallpaperStyle.THREE_D,
                prompt = "Ultra sleek 3D metallic orbital rings floating in deep space with glowing sapphire core",
                linkedLivePreset = DskLivePreset.DSK_GALAXY_MOTION
            )
        }

        return suggestions.distinctBy { it.id }
    }

    fun streamPrompt(
        prompt: String,
        mode: AiSearchMode,
        pageContextTitle: String? = null,
        pageContextUrl: String? = null,
        pageContextText: String? = null
    ): Flow<AiStreamEvent> = flow {
        emit(AiStreamEvent.Loading)
        val fullPrompt = buildString {
            appendLine(mode.systemInstruction)
            if (!pageContextText.isNullOrBlank()) {
                appendLine()
                appendLine("Active Webpage Context:")
                if (!pageContextTitle.isNullOrBlank()) appendLine("Title: $pageContextTitle")
                if (!pageContextUrl.isNullOrBlank()) appendLine("URL: $pageContextUrl")
                appendLine("Content excerpt:")
                appendLine(pageContextText.take(12_000))
            }
            appendLine()
            appendLine("User request:")
            append(prompt)
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                model.generateContentStream(fullPrompt).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        accumulated.append(chunkText)
                        emit(AiStreamEvent.Streaming(accumulated.toString()))
                    }
                }
            }
            if (accumulated.isNotBlank()) {
                emit(AiStreamEvent.Completed(accumulated.toString()))
            } else {
                emit(AiStreamEvent.Error("DSK AI Search returned an empty response. Please try again."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI stream timed out", e)
            emit(AiStreamEvent.Error("Request timed out after 60 seconds. Check your connection and try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            emit(
                AiStreamEvent.Error(
                    e.localizedMessage ?: "Unable to connect to DSK AI Search. Check internet connection."
                )
            )
        }
    }

    fun streamWebpageTranslation(
        pageTitle: String,
        pageUrl: String,
        pageText: String,
        targetLanguage: String
    ): Flow<AiStreamEvent> = flow {
        emit(AiStreamEvent.Loading)
        if (pageText.isBlank()) {
            emit(AiStreamEvent.Error("No readable text found on this webpage to translate."))
            return@flow
        }

        val translationPrompt = """
            Translate the following webpage content into $targetLanguage.
            Preserve headings, lists, and paragraph structure cleanly. Do not add commentary or extra preamble—output only the translated title and translated webpage text.
            
            Page Title: $pageTitle
            Page URL: $pageUrl
            
            Content:
            ${pageText.take(14_000)}
        """.trimIndent()

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                model.generateContentStream(translationPrompt).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        accumulated.append(chunkText)
                        emit(AiStreamEvent.Streaming(accumulated.toString()))
                    }
                }
            }
            if (accumulated.isNotBlank()) {
                emit(AiStreamEvent.Completed(accumulated.toString()))
            } else {
                emit(AiStreamEvent.Error("Translation returned an empty response."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Page translation timed out", e)
            emit(AiStreamEvent.Error("Translation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Page translation failed", e)
            emit(
                AiStreamEvent.Error(
                    e.localizedMessage ?: "Translation failed. Check your network connection."
                )
            )
        }
    }
}
