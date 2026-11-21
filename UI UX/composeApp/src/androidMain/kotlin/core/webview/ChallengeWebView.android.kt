package core.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun ChallengeWebView(
    url: String,
    userAgent: String,
    onChallengeResolved: (cookies: Map<String, String>, userAgent: String) -> Unit,
    onError: (String) -> Unit,
    onCookiesChanged: ((Map<String, String>) -> Unit)?,
    modifier: Modifier
) {
    var hasResolved = remember { false }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                val defaultUa = WebSettings.getDefaultUserAgent(context)
                // On Android, always ensure a mobile Android User-Agent is used to prevent
                // Cloudflare from rejecting the request due to desktop UA vs Android client hints mismatch.
                val resolvedUa = if (userAgent.isNotBlank() && userAgent.contains("Android", ignoreCase = true) && !userAgent.startsWith("Dokuha/")) {
                    userAgent
                } else {
                    defaultUa
                }
                settings.userAgentString = resolvedUa
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true

                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                fun checkClearance(currentUrl: String, isPageFinished: Boolean) {
                    if (hasResolved) return
                    val rawCookies = cookieManager.getCookie(currentUrl) ?: return
                    val cookieMap = parseCookies(rawCookies)

                    // Notify parent of updated cookies in real time
                    onCookiesChanged?.invoke(cookieMap)

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

                    // Only resolve if cf_clearance cookie is present OR if navigation finished on a verified destination page
                    if (hasCfClearance || (isNormalFinishedPage && cookieMap.isNotEmpty())) {
                        hasResolved = true
                        onChallengeResolved(cookieMap, resolvedUa)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        return super.onConsoleMessage(consoleMessage)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        checkClearance(view?.url ?: url, isPageFinished = false)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, targetUrl: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, targetUrl, favicon)
                        checkClearance(targetUrl ?: url, isPageFinished = false)
                    }

                    override fun onPageFinished(view: WebView?, targetUrl: String?) {
                        super.onPageFinished(view, targetUrl)
                        checkClearance(targetUrl ?: url, isPageFinished = true)
                    }

                    override fun doUpdateVisitedHistory(view: WebView?, targetUrl: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, targetUrl, isReload)
                        checkClearance(targetUrl ?: view?.url ?: url, isPageFinished = false)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            onError(error?.description?.toString() ?: "WebView connection error")
                        }
                    }
                }

                loadUrl(url)
            }
        }
    )
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
