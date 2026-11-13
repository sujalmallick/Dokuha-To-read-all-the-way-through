package core.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
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
                settings.userAgentString = userAgent
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true

                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, targetUrl: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, targetUrl, favicon)
                        checkClearance(targetUrl ?: url)
                    }

                    override fun onPageFinished(view: WebView?, targetUrl: String?) {
                        super.onPageFinished(view, targetUrl)
                        checkClearance(targetUrl ?: url)
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

                    private fun checkClearance(currentUrl: String) {
                        if (hasResolved) return
                        val rawCookies = cookieManager.getCookie(currentUrl) ?: return
                        val cookieMap = parseCookies(rawCookies)

                        val hasCfClearance = cookieMap.containsKey("cf_clearance")
                        val hasBm = cookieMap.containsKey("__cf_bm")
                        val currentTitle = title ?: ""
                        val isNormalPage = !currentTitle.contains("Just a moment", ignoreCase = true) &&
                            !currentTitle.contains("Cloudflare", ignoreCase = true) &&
                            !currentTitle.contains("Attention Required", ignoreCase = true) &&
                            !currentUrl.contains("challenge", ignoreCase = true)

                        if (hasCfClearance || (hasBm && isNormalPage) || (cookieMap.isNotEmpty() && isNormalPage)) {
                            hasResolved = true
                            onChallengeResolved(cookieMap, userAgent)
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
