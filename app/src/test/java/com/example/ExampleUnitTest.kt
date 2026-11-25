package com.example

import com.example.browser.TabState
import com.example.browser.TabStateManager
import com.example.browser.UrlUtils
import com.example.data.SearchEngine
import com.example.data.ToolbarLayout
import com.example.download.DownloadEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun urlResolverDetectsDomainsAndSearchQueries() {
        assertEquals(
            "https://youtube.com",
            UrlUtils.resolveInputToUrl("youtube.com", SearchEngine.GOOGLE, safeSearchEnabled = false)
        )
        assertEquals(
            "https://www.google.com/search?q=Best+Android+apps",
            UrlUtils.resolveInputToUrl("Best Android apps", SearchEngine.GOOGLE, safeSearchEnabled = false)
        )
        assertEquals(
            "https://duckduckgo.com/?q=privacy+browser&kp=1",
            UrlUtils.resolveInputToUrl("privacy browser", SearchEngine.DUCKDUCKGO, safeSearchEnabled = true)
        )
        assertEquals(
            "https://search.brave.com/search?q=kotlin+compose",
            UrlUtils.resolveInputToUrl("kotlin compose", SearchEngine.BRAVE, safeSearchEnabled = false)
        )
    }

    @Test
    fun safeBrowsingFlagsSuspiciousUrls() {
        assertNull(UrlUtils.checkSuspiciousUrl("https://www.wikipedia.org"))
        assertNotNull(UrlUtils.checkSuspiciousUrl("http://user:pass@192.168.1.1/login"))
        assertNotNull(UrlUtils.checkSuspiciousUrl("https://xn--google-1ra.com"))
    }

    @Test
    fun downloadFormattingIsAccurate() {
        assertEquals("0 B", DownloadEngine.formatBytes(0))
        assertEquals("512 B", DownloadEngine.formatBytes(512))
        assertEquals("1.5 KB", DownloadEngine.formatBytes(1536))
        assertEquals("2.0 MB", DownloadEngine.formatBytes(2L * 1024L * 1024L))
    }

    @Test
    fun tabStateManagerCreatesSwitchesAndDuplicatesTabs() {
        val manager = TabStateManager()
        var state = TabState()
        val firstTabId = state.activeTabId

        val (afterCreate, secondTab) = manager.createTab(state, url = "https://example.com")
        state = afterCreate
        assertEquals(2, state.tabs.size)
        assertEquals(secondTab.id, state.activeTabId)
        assertEquals("example.com", secondTab.securityHost)
        assertFalse(secondTab.isHomepage)
        assertTrue(secondTab.isSecure)

        val (afterSwitch, switched) = manager.switchTab(state, firstTabId)
        state = afterSwitch
        assertNotNull(switched)
        assertEquals(firstTabId, state.activeTabId)

        val (afterDup, dupTab) = manager.duplicateTab(state, secondTab.id)
        state = afterDup
        assertNotNull(dupTab)
        assertEquals(3, state.tabs.size)
        assertEquals(dupTab!!.id, state.activeTabId)
        assertEquals("https://example.com", dupTab.url)
    }

    @Test
    fun tabStateManagerClosesRestoresAndClearsTabs() {
        val manager = TabStateManager()
        var state = TabState()
        val (s2, pageTab) = manager.createTab(state, url = "https://kotlinlang.org")
        val (s3, incognitoTab) = manager.createTab(s2, url = "https://duckduckgo.com", isIncognito = true)
        state = s3

        // Closing the active incognito tab selects a neighbor and signals incognito session cleared
        val closeIncognito = manager.closeTab(state, incognitoTab.id, defaultDesktopMode = false)
        assertTrue(closeIncognito.clearedAllIncognito)
        state = closeIncognito.nextState
        assertEquals(2, state.tabs.size)
        assertTrue(state.closedTabs.isEmpty()) // Incognito tabs are never added to recently closed stack

        // Closing a normal webpage tab records it for restoration
        val closeNormal = manager.closeTab(state, pageTab.id, defaultDesktopMode = false)
        assertFalse(closeNormal.clearedAllIncognito)
        state = closeNormal.nextState
        assertEquals(1, state.tabs.size)
        assertEquals(1, state.closedTabs.size)
        assertEquals("https://kotlinlang.org", state.closedTabs.first().url)

        // Restore closed tab
        val (restoredState, restoredTab) = manager.restoreClosedTab(state, defaultDesktopMode = false)
        assertNotNull(restoredTab)
        state = restoredState
        assertEquals(2, state.tabs.size)
        assertTrue(state.closedTabs.isEmpty())
        assertEquals("https://kotlinlang.org", state.activeTab.url)

        // Closing all normal tabs leaves a single fresh homepage tab
        val (clearedState, closedList) = manager.closeAllTabs(state, incognitoOnly = false, defaultDesktopMode = true)
        assertEquals(2, closedList.size)
        assertEquals(1, clearedState.tabs.size)
        assertTrue(clearedState.activeTab.isHomepage)
        assertTrue(clearedState.activeTab.isDesktopMode)
    }

    @Test
    fun toolbarLayoutSupportsTopBottomAndSplitModes() {
        val layouts = ToolbarLayout.entries
        assertTrue(layouts.contains(ToolbarLayout.TOP_BAR))
        assertTrue(layouts.contains(ToolbarLayout.BOTTOM_FOCUS))
        assertTrue(layouts.contains(ToolbarLayout.STANDARD))
    }
}

