package core.extensions.js

/**
 * Result of an anti-bot or Cloudflare challenge resolution.
 */
data class AntiBotResolutionResult(
    val success: Boolean,
    val cookies: Map<String, String> = emptyMap(),
    val userAgent: String? = null,
    val errorMessage: String? = null
)

/**
 * Interface for solving anti-bot / Cloudflare challenges by suspending
 * the scraper coroutine to summon a native WebView (WKWebView on iOS, Android WebView)
 * and extracting clearance cookies before resuming the script.
 */
fun interface AntiBotResolver {
    /**
     * Suspends until the challenge at [url] is resolved or cancelled.
     */
    suspend fun resolveChallenge(url: String, userAgent: String): AntiBotResolutionResult
}
