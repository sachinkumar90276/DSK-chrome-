package com.example.browser

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import java.text.DateFormat

interface WebClientCallbacks {
    fun onPageStarted(tabId: String, url: String)
    fun onPageFinished(tabId: String, url: String, title: String, isSecure: Boolean, issuer: String, validTo: String)
    fun onNavigationStateChanged(tabId: String, canGoBack: Boolean, canGoForward: Boolean)
    fun onMainFrameError(tabId: String, errorCode: Int, description: String)
    fun onSslErrorDetected(tabId: String, url: String, description: String, handler: SslErrorHandler)
    fun onSafeBrowsingBlocked(tabId: String, url: String, reason: String)
    fun onRendererCrashed(tabId: String, didCrash: Boolean)
    fun isSafeBrowsingEnabled(): Boolean
    fun isDoNotTrackEnabled(): Boolean
}

class DskWebViewClient(
    private val context: Context,
    private val tabId: String,
    private val callbacks: WebClientCallbacks
) : WebViewClient() {

    private var bypassedUnsafeUrl: String? = null

    @Volatile
    private var disposed: Boolean = false

    fun markDisposed() {
        disposed = true
    }

    fun allowUnsafeUrlOnce(url: String) {
        bypassedUnsafeUrl = url
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        if (disposed) return true
        val uri = request?.url ?: return false
        val url = uri.toString()
        val scheme = uri.scheme?.lowercase().orEmpty()

        if (scheme == "http" || scheme == "https") {
            if (callbacks.isSafeBrowsingEnabled() && bypassedUnsafeUrl != url) {
                val suspiciousReason = UrlUtils.checkSuspiciousUrl(url)
                if (suspiciousReason != null) {
                    callbacks.onSafeBrowsingBlocked(tabId, url, suspiciousReason)
                    return true
                }
            }
            return false
        }

        if (scheme == "about" || scheme == "data" || scheme == "blob") {
            return false
        }

        return handleExternalScheme(url, scheme)
    }

    private fun handleExternalScheme(url: String, scheme: String): Boolean {
        return try {
            val intent = if (scheme == "intent") {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            true
        } catch (_: Exception) {
            true
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (disposed) return
        if (!url.isNullOrBlank()) {
            callbacks.onPageStarted(tabId, url)
        }
        view?.let {
            callbacks.onNavigationStateChanged(tabId, it.canGoBack(), it.canGoForward())
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (disposed) return
        val currentUrl = url.orEmpty()
        val title = view?.title.orEmpty().ifBlank { UrlUtils.extractHost(currentUrl).ifBlank { currentUrl } }
        val isSecure = currentUrl.startsWith("https://", ignoreCase = true)
        val cert = view?.certificate
        val issuer = cert?.issuedBy?.cName?.takeIf { it.isNotBlank() }
            ?: cert?.issuedBy?.oName
            ?: ""
        val validTo = cert?.validNotAfterDate?.let {
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(it)
        }.orEmpty()

        callbacks.onPageFinished(tabId, currentUrl, title, isSecure, issuer, validTo)
        view?.let {
            callbacks.onNavigationStateChanged(tabId, it.canGoBack(), it.canGoForward())
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (disposed) return
        if (request?.isForMainFrame == true) {
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.errorCode ?: -1 else -1
            val desc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                error?.description?.toString().orEmpty()
            } else {
                "Unable to load webpage"
            }
            callbacks.onMainFrameError(tabId, code, desc.ifBlank { "Unable to reach this website." })
        }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        if (handler == null) return
        if (disposed) {
            handler.cancel()
            return
        }
        val failingUrl = error?.url ?: view?.url.orEmpty()
        callbacks.onSslErrorDetected(
            tabId = tabId,
            url = failingUrl,
            description = UrlUtils.describeSslError(error),
            handler = handler
        )
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        if (disposed) return true
        disposed = true
        val didCrash = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            detail?.didCrash() ?: true
        } else {
            true
        }
        callbacks.onRendererCrashed(tabId, didCrash)
        return true
    }
}
