package core.extensions.js

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CloudflareChallengeHeuristicsTest {

    @Test
    fun testExtractValidatedHttpHostAcceptsPublicUrls() {
        assertEquals("mangadex.org", CloudflareChallengeHeuristics.extractValidatedHttpHost("https://MangaDex.org/title/123"))
        assertEquals("asuracomic.net", CloudflareChallengeHeuristics.extractValidatedHttpHost("http://asuracomic.net:8080/series"))
        assertEquals("sub.example.com", CloudflareChallengeHeuristics.extractValidatedHttpHost("https://sub.example.com?q=test"))
        assertEquals("fcomic.org", CloudflareChallengeHeuristics.extractValidatedHttpHost("https://fcomic.org/read"))
        assertEquals("fdscans.com", CloudflareChallengeHeuristics.extractValidatedHttpHost("https://fdscans.com/"))
    }

    @Test
    fun testExtractValidatedHttpHostRejectsUnsafeSchemesAndSsrfTargets() {
        assertNull(CloudflareChallengeHeuristics.extractValidatedHttpHost(""))
        assertNull(CloudflareChallengeHeuristics.extractValidatedHttpHost("   "))
        assertNull(CloudflareChallengeHeuristics.extractValidatedHttpHost("file:///etc/passwd"))
        assertNull(CloudflareChallengeHeuristics.extractValidatedHttpHost("javascript:alert(1)"))
        assertNull(CloudflareChallengeHeuristics.extractValidatedHttpHost("data:text/html,<script>alert(1)</script>"))
        assertNull(CloudflareChallengeHeuristics.extractValidatedHttpHost("https://example.com@127.0.0.1/admin"))
        assertNull(CloudflareChallengeHeuristics.extractValidatedHttpHost("http://localhost:3000/"))
        assertNull(CloudflareChallengeHeuristics.extractValidatedHttpHost("http://127.0.0.1/"))
        assertNull(CloudflareChallengeHeuristics.extractValidatedHttpHost("http://169.254.169.254/latest/meta-data/"))
        assertNull(CloudflareChallengeHeuristics.extractValidatedHttpHost("http://10.0.0.5/"))
        assertNull(CloudflareChallengeHeuristics.extractValidatedHttpHost("http://192.168.1.10/"))
        assertNull(CloudflareChallengeHeuristics.extractValidatedHttpHost("http://172.16.0.1/"))
        assertNull(CloudflareChallengeHeuristics.extractValidatedHttpHost("http://[::1]/"))
        assertNull(CloudflareChallengeHeuristics.extractValidatedHttpHost("http://[fd00::1]/"))
        assertNull(CloudflareChallengeHeuristics.extractValidatedHttpHost("http://[::ffff:127.0.0.1]/"))
        assertNull(CloudflareChallengeHeuristics.extractValidatedHttpHost("https://evil.com\nHost: internal"))
    }

    @Test
    fun testCookieMatchesTargetHostPreventsCrossDomainLeakage() {
        assertTrue(CloudflareChallengeHeuristics.cookieMatchesTargetHost("mangadex.org", "mangadex.org"))
        assertTrue(CloudflareChallengeHeuristics.cookieMatchesTargetHost(".mangadex.org", "api.mangadex.org"))
        assertFalse(CloudflareChallengeHeuristics.cookieMatchesTargetHost("google.com", "mangadex.org"))
        assertFalse(CloudflareChallengeHeuristics.cookieMatchesTargetHost("notmangadex.org", "mangadex.org"))
        assertFalse(CloudflareChallengeHeuristics.cookieMatchesTargetHost("dex.org", "mangadex.org"))
        assertFalse(CloudflareChallengeHeuristics.cookieMatchesTargetHost(".com", "mangadex.org"))
        assertFalse(CloudflareChallengeHeuristics.cookieMatchesTargetHost("", "mangadex.org"))
    }

    @Test
    fun testChallengeResolutionDetectsCfClearanceAndRejectsChallengePages() {
        assertTrue(
            CloudflareChallengeHeuristics.isChallengeResolved(
                title = "Just a moment...",
                currentUrl = "https://example.com/cdn-cgi/challenge-platform",
                cookies = mapOf("cf_clearance" to "abc123"),
                isPageFinished = false
            )
        )
        assertFalse(
            CloudflareChallengeHeuristics.isChallengeResolved(
                title = "Just a moment...",
                currentUrl = "https://example.com/",
                cookies = mapOf("__cf_bm" to "tmp"),
                isPageFinished = true
            )
        )
        assertFalse(
            CloudflareChallengeHeuristics.isChallengeResolved(
                title = "Manga Page",
                currentUrl = "https://example.com/cdn-cgi/l/chk_jschl",
                cookies = mapOf("__cf_bm" to "tmp"),
                isPageFinished = true
            )
        )
        assertTrue(
            CloudflareChallengeHeuristics.isChallengeResolved(
                title = "Popular Manga - Reader",
                currentUrl = "https://example.com/manga",
                cookies = mapOf("session" to "ok"),
                isPageFinished = true
            )
        )
    }

    @Test
    fun testResolveIosUserAgentRejectsAndroidAndAppStrings() {
        val webkitUa = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15"
        assertEquals(
            webkitUa,
            CloudflareChallengeHeuristics.resolveIosUserAgent(
                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36",
                webkitUa
            )
        )
        assertEquals(
            webkitUa,
            CloudflareChallengeHeuristics.resolveIosUserAgent("Dokuha/1.0", webkitUa)
        )
        assertFalse(CloudflareChallengeHeuristics.isCompatibleIosUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
        assertFalse(CloudflareChallengeHeuristics.isCompatibleIosUserAgent("Mozilla/5.0 (X11; Linux x86_64)"))
        val customSafariUa = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) Safari/604.1"
        assertTrue(CloudflareChallengeHeuristics.isCompatibleIosUserAgent(customSafariUa))
        assertEquals(
            customSafariUa,
            CloudflareChallengeHeuristics.resolveIosUserAgent(customSafariUa, webkitUa)
        )
    }
}
