package core.webview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSError
import platform.Foundation.NSHTTPCookie
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKHTTPCookieStore
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebpagePreferences
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject

/**
 * Native iOS implementation of [ChallengeWebView] embedding WebKit's [WKWebView] via Compose's [UIKitView].
 *
 * Observes page lifecycle events and cookies via [WKHTTPCookieStore] to detect Cloudflare Turnstile
 * clearance (`cf_clearance`) or successful navigation through challenges, reporting extracted cookies
 * back to the Kotlin Multiplatform networking layer.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ChallengeWebView(
    url: String,
    userAgent: String,
    onChallengeResolved: (cookies: Map<String, String>, userAgent: String) -> Unit,
    onError: (String) -> Unit,
    onCookiesChanged: ((Map<String, String>) -> Unit)?,
    modifier: Modifier
) {
    var hasResolved by remember { mutableStateOf(false) }

    val cookieStore = remember {
        WKWebsiteDataStore.defaultDataStore().httpCookieStore
    }

    val delegate = remember {
        object : NSObject(), WKNavigationDelegateProtocol {

            private fun checkClearance(webView: WKWebView, isPageFinished: Boolean) {
                if (hasResolved) return

                cookieStore.getAllCookies { cookies ->
                    val cookieList = cookies?.filterIsInstance<NSHTTPCookie>() ?: emptyList()
                    val cookieMap = cookieList.associate { it.name to it.value }

                    if (cookieMap.isNotEmpty()) {
                        onCookiesChanged?.invoke(cookieMap)
                    }

                    val hasCfClearance = cookieMap.containsKey("cf_clearance")
                    val currentTitle = webView.title ?: ""
                    val currentUrl = webView.URL?.absoluteString ?: url

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
                        hasResolved = true
                        val resolvedUa = webView.customUserAgent ?: userAgent
                        onChallengeResolved(cookieMap, resolvedUa)
                    }
                }
            }

            @ObjCSignatureOverride
            @Suppress("CONFLICTING_OVERLOADS")
            override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
                checkClearance(webView, isPageFinished = false)
            }

            @ObjCSignatureOverride
            @Suppress("CONFLICTING_OVERLOADS")
            override fun webView(webView: WKWebView, didCommitNavigation: WKNavigation?) {
                checkClearance(webView, isPageFinished = false)
            }

            @ObjCSignatureOverride
            @Suppress("CONFLICTING_OVERLOADS")
            override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                checkClearance(webView, isPageFinished = true)
            }

            @ObjCSignatureOverride
            @Suppress("CONFLICTING_OVERLOADS")
            override fun webView(
                webView: WKWebView,
                didFailNavigation: WKNavigation?,
                withError: NSError
            ) {
                onError(withError.localizedDescription ?: "Navigation failed")
            }

            @ObjCSignatureOverride
            @Suppress("CONFLICTING_OVERLOADS")
            override fun webView(
                webView: WKWebView,
                didFailProvisionalNavigation: WKNavigation?,
                withError: NSError
            ) {
                onError(withError.localizedDescription ?: "Provisional navigation failed")
            }
        }
    }

    UIKitView(
        factory = {
            val config = WKWebViewConfiguration().apply {
                defaultWebpagePreferences = WKWebpagePreferences().apply {
                    allowsContentJavaScript = true
                }
                websiteDataStore = WKWebsiteDataStore.defaultDataStore()
            }

            WKWebView(frame = CGRectZero.readValue(), configuration = config).apply {
                if (userAgent.isNotBlank() && !userAgent.startsWith("Dokuha/")) {
                    customUserAgent = userAgent
                }
                navigationDelegate = delegate
                val requestUrl = NSURL.URLWithString(url) ?: NSURL(string = url)
                if (requestUrl != null) {
                    loadRequest(NSURLRequest.requestWithURL(requestUrl))
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { webView ->
            // Keep delegate updated across recompositions
            webView.navigationDelegate = delegate
        }
    )
}
