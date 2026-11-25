package com.example.browser

import android.net.Uri
import android.net.http.SslError
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import com.example.data.SearchEngine
import com.example.data.SitePermissionType
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "",
    val title: String = "New Tab",
    val isHomepage: Boolean = true,
    val isIncognito: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isDesktopMode: Boolean = false,
    val isSecure: Boolean = false,
    val securityHost: String = "",
    val certificateIssuer: String = "",
    val certificateValidTo: String = "",
    val hasError: Boolean = false,
    val errorCode: Int = 0,
    val errorDescription: String = "",
    val faviconUrl: String = "",
    val webViewGeneration: Int = 0
)

data class ClosedTabRecord(
    val title: String,
    val url: String,
    val isHomepage: Boolean,
    val isDesktopMode: Boolean
)

sealed interface BrowserOverlay {
    data object None : BrowserOverlay
    data object Tabs : BrowserOverlay
    data object Menu : BrowserOverlay
    data object Bookmarks : BrowserOverlay
    data object History : BrowserOverlay
    data object Downloads : BrowserOverlay
    data object Settings : BrowserOverlay
    data object Permissions : BrowserOverlay
    data object AiSearch : BrowserOverlay
    data object Translate : BrowserOverlay
    data object About : BrowserOverlay
    data object WallpaperStudio : BrowserOverlay
    data class WallpaperPreview(val wallpaperId: String) : BrowserOverlay
}

data class PendingDownloadPrompt(
    val url: String,
    val fileName: String,
    val mimeType: String,
    val contentLength: Long,
    val userAgent: String
)

data class PendingSitePermissionPrompt(
    val host: String,
    val types: List<SitePermissionType>,
    val webPermissionRequest: PermissionRequest? = null,
    val geoOrigin: String? = null,
    val geoCallback: GeolocationPermissions.Callback? = null
)

data class PendingSslWarning(
    val url: String,
    val errorDescription: String,
    val handler: SslErrorHandler
)

data class PendingSafeBrowsingWarning(
    val url: String,
    val reason: String
)

data class ContextLinkAction(
    val url: String,
    val isImage: Boolean
)

data class FindInPageState(
    val isVisible: Boolean = false,
    val query: String = "",
    val currentMatchIndex: Int = 0,
    val totalMatches: Int = 0
)

object UrlUtils {
    private val domainRegex = Regex(
        "^(localhost|(\\d{1,3}\\.){3}\\d{1,3}|([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,})(:\\d+)?(/.*)?$"
    )

    fun resolveInputToUrl(
        rawInput: String,
        searchEngine: SearchEngine,
        safeSearchEnabled: Boolean
    ): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("about:", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true)
        ) {
            return trimmed
        }
        if (!trimmed.contains(' ') && domainRegex.matches(trimmed)) {
            return "https://$trimmed"
        }
        return searchEngine.buildSearchUrl(trimmed, safeSearchEnabled)
    }

    fun extractHost(url: String): String {
        val androidHost = runCatching { Uri.parse(url)?.host }.getOrNull()
        if (!androidHost.isNullOrBlank()) return androidHost
        return runCatching { java.net.URI(url).host }.getOrNull().orEmpty()
    }

    fun buildFaviconUrl(url: String): String {
        val host = extractHost(url)
        return if (host.isNotBlank()) "https://www.google.com/s2/favicons?domain=$host&sz=64" else ""
    }

    fun guessFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        return URLUtil.guessFileName(url, contentDisposition, mimeType)
    }

    fun checkSuspiciousUrl(url: String): String? {
        val lower = url.lowercase()
        if (lower.startsWith("http://") && lower.contains("@")) {
            return "This link uses embedded credentials over an unencrypted HTTP connection, which is commonly used in phishing attacks."
        }
        if (lower.startsWith("http://") && Regex("^http://(\\d{1,3}\\.){3}\\d{1,3}.*(login|verify|bank|wallet|password)").containsMatchIn(lower)) {
            return "This raw IP address is requesting sensitive login or financial details without HTTPS encryption."
        }
        if (lower.contains("xn--") && (lower.contains("google") || lower.contains("paypal") || lower.contains("apple") || lower.contains("bank"))) {
            return "This domain uses internationalized punycode characters that may impersonate a trusted website."
        }
        return null
    }

    fun describeSslError(error: SslError?): String {
        if (error == null) return "The website's security certificate could not be verified."
        return when (error.primaryError) {
            SslError.SSL_EXPIRED -> "This website's security certificate has expired."
            SslError.SSL_IDMISMATCH -> "The security certificate belongs to a different domain name."
            SslError.SSL_UNTRUSTED -> "The certificate authority that issued this site's certificate is not trusted."
            SslError.SSL_NOTYETVALID -> "This website's security certificate is not valid yet."
            SslError.SSL_DATE_INVALID -> "The security certificate date is invalid."
            else -> "An SSL/TLS certificate validation error occurred."
        }
    }
}
