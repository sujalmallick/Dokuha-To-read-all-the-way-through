package core.extensions.js

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSHTTPCookie
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKHTTPCookieStore
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebpagePreferences
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * Off-screen iOS [WKWebView] solver that attempts silent Cloudflare JS challenge clearance
 * before falling back to interactive verification UI.
 */
@OptIn(ExperimentalForeignApi::class)
actual class HeadlessCloudflareSolver : HeadlessSolver {

    actual override suspend fun solve(url: String, userAgent: String, timeoutMs: Long): HeadlessSolverResult {
        val targetHost = CloudflareChallengeHeuristics.extractValidatedHttpHost(url)
            ?: return HeadlessSolverResult(
                success = false,
                isInteractiveChallengeRequired = false,
                errorMessage = "Invalid or disallowed challenge URL"
            )

        val requestUrl = NSURL.URLWithString(url) ?: NSURL(string = url)
            ?: return HeadlessSolverResult(
                success = false,
                isInteractiveChallengeRequired = false,
                errorMessage = "Unable to parse challenge URL"
            )

        return withContext(Dispatchers.Main) {
            val deferred = CompletableDeferred<HeadlessSolverResult>()
            var webView: WKWebView? = null
            // Retain strong delegate reference across ARC lifetime; WKWebView.navigationDelegate is weak.
            var retainedDelegate: WKNavigationDelegateProtocol? = null

            try {
                val dataStore = WKWebsiteDataStore.defaultDataStore()
                val cookieStore = dataStore.httpCookieStore

                val contentController = WKUserContentController().apply {
                    addUserScript(
                        WKUserScript(
                            source = CloudflareChallengeHeuristics.CLOUDFLARE_INTERACTIVE_LISTENER_JS,
                            injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
                            forMainFrameOnly = true
                        )
                    )
                }

                val config = WKWebViewConfiguration().apply {
                    userContentController = contentController
                    defaultWebpagePreferences = WKWebpagePreferences().apply {
                        allowsContentJavaScript = true
                    }
                    websiteDataStore = dataStore
                }

                val createdWebView = WKWebView(
                    frame = CGRectMake(0.0, 0.0, 390.0, 844.0),
                    configuration = config
                )
                webView = createdWebView

                if (CloudflareChallengeHeuristics.isCompatibleIosUserAgent(userAgent)) {
                    createdWebView.customUserAgent = userAgent
                }

                fun checkClearance(activeWebView: WKWebView, isPageFinished: Boolean) {
                    if (deferred.isCompleted) return
                    val currentUrl = activeWebView.URL?.absoluteString ?: url
                    val currentHost = CloudflareChallengeHeuristics.extractValidatedHttpHost(currentUrl) ?: targetHost

                    cookieStore.getAllCookies { cookies ->
                        if (deferred.isCompleted) return@getAllCookies
                        val cookieMap = extractScopedCookies(cookies, targetHost, currentHost)
                        val currentTitle = activeWebView.title ?: ""

                        if (CloudflareChallengeHeuristics.isChallengeResolved(
                                title = currentTitle,
                                currentUrl = currentUrl,
                                cookies = cookieMap,
                                isPageFinished = isPageFinished
                            )
                        ) {
                            val effectiveUa = CloudflareChallengeHeuristics.resolveIosUserAgent(
                                requestedUserAgent = userAgent,
                                webViewUserAgent = activeWebView.customUserAgent
                            )
                            deferred.complete(
                                HeadlessSolverResult(
                                    success = true,
                                    cookies = cookieMap,
                                    userAgent = effectiveUa
                                )
                            )
                        }
                    }
                }

                val delegate = object : NSObject(), WKNavigationDelegateProtocol {
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
                        // Ignore NSURLErrorCancelled (-999) triggered by legitimate Cloudflare JS/meta redirects
                        if (withError.code == -999L) return
                        if (!deferred.isCompleted) {
                            deferred.complete(
                                HeadlessSolverResult(
                                    success = false,
                                    isInteractiveChallengeRequired = false,
                                    errorMessage = withError.localizedDescription
                                )
                            )
                        }
                    }

                    @ObjCSignatureOverride
                    @Suppress("CONFLICTING_OVERLOADS")
                    override fun webView(
                        webView: WKWebView,
                        didFailProvisionalNavigation: WKNavigation?,
                        withError: NSError
                    ) {
                        // Ignore NSURLErrorCancelled (-999) triggered by legitimate Cloudflare JS/meta redirects
                        if (withError.code == -999L) return
                        if (!deferred.isCompleted) {
                            deferred.complete(
                                HeadlessSolverResult(
                                    success = false,
                                    isInteractiveChallengeRequired = false,
                                    errorMessage = withError.localizedDescription
                                )
                            )
                        }
                    }
                }
                retainedDelegate = delegate
                createdWebView.navigationDelegate = delegate
                createdWebView.loadRequest(NSURLRequest.requestWithURL(requestUrl))

                val solvedResult = withTimeoutOrNull(timeoutMs) {
                    deferred.await()
                }

                val resolvedNavigatorUa = evaluateStringJs(createdWebView, "navigator.userAgent")
                val effectiveUa = CloudflareChallengeHeuristics.resolveIosUserAgent(
                    requestedUserAgent = userAgent,
                    webViewUserAgent = resolvedNavigatorUa ?: createdWebView.customUserAgent
                )

                if (solvedResult != null) {
                    return@withContext if (solvedResult.success) {
                        solvedResult.copy(userAgent = effectiveUa)
                    } else {
                        solvedResult
                    }
                }

                val isTurnstilePresent = inspectTurnstileWidget(createdWebView)
                val currentTitle = createdWebView.title ?: ""
                val currentUrl = createdWebView.URL?.absoluteString ?: url
                val currentHost = CloudflareChallengeHeuristics.extractValidatedHttpHost(currentUrl) ?: targetHost
                val isChallengeActive = isTurnstilePresent ||
                    CloudflareChallengeHeuristics.isChallengeTitle(currentTitle) ||
                    CloudflareChallengeHeuristics.isChallengeUrl(currentUrl)

                val finalCookies = fetchScopedCookies(cookieStore, targetHost, currentHost)

                if (finalCookies.containsKey("cf_clearance")) {
                    HeadlessSolverResult(
                        success = true,
                        cookies = finalCookies,
                        userAgent = effectiveUa
                    )
                } else {
                    HeadlessSolverResult(
                        success = false,
                        cookies = finalCookies,
                        userAgent = effectiveUa,
                        isInteractiveChallengeRequired = isChallengeActive,
                        errorMessage = if (isChallengeActive) {
                            "Interactive verification required"
                        } else {
                            "Silent clearance timeout"
                        }
                    )
                }
            } catch (e: Exception) {
                HeadlessSolverResult(
                    success = false,
                    isInteractiveChallengeRequired = false,
                    errorMessage = e.message ?: "Headless WKWebView failure"
                )
            } finally {
                try {
                    webView?.stopLoading()
                    webView?.navigationDelegate = null
                    webView?.configuration?.userContentController?.removeAllUserScripts()
                } catch (_: Exception) {
                }
                retainedDelegate = null
                webView = null
            }
        }
    }

    private fun extractScopedCookies(
        rawCookies: List<*>?,
        targetHost: String,
        currentHost: String
    ): Map<String, String> {
        return rawCookies
            ?.filterIsInstance<NSHTTPCookie>()
            ?.filter {
                CloudflareChallengeHeuristics.cookieMatchesTargetHost(it.domain, targetHost) ||
                    CloudflareChallengeHeuristics.cookieMatchesTargetHost(it.domain, currentHost)
            }
            ?.associate { it.name to it.value }
            ?: emptyMap()
    }

    private suspend fun fetchScopedCookies(
        cookieStore: WKHTTPCookieStore,
        targetHost: String,
        currentHost: String
    ): Map<String, String> {
        return withTimeoutOrNull(1000L) {
            suspendCancellableCoroutine { continuation ->
                cookieStore.getAllCookies { cookies ->
                    if (continuation.isActive) {
                        continuation.resume(extractScopedCookies(cookies, targetHost, currentHost))
                    }
                }
            }
        } ?: emptyMap()
    }

    private suspend fun inspectTurnstileWidget(webView: WKWebView): Boolean {
        val result = evaluateStringJs(webView, CloudflareChallengeHeuristics.TURNSTILE_INSPECTION_JS)
        return result?.replace("\"", "")?.trim() == "true"
    }

    private suspend fun evaluateStringJs(webView: WKWebView, script: String): String? {
        return withTimeoutOrNull(1000L) {
            suspendCancellableCoroutine { continuation ->
                webView.evaluateJavaScript(script) { result, error ->
                    if (continuation.isActive) {
                        val value = if (error == null) result?.toString() else null
                        continuation.resume(value)
                    }
                }
            }
        }
    }
}
