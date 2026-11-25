package com.example.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import com.example.data.BrowserSettings

private data class AppliedWebConfig(
    val isIncognito: Boolean,
    val isDesktopMode: Boolean,
    val fontSizePercent: Int,
    val blockPopups: Boolean,
    val softwareLayer: Boolean
)

class TabWebViewPool(
    private val context: Context,
    private val clientCallbacks: WebClientCallbacks,
    private val chromeCallbacks: WebChromeCallbacks,
    private val onDownloadRequested: (
        url: String,
        fileName: String,
        mimeType: String,
        contentLength: Long,
        userAgent: String
    ) -> Unit,
    private val onFindResults: (activeMatchOrdinal: Int, numberOfMatches: Int) -> Unit,
    private val onContextLinkLongPress: (url: String, isImage: Boolean) -> Unit
) {
    companion object {
        private const val MAX_LIVE_WEBVIEWS = 4
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val webViews = mutableMapOf<String, WebView>()
    private val clients = mutableMapOf<String, DskWebViewClient>()
    private val appliedConfigs = mutableMapOf<String, AppliedWebConfig>()
    private val pendingLoads = mutableMapOf<String, Pair<String, Boolean>>()
    private val lruTabOrder = ArrayDeque<String>()

    @Volatile
    var forceSoftwareLayer: Boolean = false
        private set

    private val mobileUserAgent: String by lazy {
        WebSettings.getDefaultUserAgent(context)
    }
    private val desktopUserAgent =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    fun setSoftwareLayerFallback(enabled: Boolean) {
        if (forceSoftwareLayer == enabled) return
        forceSoftwareLayer = enabled
        val layerType = if (enabled) View.LAYER_TYPE_SOFTWARE else View.LAYER_TYPE_NONE
        webViews.values.forEach { wv ->
            wv.setLayerType(layerType, null)
        }
    }

    private fun touchLru(tabId: String) {
        lruTabOrder.remove(tabId)
        lruTabOrder.addLast(tabId)
    }

    private fun evictOldestIfNeeded(keepTabId: String) {
        while (webViews.size >= MAX_LIVE_WEBVIEWS && lruTabOrder.isNotEmpty()) {
            val candidate = lruTabOrder.firstOrNull { it != keepTabId } ?: break
            lruTabOrder.remove(candidate)
            removeTab(candidate)
        }
    }

    fun getOrCreate(
        tabId: String,
        isIncognito: Boolean,
        isDesktopMode: Boolean,
        settings: BrowserSettings,
        activityContext: Context? = null
    ): WebView {
        touchLru(tabId)
        val existing = webViews[tabId]
        if (existing != null) {
            if (activityContext != null) {
                (existing.context as? MutableContextWrapper)?.baseContext = activityContext
                runCatching { existing.onResume() }
            }
            applySettingsIfChanged(tabId, existing, isIncognito, isDesktopMode, settings)
            return existing
        }
        evictOldestIfNeeded(tabId)
        return createWebView(tabId, isIncognito, isDesktopMode, settings, activityContext).also {
            webViews[tabId] = it
        }
    }

    fun flushPendingLoadIfAttached(tabId: String, webView: WebView) {
        val pending = pendingLoads.remove(tabId) ?: return
        val (url, doNotTrack) = pending
        webView.post {
            if (webViews[tabId] !== webView) return@post
            if (doNotTrack) {
                webView.loadUrl(url, mapOf("DNT" to "1"))
            } else {
                webView.loadUrl(url)
            }
        }
    }

    fun detachActivityContext(tabId: String) {
        val existing = webViews[tabId] ?: return
        runCatching { existing.onPause() }
        (existing.context as? MutableContextWrapper)?.baseContext = context.applicationContext
    }

    fun resumeTab(tabId: String) {
        webViews[tabId]?.let { wv ->
            runCatching { wv.onResume() }
        }
    }

    fun trimBackgroundTabs(keepTabId: String?) {
        val toRemove = webViews.keys.filter { it != keepTabId }
        toRemove.forEach { removeTab(it) }
    }

    fun getExisting(tabId: String): WebView? = webViews[tabId]

    fun getClient(tabId: String): DskWebViewClient? = clients[tabId]

    fun applySettingsToAll(tabs: List<BrowserTab>, settings: BrowserSettings) {
        tabs.forEach { tab ->
            webViews[tab.id]?.let { wv ->
                applySettingsIfChanged(tab.id, wv, tab.isIncognito, tab.isDesktopMode, settings)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(
        tabId: String,
        isIncognito: Boolean,
        isDesktopMode: Boolean,
        settings: BrowserSettings,
        activityContext: Context? = null
    ): WebView {
        val wrappedContext = MutableContextWrapper(activityContext ?: context)
        val webView = WebView(wrappedContext).apply {
            setBackgroundColor(if (isIncognito) 0xFF0E0C15.toInt() else Color.WHITE)
            if (forceSoftwareLayer) {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)
            }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val client = DskWebViewClient(context, tabId, clientCallbacks)
        val chromeClient = DskWebChromeClient(tabId, chromeCallbacks)
        clients[tabId] = client
        webView.webViewClient = client
        webView.webChromeClient = chromeClient

        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val guessed = URLUtil.guessFileName(url, contentDisposition, mimetype)
            onDownloadRequested(url, guessed, mimetype.orEmpty(), contentLength, userAgent.orEmpty())
        }

        webView.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
            if (isDoneCounting || numberOfMatches == 0) {
                onFindResults(activeMatchOrdinal, numberOfMatches)
            }
        }

        webView.setOnLongClickListener { v ->
            val wv = v as? WebView ?: return@setOnLongClickListener false
            val result = wv.hitTestResult
            val extra = result.extra
            if (!extra.isNullOrBlank()) {
                when (result.type) {
                    WebView.HitTestResult.IMAGE_TYPE,
                    WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                        onContextLinkLongPress(extra, true)
                        return@setOnLongClickListener true
                    }
                    WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                        onContextLinkLongPress(extra, false)
                        return@setOnLongClickListener true
                    }
                }
            }
            false
        }

        applySettingsIfChanged(tabId, webView, isIncognito, isDesktopMode, settings, force = true)
        return webView
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun applySettingsIfChanged(
        tabId: String,
        webView: WebView,
        isIncognito: Boolean,
        isDesktopMode: Boolean,
        settings: BrowserSettings,
        force: Boolean = false
    ) {
        val target = AppliedWebConfig(
            isIncognito = isIncognito,
            isDesktopMode = isDesktopMode,
            fontSizePercent = settings.webFontSizePercent,
            blockPopups = settings.blockPopups,
            softwareLayer = forceSoftwareLayer
        )
        val previous = appliedConfigs[tabId]
        if (!force && previous == target) return
        appliedConfigs[tabId] = target

        val ws = webView.settings
        if (force || previous == null) {
            ws.javaScriptEnabled = true
            ws.domStorageEnabled = true
            ws.setSupportZoom(true)
            ws.builtInZoomControls = true
            ws.displayZoomControls = false
            ws.useWideViewPort = true
            ws.loadWithOverviewMode = true
            ws.mediaPlaybackRequiresUserGesture = false
            ws.allowFileAccess = true
            ws.allowContentAccess = true
            ws.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            CookieManager.getInstance().setAcceptCookie(true)
        }

        if (force || previous?.isIncognito != isIncognito) {
            ws.databaseEnabled = !isIncognito
            if (isIncognito) {
                ws.cacheMode = WebSettings.LOAD_NO_CACHE
                CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)
            } else {
                ws.cacheMode = WebSettings.LOAD_DEFAULT
                CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
            }
        }

        if (force || previous?.fontSizePercent != settings.webFontSizePercent) {
            ws.textZoom = settings.webFontSizePercent
        }

        if (force || previous?.isDesktopMode != isDesktopMode) {
            ws.userAgentString = if (isDesktopMode) desktopUserAgent else mobileUserAgent
        }

        if (force || previous?.blockPopups != settings.blockPopups) {
            ws.setSupportMultipleWindows(!settings.blockPopups)
            ws.javaScriptCanOpenWindowsAutomatically = !settings.blockPopups
        }

        val desiredLayer = if (forceSoftwareLayer) View.LAYER_TYPE_SOFTWARE else View.LAYER_TYPE_NONE
        if (webView.layerType != desiredLayer) {
            webView.setLayerType(desiredLayer, null)
        }
    }

    fun loadUrl(
        tabId: String,
        url: String,
        isIncognito: Boolean,
        isDesktopMode: Boolean,
        settings: BrowserSettings
    ) {
        touchLru(tabId)
        val existing = webViews[tabId]
        if (existing != null && existing.isAttachedToWindow) {
            applySettingsIfChanged(tabId, existing, isIncognito, isDesktopMode, settings)
            runCatching { existing.onResume() }
            if (settings.doNotTrackEnabled) {
                existing.loadUrl(url, mapOf("DNT" to "1"))
            } else {
                existing.loadUrl(url)
            }
        } else {
            // Queue until AndroidView creates & attaches the WebView with a valid Activity Window context
            pendingLoads[tabId] = url to settings.doNotTrackEnabled
        }
    }

    fun deliverPopupTransport(tabId: String, isIncognito: Boolean, settings: BrowserSettings, resultMsg: Message) {
        val wv = getOrCreate(tabId, isIncognito, settings.desktopSiteDefault, settings)
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return
        transport.webView = wv
        resultMsg.sendToTarget()
    }

    fun reload(tabId: String) = webViews[tabId]?.reload()
    fun stopLoading(tabId: String) = webViews[tabId]?.stopLoading()
    fun goBack(tabId: String): Boolean {
        val wv = webViews[tabId] ?: return false
        return if (wv.canGoBack()) {
            wv.goBack()
            true
        } else {
            false
        }
    }

    fun goForward(tabId: String): Boolean {
        val wv = webViews[tabId] ?: return false
        return if (wv.canGoForward()) {
            wv.goForward()
            true
        } else {
            false
        }
    }

    fun findAll(tabId: String, query: String) {
        val wv = webViews[tabId] ?: return
        if (query.isBlank()) {
            wv.clearMatches()
            onFindResults(0, 0)
        } else {
            wv.findAllAsync(query)
        }
    }

    fun findNext(tabId: String, forward: Boolean) {
        webViews[tabId]?.findNext(forward)
    }

    fun clearFind(tabId: String) {
        webViews[tabId]?.clearMatches()
    }

    fun extractPageText(tabId: String, onText: (String) -> Unit) {
        val wv = webViews[tabId]
        if (wv == null) {
            onText("")
            return
        }
        wv.evaluateJavascript("(function(){ return document.body ? document.body.innerText : ''; })()") { raw ->
            val cleaned = raw
                ?.removeSurrounding("\"")
                ?.replace("\\n", "\n")
                ?.replace("\\t", "\t")
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")
                .orEmpty()
            onText(cleaned)
        }
    }

    fun printPage(activityContext: Context, tabId: String, title: String): Boolean {
        val wv = webViews[tabId] ?: return false
        val printManager = activityContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return false
        val jobName = title.ifBlank { "DSK CHROME Document" }
        val adapter = wv.createPrintDocumentAdapter(jobName)
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
        return true
    }

    private fun safeDestroyWebView(wv: WebView) {
        (wv.parent as? ViewGroup)?.removeView(wv)
        runCatching {
            wv.stopLoading()
            wv.webChromeClient = null
            wv.setDownloadListener(null)
            wv.setFindListener(null)
            wv.setOnLongClickListener(null)
        }
        mainHandler.post {
            runCatching { wv.destroy() }
        }
    }

    fun removeTab(tabId: String) {
        clients.remove(tabId)?.markDisposed()
        appliedConfigs.remove(tabId)
        pendingLoads.remove(tabId)
        lruTabOrder.remove(tabId)
        webViews.remove(tabId)?.let { wv ->
            safeDestroyWebView(wv)
        }
    }

    fun clearAllCache() {
        webViews.values.forEach { it.clearCache(true) }
    }

    fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    fun clearWebsiteData() {
        WebStorage.getInstance().deleteAllData()
        webViews.values.forEach {
            it.clearFormData()
            it.clearHistory()
        }
    }

    fun clearIncognitoSessionData() {
        CookieManager.getInstance().removeSessionCookies(null)
        CookieManager.getInstance().flush()
    }

    fun pauseAll() = webViews.values.forEach { it.onPause() }
    fun resumeAll() = webViews.values.forEach { it.onResume() }

    fun destroyAll() {
        clients.values.forEach { it.markDisposed() }
        webViews.values.forEach { wv ->
            safeDestroyWebView(wv)
        }
        webViews.clear()
        clients.clear()
        appliedConfigs.clear()
        pendingLoads.clear()
        lruTabOrder.clear()
    }
}
