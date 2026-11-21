package core.extensions.js

data class HeadlessSolverResult(
    val success: Boolean,
    val cookies: Map<String, String> = emptyMap(),
    val userAgent: String? = null,
    val isInteractiveChallengeRequired: Boolean = false,
    val errorMessage: String? = null
)

interface HeadlessSolver {
    suspend fun solve(url: String, userAgent: String, timeoutMs: Long = 4000L): HeadlessSolverResult
}

/**
 * Platform-specific headless solver that runs an invisible browser context
 * to automatically solve non-interactive Cloudflare JavaScript challenges without UI disruption.
 */
expect class HeadlessCloudflareSolver : HeadlessSolver {
    override suspend fun solve(url: String, userAgent: String, timeoutMs: Long): HeadlessSolverResult
}
