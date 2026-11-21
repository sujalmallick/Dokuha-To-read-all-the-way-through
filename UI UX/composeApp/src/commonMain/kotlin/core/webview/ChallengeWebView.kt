package core.webview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Multiplatform WebView composable tailored for interactive anti-bot and Cloudflare Turnstile challenges.
 * Renders the challenge page at [url] with [userAgent], observes clearance cookies (e.g. cf_clearance),
 * and invokes [onChallengeResolved] upon successful verification.
 */
@Composable
expect fun ChallengeWebView(
    url: String,
    userAgent: String,
    onChallengeResolved: (cookies: Map<String, String>, userAgent: String) -> Unit,
    onError: (String) -> Unit,
    onCookiesChanged: ((Map<String, String>) -> Unit)? = null,
    modifier: Modifier = Modifier
)
