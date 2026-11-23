# [Security] JsBridge lacks SSRF protection and private IP subnet blocking

- **Labels**: `backend`, `security`, `priority:high`
- **Blocked by**: None

### Summary
The JavaScript extension bridge `JsBridge.kt` exposes an HTTP request function (`__native_http_request`) to untrusted or community-sideloaded JavaScript code. This function does not validate the target URL, domain, or IP address, allowing scripts to execute arbitrary HTTP GET/POST requests against `localhost` (127.0.0.1) and internal private network subnets (RFC 1918).

### Evidence
- **File**: `UI UX/composeApp/src/commonMain/kotlin/core/extensions/js/JsBridge.kt#L65-L100`
```kotlin
jsEngine.setGlobalAsyncFunction("__native_http_request") { args ->
    val url = args.getOrNull(0) as? String ?: return@setGlobalAsyncFunction null
    val options = args.getOrNull(1) as? Map<String, Any?> ?: emptyMap()
    // Directly performs httpClient.request(url) without IP or domain filtering
    val response = httpClient.request(url) { ... }
    // ...
}
```

### Impact
A malicious community extension or compromised repository could probe the user's local network, interact with local IoT devices, exfiltrate data from local development servers (`http://localhost:*`), or abuse cloud metadata endpoints if running in containerized environments (SSRF vulnerability).

### Expected Behavior
`__native_http_request` must enforce strict URL validation:
1. Only `http://` and `https://` protocols are allowed.
2. Resolve target hostnames and reject private, loopback, link-local, and broadcast IP ranges:
   - `127.0.0.0/8` (Loopback)
   - `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16` (Private IPv4)
   - `169.254.0.0/16` (Link-local)
   - `::1`, `fc00::/7`, `fe80::/10` (IPv6 equivalents)
3. Optionally validate target domains against the extension manifest's declared `networkDomains` or `baseUrl`.

### Suggested Approach
1. Add an `IpAddressValidator` utility in `core.network` that parses host strings and resolves DNS records before dispatching requests.
2. Throw a security exception if an extension attempts to connect to a private or loopback address.
3. Add unit tests verifying that requests to `http://localhost`, `http://127.0.0.1`, `http://192.168.1.1`, and `http://169.254.169.254` are blocked.

### Acceptance Criteria
- [ ] Requests to loopback addresses are blocked and return a security error.
- [ ] Requests to RFC 1918 private subnets are blocked.
- [ ] Valid public internet endpoints continue to function for legitimate sources.

- **Effort**: S
- **Related findings**: SEC-02, BRK-01
