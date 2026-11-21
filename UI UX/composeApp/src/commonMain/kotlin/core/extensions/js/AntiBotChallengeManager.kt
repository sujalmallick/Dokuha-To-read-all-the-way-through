package core.extensions.js

import core.platform.currentTimeMillis
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Represents an active interactive challenge request awaiting user action.
 */
data class AntiBotChallengeRequest(
    val id: String,
    val url: String,
    val domain: String,
    val userAgent: String,
    val onCompleted: (AntiBotResolutionResult) -> Unit,
    val onDismiss: () -> Unit
)

/**
 * Coordinates and coalesces Cloudflare Turnstile / anti-bot challenges across scrapers.
 * Implements two-stage resolution:
 * 1. Silent background headless WebView resolution (Mihon pattern) for automated JS challenges.
 * 2. Interactive modal sheet presentation strictly when an interactive checkbox is detected.
 */
class AntiBotChallengeManager(
    private val headlessSolver: HeadlessSolver? = null
) : AntiBotResolver {

    private val mutex = Mutex()
    private val inFlightChallenges = mutableMapOf<String, CompletableDeferred<AntiBotResolutionResult>>()

    private val _activeChallenge = MutableStateFlow<AntiBotChallengeRequest?>(null)
    val activeChallenge: StateFlow<AntiBotChallengeRequest?> = _activeChallenge.asStateFlow()

    override suspend fun resolveChallenge(url: String, userAgent: String): AntiBotResolutionResult {
        val domain = extractDomain(url)
        val (deferred, shouldResolve) = mutex.withLock {
            val existing = inFlightChallenges[domain]
            if (existing != null && !existing.isCompleted) {
                Pair(existing, false)
            } else {
                val newDeferred = CompletableDeferred<AntiBotResolutionResult>()
                inFlightChallenges[domain] = newDeferred
                Pair(newDeferred, true)
            }
        }

        if (shouldResolve) {
            // Stage 1: Silent background resolution (Mihon pattern)
            var silentResult: HeadlessSolverResult? = null
            if (headlessSolver != null) {
                try {
                    silentResult = headlessSolver.solve(url, userAgent)
                    if (silentResult.success) {
                        deferred.complete(
                            AntiBotResolutionResult(
                                success = true,
                                cookies = silentResult.cookies,
                                userAgent = silentResult.userAgent
                            )
                        )
                    }
                } catch (_: Exception) {
                }
            }

            // Stage 2: Present interactive modal UI ONLY if an interactive checkbox is required
            if (!deferred.isCompleted) {
                val isInteractiveRequired = silentResult?.isInteractiveChallengeRequired ?: true
                if (isInteractiveRequired) {
                    val requestId = "${domain}_${currentTimeMillis()}"
                    _activeChallenge.value = AntiBotChallengeRequest(
                        id = requestId,
                        url = url,
                        domain = domain,
                        userAgent = userAgent,
                        onCompleted = { result ->
                            deferred.complete(result)
                            _activeChallenge.value = null
                        },
                        onDismiss = {
                            deferred.complete(
                                AntiBotResolutionResult(
                                    success = false,
                                    errorMessage = "Challenge dismissed by user"
                                )
                            )
                            _activeChallenge.value = null
                        }
                    )
                } else {
                    deferred.complete(
                        AntiBotResolutionResult(
                            success = false,
                            errorMessage = silentResult?.errorMessage ?: "No interactive challenge detected"
                        )
                    )
                }
            }
        }

        try {
            return deferred.await()
        } finally {
            mutex.withLock {
                if (inFlightChallenges[domain] === deferred) {
                    inFlightChallenges.remove(domain)
                }
            }
        }
    }

    fun dismissActiveChallenge() {
        _activeChallenge.value?.onDismiss?.invoke()
    }

    private fun extractDomain(url: String): String {
        val clean = url.substringAfter("://", url).substringBefore('/').substringBefore('?')
        return clean.substringBefore(':').lowercase()
    }
}
