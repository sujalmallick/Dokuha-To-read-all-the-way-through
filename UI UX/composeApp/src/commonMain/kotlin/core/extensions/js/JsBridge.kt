package core.extensions.js

import core.extensions.Sha256
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.withTimeout

data class JsHttpResponse(
    val status: Int,
    val ok: Boolean,
    val headers: Map<String, String>,
    val body: String
)

/**
 * Provides host capabilities to the sandboxed JS execution context.
 */
class JsBridge(
    private val httpClient: HttpClient? = null,
    private val antiBotResolver: AntiBotResolver? = null,
    val diagnosticLogs: MutableList<String> = mutableListOf()
) {
    private val cookieJar = mutableMapOf<String, String>()
    private var currentUserAgent: String =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    /**
     * Executes an HTTP request on behalf of the JavaScript extension.
     */
    suspend fun fetch(
        url: String,
        method: String = "GET",
        headersMap: Map<String, String> = emptyMap(),
        body: String? = null,
        timeoutMs: Long = 15_000L
    ): JsHttpResponse {
        val client = httpClient ?: error("HttpClient is not configured in JsBridge")

        val mergedHeaders = headersMap.toMutableMap()
        if (!mergedHeaders.keys.any { it.equals("User-Agent", ignoreCase = true) }) {
            mergedHeaders["User-Agent"] = currentUserAgent
        }

        val domain = extractDomain(url)
        val relevantCookies = cookieJar.filter { domain.endsWith(it.key.substringBefore(':')) || it.key == domain }
        if (relevantCookies.isNotEmpty() && !mergedHeaders.containsKey("Cookie")) {
            mergedHeaders["Cookie"] = relevantCookies.values.joinToString("; ")
        }

        var response = performRequest(client, url, method, mergedHeaders, body, timeoutMs)

        // Cloudflare / Anti-bot challenge detection (403, 503 or HTML challenge markers)
        if ((response.status == 403 || response.status == 503 || isCloudflareChallenge(response.body)) && antiBotResolver != null) {
            diagnosticLogs.add("[JsBridge] Anti-bot challenge detected at $url (Status: ${response.status}). Suspending coroutine to summon resolver...")
            val result = antiBotResolver.resolveChallenge(url, currentUserAgent)
            if (result.success) {
                diagnosticLogs.add("[JsBridge] Anti-bot challenge resolved! Updating cookies and retrying request...")
                result.cookies.forEach { (name, value) ->
                    cookieJar["$domain:$name"] = "$name=$value"
                }
                if (result.userAgent != null) {
                    currentUserAgent = result.userAgent
                    mergedHeaders["User-Agent"] = result.userAgent
                }
                val updatedCookies = cookieJar.filter { domain.endsWith(it.key.substringBefore(':')) || it.key == domain }
                mergedHeaders["Cookie"] = updatedCookies.values.joinToString("; ")
                response = performRequest(client, url, method, mergedHeaders, body, timeoutMs)
            } else {
                diagnosticLogs.add("[JsBridge] Anti-bot resolution failed or cancelled: ${result.errorMessage}")
            }
        }

        return response
    }

    private suspend fun performRequest(
        client: HttpClient,
        url: String,
        method: String,
        headersMap: Map<String, String>,
        body: String?,
        timeoutMs: Long
    ): JsHttpResponse {
        return try {
            withTimeout(timeoutMs) {
                val httpResponse: HttpResponse = client.request(url) {
                    this.method = HttpMethod.parse(method.uppercase())
                    headers {
                        headersMap.forEach { (k, v) -> append(k, v) }
                    }
                    if (body != null) {
                        setBody(body)
                    }
                }

                // Extract Set-Cookie headers
                val domain = extractDomain(url)
                val setCookies = httpResponse.headers.getAll("Set-Cookie") ?: emptyList()
                for (sc in setCookies) {
                    val cookiePair = sc.substringBefore(';')
                    val name = cookiePair.substringBefore('=').trim()
                    val value = cookiePair.substringAfter('=').trim()
                    if (name.isNotEmpty()) {
                        cookieJar["$domain:$name"] = "$name=$value"
                    }
                }

                val responseBody = httpResponse.bodyAsText()
                val respHeaders = mutableMapOf<String, String>()
                httpResponse.headers.forEach { k, v ->
                    respHeaders[k] = v.joinToString(", ")
                }

                JsHttpResponse(
                    status = httpResponse.status.value,
                    ok = httpResponse.status.value in 200..299,
                    headers = respHeaders,
                    body = responseBody
                )
            }
        } catch (e: Exception) {
            diagnosticLogs.add("[JsBridge] HTTP request error for $url: ${e.message}")
            JsHttpResponse(
                status = 0,
                ok = false,
                headers = emptyMap(),
                body = e.message ?: "Request failed"
            )
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            val noScheme = if (url.contains("://")) url.substringAfter("://") else url
            noScheme.substringBefore('/').substringBefore(':').substringBefore('?')
        } catch (_: Exception) {
            ""
        }
    }

    fun isCloudflareChallenge(html: String): Boolean {
        val lower = html.lowercase()
        return (lower.contains("cf-chl-bypass") ||
            lower.contains("just a moment...") ||
            (lower.contains("cloudflare") && lower.contains("ray id")) ||
            lower.contains("challenge-running"))
    }

    fun btoa(str: String): String {
        return Base64Util.encode(str.encodeToByteArray())
    }

    fun atob(base64: String): String {
        return Base64Util.decode(base64).decodeToString()
    }

    fun sha256(input: String): String {
        return Sha256.hashHex(input)
    }

    fun log(level: String, message: String) {
        val entry = "[$level] $message"
        diagnosticLogs.add(entry)
        println("[JsBridge] $entry")
    }

    /**
     * JavaScript preamble injected into the JS runtime environment to wire up
     * Cheerio DOM parsing, dokuha.fetch, crypto, encoding, and console logging.
     */
    fun getPreambleScript(): String = """
(function() {
    if (typeof globalThis.dokuha === 'undefined') {
        globalThis.dokuha = {};
    }

    // Console redirection
    const _log = (lvl, ...args) => {
        const msg = args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ');
        if (typeof globalThis.__dokuha_log === 'function') {
            globalThis.__dokuha_log(lvl, msg);
        }
    };
    globalThis.console = {
        log: (...args) => _log('LOG', ...args),
        warn: (...args) => _log('WARN', ...args),
        error: (...args) => _log('ERROR', ...args),
        info: (...args) => _log('INFO', ...args)
    };

    // Encoding & Crypto
    if (typeof globalThis.btoa === 'undefined') {
        globalThis.btoa = (str) => globalThis.__dokuha_btoa(str);
    }
    if (typeof globalThis.atob === 'undefined') {
        globalThis.atob = (b64) => globalThis.__dokuha_atob(b64);
    }
    if (typeof globalThis.crypto === 'undefined') {
        globalThis.crypto = {};
    }
    globalThis.crypto.sha256 = (str) => globalThis.__dokuha_sha256(str);

    // Dokuha Fetch Bridge
    globalThis.dokuha.fetch = async (url, options = {}) => {
        const method = options.method || 'GET';
        const headers = options.headers || {};
        const body = options.body || null;
        const resStr = await globalThis.__dokuha_fetch(url, method, JSON.stringify(headers), body);
        const res = JSON.parse(resStr);
        return {
            status: res.status,
            ok: res.ok,
            headers: res.headers,
            text: async () => res.body,
            json: async () => JSON.parse(res.body)
        };
    };

    // Lightweight Cheerio-compatible DOM parser
    globalThis.dokuha.parseHtml = function(htmlString) {
        function tokenize(html) {
            const tokens = [];
            let i = 0;
            while (i < html.length) {
                if (html[i] === '<') {
                    if (html.startsWith('<!--', i)) {
                        const end = html.indexOf('-->', i);
                        i = end === -1 ? html.length : end + 3;
                        continue;
                    }
                    if (html.startsWith('<!', i)) {
                        const end = html.indexOf('>', i);
                        i = end === -1 ? html.length : end + 1;
                        continue;
                    }
                    const isClose = html[i + 1] === '/';
                    const start = isClose ? i + 2 : i + 1;
                    const end = html.indexOf('>', start);
                    if (end === -1) break;
                    const rawTag = html.slice(start, end).trim();
                    const isSelfClosing = rawTag.endsWith('/') || ['img', 'br', 'hr', 'input', 'meta', 'link'].includes(rawTag.split(/\s+/)[0].toLowerCase());
                    const tagBody = rawTag.endsWith('/') ? rawTag.slice(0, -1).trim() : rawTag;
                    const tagParts = tagBody.split(/\s+/);
                    const tagName = tagParts[0].toLowerCase();

                    const attrs = {};
                    const attrRegex = /([a-zA-Z0-9_:-]+)(?:=(?:"([^"]*)"|'([^']*)'|([^\s>]+)))?/g;
                    let match;
                    const attrStr = tagBody.slice(tagName.length);
                    while ((match = attrRegex.exec(attrStr)) !== null) {
                        const name = match[1].toLowerCase();
                        const val = match[2] !== undefined ? match[2] : match[3] !== undefined ? match[3] : match[4] !== undefined ? match[4] : '';
                        attrs[name] = val;
                    }

                    tokens.push({
                        type: isClose ? 'close' : 'open',
                        tag: tagName,
                        attrs: attrs,
                        selfClosing: isSelfClosing
                    });
                    i = end + 1;
                } else {
                    const nextTag = html.indexOf('<', i);
                    const text = nextTag === -1 ? html.slice(i) : html.slice(i, nextTag);
                    if (text) {
                        tokens.push({ type: 'text', content: text });
                    }
                    i = nextTag === -1 ? html.length : nextTag;
                }
            }
            return tokens;
        }

        function buildTree(tokens) {
            const root = { tag: 'root', attrs: {}, children: [], parent: null };
            let current = root;
            for (const token of tokens) {
                if (token.type === 'open') {
                    const node = {
                        tag: token.tag,
                        attrs: token.attrs,
                        children: [],
                        parent: current
                    };
                    current.children.push(node);
                    if (!token.selfClosing) {
                        current = node;
                    }
                } else if (token.type === 'close') {
                    let p = current;
                    while (p && p.tag !== token.tag && p !== root) {
                        p = p.parent;
                    }
                    if (p && p !== root) {
                        current = p.parent || root;
                    }
                } else if (token.type === 'text') {
                    current.children.push({
                        tag: '#text',
                        content: token.content,
                        parent: current
                    });
                }
            }
            return root;
        }

        function matchesSimple(node, selector) {
            if (!node || node.tag === '#text' || node.tag === 'root') return false;
            let rem = selector.trim();
            if (rem === '*' || rem === '') return true;

            const tagMatch = rem.match(/^[a-zA-Z0-9_-]+/);
            if (tagMatch) {
                if (node.tag !== tagMatch[0].toLowerCase()) return false;
                rem = rem.slice(tagMatch[0].length);
            }

            const idMatch = rem.match(/^#([a-zA-Z0-9_-]+)/);
            if (idMatch) {
                if (node.attrs['id'] !== idMatch[1]) return false;
                rem = rem.slice(idMatch[0].length);
            }

            while (true) {
                const classMatch = rem.match(/^\.([a-zA-Z0-9_-]+)/);
                if (!classMatch) break;
                const classAttr = node.attrs['class'] || '';
                const classes = classAttr.split(/\s+/);
                if (!classes.includes(classMatch[1])) return false;
                rem = rem.slice(classMatch[0].length);
            }

            while (true) {
                const attrMatch = rem.match(/^\[([a-zA-Z0-9_-]+)(?:([*^$]?=)(?:"([^"]*)"|'([^']*)'|([^\]]+)))?\]/);
                if (!attrMatch) break;
                const aName = attrMatch[1].toLowerCase();
                const op = attrMatch[2];
                const aVal = attrMatch[3] !== undefined ? attrMatch[3] : attrMatch[4] !== undefined ? attrMatch[4] : attrMatch[5];
                if (!(aName in node.attrs)) return false;
                if (op) {
                    const nodeVal = node.attrs[aName];
                    if (op === '=' && nodeVal !== aVal) return false;
                    if (op === '*=' && !nodeVal.includes(aVal)) return false;
                    if (op === '^=' && !nodeVal.startsWith(aVal)) return false;
                    if (op === '$=' && !nodeVal.endsWith(aVal)) return false;
                }
                rem = rem.slice(attrMatch[0].length);
            }

            return rem.length === 0;
        }

        function query(rootNode, selector) {
            const subSelectors = selector.split(',').map(s => s.trim()).filter(Boolean);
            if (subSelectors.length > 1) {
                const resultSet = [];
                for (const sel of subSelectors) {
                    for (const n of query(rootNode, sel)) {
                        if (!resultSet.includes(n)) resultSet.push(n);
                    }
                }
                return resultSet;
            }

            const parts = selector.trim().split(/\s+/);
            let currentNodes = [rootNode];

            for (let pIdx = 0; pIdx < parts.length; pIdx++) {
                const part = parts[pIdx];
                if (part === '>') {
                    const nextPart = parts[++pIdx];
                    const nextNodes = [];
                    for (const n of currentNodes) {
                        if (n.children) {
                            for (const child of n.children) {
                                if (matchesSimple(child, nextPart) && !nextNodes.includes(child)) {
                                    nextNodes.push(child);
                                }
                            }
                        }
                    }
                    currentNodes = nextNodes;
                } else {
                    const nextNodes = [];
                    function collect(n) {
                        if (!n || !n.children) return;
                        for (const child of n.children) {
                            if (matchesSimple(child, part) && !nextNodes.includes(child)) {
                                nextNodes.push(child);
                            }
                            collect(child);
                        }
                    }
                    for (const n of currentNodes) {
                        collect(n);
                    }
                    currentNodes = nextNodes;
                }
            }
            return currentNodes;
        }

        function nodeText(node) {
            if (!node) return '';
            if (node.tag === '#text') return node.content || '';
            if (!node.children) return '';
            return node.children.map(nodeText).join('');
        }

        function nodeHtml(node) {
            if (!node) return '';
            if (node.tag === '#text') return node.content || '';
            if (node.tag === 'root') return (node.children || []).map(nodeHtml).join('');
            const attrs = Object.entries(node.attrs || {}).map(([k, v]) => ' ' + k + '="' + v + '"').join('');
            const inner = (node.children || []).map(nodeHtml).join('');
            if (['img', 'br', 'hr', 'input'].includes(node.tag)) {
                return '<' + node.tag + attrs + '/>';
            }
            return '<' + node.tag + attrs + '>' + inner + '</' + node.tag + '>';
        }

        function wrap(nodes) {
            const list = Array.isArray(nodes) ? nodes : [nodes];
            const fn = function(sel) {
                if (typeof sel === 'string') {
                    const found = [];
                    for (const n of list) {
                        for (const item of query(n, sel)) {
                            if (!found.includes(item)) found.push(item);
                        }
                    }
                    return wrap(found);
                }
                return wrap(sel);
            };

            fn.length = list.length;
            fn.nodes = list;

            fn.text = function() {
                return list.map(nodeText).join(' ').trim();
            };

            fn.html = function() {
                return list.length > 0 ? nodeHtml(list[0]) : '';
            };

            fn.attr = function(name) {
                if (list.length === 0 || !list[0].attrs) return undefined;
                return list[0].attrs[name.toLowerCase()];
            };

            fn.find = function(sel) {
                const found = [];
                for (const n of list) {
                    for (const item of query(n, sel)) {
                        if (!found.includes(item)) found.push(item);
                    }
                }
                return wrap(found);
            };

            fn.each = function(callback) {
                for (let i = 0; i < list.length; i++) {
                    callback(i, list[i]);
                }
                return fn;
            };

            fn.map = function(callback) {
                const results = [];
                for (let i = 0; i < list.length; i++) {
                    results.push(callback(i, list[i]));
                }
                return results;
            };

            fn.first = function() {
                return wrap(list.slice(0, 1));
            };

            fn.last = function() {
                return wrap(list.slice(-1));
            };

            fn.eq = function(i) {
                return wrap(list.slice(i, i + 1));
            };

            return fn;
        }

        const tokens = tokenize(htmlString);
        const root = buildTree(tokens);
        return wrap(root);
    };
})();
    """.trimIndent()
}

