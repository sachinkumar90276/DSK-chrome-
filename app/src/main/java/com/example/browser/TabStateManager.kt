package com.example.browser

import java.util.UUID

data class TabState(
    val tabs: List<BrowserTab> = listOf(BrowserTab()),
    val activeTabId: String = tabs.first().id,
    val closedTabs: List<ClosedTabRecord> = emptyList()
) {
    val activeTab: BrowserTab
        get() = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull() ?: BrowserTab()
}

class TabStateManager(initialState: TabState = TabState()) {

    fun createTab(
        state: TabState,
        url: String = "",
        isIncognito: Boolean = false,
        isDesktopMode: Boolean = false
    ): Pair<TabState, BrowserTab> {
        val isHome = url.isBlank()
        val newTab = BrowserTab(
            url = url,
            title = if (isIncognito) "Private Tab" else "New Tab",
            isHomepage = isHome,
            isIncognito = isIncognito,
            isDesktopMode = isDesktopMode,
            isSecure = url.startsWith("https://", ignoreCase = true),
            securityHost = if (!isHome) UrlUtils.extractHost(url) else "",
            faviconUrl = if (!isHome) UrlUtils.buildFaviconUrl(url) else ""
        )
        val next = state.copy(
            tabs = state.tabs + newTab,
            activeTabId = newTab.id
        )
        return next to newTab
    }

    fun switchTab(state: TabState, tabId: String): Pair<TabState, BrowserTab?> {
        val target = state.tabs.find { it.id == tabId } ?: return state to null
        return state.copy(activeTabId = target.id) to target
    }

    fun duplicateTab(state: TabState, tabId: String): Pair<TabState, BrowserTab?> {
        val source = state.tabs.find { it.id == tabId } ?: return state to null
        val copy = source.copy(
            id = UUID.randomUUID().toString(),
            isLoading = false,
            progress = 0,
            canGoBack = false,
            canGoForward = false
        )
        val sourceIndex = state.tabs.indexOfFirst { it.id == tabId }
        val updatedTabs = state.tabs.toMutableList().apply {
            add(if (sourceIndex >= 0) sourceIndex + 1 else size, copy)
        }
        return state.copy(tabs = updatedTabs, activeTabId = copy.id) to copy
    }

    data class CloseTabResult(
        val nextState: TabState,
        val closedTab: BrowserTab?,
        val clearedAllIncognito: Boolean,
        val newActiveTab: BrowserTab
    )

    fun closeTab(
        state: TabState,
        tabId: String,
        defaultDesktopMode: Boolean
    ): CloseTabResult {
        val closingIndex = state.tabs.indexOfFirst { it.id == tabId }
        if (closingIndex == -1) {
            return CloseTabResult(state, null, clearedAllIncognito = false, newActiveTab = state.activeTab)
        }
        val closing = state.tabs[closingIndex]
        val updatedClosedStack = if (!closing.isIncognito && !closing.isHomepage && closing.url.isNotBlank()) {
            (listOf(ClosedTabRecord(closing.title, closing.url, closing.isHomepage, closing.isDesktopMode)) + state.closedTabs)
                .take(10)
        } else {
            state.closedTabs
        }

        val remaining = state.tabs.filterNot { it.id == tabId }
        val clearedAllIncognito = closing.isIncognito && remaining.none { it.isIncognito }

        if (remaining.isEmpty()) {
            val fresh = BrowserTab(isIncognito = false, isDesktopMode = defaultDesktopMode)
            val next = state.copy(
                tabs = listOf(fresh),
                activeTabId = fresh.id,
                closedTabs = updatedClosedStack
            )
            return CloseTabResult(next, closing, clearedAllIncognito, fresh)
        }

        val nextActiveId = if (state.activeTabId == tabId) {
            val sameModeNeighbor = remaining.filter { it.isIncognito == closing.isIncognito }
            if (sameModeNeighbor.isNotEmpty()) {
                val clampedIndex = (closingIndex - 1).coerceIn(0, remaining.lastIndex)
                val candidate = remaining[clampedIndex]
                if (candidate.isIncognito == closing.isIncognito) candidate.id else sameModeNeighbor.last().id
            } else {
                remaining.last().id
            }
        } else {
            state.activeTabId
        }

        val nextState = state.copy(
            tabs = remaining,
            activeTabId = nextActiveId,
            closedTabs = updatedClosedStack
        )
        return CloseTabResult(nextState, closing, clearedAllIncognito, nextState.activeTab)
    }

    fun restoreClosedTab(state: TabState, defaultDesktopMode: Boolean): Pair<TabState, BrowserTab?> {
        val recent = state.closedTabs.firstOrNull() ?: return state to null
        val (createdState, newTab) = createTab(
            state = state.copy(closedTabs = state.closedTabs.drop(1)),
            url = recent.url,
            isIncognito = false,
            isDesktopMode = recent.isDesktopMode || defaultDesktopMode
        )
        return createdState.copy(
            tabs = createdState.tabs.map {
                if (it.id == newTab.id) it.copy(title = recent.title) else it
            }
        ) to newTab.copy(title = recent.title)
    }

    fun closeAllTabs(
        state: TabState,
        incognitoOnly: Boolean,
        defaultDesktopMode: Boolean
    ): Pair<TabState, List<BrowserTab>> {
        val closed = state.tabs.filter { it.isIncognito == incognitoOnly }
        val remaining = state.tabs.filterNot { it.isIncognito == incognitoOnly }
        val nextState = if (remaining.isEmpty()) {
            val fresh = BrowserTab(isIncognito = false, isDesktopMode = defaultDesktopMode)
            state.copy(tabs = listOf(fresh), activeTabId = fresh.id)
        } else {
            val activeStillExists = remaining.any { it.id == state.activeTabId }
            state.copy(
                tabs = remaining,
                activeTabId = if (activeStillExists) state.activeTabId else remaining.last().id
            )
        }
        return nextState to closed
    }

    fun updateTab(state: TabState, tabId: String, transform: (BrowserTab) -> BrowserTab): TabState {
        return state.copy(
            tabs = state.tabs.map { if (it.id == tabId) transform(it) else it }
        )
    }
}
