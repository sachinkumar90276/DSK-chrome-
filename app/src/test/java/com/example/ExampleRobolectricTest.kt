package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.browser.UrlUtils
import com.example.data.SearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches DSK CHROME`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DSK CHROME", appName)
    }

    @Test
    fun `url resolver detects domain and search queries`() {
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
    }

    @Test
    fun `safe browsing heuristics flag suspicious links`() {
        assertNull(UrlUtils.checkSuspiciousUrl("https://www.wikipedia.org"))
        assertNotNull(UrlUtils.checkSuspiciousUrl("http://user:pass@192.168.1.1/login"))
    }
}
