package core.extensions.js

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@SuppressLint("SetJavaScriptEnabled")
actual class HeadlessCloudflareSolver(private val context: Context) : HeadlessSolver {

    actual override suspend fun solve(url: String, userAgent: String, timeoutMs: Long): HeadlessSolverResult {
        return withContext(Dispatchers.Main) {
            val deferred = CompletableDeferred<HeadlessSolverResult>()
            var webView: WebView? = null

            try {
                webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

                    val defaultUa = WebSettings.getDefaultUserAgent(context)
                    val resolvedUa = if (userAgent.isNotBlank() && userAgent.contains("Android", ignoreCase = true) && !userAgent.startsWith("Dokuha/")) {
                        userAgent
                    } else {
                        defaultUa
                    }
                    settings.userAgentString = resolvedUa

                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    fun checkClearance(currentUrl: String, isPageFinished: Boolean) {
                        if (deferred.isCompleted) return
                        val rawCookies = cookieManager.getCookie(currentUrl) ?: return
                        val cookieMap = parseCookies(rawCookies)

                        val hasCfClearance = cookieMap.containsKey("cf_clearance")
                        val currentTitle = title ?: ""
                        val isChallengeTitle = currentTitle.contains("Just a moment", ignoreCase = true) ||
                            currentTitle.contains("Cloudflare", ignoreCase = true) ||
                            currentTitle.contains("Attention Required", ignoreCase = true) ||
                            currentTitle.contains("Security Check", ignoreCase = true)
                        val isChallengeUrl = currentUrl.contains("challenge-platform", ignoreCase = true) ||
                            currentUrl.contains("/cdn-cgi/", ignoreCase = true)

                        val isNormalFinishedPage = isPageFinished &&
                            currentTitle.isNotBlank() &&
                            !isChallengeTitle &&
                            !isChallengeUrl

                        if (hasCfClearance || (isNormalFinishedPage && cookieMap.isNotEmpty())) {
                            deferred.complete(
                                HeadlessSolverResult(
                                    success = true,
                                    cookies = cookieMap,
                                    userAgent = resolvedUa
                                )
                            )
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            checkClearance(view?.url ?: url, isPageFinished = false)
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, targetUrl: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, targetUrl, favicon)
                            view?.evaluateJavascript(
                                """
                                window.addEventListener("message", function(e) {
                                    if (e.data && e.data.source === "cloudflare-challenge") {
                                        if (e.data.event === "interactiveBegin") {
                                            window.__dokuha_interactive = true;
                                        }
                                    }
                                });
                                """.trimIndent(),
                                null
                            )
                            checkClearance(targetUrl ?: url, isPageFinished = false)
                        }

                        override fun onPageFinished(view: WebView?, targetUrl: String?) {
                            super.onPageFinished(view, targetUrl)
                            checkClearance(targetUrl ?: url, isPageFinished = true)
                        }
                    }

                    loadUrl(url)
                }

                // Wait up to timeoutMs for silent clearance
                val result = withTimeoutOrNull(timeoutMs) {
                    deferred.await()
                }

                if (result != null) {
                    return@withContext result
                }

                // If timeout elapsed, evaluate whether an interactive Turnstile / Checkbox challenge is rendered
                val isTurnstilePresent = inspectTurnstileWidget(webView)
                val rawCookies = CookieManager.getInstance().getCookie(url) ?: ""
                val cookieMap = parseCookies(rawCookies)

                if (cookieMap.containsKey("cf_clearance")) {
                    HeadlessSolverResult(
                        success = true,
                        cookies = cookieMap,
                        userAgent = webView.settings.userAgentString
                    )
                } else {
                    HeadlessSolverResult(
                        success = false,
                        cookies = cookieMap,
                        userAgent = webView.settings.userAgentString,
                        isInteractiveChallengeRequired = isTurnstilePresent,
                        errorMessage = if (isTurnstilePresent) "Interactive verification required" else "Silent clearance timeout"
                    )
                }
            } catch (e: Exception) {
                HeadlessSolverResult(
                    success = false,
                    isInteractiveChallengeRequired = false,
                    errorMessage = e.message
                )
            } finally {
                try {
                    webView?.stopLoading()
                    webView?.destroy()
                } catch (_: Exception) {
                }
            }
        }
    }

    private suspend fun inspectTurnstileWidget(webView: WebView): Boolean {
        return withContext(Dispatchers.Main) {
            val evalDeferred = CompletableDeferred<Boolean>()
            val script = """
                (function() {
                    if (window.__dokuha_interactive) return 'true';
                    var hasTurnstileIframe = !!document.querySelector('iframe[src*="challenges.cloudflare.com"]');
                    var hasTurnstileEl = !!document.querySelector('#cf-turnstile, .cf-turnstile, [name="cf-turnstile-response"]');
                    var hasStage = !!document.querySelector('#challenge-stage');
                    return (hasTurnstileIframe || hasTurnstileEl || hasStage) ? 'true' : 'false';
                })();
            """.trimIndent()

            webView.evaluateJavascript(script) { result ->
                val clean = result?.replace("\"", "")?.trim()
                evalDeferred.complete(clean == "true")
            }

            withTimeoutOrNull(1000L) { evalDeferred.await() } ?: false
        }
    }

    private fun parseCookies(raw: String): Map<String, String> {
        return raw.split(';')
            .mapNotNull {
                val trimmed = it.trim()
                if (trimmed.isEmpty() || !trimmed.contains('=')) null
                else {
                    val name = trimmed.substringBefore('=').trim()
                    val value = trimmed.substringAfter('=').trim()
                    if (name.isNotEmpty()) Pair(name, value) else null
                }
            }.toMap()
    }
}