/**
 * Pure Kotlin Base64 implementation.
 */
object Base64Util {
    private const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun encode(bytes: ByteArray): String {
        val sb = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i++].toInt() and 0xFF
            val b1 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else -1
            val b2 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else -1

            sb.append(CHARS[b0 ushr 2])
            if (b1 != -1) {
                sb.append(CHARS[((b0 and 0x03) shl 4) or (b1 ushr 4)])
                if (b2 != -1) {
                    sb.append(CHARS[((b1 and 0x0F) shl 2) or (b2 ushr 6)])
                    sb.append(CHARS[b2 and 0x3F])
                } else {
                    sb.append(CHARS[(b1 and 0x0F) shl 2])
                    sb.append('=')
                }
            } else {
                sb.append(CHARS[(b0 and 0x03) shl 4])
                sb.append("==")
            }
        }
        return sb.toString()
    }

    fun decode(str: String): ByteArray {
        val cleaned = str.replace("\n", "").replace("\r", "").replace(" ", "")
        val out = mutableListOf<Byte>()
        var i = 0
        while (i < cleaned.length) {
            val c0 = CHARS.indexOf(cleaned[i++])
            val c1 = if (i < cleaned.length) CHARS.indexOf(cleaned[i++]) else -1
            val c2 = if (i < cleaned.length) CHARS.indexOf(cleaned[i++]) else -1
            val c3 = if (i < cleaned.length) CHARS.indexOf(cleaned[i++]) else -1

            if (c0 != -1 && c1 != -1) {
                out.add(((c0 shl 2) or (c1 ushr 4)).toByte())
                if (c2 != -1) {
                    out.add((((c1 and 0x0F) shl 4) or (c2 ushr 2)).toByte())
                    if (c3 != -1) {
                        out.add((((c2 and 0x03) shl 6) or c3).toByte())
                    }
                }
            }
        }
        return out.toByteArray()
    }
}
