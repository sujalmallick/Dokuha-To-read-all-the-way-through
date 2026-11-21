package core.extensions.js

actual class HeadlessCloudflareSolver : HeadlessSolver {
    actual override suspend fun solve(url: String, userAgent: String, timeoutMs: Long): HeadlessSolverResult {
        return HeadlessSolverResult(
            success = false,
            isInteractiveChallengeRequired = true,
            errorMessage = "Headless solver not supported on desktop"
        )
    }
}
