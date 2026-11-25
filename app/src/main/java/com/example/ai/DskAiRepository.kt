package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
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
