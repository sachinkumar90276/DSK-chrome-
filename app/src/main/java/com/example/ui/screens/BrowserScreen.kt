package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.browser.BrowserOverlay
import com.example.browser.BrowserViewModel
import com.example.browser.UrlUtils
import com.example.data.ThemeMode
import com.example.data.ToolbarLayout
import com.example.data.WallpaperEntity
import com.example.data.WallpaperPreset
import com.example.wallpaper.BrowserWallpaperBackdrop

@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onVoiceSearch: () -> Unit,
    onUploadWallpaper: () -> Unit,
    onPickVideoWallpaper: () -> Unit,
    onLaunchAndroidLiveWallpaperChooser: (WallpaperEntity) -> Unit,
    onAddToHomeScreen: (title: String, url: String) -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isActiveTabBookmarked.collectAsStateWithLifecycle()
    val closedTabs by viewModel.closedTabs.collectAsStateWithLifecycle()
    val activeOverlay by viewModel.activeOverlay.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()
    val recentHistory by viewModel.recentHistory.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val sitePermissions by viewModel.sitePermissions.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val findInPage by viewModel.findInPage.collectAsStateWithLifecycle()
    val showSecuritySheet by viewModel.showSecuritySheet.collectAsStateWithLifecycle()
    val showBookmarkDialog by viewModel.showBookmarkDialog.collectAsStateWithLifecycle()
    val editingBookmark by viewModel.editingBookmark.collectAsStateWithLifecycle()
    val showAddShortcutDialog by viewModel.showAddShortcutDialog.collectAsStateWithLifecycle()
    val pendingDownload by viewModel.pendingDownload.collectAsStateWithLifecycle()
    val pendingPermission by viewModel.pendingPermission.collectAsStateWithLifecycle()
    val pendingSslWarning by viewModel.pendingSslWarning.collectAsStateWithLifecycle()
    val pendingSafeBrowsing by viewModel.pendingSafeBrowsing.collectAsStateWithLifecycle()
    val contextLinkAction by viewModel.contextLinkAction.collectAsStateWithLifecycle()
    val fullscreenView by viewModel.fullscreenCustomView.collectAsStateWithLifecycle()
    val showSplash by viewModel.showSplash.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val translationState by viewModel.translationState.collectAsStateWithLifecycle()
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val favoriteWallpapers by viewModel.favoriteWallpapers.collectAsStateWithLifecycle()
    val recentWallpapers by viewModel.recentWallpapers.collectAsStateWithLifecycle()
    val activeBrowserWallpaper by viewModel.activeBrowserWallpaper.collectAsStateWithLifecycle()
    val activeNewTabWallpaper by viewModel.activeNewTabWallpaper.collectAsStateWithLifecycle()
    val aiWallpaperGenState by viewModel.aiWallpaperGenState.collectAsStateWithLifecycle()
    val isSystemLiveWallpaperActive by viewModel.isSystemLiveWallpaperActive.collectAsStateWithLifecycle()

    val systemDark = isSystemInDarkTheme()
    val useDarkUi = activeTab.isIncognito || settings.themeMode == ThemeMode.DARK ||
        (settings.themeMode == ThemeMode.SYSTEM && systemDark)

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearMessage()
    }

    val shouldInterceptBack = activeOverlay != BrowserOverlay.None ||
        findInPage.isVisible ||
        fullscreenView != null ||
        !activeTab.isHomepage

    BackHandler(enabled = shouldInterceptBack) {
        viewModel.goBack()
    }

    val currentModeTabs = remember(tabs, activeTab.isIncognito) {
        tabs.filter { it.isIncognito == activeTab.isIncognito }
    }
    val currentModeTabCount = currentModeTabs.size
    val isSplitLayout = settings.toolbarLayout == ToolbarLayout.STANDARD
    val isChromeTop = settings.toolbarLayout == ToolbarLayout.TOP_BAR
    val isChromeBottom = settings.toolbarLayout == ToolbarLayout.BOTTOM_FOCUS
    val showBars = !settings.fullscreenBrowsing

    val showChromeTopWebBar = !findInPage.isVisible && showBars && !activeTab.isHomepage && isChromeTop
    val showChromeBottomBar = !findInPage.isVisible && showBars && isChromeBottom
    val showSplitTopAddressBar = !findInPage.isVisible && showBars && !activeTab.isHomepage && isSplitLayout
    val showSplitBottomNavBar = (showBars || activeTab.isHomepage) && isSplitLayout

    val showTabStrip = !findInPage.isVisible && showBars && currentModeTabs.size > 1
    val showTopTabStrip = showTabStrip && !isChromeBottom
    val showBottomTabStrip = showTabStrip && isChromeBottom

    val hasTopBarContent = findInPage.isVisible || showChromeTopWebBar || showSplitTopAddressBar || showTopTabStrip
    val hasBottomBarContent = showChromeBottomBar || showSplitBottomNavBar || showBottomTabStrip

    val backdropWallpaper = if (activeTab.isHomepage) activeNewTabWallpaper else activeBrowserWallpaper
    val shouldRenderWallpaper = activeTab.isHomepage ||
        backdropWallpaper != null ||
        (settings.wallpaperPreset == WallpaperPreset.CUSTOM && settings.customWallpaperPath.isNotBlank())

    Box(modifier = Modifier.fillMaxSize()) {
        if (shouldRenderWallpaper) {
            BrowserWallpaperBackdrop(
                wallpaper = backdropWallpaper,
                settings = settings,
                isIncognito = activeTab.isIncognito,
                useDarkUi = useDarkUi,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.navigationBarsPadding()
                )
            },
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (findInPage.isVisible) {
                        FindInPageBar(
                            state = findInPage,
                            onQueryChange = viewModel::updateFindQuery,
                            onPrevious = { viewModel.findNext(false) },
                            onNext = { viewModel.findNext(true) },
                            onClose = viewModel::closeFindInPage
                        )
                    } else {
                        if (showChromeTopWebBar) {
                            ChromeUnifiedBar(
                                viewModel = viewModel,
                                tab = activeTab,
                                searchEngine = settings.searchEngine,
                                suggestions = suggestions,
                                tabCount = currentModeTabCount,
                                isAtBottom = false,
                                onQueryChange = viewModel::updateOmniboxSuggestions,
                                onSubmitUrl = viewModel::submitOmniboxInput,
                                onClearSuggestions = viewModel::clearSuggestions,
                                onOpenSecurityInfo = { viewModel.setSecuritySheetVisible(true) },
                                onVoiceSearch = onVoiceSearch,
                                onCopyUrl = { url ->
                                    copyToClipboard(context, url)
                                    viewModel.showMessage("URL copied to clipboard")
                                },
                                uiTransparency = settings.uiTransparency,
                                glassEffectEnabled = settings.glassEffectEnabled
                            )
                        } else if (showSplitTopAddressBar) {
                            BrowserAddressBar(
                                tab = activeTab,
                                searchEngine = settings.searchEngine,
                                isBookmarked = isBookmarked,
                                suggestions = suggestions,
                                applyTopStatusInset = true,
                                onQueryChange = viewModel::updateOmniboxSuggestions,
                                onSubmitUrl = viewModel::submitOmniboxInput,
                                onClearSuggestions = viewModel::clearSuggestions,
                                onReloadOrStop = viewModel::reloadOrStop,
                                onToggleBookmark = viewModel::toggleBookmarkForActiveTab,
                                onOpenSecurityInfo = { viewModel.setSecuritySheetVisible(true) },
                                onVoiceSearch = onVoiceSearch,
                                onCopyUrl = { url ->
                                    copyToClipboard(context, url)
                                    viewModel.showMessage("URL copied to clipboard")
                                },
                                uiTransparency = settings.uiTransparency,
                                glassEffectEnabled = settings.glassEffectEnabled
                            )
                        }

                        if (showTopTabStrip) {
                            BrowserTabStrip(
                                tabs = currentModeTabs,
                                activeTabId = activeTab.id,
                                applyTopStatusInset = !showChromeTopWebBar && !showSplitTopAddressBar,
                                onSelectTab = viewModel::switchTab,
                                onCloseTab = viewModel::closeTab,
                                onNewTab = { viewModel.createNewTab(isIncognito = activeTab.isIncognito) },
                                uiTransparency = settings.uiTransparency,
                                glassEffectEnabled = settings.glassEffectEnabled
                            )
                        }
                    }

                    // Offline Banner
                    AnimatedVisibility(visible = isOffline && !activeTab.isHomepage) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (!hasTopBarContent) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "No internet connection",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                ) {
                    if (showBottomTabStrip) {
                        BrowserTabStrip(
                            tabs = currentModeTabs,
                            activeTabId = activeTab.id,
                            applyTopStatusInset = false,
                            onSelectTab = viewModel::switchTab,
                            onCloseTab = viewModel::closeTab,
                            onNewTab = { viewModel.createNewTab(isIncognito = activeTab.isIncognito) },
                            uiTransparency = settings.uiTransparency,
                            glassEffectEnabled = settings.glassEffectEnabled
                        )
                    }

                    if (showChromeBottomBar) {
                        ChromeUnifiedBar(
                            viewModel = viewModel,
                            tab = activeTab,
                            searchEngine = settings.searchEngine,
                            suggestions = suggestions,
                            tabCount = currentModeTabCount,
                            isAtBottom = true,
                            onQueryChange = viewModel::updateOmniboxSuggestions,
                            onSubmitUrl = viewModel::submitOmniboxInput,
                            onClearSuggestions = viewModel::clearSuggestions,
                            onOpenSecurityInfo = { viewModel.setSecuritySheetVisible(true) },
                            onVoiceSearch = onVoiceSearch,
                            onCopyUrl = { url ->
                                copyToClipboard(context, url)
                                viewModel.showMessage("URL copied to clipboard")
                            },
                            uiTransparency = settings.uiTransparency,
                            glassEffectEnabled = settings.glassEffectEnabled
                        )
                    }

                    if (showSplitBottomNavBar) {
                        BrowserBottomNavBar(viewModel = viewModel)
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(if (!hasBottomBarContent) Modifier.navigationBarsPadding() else Modifier)
            ) {
                if (activeTab.isHomepage) {
                    HomeScreen(
                        isIncognito = activeTab.isIncognito,
                        settings = settings,
                        shortcuts = shortcuts,
                        recentHistory = recentHistory,
                        suggestions = suggestions,
                        onQueryChange = viewModel::updateOmniboxSuggestions,
                        onSubmitSearchOrUrl = viewModel::submitOmniboxInput,
                        onClearSuggestions = viewModel::clearSuggestions,
                        onVoiceSearch = onVoiceSearch,
                        onOpenSettings = { viewModel.setOverlay(BrowserOverlay.Settings) },
                        onOpenAiSearch = { viewModel.setOverlay(BrowserOverlay.AiSearch) },
                        onOpenAddShortcut = viewModel::openAddShortcutDialog,
                        onDeleteShortcut = viewModel::deleteShortcut,
                        onOpenUrl = viewModel::loadUrlInActiveTab,
                        tabCount = currentModeTabCount,
                        showChromeTopBar = isChromeTop,
                        onOpenTabs = { viewModel.setOverlay(BrowserOverlay.Tabs) },
                        onOpenMenu = { viewModel.setOverlay(BrowserOverlay.Menu) },
                        onOpenNewIncognitoTab = { viewModel.createNewTab(isIncognito = true) },
                        applyTopStatusInset = !hasTopBarContent
                    )
                } else {
                    // Live WebView Surface keyed by active tab id & generation so crash recovery mounts cleanly
                    key(activeTab.id, activeTab.webViewGeneration) {
                        val currentTabId = activeTab.id
                        AndroidView(
                            factory = {
                                FrameLayout(it).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            update = { container ->
                                val wv = viewModel.webViewPool.getOrCreate(
                                    tabId = currentTabId,
                                    isIncognito = activeTab.isIncognito,
                                    isDesktopMode = activeTab.isDesktopMode,
                                    settings = settings,
                                    activityContext = container.context
                                )
                                if (wv.parent !== container) {
                                    (wv.parent as? ViewGroup)?.removeView(wv)
                                    container.removeAllViews()
                                    container.addView(
                                        wv,
                                        FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    )
                                }
                                viewModel.webViewPool.flushPendingLoadIfAttached(currentTabId, wv)
                            },
                            onRelease = { container ->
                                container.removeAllViews()
                                viewModel.webViewPool.detachActivityContext(currentTabId)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (!hasTopBarContent && !(isOffline && !activeTab.isHomepage)) {
                                        Modifier.windowInsetsPadding(WindowInsets.statusBars)
                                    } else Modifier
                                )
                                .testTag("browser_webview_container")
                        )
                    }

                    // Webpage Error / Offline Screen with Retry Button
                    if (activeTab.hasError) {
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(60.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (isOffline) "You're Offline" else "This Page Can't Be Reached",
                                    style = MaterialTheme.typography.headlineMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isOffline) {
                                        "Check your mobile data or Wi-Fi connection and tap Retry."
                                    } else {
                                        activeTab.errorDescription.ifBlank { "Unable to load ${activeTab.url}" }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(22.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = viewModel::reloadOrStop,
                                        modifier = Modifier.testTag("error_retry_button")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry")
                                    }
                                    Button(onClick = viewModel::goHome) {
                                        Text("Home")
                                    }
                                }
                            }
                        }
                    }

                    // Fullscreen Browsing Exit Pill
                    if (settings.fullscreenBrowsing) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .navigationBarsPadding()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.updateFullscreenBrowsing(false) }
                                .testTag("exit_fullscreen_pill")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "Exit full-screen browsing",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Show Bars", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // Fullscreen HTML5 Video Container
        fullscreenView?.let { videoView ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            (videoView.parent as? ViewGroup)?.removeView(videoView)
                            addView(
                                videoView,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Overlays (Tabs, Menu, Bookmarks, History, Downloads, Settings, Permissions, AI Search, Translate, Wallpaper Studio, About)
        AnimatedVisibility(
            visible = activeOverlay != BrowserOverlay.None,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 16 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 16 })
        ) {
            when (val overlay = activeOverlay) {
                BrowserOverlay.Tabs -> TabOverviewScreen(
                    tabs = tabs,
                    activeTabId = activeTab.id,
                    closedTabs = closedTabs,
                    onSelectTab = viewModel::switchTab,
                    onCloseTab = viewModel::closeTab,
                    onDuplicateTab = viewModel::duplicateTab,
                    onNewTab = { incognito -> viewModel.createNewTab(isIncognito = incognito) },
                    onRestoreClosedTab = viewModel::restoreClosedTab,
                    onCloseAllTabs = viewModel::closeAllTabs,
                    onBack = { viewModel.setOverlay(BrowserOverlay.None) }
                )

                BrowserOverlay.Menu -> BrowserMenuSheet(
                    isHomepage = activeTab.isHomepage,
                    isLoading = activeTab.isLoading,
                    canGoBack = activeTab.canGoBack || !activeTab.isHomepage,
                    canGoForward = activeTab.canGoForward,
                    isBookmarked = isBookmarked,
                    isDesktopMode = activeTab.isDesktopMode,
                    onDismiss = { viewModel.setOverlay(BrowserOverlay.None) },
                    onBack = { viewModel.goBack() },
                    onForward = viewModel::goForward,
                    onReloadOrStop = viewModel::reloadOrStop,
                    onToggleBookmark = viewModel::toggleBookmarkForActiveTab,
                    onOpenSecurityInfo = { viewModel.setSecuritySheetVisible(true) },
                    onNewTab = { viewModel.createNewTab(isIncognito = false) },
                    onNewIncognitoTab = { viewModel.createNewTab(isIncognito = true) },
                    onOpenBookmarks = { viewModel.setOverlay(BrowserOverlay.Bookmarks) },
                    onOpenHistory = { viewModel.setOverlay(BrowserOverlay.History) },
                    onOpenDownloads = { viewModel.setOverlay(BrowserOverlay.Downloads) },
                    onOpenAiSearch = { viewModel.setOverlay(BrowserOverlay.AiSearch) },
                    onShare = {
                        viewModel.setOverlay(BrowserOverlay.None)
                        shareLink(context, activeTab.title, activeTab.url)
                    },
                    onFindInPage = viewModel::openFindInPage,
                    onToggleDesktopSite = {
                        viewModel.toggleDesktopModeForActiveTab()
                        viewModel.setOverlay(BrowserOverlay.None)
                    },
                    onTranslatePage = viewModel::openTranslateSheet,
                    onAddToHomeScreen = {
                        viewModel.setOverlay(BrowserOverlay.None)
                        onAddToHomeScreen(activeTab.title, activeTab.url)
                    },
                    onPrintPage = {
                        viewModel.setOverlay(BrowserOverlay.None)
                        val started = viewModel.webViewPool.printPage(context, activeTab.id, activeTab.title)
                        if (!started) viewModel.showMessage("Open a webpage first to print")
                    },
                    onOpenWallpaperStudio = { viewModel.setOverlay(BrowserOverlay.WallpaperStudio) },
                    onOpenSettings = { viewModel.setOverlay(BrowserOverlay.Settings) },
                    onOpenAbout = { viewModel.setOverlay(BrowserOverlay.About) }
                )

                BrowserOverlay.Bookmarks -> BookmarksScreen(
                    bookmarks = bookmarks,
                    onBack = { viewModel.setOverlay(BrowserOverlay.None) },
                    onOpenBookmark = viewModel::loadUrlInActiveTab,
                    onAddBookmark = { viewModel.openAddBookmarkDialog(null) },
                    onEditBookmark = { viewModel.openAddBookmarkDialog(it) },
                    onDeleteBookmark = viewModel::deleteBookmark
                )

                BrowserOverlay.History -> HistoryScreen(
                    history = history,
                    onBack = { viewModel.setOverlay(BrowserOverlay.None) },
                    onOpenUrl = viewModel::loadUrlInActiveTab,
                    onDeleteItem = viewModel::deleteHistoryItem,
                    onClearByPeriod = viewModel::clearHistoryByPeriod
                )

                BrowserOverlay.Downloads -> DownloadsScreen(
                    downloads = downloads,
                    onBack = { viewModel.setOverlay(BrowserOverlay.None) },
                    onPause = viewModel.downloadEngine::pauseDownload,
                    onResume = { id ->
                        viewModel.downloadEngine.resumeDownload(id, settings.downloadNotificationsEnabled)
                    },
                    onCancel = viewModel.downloadEngine::cancelDownload,
                    onOpen = { item ->
                        if (!viewModel.downloadEngine.openDownloadedFile(item)) {
                            viewModel.showMessage("No app available to open ${item.fileName}")
                        }
                    },
                    onShare = { item ->
                        if (!viewModel.downloadEngine.shareDownloadedFile(item)) {
                            viewModel.showMessage("Unable to share ${item.fileName}")
                        }
                    },
                    onDelete = viewModel.downloadEngine::deleteDownload,
                    onClearFinished = viewModel.downloadEngine::clearFinished
                )

                BrowserOverlay.Settings -> SettingsScreen(
                    settings = settings,
                    onBack = { viewModel.setOverlay(BrowserOverlay.None) },
                    onOpenPermissions = { viewModel.setOverlay(BrowserOverlay.Permissions) },
                    onOpenWallpaperStudio = { viewModel.setOverlay(BrowserOverlay.WallpaperStudio) },
                    onOpenAbout = { viewModel.setOverlay(BrowserOverlay.About) },
                    onUpdateThemeMode = viewModel::updateThemeMode,
                    onUpdateSearchEngine = viewModel::updateSearchEngine,
                    onUpdateSearchSuggestions = viewModel::updateSearchSuggestions,
                    onUpdateSearchHistory = viewModel::updateSearchHistory,
                    onUpdateSafeSearch = viewModel::updateSafeSearch,
                    onUpdateWallpaperPreset = viewModel::updateWallpaperPreset,
                    onUploadWallpaper = onUploadWallpaper,
                    onUpdateWallpaperAppearance = viewModel::updateWallpaperAppearance,
                    onUpdateHomepageVisibility = viewModel::updateHomepageVisibility,
                    onUpdateToolbarLayout = viewModel::updateToolbarLayout,
                    onUpdateFontSize = viewModel::updateWebFontSize,
                    onUpdateDesktopDefault = viewModel::updateDesktopDefault,
                    onUpdateFullscreenBrowsing = viewModel::updateFullscreenBrowsing,
                    onUpdateStartupBehavior = viewModel::updateStartupBehavior,
                    onUpdateCustomHomepageUrl = viewModel::updateCustomHomepageUrl,
                    onUpdateLanguage = viewModel::updateAppLanguage,
                    onUpdateBlockPopups = viewModel::updateBlockPopups,
                    onUpdateSafeBrowsing = viewModel::updateSafeBrowsing,
                    onUpdateDoNotTrack = viewModel::updateDoNotTrack,
                    onUpdateDownloadNotifications = viewModel::updateDownloadNotifications,
                    onUpdateAskBeforeDownloading = viewModel::updateAskBeforeDownloading,
                    onClearCache = viewModel::clearCache,
                    onClearCookies = viewModel::clearCookies,
                    onClearWebsiteData = viewModel::clearWebsiteData,
                    onClearHistory = { viewModel.clearHistoryByPeriod(null) }
                )

                BrowserOverlay.Permissions -> PermissionsScreen(
                    settings = settings,
                    sitePermissions = sitePermissions,
                    onBack = { viewModel.setOverlay(BrowserOverlay.Settings) },
                    onUpdateDefaultPermission = viewModel::updateDefaultSitePermission,
                    onUpdateSitePermission = viewModel::updateSitePermission,
                    onRemoveSite = viewModel::removeSitePermissions
                )

                BrowserOverlay.AiSearch -> AiSearchSheet(
                    activeTab = activeTab,
                    messages = aiMessages,
                    onBack = { viewModel.setOverlay(BrowserOverlay.None) },
                    onSendPrompt = viewModel::sendAiPrompt,
                    onClearChat = viewModel::clearAiChat,
                    onCopiedToast = { viewModel.showMessage("AI response copied") }
                )

                BrowserOverlay.Translate -> TranslateSheet(
                    state = translationState,
                    onBack = { viewModel.setOverlay(BrowserOverlay.None) },
                    onSelectLanguage = viewModel::translateCurrentPage,
                    onCopiedToast = { viewModel.showMessage("Translation copied") }
                )

                BrowserOverlay.WallpaperStudio -> {
                    val aiSuggestions = remember(wallpapers, useDarkUi) {
                        viewModel.computeAiWallpaperSuggestions(useDarkUi)
                    }
                    WallpaperStudioScreen(
                        settings = settings,
                        wallpapers = wallpapers,
                        favoriteWallpapers = favoriteWallpapers,
                        recentWallpapers = recentWallpapers,
                        suggestions = aiSuggestions,
                        aiGenState = aiWallpaperGenState,
                        isSystemLiveWallpaperActive = isSystemLiveWallpaperActive,
                        onBack = { viewModel.setOverlay(BrowserOverlay.None) },
                        onPickPhoto = onUploadWallpaper,
                        onPickVideo = onPickVideoWallpaper,
                        onOpenPreview = { wp -> viewModel.setOverlay(BrowserOverlay.WallpaperPreview(wp.id)) },
                        onToggleFavorite = viewModel::toggleWallpaperFavorite,
                        onApplyBrowserWallpaper = viewModel::applyWallpaperToBrowser,
                        onApplyNewTabWallpaper = viewModel::applyWallpaperToNewTab,
                        onSetAndroidLiveWallpaper = onLaunchAndroidLiveWallpaperChooser,
                        onClearActiveWallpapers = viewModel::clearBrowserAndNewTabWallpaper,
                        onGenerateAiWallpaper = viewModel::generateAiWallpaper,
                        onResetAiState = viewModel::resetAiWallpaperState,
                        onUpdateAppearance = viewModel::updateWallpaperAppearance
                    )
                }

                is BrowserOverlay.WallpaperPreview -> {
                    val previewWallpaper = wallpapers.firstOrNull { it.id == overlay.wallpaperId }
                    if (previewWallpaper != null) {
                        WallpaperPreviewScreen(
                            wallpaper = previewWallpaper,
                            settings = settings,
                            isSystemLiveWallpaperActive = isSystemLiveWallpaperActive,
                            onBack = { viewModel.setOverlay(BrowserOverlay.WallpaperStudio) },
                            onSaveEdits = viewModel::saveWallpaperEdits,
                            onSetBrowserWallpaper = viewModel::applyWallpaperToBrowser,
                            onSetNewTabWallpaper = viewModel::applyWallpaperToNewTab,
                            onSetAndroidLiveWallpaper = onLaunchAndroidLiveWallpaperChooser,
                            onToggleFavorite = viewModel::toggleWallpaperFavorite,
                            onDeleteWallpaper = viewModel::deleteWallpaper,
                            onUpdateAppearance = viewModel::updateWallpaperAppearance
                        )
                    } else {
                        LaunchedEffect(overlay.wallpaperId) {
                            viewModel.setOverlay(BrowserOverlay.WallpaperStudio)
                        }
                    }
                }

                BrowserOverlay.About -> AboutDskChromeScreen(
                    onBack = { viewModel.setOverlay(BrowserOverlay.None) }
                )

                BrowserOverlay.None -> Unit
            }
        }

        // Modals & Prompts
        if (showSecuritySheet) {
            SecurityConnectionSheet(
                tab = activeTab,
                onDismiss = { viewModel.setSecuritySheetVisible(false) },
                onOpenPermissions = { viewModel.setOverlay(BrowserOverlay.Permissions) }
            )
        }

        if (showBookmarkDialog && editingBookmark != null) {
            BookmarkEditDialog(
                initial = editingBookmark!!,
                onSave = viewModel::saveBookmark,
                onDismiss = viewModel::closeBookmarkDialog
            )
        }

        if (showAddShortcutDialog) {
            AddShortcutDialog(
                onAdd = viewModel::addShortcut,
                onDismiss = viewModel::closeAddShortcutDialog
            )
        }

        pendingDownload?.let { prompt ->
            DownloadConfirmDialog(
                prompt = prompt,
                onConfirm = viewModel::confirmPendingDownload,
                onDismiss = viewModel::dismissPendingDownload
            )
        }

        pendingPermission?.let { prompt ->
            SitePermissionPromptDialog(
                prompt = prompt,
                onResolve = viewModel::resolvePendingSitePermission
            )
        }

        pendingSslWarning?.let { warning ->
            SslWarningDialog(
                warning = warning,
                onResolve = viewModel::resolveSslWarning
            )
        }

        pendingSafeBrowsing?.let { warning ->
            SafeBrowsingWarningDialog(
                warning = warning,
                onResolve = viewModel::resolveSafeBrowsingWarning
            )
        }

        contextLinkAction?.let { action ->
            ContextLinkActionDialog(
                action = action,
                onOpenNewTab = { url -> viewModel.createNewTab(url, activeTab.isIncognito) },
                onDownload = { url ->
                    val name = UrlUtils.guessFileName(url, null, if (action.isImage) "image/jpeg" else null)
                    viewModel.startConfirmedDownload(
                        url = url,
                        fileName = name,
                        mimeType = if (action.isImage) "image/jpeg" else "application/octet-stream"
                    )
                },
                onCopy = { url ->
                    copyToClipboard(context, url)
                    viewModel.showMessage("Address copied")
                },
                onShare = { url -> shareLink(context, url, url) },
                onDismiss = viewModel::dismissContextLinkAction
            )
        }

        // Startup Animated Splash Intro (non-blocking ~1s)
        SplashOverlay(visible = showSplash)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("DSK CHROME", text))
}

private fun shareLink(context: Context, title: String, url: String) {
    if (url.isBlank()) return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(intent, "Share webpage"))
}
