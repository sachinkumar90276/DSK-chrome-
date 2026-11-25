package com.example.browser

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiSearchMode
import com.example.ai.AiStreamEvent
import com.example.ai.DskAiRepository
import com.example.data.BookmarkEntity
import com.example.data.BrowserDatabase
import com.example.data.BrowserRepository
import com.example.data.BrowserSettings
import com.example.data.DownloadEntity
import com.example.data.HistoryEntity
import com.example.data.PermissionDecision
import com.example.data.SavedTabEntity
import com.example.data.SearchEngine
import com.example.data.SettingsDataStore
import com.example.data.ShortcutEntity
import com.example.data.SitePermissionEntity
import com.example.data.SitePermissionType
import com.example.data.StartupBehavior
import com.example.data.ThemeMode
import com.example.data.ToolbarLayout
import com.example.data.WallpaperPreset
import com.example.download.DownloadEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val modeLabel: String = "",
    val isError: Boolean = false,
    val isStreaming: Boolean = false
)

data class TranslationUiState(
    val targetLanguage: String = "Spanish",
    val isTranslating: Boolean = false,
    val translatedText: String = "",
    val errorMessage: String? = null,
    val sourceTitle: String = "",
    val sourceUrl: String = ""
)

data class SearchSuggestionItem(
    val text: String,
    val url: String? = null,
    val isHistory: Boolean = false
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BrowserDatabase.getInstance(application)
    private val settingsStore = SettingsDataStore(application)
    val repository = BrowserRepository(application, database, settingsStore)
    val downloadEngine = DownloadEngine(application, repository)
    private val aiRepository = DskAiRepository()

    val settings: StateFlow<BrowserSettings> = repository.settings.stateIn(
        viewModelScope, SharingStarted.Eagerly, BrowserSettings()
    )
    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.bookmarks.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val history: StateFlow<List<HistoryEntity>> = repository.history.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val recentHistory: StateFlow<List<HistoryEntity>> = repository.recentHistory.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val downloads: StateFlow<List<DownloadEntity>> = repository.downloads.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val sitePermissions: StateFlow<List<SitePermissionEntity>> = repository.sitePermissions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val shortcuts: StateFlow<List<ShortcutEntity>> = repository.shortcuts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _showSplash = MutableStateFlow(true)
    val showSplash: StateFlow<Boolean> = _showSplash.asStateFlow()

    private val tabStateManager = TabStateManager()
    private val _tabState = MutableStateFlow(TabState())
    val tabState: StateFlow<TabState> = _tabState.asStateFlow()

    val tabs: StateFlow<List<BrowserTab>> = _tabState
        .map { it.tabs }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _tabState.value.tabs)

    val activeTabId: StateFlow<String> = _tabState
        .map { it.activeTabId }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _tabState.value.activeTabId)

    val activeTab: StateFlow<BrowserTab> = _tabState
        .map { it.activeTab }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _tabState.value.activeTab)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isActiveTabBookmarked: StateFlow<Boolean> = activeTab
        .flatMapLatest { tab ->
            if (tab.isHomepage || tab.url.isBlank()) flowOf(false)
            else repository.observeIsBookmarked(tab.url)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val closedTabs: StateFlow<List<ClosedTabRecord>> = _tabState
        .map { it.closedTabs }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _activeOverlay = MutableStateFlow<BrowserOverlay>(BrowserOverlay.None)
    val activeOverlay: StateFlow<BrowserOverlay> = _activeOverlay.asStateFlow()

    private val _suggestions = MutableStateFlow<List<SearchSuggestionItem>>(emptyList())
    val suggestions: StateFlow<List<SearchSuggestionItem>> = _suggestions.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _findInPage = MutableStateFlow(FindInPageState())
    val findInPage: StateFlow<FindInPageState> = _findInPage.asStateFlow()

    private val _showSecuritySheet = MutableStateFlow(false)
    val showSecuritySheet: StateFlow<Boolean> = _showSecuritySheet.asStateFlow()

    private val _editingBookmark = MutableStateFlow<BookmarkEntity?>(null)
    val editingBookmark: StateFlow<BookmarkEntity?> = _editingBookmark.asStateFlow()

    private val _showBookmarkDialog = MutableStateFlow(false)
    val showBookmarkDialog: StateFlow<Boolean> = _showBookmarkDialog.asStateFlow()

    private val _showAddShortcutDialog = MutableStateFlow(false)
    val showAddShortcutDialog: StateFlow<Boolean> = _showAddShortcutDialog.asStateFlow()

    private val _pendingDownload = MutableStateFlow<PendingDownloadPrompt?>(null)
    val pendingDownload: StateFlow<PendingDownloadPrompt?> = _pendingDownload.asStateFlow()

    private val _pendingPermission = MutableStateFlow<PendingSitePermissionPrompt?>(null)
    val pendingPermission: StateFlow<PendingSitePermissionPrompt?> = _pendingPermission.asStateFlow()

    private val _pendingSslWarning = MutableStateFlow<PendingSslWarning?>(null)
    val pendingSslWarning: StateFlow<PendingSslWarning?> = _pendingSslWarning.asStateFlow()

    private val _pendingSafeBrowsing = MutableStateFlow<PendingSafeBrowsingWarning?>(null)
    val pendingSafeBrowsing: StateFlow<PendingSafeBrowsingWarning?> = _pendingSafeBrowsing.asStateFlow()

    private val _contextLinkAction = MutableStateFlow<ContextLinkAction?>(null)
    val contextLinkAction: StateFlow<ContextLinkAction?> = _contextLinkAction.asStateFlow()

    private val _fullscreenCustomView = MutableStateFlow<View?>(null)
    val fullscreenCustomView: StateFlow<View?> = _fullscreenCustomView.asStateFlow()
    private var fullscreenCustomCallback: WebChromeClient.CustomViewCallback? = null

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private val _translationState = MutableStateFlow(TranslationUiState())
    val translationState: StateFlow<TranslationUiState> = _translationState.asStateFlow()

    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    var onLaunchFileChooserUi: ((WebChromeClient.FileChooserParams?) -> Unit)? = null
    var onRequestRuntimeOsPermissions: ((Array<String>, (Boolean) -> Unit) -> Unit)? = null

    private var suggestionJob: Job? = null
    private var aiJob: Job? = null
    private var translateJob: Job? = null
    private val rendererRecoveredTabs = mutableSetOf<String>()

    private val clientCallbacks: WebClientCallbacks = object : WebClientCallbacks {
        override fun onPageStarted(tabId: String, url: String) {
            updateTab(tabId) {
                it.copy(
                    url = url,
                    isHomepage = false,
                    isLoading = true,
                    progress = 15,
                    hasError = false,
                    errorDescription = "",
                    isSecure = url.startsWith("https://", ignoreCase = true),
                    securityHost = UrlUtils.extractHost(url),
                    faviconUrl = UrlUtils.buildFaviconUrl(url)
                )
            }
        }

        override fun onPageFinished(
            tabId: String,
            url: String,
            title: String,
            isSecure: Boolean,
            issuer: String,
            validTo: String
        ) {
            rendererRecoveredTabs.remove(tabId)
            var shouldRecordHistory = false
            updateTab(tabId) {
                shouldRecordHistory = !it.isIncognito && !it.hasError && settings.value.searchHistoryEnabled
                it.copy(
                    url = url,
                    title = title,
                    isHomepage = false,
                    isLoading = false,
                    progress = 100,
                    isSecure = isSecure,
                    securityHost = UrlUtils.extractHost(url),
                    certificateIssuer = issuer,
                    certificateValidTo = validTo,
                    faviconUrl = UrlUtils.buildFaviconUrl(url)
                )
            }
            if (shouldRecordHistory) {
                viewModelScope.launch {
                    repository.recordVisit(title, url)
                }
            }
            saveNormalTabs()
        }

        override fun onNavigationStateChanged(tabId: String, canGoBack: Boolean, canGoForward: Boolean) {
            updateTab(tabId) { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
        }

        override fun onMainFrameError(tabId: String, errorCode: Int, description: String) {
            updateTab(tabId) {
                it.copy(
                    isLoading = false,
                    progress = 0,
                    hasError = true,
                    errorCode = errorCode,
                    errorDescription = description
                )
            }
        }

        override fun onSslErrorDetected(
            tabId: String,
            url: String,
            description: String,
            handler: SslErrorHandler
        ) {
            _pendingSslWarning.value = PendingSslWarning(url, description, handler)
        }

        override fun onSafeBrowsingBlocked(tabId: String, url: String, reason: String) {
            _pendingSafeBrowsing.value = PendingSafeBrowsingWarning(url, reason)
        }

        override fun onRendererCrashed(tabId: String, didCrash: Boolean) {
            val crashedTab = tabs.value.find { it.id == tabId }
            val currentActiveId = activeTab.value.id
            val isActive = currentActiveId == tabId
            webViewPool.removeTab(tabId)

            if (!didCrash) {
                // OS/LMK killed the renderer under memory pressure: evict background renderers
                webViewPool.trimBackgroundTabs(keepTabId = currentActiveId)
            } else {
                // Toggle software layer fallback once on genuine crash, revert if software layer itself crashed
                webViewPool.setSoftwareLayerFallback(!webViewPool.forceSoftwareLayer)
            }

            if (!isActive) {
                // Background tab will reload lazily when the user switches back to it
                rendererRecoveredTabs.remove(tabId)
                updateTab(tabId) {
                    it.copy(isLoading = false, webViewGeneration = it.webViewGeneration + 1)
                }
                return
            }

            if (crashedTab != null && !crashedTab.isHomepage && crashedTab.url.isNotBlank() && rendererRecoveredTabs.add(tabId)) {
                webViewPool.loadUrl(tabId, crashedTab.url, crashedTab.isIncognito, crashedTab.isDesktopMode, settings.value)
                updateTab(tabId) {
                    it.copy(
                        isLoading = true,
                        progress = 15,
                        hasError = false,
                        webViewGeneration = it.webViewGeneration + 1
                    )
                }
            } else {
                updateTab(tabId) {
                    it.copy(
                        isLoading = false,
                        hasError = true,
                        errorDescription = "This webpage stopped responding. Tap Retry to reload it safely.",
                        webViewGeneration = it.webViewGeneration + 1
                    )
                }
            }
        }

        override fun isSafeBrowsingEnabled(): Boolean = settings.value.safeBrowsingEnabled
        override fun isDoNotTrackEnabled(): Boolean = settings.value.doNotTrackEnabled
    }

    private val chromeCallbacks: WebChromeCallbacks = object : WebChromeCallbacks {
        override fun onProgressChanged(tabId: String, progress: Int) {
            updateTab(tabId) {
                it.copy(
                    progress = progress,
                    isLoading = progress in 1..99
                )
            }
        }

        override fun onTitleReceived(tabId: String, title: String) {
            updateTab(tabId) { it.copy(title = title) }
        }

        override fun onFileChooserRequested(
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: WebChromeClient.FileChooserParams?
        ) {
            fileChooserCallback?.onReceiveValue(null)
            fileChooserCallback = filePathCallback
            onLaunchFileChooserUi?.invoke(fileChooserParams)
        }

        override fun onSitePermissionRequested(tabId: String, request: PermissionRequest) {
            val host = request.origin?.host?.takeIf { it.isNotBlank() }
                ?: UrlUtils.extractHost(tabs.value.find { it.id == tabId }?.url.orEmpty())
            val requestedTypes = mutableListOf<SitePermissionType>()
            if (request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                requestedTypes.add(SitePermissionType.CAMERA)
            }
            if (request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                requestedTypes.add(SitePermissionType.MICROPHONE)
            }
            if (requestedTypes.isEmpty()) {
                request.deny()
                return
            }

            viewModelScope.launch {
                val decisions = requestedTypes.map { type ->
                    val defaultDecision = when (type) {
                        SitePermissionType.CAMERA -> settings.value.defaultCameraDecision
                        SitePermissionType.MICROPHONE -> settings.value.defaultMicDecision
                        else -> PermissionDecision.ASK
                    }
                    type to repository.getPermissionForHost(host, type, defaultDecision)
                }
                when {
                    decisions.any { it.second == PermissionDecision.BLOCK } -> request.deny()
                    decisions.all { it.second == PermissionDecision.ALLOW } -> {
                        ensureOsPermissionsAndGrant(requestedTypes, request)
                    }
                    else -> {
                        _pendingPermission.value = PendingSitePermissionPrompt(
                            host = host.ifBlank { "This website" },
                            types = requestedTypes,
                            webPermissionRequest = request
                        )
                    }
                }
            }
        }

        override fun onGeolocationRequested(
            tabId: String,
            origin: String,
            callback: GeolocationPermissions.Callback
        ) {
            val host = runCatching { Uri.parse(origin).host.orEmpty() }.getOrDefault(origin)
            viewModelScope.launch {
                val decision = repository.getPermissionForHost(
                    host,
                    SitePermissionType.LOCATION,
                    settings.value.defaultLocationDecision
                )
                when (decision) {
                    PermissionDecision.BLOCK -> callback.invoke(origin, false, false)
                    PermissionDecision.ALLOW -> ensureOsLocationAndGrant(origin, callback)
                    PermissionDecision.ASK -> {
                        _pendingPermission.value = PendingSitePermissionPrompt(
                            host = host.ifBlank { origin },
                            types = listOf(SitePermissionType.LOCATION),
                            geoOrigin = origin,
                            geoCallback = callback
                        )
                    }
                }
            }
        }

        override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
            fullscreenCustomCallback?.onCustomViewHidden()
            fullscreenCustomCallback = callback
            _fullscreenCustomView.value = view
        }

        override fun onHideCustomView() {
            fullscreenCustomCallback?.onCustomViewHidden()
            fullscreenCustomCallback = null
            _fullscreenCustomView.value = null
        }

        override fun onPopupRequested(sourceTabId: String, resultMsg: Message) {
            val sourceTab = tabs.value.find { it.id == sourceTabId } ?: activeTab.value
            val host = UrlUtils.extractHost(sourceTab.url)
            viewModelScope.launch {
                val defaultDecision = if (settings.value.blockPopups) PermissionDecision.BLOCK else PermissionDecision.ALLOW
                val decision = if (host.isNotBlank()) {
                    repository.getPermissionForHost(host, SitePermissionType.POPUPS, defaultDecision)
                } else {
                    defaultDecision
                }
                if (decision == PermissionDecision.BLOCK) {
                    showMessage("Pop-up blocked from ${host.ifBlank { "website" }}")
                } else {
                    val (nextState, newTab) = tabStateManager.createTab(
                        state = _tabState.value,
                        url = "",
                        isIncognito = sourceTab.isIncognito,
                        isDesktopMode = sourceTab.isDesktopMode
                    )
                    _tabState.value = nextState.copy(
                        tabs = nextState.tabs.map {
                            if (it.id == newTab.id) it.copy(isHomepage = false) else it
                        }
                    )
                    webViewPool.deliverPopupTransport(newTab.id, newTab.isIncognito, settings.value, resultMsg)
                }
            }
        }
    }

    val webViewPool: TabWebViewPool = TabWebViewPool(
        context = application,
        clientCallbacks = clientCallbacks,
        chromeCallbacks = chromeCallbacks,
        onDownloadRequested = { url, fileName, mimeType, contentLength, userAgent ->
            handleIncomingDownload(url, fileName, mimeType, contentLength, userAgent)
        },
        onFindResults = { activeOrdinal, total ->
            _findInPage.update {
                it.copy(
                    currentMatchIndex = if (total > 0) activeOrdinal + 1 else 0,
                    totalMatches = total
                )
            }
        },
        onContextLinkLongPress = { url, isImage ->
            _contextLinkAction.value = ContextLinkAction(url, isImage)
        }
    )

    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOffline.value = false
        }

        override fun onLost(network: Network) {
            updateOfflineStatus()
        }
    }

    init {
        updateOfflineStatus()
        runCatching { connectivityManager.registerDefaultNetworkCallback(networkCallback) }

        viewModelScope.launch {
            repository.ensureDefaultShortcuts()
            val currentSettings = repository.settings.first()
            if (currentSettings.startupBehavior == StartupBehavior.RESTORE_TABS) {
                val saved = repository.getSavedTabs()
                if (saved.isNotEmpty()) {
                    val restored = saved.map {
                        BrowserTab(
                            id = it.tabId,
                            title = it.title,
                            url = it.url,
                            isHomepage = it.isHomepage,
                            isDesktopMode = it.isDesktopMode,
                            isSecure = it.url.startsWith("https://"),
                            securityHost = UrlUtils.extractHost(it.url),
                            faviconUrl = UrlUtils.buildFaviconUrl(it.url)
                        )
                    }
                    _tabState.value = TabState(
                        tabs = restored,
                        activeTabId = restored.first().id,
                        closedTabs = _tabState.value.closedTabs
                    )
                    val firstTab = restored.first()
                    if (!firstTab.isHomepage && firstTab.url.isNotBlank()) {
                        webViewPool.loadUrl(firstTab.id, firstTab.url, false, firstTab.isDesktopMode, currentSettings)
                    }
                }
            }
            delay(850L)
            _showSplash.value = false
        }

        viewModelScope.launch {
            settings.collect { latest ->
                webViewPool.applySettingsToAll(tabs.value, latest)
            }
        }
    }

    private fun updateOfflineStatus() {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        val connected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _isOffline.value = !connected
    }

    fun handleIncomingIntentUrl(url: String?) {
        if (url.isNullOrBlank()) return
        if (url.startsWith("http://") || url.startsWith("https://")) {
            createNewTab(initialUrl = url, isIncognito = false)
        }
    }

    fun onFileChooserResult(uris: Array<Uri>?) {
        fileChooserCallback?.onReceiveValue(uris)
        fileChooserCallback = null
    }

    fun dismissFullscreenVideo() {
        chromeCallbacks.onHideCustomView()
    }

    fun submitOmniboxInput(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return
        val s = settings.value
        val resolved = UrlUtils.resolveInputToUrl(trimmed, s.searchEngine, s.safeSearchEnabled)
        loadUrlInActiveTab(resolved)
        _suggestions.value = emptyList()
    }

    fun loadUrlInActiveTab(url: String) {
        val tab = activeTab.value
        updateTab(tab.id) {
            it.copy(
                url = url,
                isHomepage = false,
                isLoading = true,
                progress = 10,
                hasError = false,
                isSecure = url.startsWith("https://"),
                securityHost = UrlUtils.extractHost(url),
                faviconUrl = UrlUtils.buildFaviconUrl(url)
            )
        }
        webViewPool.loadUrl(tab.id, url, tab.isIncognito, tab.isDesktopMode, settings.value)
        _activeOverlay.value = BrowserOverlay.None
    }

    fun updateOmniboxSuggestions(query: String) {
        suggestionJob?.cancel()
        if (query.isBlank() || !settings.value.searchSuggestionsEnabled) {
            _suggestions.value = emptyList()
            return
        }
        suggestionJob = viewModelScope.launch {
            delay(160L)
            val localMatches = if (!activeTab.value.isIncognito) {
                repository.searchLocalHistory(query).take(3).map {
                    SearchSuggestionItem(text = it.title, url = it.url, isHistory = true)
                }
            } else {
                emptyList()
            }
            val remote = repository.fetchSearchSuggestions(query).map {
                SearchSuggestionItem(text = it, url = null, isHistory = false)
            }
            _suggestions.value = (localMatches + remote).distinctBy { it.text.lowercase() }.take(7)
        }
    }

    fun clearSuggestions() {
        suggestionJob?.cancel()
        _suggestions.value = emptyList()
    }

    fun createNewTab(initialUrl: String? = null, isIncognito: Boolean = false) {
        val s = settings.value
        val customHome = s.customHomepageUrl
        val shouldOpenCustomHome = initialUrl == null && !isIncognito && customHome.isNotBlank()
        val targetUrl = initialUrl ?: if (shouldOpenCustomHome) customHome else ""
        val (nextState, newTab) = tabStateManager.createTab(
            state = _tabState.value,
            url = targetUrl,
            isIncognito = isIncognito,
            isDesktopMode = s.desktopSiteDefault
        )
        _tabState.value = nextState
        _activeOverlay.value = BrowserOverlay.None
        if (!newTab.isHomepage) {
            webViewPool.loadUrl(newTab.id, targetUrl, isIncognito, newTab.isDesktopMode, s)
        }
        saveNormalTabs()
    }

    fun switchTab(tabId: String) {
        val (nextState, target) = tabStateManager.switchTab(_tabState.value, tabId)
        if (target == null) return
        _tabState.value = nextState
        _activeOverlay.value = BrowserOverlay.None
        if (!target.isHomepage && target.url.isNotBlank() && webViewPool.getExisting(target.id) == null) {
            webViewPool.loadUrl(target.id, target.url, target.isIncognito, target.isDesktopMode, settings.value)
        }
    }

    fun duplicateTab(tabId: String) {
        val (nextState, copy) = tabStateManager.duplicateTab(_tabState.value, tabId)
        if (copy == null) return
        _tabState.value = nextState
        _activeOverlay.value = BrowserOverlay.None
        if (!copy.isHomepage && copy.url.isNotBlank()) {
            webViewPool.loadUrl(copy.id, copy.url, copy.isIncognito, copy.isDesktopMode, settings.value)
        }
        saveNormalTabs()
    }

    fun closeTab(tabId: String) {
        val result = tabStateManager.closeTab(
            state = _tabState.value,
            tabId = tabId,
            defaultDesktopMode = settings.value.desktopSiteDefault
        )
        if (result.closedTab == null) return

        webViewPool.removeTab(tabId)
        if (result.clearedAllIncognito) {
            webViewPool.clearIncognitoSessionData()
        }

        _tabState.value = result.nextState
        val next = result.newActiveTab
        if (!next.isHomepage && next.url.isNotBlank() && webViewPool.getExisting(next.id) == null) {
            webViewPool.loadUrl(next.id, next.url, next.isIncognito, next.isDesktopMode, settings.value)
        }
        saveNormalTabs()
    }

    fun restoreClosedTab() {
        val (nextState, restored) = tabStateManager.restoreClosedTab(
            state = _tabState.value,
            defaultDesktopMode = settings.value.desktopSiteDefault
        )
        if (restored == null) return
        _tabState.value = nextState
        _activeOverlay.value = BrowserOverlay.None
        if (!restored.isHomepage && restored.url.isNotBlank()) {
            webViewPool.loadUrl(restored.id, restored.url, false, restored.isDesktopMode, settings.value)
        }
        saveNormalTabs()
    }

    fun closeAllTabs(incognitoOnly: Boolean) {
        val (nextState, closedList) = tabStateManager.closeAllTabs(
            state = _tabState.value,
            incognitoOnly = incognitoOnly,
            defaultDesktopMode = settings.value.desktopSiteDefault
        )
        closedList.forEach { webViewPool.removeTab(it.id) }
        if (incognitoOnly) {
            webViewPool.clearIncognitoSessionData()
        }
        _tabState.value = nextState
        _activeOverlay.value = BrowserOverlay.None
        val next = nextState.activeTab
        if (!next.isHomepage && next.url.isNotBlank() && webViewPool.getExisting(next.id) == null) {
            webViewPool.loadUrl(next.id, next.url, next.isIncognito, next.isDesktopMode, settings.value)
        }
        saveNormalTabs()
    }

    fun goHome() {
        val tab = activeTab.value
        val customHome = settings.value.customHomepageUrl
        if (!tab.isIncognito && customHome.isNotBlank()) {
            loadUrlInActiveTab(customHome)
        } else {
            updateTab(tab.id) {
                it.copy(
                    isHomepage = true,
                    isLoading = false,
                    hasError = false,
                    title = if (tab.isIncognito) "Private Tab" else "New Tab"
                )
            }
        }
        closeFindInPage()
    }

    fun goBack(): Boolean {
        if (_fullscreenCustomView.value != null) {
            dismissFullscreenVideo()
            return true
        }
        if (_findInPage.value.isVisible) {
            closeFindInPage()
            return true
        }
        if (_activeOverlay.value != BrowserOverlay.None) {
            _activeOverlay.value = BrowserOverlay.None
            return true
        }
        val tab = activeTab.value
        if (tab.isHomepage) return false
        val wentBack = webViewPool.goBack(tab.id)
        if (!wentBack) {
            updateTab(tab.id) {
                it.copy(
                    isHomepage = true,
                    canGoForward = it.url.isNotBlank(),
                    title = if (it.isIncognito) "Private Tab" else "New Tab"
                )
            }
        }
        return true
    }

    fun goForward() {
        val tab = activeTab.value
        if (tab.isHomepage && tab.url.isNotBlank()) {
            updateTab(tab.id) { it.copy(isHomepage = false) }
            if (webViewPool.getExisting(tab.id) == null) {
                webViewPool.loadUrl(tab.id, tab.url, tab.isIncognito, tab.isDesktopMode, settings.value)
            }
        } else {
            webViewPool.goForward(tab.id)
        }
    }

    fun reloadOrStop() {
        val tab = activeTab.value
        if (tab.isHomepage) return
        if (tab.isLoading) {
            webViewPool.stopLoading(tab.id)
            updateTab(tab.id) { it.copy(isLoading = false, progress = 0) }
        } else {
            updateOfflineStatus()
            rendererRecoveredTabs.remove(tab.id)
            val missingWebView = webViewPool.getExisting(tab.id) == null
            if (missingWebView && tab.url.isNotBlank()) {
                webViewPool.loadUrl(tab.id, tab.url, tab.isIncognito, tab.isDesktopMode, settings.value)
                updateTab(tab.id) {
                    it.copy(
                        hasError = false,
                        isLoading = true,
                        progress = 15,
                        webViewGeneration = it.webViewGeneration + 1
                    )
                }
            } else {
                updateTab(tab.id) { it.copy(hasError = false, isLoading = true, progress = 15) }
                webViewPool.reload(tab.id)
            }
        }
    }

    fun toggleDesktopModeForActiveTab() {
        val tab = activeTab.value
        val next = !tab.isDesktopMode
        updateTab(tab.id) { it.copy(isDesktopMode = next) }
        if (!tab.isHomepage && tab.url.isNotBlank()) {
            webViewPool.loadUrl(tab.id, tab.url, tab.isIncognito, next, settings.value)
        }
        showMessage(if (next) "Desktop site enabled" else "Mobile site enabled")
    }

    fun toggleBookmarkForActiveTab() {
        val tab = activeTab.value
        if (tab.isHomepage || tab.url.isBlank()) {
            showMessage("Open a webpage first to bookmark it")
            return
        }
        if (tab.isIncognito) {
            // Private mode doesn't auto-save, open confirmation dialog if user explicitly wants to save
            _editingBookmark.value = BookmarkEntity(
                title = tab.title,
                url = tab.url,
                folder = "Bookmarks"
            )
            _showBookmarkDialog.value = true
            return
        }
        viewModelScope.launch {
            val added = repository.toggleBookmark(tab.title, tab.url)
            showMessage(if (added) "Bookmark saved" else "Bookmark removed")
        }
    }

    fun openAddBookmarkDialog(bookmark: BookmarkEntity? = null) {
        val tab = activeTab.value
        _editingBookmark.value = bookmark ?: BookmarkEntity(
            title = if (!tab.isHomepage) tab.title else "",
            url = if (!tab.isHomepage) tab.url else "https://",
            folder = "Bookmarks"
        )
        _showBookmarkDialog.value = true
    }

    fun closeBookmarkDialog() {
        _showBookmarkDialog.value = false
        _editingBookmark.value = null
    }

    fun saveBookmark(id: Long, title: String, url: String, folder: String) {
        val cleanUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        viewModelScope.launch {
            repository.addOrUpdateBookmark(
                BookmarkEntity(id = id, title = title.trim(), url = cleanUrl.trim(), folder = folder.ifBlank { "Bookmarks" })
            )
            closeBookmarkDialog()
            showMessage("Bookmark saved")
        }
    }

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmark)
        }
    }

    fun openAddShortcutDialog() { _showAddShortcutDialog.value = true }
    fun closeAddShortcutDialog() { _showAddShortcutDialog.value = false }
    fun addShortcut(title: String, url: String) {
        viewModelScope.launch {
            repository.addShortcut(title, url)
            _showAddShortcutDialog.value = false
            showMessage("Shortcut added to homepage")
        }
    }
    fun deleteShortcut(id: Long) {
        viewModelScope.launch { repository.deleteShortcut(id) }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch { repository.deleteHistoryItem(id) }
    }

    fun clearHistoryByPeriod(hoursBack: Int?) {
        val since = hoursBack?.let { System.currentTimeMillis() - it * 3_600_000L }
        viewModelScope.launch {
            repository.clearHistorySince(since)
            showMessage("Browsing history cleared")
        }
    }

    fun setOverlay(overlay: BrowserOverlay) {
        _activeOverlay.value = overlay
    }

    fun setSecuritySheetVisible(visible: Boolean) {
        _showSecuritySheet.value = visible
    }

    // Find in Page
    fun openFindInPage() {
        _activeOverlay.value = BrowserOverlay.None
        _findInPage.value = FindInPageState(isVisible = true)
    }

    fun updateFindQuery(query: String) {
        _findInPage.update { it.copy(query = query) }
        webViewPool.findAll(activeTab.value.id, query)
    }

    fun findNext(forward: Boolean) {
        webViewPool.findNext(activeTab.value.id, forward)
    }

    fun closeFindInPage() {
        webViewPool.clearFind(activeTab.value.id)
        _findInPage.value = FindInPageState(isVisible = false)
    }

    // Downloads
    private fun handleIncomingDownload(
        url: String,
        fileName: String,
        mimeType: String,
        contentLength: Long,
        userAgent: String
    ) {
        val host = UrlUtils.extractHost(url)
        viewModelScope.launch {
            val hostPolicy = if (host.isNotBlank()) {
                repository.getPermissionForHost(host, SitePermissionType.DOWNLOADS, PermissionDecision.ALLOW)
            } else PermissionDecision.ALLOW

            if (hostPolicy == PermissionDecision.BLOCK) {
                showMessage("Download blocked by site permission for $host")
                return@launch
            }

            if (settings.value.askBeforeDownloading || hostPolicy == PermissionDecision.ASK) {
                _pendingDownload.value = PendingDownloadPrompt(
                    url = url,
                    fileName = fileName,
                    mimeType = mimeType,
                    contentLength = contentLength,
                    userAgent = userAgent
                )
            } else {
                startConfirmedDownload(url, fileName, mimeType, userAgent)
            }
        }
    }

    fun confirmPendingDownload() {
        val prompt = _pendingDownload.value ?: return
        _pendingDownload.value = null
        startConfirmedDownload(prompt.url, prompt.fileName, prompt.mimeType, prompt.userAgent)
    }

    fun dismissPendingDownload() {
        _pendingDownload.value = null
    }

    fun startConfirmedDownload(url: String, fileName: String, mimeType: String, userAgent: String = "") {
        val cookies = CookieManager.getInstance().getCookie(url)
        downloadEngine.startDownload(
            url = url,
            fileName = fileName,
            mimeType = mimeType,
            userAgent = userAgent,
            cookies = cookies,
            notifyOnComplete = settings.value.downloadNotificationsEnabled
        )
        showMessage("Downloading $fileName")
    }

    fun dismissContextLinkAction() { _contextLinkAction.value = null }

    // Site permissions
    fun resolvePendingSitePermission(allow: Boolean, remember: Boolean) {
        val prompt = _pendingPermission.value ?: return
        _pendingPermission.value = null

        if (remember && prompt.host.isNotBlank() && prompt.host != "This website") {
            val decision = if (allow) PermissionDecision.ALLOW else PermissionDecision.BLOCK
            viewModelScope.launch {
                prompt.types.forEach { type ->
                    repository.setPermissionForHost(prompt.host, type, decision)
                }
            }
        }

        if (!allow) {
            prompt.webPermissionRequest?.deny()
            prompt.geoOrigin?.let { prompt.geoCallback?.invoke(it, false, false) }
            return
        }

        prompt.webPermissionRequest?.let { req ->
            ensureOsPermissionsAndGrant(prompt.types, req)
        }
        if (prompt.geoOrigin != null && prompt.geoCallback != null) {
            ensureOsLocationAndGrant(prompt.geoOrigin, prompt.geoCallback)
        }
    }

    private fun ensureOsPermissionsAndGrant(types: List<SitePermissionType>, request: PermissionRequest) {
        val androidPerms = mutableListOf<String>()
        if (types.contains(SitePermissionType.CAMERA)) androidPerms.add(android.Manifest.permission.CAMERA)
        if (types.contains(SitePermissionType.MICROPHONE)) androidPerms.add(android.Manifest.permission.RECORD_AUDIO)

        val callback = onRequestRuntimeOsPermissions
        if (androidPerms.isNotEmpty() && callback != null) {
            callback(androidPerms.toTypedArray()) { granted ->
                if (granted) request.grant(request.resources) else request.deny()
            }
        } else {
            request.grant(request.resources)
        }
    }

    private fun ensureOsLocationAndGrant(origin: String, callback: GeolocationPermissions.Callback) {
        val launcher = onRequestRuntimeOsPermissions
        if (launcher != null) {
            launcher(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION)) { granted ->
                callback.invoke(origin, granted, false)
            }
        } else {
            callback.invoke(origin, true, false)
        }
    }

    fun updateSitePermission(host: String, type: SitePermissionType, decision: PermissionDecision) {
        viewModelScope.launch {
            repository.setPermissionForHost(host, type, decision)
        }
    }

    fun removeSitePermissions(host: String) {
        viewModelScope.launch { repository.deleteSitePermissions(host) }
    }

    // SSL & Safe Browsing Warnings
    fun resolveSslWarning(proceed: Boolean) {
        val warning = _pendingSslWarning.value ?: return
        _pendingSslWarning.value = null
        if (proceed) warning.handler.proceed() else warning.handler.cancel()
    }

    fun resolveSafeBrowsingWarning(proceed: Boolean) {
        val warning = _pendingSafeBrowsing.value ?: return
        _pendingSafeBrowsing.value = null
        if (proceed) {
            val tab = activeTab.value
            webViewPool.getClient(tab.id)?.allowUnsafeUrlOnce(warning.url)
            loadUrlInActiveTab(warning.url)
        }
    }

    // Privacy Clearing
    fun clearCache() {
        webViewPool.clearAllCache()
        showMessage("Browser cache cleared")
    }

    fun clearCookies() {
        webViewPool.clearCookies()
        showMessage("Cookies cleared")
    }

    fun clearWebsiteData() {
        webViewPool.clearWebsiteData()
        showMessage("Website storage and form data cleared")
    }

    // Settings updates
    fun updateThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsStore.updateThemeMode(mode) }
    fun updateSearchEngine(engine: SearchEngine) = viewModelScope.launch { settingsStore.updateSearchEngine(engine) }
    fun updateSearchSuggestions(enabled: Boolean) = viewModelScope.launch { settingsStore.updateSearchSuggestions(enabled) }
    fun updateSearchHistory(enabled: Boolean) = viewModelScope.launch { settingsStore.updateSearchHistory(enabled) }
    fun updateSafeSearch(enabled: Boolean) = viewModelScope.launch { settingsStore.updateSafeSearch(enabled) }
    fun updateWallpaperPreset(preset: WallpaperPreset) = viewModelScope.launch { settingsStore.updateWallpaperPreset(preset) }
    fun setCustomWallpaperUri(uri: Uri) {
        viewModelScope.launch {
            val path = repository.saveCustomWallpaperFromUri(uri)
            if (path != null) {
                settingsStore.updateCustomWallpaperPath(path)
                showMessage("Custom wallpaper updated")
            } else {
                showMessage("Could not load selected image")
            }
        }
    }
    fun updateHomepageVisibility(clock: Boolean? = null, popular: Boolean? = null, recent: Boolean? = null) =
        viewModelScope.launch { settingsStore.updateHomepageVisibility(clock, popular, recent) }
    fun updateToolbarLayout(layout: ToolbarLayout) = viewModelScope.launch { settingsStore.updateToolbarLayout(layout) }
    fun updateWebFontSize(percent: Int) = viewModelScope.launch { settingsStore.updateWebFontSize(percent) }
    fun updateDesktopDefault(enabled: Boolean) = viewModelScope.launch { settingsStore.updateDesktopDefault(enabled) }
    fun updateFullscreenBrowsing(enabled: Boolean) = viewModelScope.launch { settingsStore.updateFullscreenBrowsing(enabled) }
    fun updateStartupBehavior(behavior: StartupBehavior) = viewModelScope.launch { settingsStore.updateStartupBehavior(behavior) }
    fun updateCustomHomepageUrl(url: String) = viewModelScope.launch { settingsStore.updateCustomHomepageUrl(url) }
    fun updateAppLanguage(language: String) = viewModelScope.launch { settingsStore.updateAppLanguage(language) }
    fun updateBlockPopups(enabled: Boolean) = viewModelScope.launch { settingsStore.updateBlockPopups(enabled) }
    fun updateSafeBrowsing(enabled: Boolean) = viewModelScope.launch { settingsStore.updateSafeBrowsing(enabled) }
    fun updateDoNotTrack(enabled: Boolean) = viewModelScope.launch { settingsStore.updateDoNotTrack(enabled) }
    fun updateDownloadNotifications(enabled: Boolean) = viewModelScope.launch { settingsStore.updateDownloadNotifications(enabled) }
    fun updateAskBeforeDownloading(enabled: Boolean) = viewModelScope.launch { settingsStore.updateAskBeforeDownloading(enabled) }
    fun updateDefaultSitePermission(type: SitePermissionType, decision: PermissionDecision) =
        viewModelScope.launch { settingsStore.updateDefaultPermission(type, decision) }

    // DSK AI Search
    fun sendAiPrompt(prompt: String, mode: AiSearchMode) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty() && mode != AiSearchMode.SUMMARIZE_PAGE) return

        val tab = activeTab.value
        val needsPageContext = mode == AiSearchMode.SUMMARIZE_PAGE || (!tab.isHomepage && cleanPrompt.contains("page", ignoreCase = true))

        if (needsPageContext && !tab.isHomepage) {
            webViewPool.extractPageText(tab.id) { pageText ->
                executeAiStream(
                    userDisplayPrompt = cleanPrompt.ifBlank { "Summarize ${tab.title}" },
                    actualPrompt = cleanPrompt.ifBlank { "Please summarize this webpage." },
                    mode = mode,
                    pageTitle = tab.title,
                    pageUrl = tab.url,
                    pageText = pageText
                )
            }
        } else {
            executeAiStream(
                userDisplayPrompt = cleanPrompt,
                actualPrompt = cleanPrompt,
                mode = mode,
                pageTitle = null,
                pageUrl = null,
                pageText = null
            )
        }
    }

    private fun executeAiStream(
        userDisplayPrompt: String,
        actualPrompt: String,
        mode: AiSearchMode,
        pageTitle: String?,
        pageUrl: String?,
        pageText: String?
    ) {
        aiJob?.cancel()
        val userMsg = AiChatMessage(isUser = true, text = userDisplayPrompt, modeLabel = mode.label)
        val assistantMsgId = java.util.UUID.randomUUID().toString()
        val placeholderAssistant = AiChatMessage(
            id = assistantMsgId,
            isUser = false,
            text = "",
            modeLabel = mode.label,
            isStreaming = true
        )
        _aiMessages.update { it + userMsg + placeholderAssistant }

        aiJob = viewModelScope.launch {
            aiRepository.streamPrompt(
                prompt = actualPrompt,
                mode = mode,
                pageContextTitle = pageTitle,
                pageContextUrl = pageUrl,
                pageContextText = pageText
            ).collect { event ->
                when (event) {
                    AiStreamEvent.Loading -> Unit
                    is AiStreamEvent.Streaming -> {
                        _aiMessages.update { list ->
                            list.map {
                                if (it.id == assistantMsgId) it.copy(text = event.text, isStreaming = true) else it
                            }
                        }
                    }
                    is AiStreamEvent.Completed -> {
                        _aiMessages.update { list ->
                            list.map {
                                if (it.id == assistantMsgId) it.copy(text = event.text, isStreaming = false) else it
                            }
                        }
                    }
                    is AiStreamEvent.Error -> {
                        _aiMessages.update { list ->
                            list.map {
                                if (it.id == assistantMsgId) {
                                    it.copy(text = event.message, isError = true, isStreaming = false)
                                } else it
                            }
                        }
                    }
                }
            }
        }
    }

    fun clearAiChat() {
        aiJob?.cancel()
        _aiMessages.value = emptyList()
    }

    // Translate Page
    fun openTranslateSheet() {
        val tab = activeTab.value
        _activeOverlay.value = BrowserOverlay.Translate
        if (!tab.isHomepage && tab.url.isNotBlank()) {
            translateCurrentPage(_translationState.value.targetLanguage)
        } else {
            _translationState.update {
                it.copy(
                    isTranslating = false,
                    translatedText = "",
                    errorMessage = "Open a webpage first to translate its contents."
                )
            }
        }
    }

    fun translateCurrentPage(targetLanguage: String) {
        val tab = activeTab.value
        if (tab.isHomepage || tab.url.isBlank()) {
            _translationState.update {
                it.copy(
                    targetLanguage = targetLanguage,
                    isTranslating = false,
                    errorMessage = "Open a webpage first to translate it."
                )
            }
            return
        }

        translateJob?.cancel()
        _translationState.value = TranslationUiState(
            targetLanguage = targetLanguage,
            isTranslating = true,
            translatedText = "",
            errorMessage = null,
            sourceTitle = tab.title,
            sourceUrl = tab.url
        )

        webViewPool.extractPageText(tab.id) { extracted ->
            translateJob = viewModelScope.launch {
                aiRepository.streamWebpageTranslation(
                    pageTitle = tab.title,
                    pageUrl = tab.url,
                    pageText = extracted,
                    targetLanguage = targetLanguage
                ).collect { event ->
                    when (event) {
                        AiStreamEvent.Loading -> {
                            _translationState.update { it.copy(isTranslating = true, errorMessage = null) }
                        }
                        is AiStreamEvent.Streaming -> {
                            _translationState.update { it.copy(isTranslating = true, translatedText = event.text) }
                        }
                        is AiStreamEvent.Completed -> {
                            _translationState.update { it.copy(isTranslating = false, translatedText = event.text) }
                        }
                        is AiStreamEvent.Error -> {
                            _translationState.update { it.copy(isTranslating = false, errorMessage = event.message) }
                        }
                    }
                }
            }
        }
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    private fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _tabState.update { state -> tabStateManager.updateTab(state, tabId, transform) }
    }

    private fun saveNormalTabs() {
        viewModelScope.launch {
            val normalTabs = _tabState.value.tabs
                .filter { !it.isIncognito }
                .mapIndexed { index, tab ->
                    SavedTabEntity(
                        tabId = tab.id,
                        title = tab.title,
                        url = tab.url,
                        isHomepage = tab.isHomepage,
                        isDesktopMode = tab.isDesktopMode,
                        sortOrder = index
                    )
                }
            repository.persistTabs(normalTabs)
        }
    }

    fun onTrimMemory() {
        webViewPool.trimBackgroundTabs(keepTabId = activeTab.value.id)
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        webViewPool.destroyAll()
    }
}
