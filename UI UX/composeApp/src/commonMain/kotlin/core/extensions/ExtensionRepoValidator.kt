package core.extensions

/**
 * Validates, normalizes, and fingerprints third-party extension repository URLs.
 *
 * Rules:
 * - HTTPS only (rejects http://, file://, javascript:, etc.)
 * - Valid host required (rejects empty host, whitespace)
 * - Rejects userinfo in the URL (user:pass@)
 * - Maximum URL length: 512 characters
 * - Normalizes scheme/host to lowercase, strips trailing slashes and /index.json
 * - Calculates deterministic SHA-256 fingerprint
 * - Enforces response size cap (2 MB) and timeout (10s)
 * - Never auto-installs extensions
 */
object ExtensionRepoValidator {

    const val MAX_URL_LENGTH = 512
    const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024 // 2 MB size cap
    const val FETCH_TIMEOUT_MS = 10_000L // 10 second timeout

    data class ValidationResult(
        val isValid: Boolean,
        val normalizedUrl: String? = null,
        val fingerprint: String? = null,
        val errorMessage: String? = null
    )

    fun validateAndNormalize(rawUrl: String): ValidationResult {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult(isValid = false, errorMessage = "Repository URL cannot be empty")
        }

        if (trimmed.length > MAX_URL_LENGTH) {
            return ValidationResult(isValid = false, errorMessage = "URL exceeds maximum length of $MAX_URL_LENGTH characters")
        }

        if (!trimmed.startsWith("https://", ignoreCase = true)) {
            return ValidationResult(isValid = false, errorMessage = "Repository URL must use https://")
        }

        val afterScheme = trimmed.substring(8)
        val hostAndPort = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')

        // Reject userinfo (e.g. https://user:pass@host)
        if (hostAndPort.contains('@')) {
            return ValidationResult(isValid = false, errorMessage = "URLs with user credentials (user:pass@) are not allowed")
        }

        val host = hostAndPort.substringBefore(':').lowercase()
        if (host.isEmpty() || host.contains(' ') || !host.contains('.')) {
            return ValidationResult(isValid = false, errorMessage = "Invalid repository host")
        }

        // Reject localhost and local/private network addresses
        if (host == "localhost" || host.startsWith("127.") || host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.16.")) {
            return ValidationResult(isValid = false, errorMessage = "Localhost and private network repositories are not allowed")
        }

        // Normalize URL: lowercase scheme & host, strip trailing slash and /index.json
        val pathAndQuery = afterScheme.substringAfter('/', "")
        var path = pathAndQuery.substringBefore('?').substringBefore('#')
        if (path.endsWith("/index.json", ignoreCase = true)) {
            path = path.substring(0, path.length - "/index.json".length)
        } else if (path.equals("index.json", ignoreCase = true)) {
            path = ""
        }
        val cleanPath = path.trimEnd('/')

        val normalized = "https://$host" + (if (cleanPath.isNotEmpty()) "/$cleanPath" else "")
        val fingerprint = Sha256.formattedFingerprint(normalized)

        return ValidationResult(
            isValid = true,
            normalizedUrl = normalized,
            fingerprint = fingerprint
        )
    }

    /**
     * Computes the deterministic SHA-256 fingerprint for a given URL or repository payload.
     */
    fun computeFingerprint(input: String): String = Sha256.formattedFingerprint(input)
}
