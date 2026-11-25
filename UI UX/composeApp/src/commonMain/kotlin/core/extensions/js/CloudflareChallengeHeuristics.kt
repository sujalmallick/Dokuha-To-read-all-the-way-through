package core.extensions.js

import core.platform.PlatformInfo

/**
 * Shared security validation and Cloudflare clearance heuristics used across
 * headless and interactive anti-bot WebView solvers.
 */
object CloudflareChallengeHeuristics {

    const val FALLBACK_ANDROID_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    const val FALLBACK_IOS_USER_AGENT =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"

    fun defaultReaderUserAgent(): String {
        return if (PlatformInfo.platformName == "iOS") {
            FALLBACK_IOS_USER_AGENT
        } else {
            FALLBACK_ANDROID_USER_AGENT
        }
    }

    val CLOUDFLARE_INTERACTIVE_LISTENER_JS: String = """
        window.addEventListener("message", function(e) {
            if (e.data && e.data.source === "cloudflare-challenge") {
                if (e.data.event === "interactiveBegin") {
                    window.__dokuha_interactive = true;
                }
            }
        });
    """.trimIndent()

    val TURNSTILE_INSPECTION_JS: String = """
        (function() {
            if (window.__dokuha_interactive) return 'true';
            var hasTurnstileIframe = !!document.querySelector('iframe[src*="challenges.cloudflare.com"]');
            var hasTurnstileEl = !!document.querySelector('#cf-turnstile, .cf-turnstile, [name="cf-turnstile-response"]');
            var hasStage = !!document.querySelector('#challenge-stage');
            return (hasTurnstileIframe || hasTurnstileEl || hasStage) ? 'true' : 'false';
        })();
    """.trimIndent()

    /**
     * Validates that [url] uses an HTTP/HTTPS scheme and targets a public, non-loopback host.
     * Returns the normalized lowercase hostname if valid, or `null` if unsafe/malformed.
     */
    fun extractValidatedHttpHost(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() || it.code < 0x20 }) {
            return null
        }

        val schemeSep = trimmed.indexOf("://")
        if (schemeSep <= 0) return null
        val scheme = trimmed.substring(0, schemeSep).lowercase()
        if (scheme != "http" && scheme != "https") return null

        val afterScheme = trimmed.substring(schemeSep + 3)
        val authorityEnd = afterScheme.indexOfAny(charArrayOf('/', '?', '#'))
        val authority = if (authorityEnd == -1) afterScheme else afterScheme.substring(0, authorityEnd)
        if (authority.isEmpty() || authority.contains('@')) return null

        val host = if (authority.startsWith("[")) {
            val closeBracket = authority.indexOf(']')
            if (closeBracket <= 1) return null
            authority.substring(1, closeBracket).lowercase()
        } else {
            val colonIdx = authority.indexOf(':')
            if (colonIdx == -1) authority.lowercase() else authority.substring(0, colonIdx).lowercase()
        }.trimEnd('.')

        if (host.isEmpty() || isPrivateOrLocalHost(host)) {
            return null
        }
        return host
    }

    /**
     * Matches an HTTP cookie domain against [targetHost] using RFC 6265 suffix rules.
     */
    fun cookieMatchesTargetHost(cookieDomain: String, targetHost: String): Boolean {
        val normalizedCookie = cookieDomain.trim().trimStart('.').trimEnd('.').lowercase()
        val normalizedTarget = targetHost.trim().trimStart('.').trimEnd('.').lowercase()
        if (normalizedCookie.isEmpty() || normalizedTarget.isEmpty()) return false
        if (normalizedCookie == normalizedTarget) return true
        // Prevent overly broad single-label cookie domain matching (e.g. "com")
        if (!normalizedCookie.contains('.')) return false
        return normalizedTarget.endsWith(".$normalizedCookie")
    }

    fun isChallengeTitle(title: String): Boolean {
        return title.contains("Just a moment", ignoreCase = true) ||
            title.contains("Cloudflare", ignoreCase = true) ||
            title.contains("Attention Required", ignoreCase = true) ||
            title.contains("Security Check", ignoreCase = true) ||
            title.contains("Checking your browser", ignoreCase = true)
    }

    fun isChallengeUrl(url: String): Boolean {
        return url.contains("challenge-platform", ignoreCase = true) ||
            url.contains("/cdn-cgi/", ignoreCase = true) ||
            url.contains("__cf_chl_", ignoreCase = true)
    }

    fun isChallengeResolved(
        title: String,
        currentUrl: String,
        cookies: Map<String, String>,
        isPageFinished: Boolean
    ): Boolean {
        if (cookies.containsKey("cf_clearance")) return true
        return isPageFinished &&
            title.isNotBlank() &&
            !isChallengeTitle(title) &&
            !isChallengeUrl(currentUrl) &&
            cookies.isNotEmpty()
    }

    /**
     * Returns true if [userAgent] is safe to apply to an iOS WKWebView without creating
     * an Android/Windows/app UA-vs-TLS mismatch.
     */
    fun isCompatibleIosUserAgent(userAgent: String): Boolean {
        return userAgent.isNotBlank() &&
            !userAgent.startsWith("Dokuha/") &&
            !userAgent.contains("Android", ignoreCase = true) &&
            !userAgent.contains("Windows", ignoreCase = true) &&
            !userAgent.contains("X11; Linux", ignoreCase = true)
    }

    /**
     * Selects a consistent iOS WebKit User-Agent, avoiding Android/custom app UAs that break TLS-UA matching.
     */
    fun resolveIosUserAgent(requestedUserAgent: String, webViewUserAgent: String?): String {
        return when {
            isCompatibleIosUserAgent(requestedUserAgent) -> requestedUserAgent
            !webViewUserAgent.isNullOrBlank() -> webViewUserAgent
            else -> FALLBACK_IOS_USER_AGENT
        }
    }

    private fun isPrivateOrLocalHost(host: String): Boolean {
        if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local")) return true
        if (host.contains(':')) {
            if (host == "::1" || host == "::" || host.startsWith("fe80:") ||
                host.startsWith("fc") || host.startsWith("fd") ||
                host.contains("127.0.0.1") || host.contains("169.254.")
            ) {
                return true
            }
        }
        val ipv4Parts = host.split('.')
        if (ipv4Parts.size == 4 && ipv4Parts.all { part -> part.toIntOrNull()?.let { it in 0..255 } == true }) {
            val a = ipv4Parts[0].toInt()
            val b = ipv4Parts[1].toInt()
            if (a == 0 || a == 10 || a == 127) return true
            if (a == 169 && b == 254) return true
            if (a == 172 && b in 16..31) return true
            if (a == 192 && b == 168) return true
            if (a == 100 && b in 64..127) return true // CGNAT 100.64.0.0/10
        }
        return false
    }
}
