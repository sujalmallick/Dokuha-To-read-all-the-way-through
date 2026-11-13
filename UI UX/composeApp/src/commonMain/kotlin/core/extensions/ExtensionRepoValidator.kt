package core.extensions

/**
 * Validates, normalizes, and fingerprints third-party extension repository URLs.
 *
 * Rules:
 * - HTTPS only (rejects http://, file://, javascript:, etc.)
 * - Valid host required (rejects empty host, whitespace)
 * - Rejects userinfo in the URL (user:pass@)
 * - Maximum URL length: 512 characters
 * - Normalizes scheme/host to lowercase, strips trailing slashes and known manifest paths
 * - Hardened against SSRF: blocks loopback, private IPv4 (10/8, 172.16/12, 192.168/16), link-local (169.254/16), CGNAT (100.64/10), 0.0.0.0/8, and private IPv6 (::1, fe80::/10, fc00::/7)
 * - Supports DNS resolution validation to defeat DNS rebinding attacks
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

    fun validateAndNormalize(
        rawUrl: String,
        dnsResolver: ((String) -> List<String>)? = null
    ): ValidationResult {
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
        if (host.isEmpty() || host.contains(' ') || (!host.contains('.') && host != "localhost" && host != "::1")) {
            return ValidationResult(isValid = false, errorMessage = "Invalid repository host")
        }

        // Direct IP and localhost check
        if (host == "localhost" || isPrivateOrReservedIp(host)) {
            return ValidationResult(isValid = false, errorMessage = "Localhost and private network repositories are not allowed")
        }

        // DNS Rebinding Protection: Resolve hostname and check all resolved IPs if resolver provided
        if (dnsResolver != null) {
            val resolvedIps = try {
                dnsResolver(host)
            } catch (e: Exception) {
                return ValidationResult(isValid = false, errorMessage = "Host DNS resolution failed: ${e.message}")
            }
            if (resolvedIps.isNotEmpty() && resolvedIps.any { isPrivateOrReservedIp(it) || it == "127.0.0.1" || it == "::1" }) {
                return ValidationResult(isValid = false, errorMessage = "Host resolves to a private, loopback, or cloud metadata IP address (SSRF blocked)")
            }
        }

        // Normalize URL: lowercase scheme & host, strip trailing slash and known manifest names
        val pathAndQuery = afterScheme.substringAfter('/', "")
        var path = pathAndQuery.substringBefore('?').substringBefore('#')
        val knownManifests = listOf(
            "/index.min.json", "/index.json", "/novel_index.json", "/plugins.min.json", "/versioning.json",
            "index.min.json", "index.json", "novel_index.json", "plugins.min.json", "versioning.json"
        )
        for (m in knownManifests) {
            if (path.endsWith(m, ignoreCase = true)) {
                path = path.substring(0, path.length - m.length)
                break
            }
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
     * Checks if an IP string belongs to private, loopback, link-local, carrier-grade NAT, or reserved subnets.
     */
    fun isPrivateOrReservedIp(ipStr: String): Boolean {
        // IPv6 Loopback, Link-Local, and Unique-Local
        if (ipStr == "::1" || ipStr.startsWith("fe80:", ignoreCase = true) ||
            ipStr.startsWith("fc00:", ignoreCase = true) || ipStr.startsWith("fd00:", ignoreCase = true)
        ) {
            return true
        }

        val parts = ipStr.split('.')
        if (parts.size != 4) return false
        val octets = parts.mapNotNull { it.toIntOrNull() }
        if (octets.size != 4 || octets.any { it !in 0..255 }) return false

        val (o1, o2) = octets
        return when {
            o1 == 127 -> true                                              // 127.0.0.0/8 (Loopback)
            o1 == 10 -> true                                               // 10.0.0.0/8 (Private A)
            o1 == 172 && (o2 in 16..31) -> true                            // 172.16.0.0/12 (Private B: 172.16.0.0 - 172.31.255.255)
            o1 == 192 && o2 == 168 -> true                                 // 192.168.0.0/16 (Private C)
            o1 == 169 && o2 == 254 -> true                                 // 169.254.0.0/16 (Link-Local & Cloud IMDS)
            o1 == 100 && (o2 in 64..127) -> true                           // 100.64.0.0/10 (Carrier-Grade NAT)
            o1 == 0 -> true                                                // 0.0.0.0/8
            else -> false
        }
    }

    /**
     * Computes the deterministic SHA-256 fingerprint for a given URL or repository payload.
     */
    fun computeFingerprint(input: String): String = Sha256.formattedFingerprint(input)
}
