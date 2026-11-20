package core.extensions.js

import core.extensions.Sha256
import core.storage.KeyValueStorage
import core.storage.SecureStorage
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

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
    private val storage: KeyValueStorage? = null,
    private val secureStorage: SecureStorage? = null,
    val diagnosticLogs: MutableList<String> = mutableListOf()
) {
    private val cookieJar = mutableMapOf<String, String>()
    private val inMemoryStorage = mutableMapOf<String, String>()
    private val inMemorySecureStorage = mutableMapOf<String, String>()
    private val json = Json { ignoreUnknownKeys = true }
    private var currentUserAgent: String =
        "Dokuha/1.0.0 (https://github.com/RitwikGupta-0501/dokuha)"

    /**
     * Registers host functions into a JsEngine instance for this bridge.
     */
    fun registerFunctions(engine: JsEngine, sourceId: Long = 0L) {
        engine.setGlobalFunction("__dokuha_log") { args ->
            val lvl = args.getOrNull(0)?.toString() ?: "LOG"
            val msg = args.getOrNull(1)?.toString() ?: ""
            log(lvl, msg)
            null
        }
        engine.setGlobalFunction("__dokuha_btoa") { args ->
            val str = args.getOrNull(0)?.toString() ?: ""
            btoa(str)
        }
        engine.setGlobalFunction("__dokuha_atob") { args ->
            val str = args.getOrNull(0)?.toString() ?: ""
            atob(str)
        }
        engine.setGlobalFunction("__dokuha_sha256") { args ->
            val str = args.getOrNull(0)?.toString() ?: ""
            sha256(str)
        }
        engine.setGlobalFunction("__dokuha_storage_get") { args ->
            val key = args.getOrNull(0)?.toString() ?: ""
            storage?.getString(key, "") ?: inMemoryStorage[key] ?: ""
        }
        engine.setGlobalFunction("__dokuha_storage_set") { args ->
            val key = args.getOrNull(0)?.toString() ?: ""
            val value = args.getOrNull(1)?.toString() ?: ""
            if (storage != null) {
                storage.putString(key, value)
            } else {
                inMemoryStorage[key] = value
            }
            null
        }
        engine.setGlobalFunction("__dokuha_secure_get") { args ->
            val key = args.getOrNull(0)?.toString() ?: ""
            secureStorage?.getSecret(key) ?: inMemorySecureStorage[key] ?: ""
        }
        engine.setGlobalFunction("__dokuha_secure_set") { args ->
            val key = args.getOrNull(0)?.toString() ?: ""
            val value = args.getOrNull(1)?.toString() ?: ""
            if (secureStorage != null) {
                secureStorage.putSecret(key, value)
            } else {
                inMemorySecureStorage[key] = value
            }
            null
        }
        engine.setGlobalAsyncFunction("__dokuha_sleep") { args ->
            val ms = args.getOrNull(0)?.toString()?.toLongOrNull() ?: 0L
            if (ms > 0) {
                kotlinx.coroutines.delay(ms)
            }
            null
        }
        engine.setGlobalAsyncFunction("__dokuha_fetch") { args ->
            val url = args.getOrNull(0)?.toString() ?: ""
            val method = args.getOrNull(1)?.toString() ?: "GET"
            val headersJson = args.getOrNull(2)?.toString() ?: "{}"
            val body = args.getOrNull(3)?.toString()
            val headersMap = try {
                if (headersJson.isNotEmpty() && headersJson != "{}") {
                    val elem = json.parseToJsonElement(headersJson).jsonObject
                    elem.mapValues { it.value.jsonPrimitive.content }
                } else emptyMap()
            } catch (_: Exception) {
                emptyMap()
            }
            val res = fetch(url, method, headersMap, body)
            buildJsonObject {
                put("status", res.status)
                put("ok", res.ok)
                putJsonObject("headers") {
                    res.headers.forEach { (k, v) -> put(k, v) }
                }
                put("body", res.body)
            }.toString()
        }
    }

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
        val relevantCookies = cookieJar.filter {
            val cookieDomain = it.key.substringBefore(':')
            isDomainMatch(cookieDomain, domain)
        }
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
                val updatedCookies = cookieJar.filter {
                    val cookieDomain = it.key.substringBefore(':')
                    isDomainMatch(cookieDomain, domain)
                }
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

    fun isDomainMatch(cookieDomain: String, requestDomain: String): Boolean {
        val cDom = cookieDomain.removePrefix(".").lowercase()
        val rDom = requestDomain.lowercase()
        return cDom == rDom || rDom.endsWith(".$cDom")
    }

    fun isCloudflareChallenge(html: String): Boolean {
        val lower = html.lowercase()
        return (lower.contains("cf-chl-bypass") ||
            lower.contains("just a moment...") ||
            (lower.contains("cloudflare") && lower.contains("ray id")) ||
            lower.contains("challenge-running") ||
            lower.contains("cf-turnstile") ||
            lower.contains("challenges.cloudflare.com") ||
            lower.contains("cf-chl-widget") ||
            lower.contains("turnstile.render") ||
            lower.contains("cf_chl_prog"))
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
     * DOM parsing, dokuha.fetch, crypto, encoding, CommonJS require, and ecosystem shims.
     */
    fun getPreambleScript(sourceId: Long = 0L): String = """
(function() {
    if (typeof globalThis.dokuha === 'undefined') {
        globalThis.dokuha = {};
    }
    globalThis.__dokuha_source_id = "$sourceId";

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
        info: (...args) => _log('INFO', ...args),
        debug: (...args) => _log('DEBUG', ...args)
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

    // Async Sleep & Timers
    globalThis.dokuha.sleep = async (ms) => {
        if (typeof globalThis.__dokuha_sleep === 'function') {
            await globalThis.__dokuha_sleep(ms || 0);
        }
    };
    if (typeof globalThis.setTimeout === 'undefined') {
        globalThis.setTimeout = (fn, delay) => {
            (async () => {
                if (typeof globalThis.__dokuha_sleep === 'function') {
                    await globalThis.__dokuha_sleep(delay || 0);
                }
                if (typeof fn === 'function') fn();
            })();
            return 0;
        };
        globalThis.clearTimeout = () => {};
    }

    // Fetch Standard Headers Polyfill
    class Headers {
        constructor(init = {}) {
            this._map = {};
            if (init instanceof Headers) {
                for (const [k, v] of init.entries()) this.set(k, v);
            } else if (Array.isArray(init)) {
                for (const [k, v] of init) this.set(k, v);
            } else if (typeof init === 'object' && init !== null) {
                for (const k of Object.keys(init)) this.set(k, init[k]);
            }
        }
        get(name) {
            const key = String(name).toLowerCase();
            return this._map[key] !== undefined ? this._map[key] : null;
        }
        has(name) {
            return String(name).toLowerCase() in this._map;
        }
        set(name, value) {
            this._map[String(name).toLowerCase()] = String(value);
        }
        append(name, value) {
            const key = String(name).toLowerCase();
            if (this._map[key] !== undefined) {
                this._map[key] += ', ' + String(value);
            } else {
                this._map[key] = String(value);
            }
        }
        delete(name) {
            delete this._map[String(name).toLowerCase()];
        }
        entries() {
            return Object.entries(this._map);
        }
        keys() {
            return Object.keys(this._map);
        }
        values() {
            return Object.values(this._map);
        }
    }
    globalThis.Headers = Headers;

    // Storage Key Sandboxing Helper
    const _getStorageKey = (k) => {
        const srcId = globalThis.__dokuha_source_id || "$sourceId";
        return "ext_" + srcId + "_" + k;
    };

    // Dokuha Fetch Bridge
    globalThis.dokuha.fetch = async (url, options = {}) => {
        const method = options.method || 'GET';
        let headersObj = {};
        if (options.headers instanceof Headers) {
            for (const [k, v] of options.headers.entries()) headersObj[k] = v;
        } else if (options.headers && typeof options.headers === 'object') {
            headersObj = options.headers;
        }
        const body = (typeof options.body === 'object' && options.body !== null) ? JSON.stringify(options.body) : (options.body != null ? String(options.body) : null);
        const resStr = await globalThis.__dokuha_fetch(url, method, JSON.stringify(headersObj), body);
        const res = JSON.parse(resStr);
        return {
            status: res.status,
            statusCode: res.status,
            ok: res.ok,
            headers: new Headers(res.headers),
            body: res.body,
            text: async () => res.body,
            json: async () => JSON.parse(res.body)
        };
    };
    if (typeof globalThis.fetch === 'undefined') {
        globalThis.fetch = globalThis.dokuha.fetch;
    }

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
                    const attrRegex = /([a-zA-Z0-9_:-]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+)))?/g;
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
                const attrMatch = rem.match(/^\[([a-zA-Z0-9_:-]+)(?:\s*([*^${'$'}?]=)\s*(?:"([^"]*)"|'([^']*)'|([^\]]+)))?\]/);
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
                    if (op === '${'$'}=' && !nodeVal.endsWith(aVal)) return false;
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

    // Mangayomi & Universal DOM Element / Document Classes
    function resolveRelativeUrl(url, baseUrl) {
        if (!url || url.startsWith("http://") || url.startsWith("https://")) return url;
        if (url.startsWith("//")) return "https:" + url;
        if (!baseUrl) return url;
        const cleanBase = baseUrl.endsWith('/') ? baseUrl.substring(0, baseUrl.length - 1) : baseUrl;
        if (url.startsWith('/')) return cleanBase + url;
        return cleanBase + '/' + url;
    }

    class Element {
        constructor(cheerioEl, cheerioRoot, baseUrl = "") {
            this._el = cheerioEl;
            this._root = cheerioRoot;
            this._baseUrl = baseUrl;
        }

        select(selector) {
            const elements = [];
            const found = this._el.find(selector);
            found.each((_, node) => {
                elements.push(new Element(this._root(node), this._root, this._baseUrl));
            });
            return elements;
        }

        selectFirst(selector) {
            const found = this._el.find(selector).first();
            return found.length > 0 ? new Element(found, this._root, this._baseUrl) : null;
        }

        xpath(xpathQuery) {
            const css = xpathQuery.replace(/\/\//g, ' ').replace(/\//g, ' > ').replace(/@([a-zA-Z-]+)/g, '[$1]');
            return this.select(css);
        }

        xpathFirst(xpathQuery) {
            const items = this.xpath(xpathQuery);
            return items.length > 0 ? items[0] : null;
        }

        attr(name) {
            const val = this._el.attr(name);
            return val !== undefined ? val : null;
        }

        get text() {
            return (this._el.text() || "").trim();
        }

        get innerHtml() {
            return this._el.html() || "";
        }

        get outerHtml() {
            return this._el.html() || "";
        }

        get getSrc() {
            const src = this._el.attr("src") || 
                        this._el.attr("data-src") || 
                        this._el.attr("data-lazy-src") || 
                        this._el.attr("data-original") || 
                        this._el.attr("data-cfsrc") || "";
            return resolveRelativeUrl(src.trim(), this._baseUrl);
        }

        get getHref() {
            const href = this._el.attr("href") || "";
            return resolveRelativeUrl(href.trim(), this._baseUrl);
        }

        get getDataSrc() {
            const src = this._el.attr("data-src") || this._el.attr("data-original") || this.getSrc;
            return resolveRelativeUrl(src.trim(), this._baseUrl);
        }

        get getImg() {
            const img = this.selectFirst("img");
            return img ? img.getSrc : this.getSrc;
        }

        get children() {
            const elements = [];
            const kids = this._el.nodes ? (this._el.nodes[0]?.children || []) : [];
            for (const child of kids) {
                if (child.tag !== '#text') {
                    elements.push(new Element(this._root(child), this._root, this._baseUrl));
                }
            }
            return elements;
        }

        get nextElementSibling() {
            const parent = this._el.nodes ? this._el.nodes[0]?.parent : null;
            if (!parent || !parent.children) return null;
            const idx = parent.children.indexOf(this._el.nodes[0]);
            if (idx !== -1) {
                for (let i = idx + 1; i < parent.children.length; i++) {
                    if (parent.children[i].tag !== '#text') {
                        return new Element(this._root(parent.children[i]), this._root, this._baseUrl);
                    }
                }
            }
            return null;
        }

        get previousElementSibling() {
            const parent = this._el.nodes ? this._el.nodes[0]?.parent : null;
            if (!parent || !parent.children) return null;
            const idx = parent.children.indexOf(this._el.nodes[0]);
            if (idx > 0) {
                for (let i = idx - 1; i >= 0; i--) {
                    if (parent.children[i].tag !== '#text') {
                        return new Element(this._root(parent.children[i]), this._root, this._baseUrl);
                    }
                }
            }
            return null;
        }
    }

    class Document {
        constructor(htmlString) {
            this._root = globalThis.dokuha.parseHtml(htmlString);
            this._rawHtml = htmlString;
            let meta = globalThis.__mangayomi_source_meta;
            if (typeof meta === 'string') {
                try { meta = JSON.parse(meta); } catch(_) { meta = null; }
            }
            this._baseUrl = (meta && meta.baseUrl) || globalThis.__mangayomi_source_baseUrl || "";
        }

        select(selector) {
            const elements = [];
            this._root(selector).each((_, node) => {
                elements.push(new Element(this._root(node), this._root, this._baseUrl));
            });
            return elements;
        }

        selectFirst(selector) {
            const found = this._root(selector).first();
            return found.length > 0 ? new Element(found, this._root, this._baseUrl) : null;
        }

        xpath(xpathQuery) {
            const css = xpathQuery.replace(/\/\//g, ' ').replace(/\//g, ' > ').replace(/@([a-zA-Z-]+)/g, '[$1]');
            return this.select(css);
        }

        xpathFirst(xpathQuery) {
            const items = this.xpath(xpathQuery);
            return items.length > 0 ? items[0] : null;
        }

        getElementById(id) {
            return this.selectFirst('#' + id);
        }

        getElementsByClassName(cls) {
            return this.select('.' + cls);
        }

        get text() {
            return (this._root.text() || "").trim();
        }

        get body() {
            return this.selectFirst("body");
        }

        get outerHtml() {
            return this._rawHtml;
        }
    }

    globalThis.Document = Document;
    globalThis.Element = Element;

    // CommonJS Module Emulation (LNReader Support)
    globalThis.module = { exports: {} };
    globalThis.exports = globalThis.module.exports;

    const _moduleRegistry = {};
    globalThis.dokuha.registerModule = function(id, exportsObj) {
        _moduleRegistry[id] = exportsObj;
    };

    // Register standard LNReader @libs
    globalThis.dokuha.registerModule('@libs/fetch', {
        fetchApi: async (url, init = {}) => globalThis.dokuha.fetch(url, init),
        fetchText: async (url, init = {}) => {
            const res = await globalThis.dokuha.fetch(url, init);
            return res.body;
        },
        fetchFile: async (url, init = {}) => {
            const res = await globalThis.dokuha.fetch(url, init);
            return res.body;
        }
    });

    globalThis.dokuha.registerModule('@libs/storage', {
        storage: {
            get: (key) => {
                const val = globalThis.__dokuha_storage_get(_getStorageKey(key));
                try { return val ? JSON.parse(val) : null; } catch (_) { return val; }
            },
            set: (key, val) => {
                globalThis.__dokuha_storage_set(_getStorageKey(key), JSON.stringify(val));
            }
        }
    });

    globalThis.dokuha.registerModule('@libs/novelStatus', {
        NovelStatus: {
            Ongoing: 'Ongoing',
            Completed: 'Completed',
            Unknown: 'Unknown'
        }
    });

    globalThis.cheerio = {
        load: function(html) {
            return globalThis.dokuha.parseHtml(html);
        }
    };

    globalThis.require = function(id) {
        if (_moduleRegistry[id]) {
            return _moduleRegistry[id];
        }
        if (id === 'cheerio') return globalThis.cheerio;
        if (id === 'htmlparser2') return { parseDOM: globalThis.dokuha.parseHtml };
        if (id === 'dayjs') {
            const dayjsFn = function(d) {
                return {
                    format: function(fmt) { return String(d || ''); },
                    valueOf: function() { return Date.now(); }
                };
            };
            return dayjsFn;
        }
        if (id === 'urlencode') return {
            encode: (s) => encodeURIComponent(s),
            decode: (s) => decodeURIComponent(s)
        };
        if (id.startsWith('@libs/')) {
            const sub = id.substring(6);
            if (globalThis.dokuha.libs && globalThis.dokuha.libs[sub]) {
                return globalThis.dokuha.libs[sub];
            }
        }
        throw new Error("[Dokuha CommonJS] Cannot resolve module: " + id);
    };

    // Mangayomi Host Environment Shims
    globalThis.MProvider = class MProvider {
        constructor() {
            let meta = globalThis.__mangayomi_source_meta;
            if (typeof meta === 'string') {
                try { meta = JSON.parse(meta); } catch(_) { meta = {}; }
            }
            this.source = meta || {};
        }
        getHeaders(url) { return {}; }
    };

    globalThis.Client = class Client {
        async get(url, headers = {}) {
            let targetUrl = url;
            if (typeof targetUrl === 'string') {
                if (targetUrl.startsWith("null/") || targetUrl.startsWith("undefined/")) {
                    const base = (globalThis.__mangayomi_source_baseUrl || (typeof globalThis.__mangayomi_source_meta === 'object' ? globalThis.__mangayomi_source_meta?.baseUrl : null) || "");
                    targetUrl = base + targetUrl.substring(targetUrl.indexOf('/'));
                } else if (targetUrl.startsWith("/")) {
                    const base = (globalThis.__mangayomi_source_baseUrl || (typeof globalThis.__mangayomi_source_meta === 'object' ? globalThis.__mangayomi_source_meta?.baseUrl : null) || "");
                    targetUrl = base + targetUrl;
                }
            }
            const res = await globalThis.dokuha.fetch(targetUrl, { method: 'GET', headers });
            return {
                statusCode: res.status,
                body: res.body,
                headers: res.headers,
                isRedirect: false,
                reasonPhrase: res.ok ? "OK" : "Error"
            };
        }
        async post(url, headers = {}, body = null) {
            let targetUrl = url;
            if (typeof targetUrl === 'string') {
                if (targetUrl.startsWith("null/") || targetUrl.startsWith("undefined/")) {
                    const base = (globalThis.__mangayomi_source_baseUrl || (typeof globalThis.__mangayomi_source_meta === 'object' ? globalThis.__mangayomi_source_meta?.baseUrl : null) || "");
                    targetUrl = base + targetUrl.substring(targetUrl.indexOf('/'));
                } else if (targetUrl.startsWith("/")) {
                    const base = (globalThis.__mangayomi_source_baseUrl || (typeof globalThis.__mangayomi_source_meta === 'object' ? globalThis.__mangayomi_source_meta?.baseUrl : null) || "");
                    targetUrl = base + targetUrl;
                }
            }
            const res = await globalThis.dokuha.fetch(targetUrl, { method: 'POST', headers, body });
            return {
                statusCode: res.status,
                body: res.body,
                headers: res.headers,
                isRedirect: false,
                reasonPhrase: res.ok ? "OK" : "Error"
            };
        }
    };

    // String Prototype Helpers (Kotlin style)
    String.prototype.substringAfter = function(p) {
        const idx = this.indexOf(p);
        return idx === -1 ? this.toString() : this.substring(idx + p.length);
    };
    String.prototype.substringAfterLast = function(p) {
        const idx = this.lastIndexOf(p);
        return idx === -1 ? this.toString() : this.substring(idx + p.length);
    };
    String.prototype.substringBefore = function(p) {
        const idx = this.indexOf(p);
        return idx === -1 ? this.toString() : this.substring(0, idx);
    };
    String.prototype.substringBeforeLast = function(p) {
        const idx = this.lastIndexOf(p);
        return idx === -1 ? this.toString() : this.substring(0, idx);
    };
    String.prototype.substringBetween = function(left, right) {
        const start = this.indexOf(left);
        if (start === -1) return "";
        const end = this.indexOf(right, start + left.length);
        if (end === -1) return "";
        return this.substring(start + left.length, end);
    };

    // SharedPreferences Shim (Sandboxed per Extension with fallback to extension preferences & manifest baseUrl)
    globalThis.SharedPreferences = class SharedPreferences {
        get(key) {
            const val = globalThis.__dokuha_storage_get(_getStorageKey(key));
            if (val !== "" && val !== null && val !== undefined) return val;

            if (typeof globalThis.extention !== 'undefined' && typeof globalThis.extention.getSourcePreferences === 'function') {
                try {
                    const prefs = globalThis.extention.getSourcePreferences();
                    if (Array.isArray(prefs)) {
                        for (const pref of prefs) {
                            if (pref && pref.key === key) {
                                const listVal = pref.listPreference ? (pref.listPreference.value || (pref.listPreference.entryValues && pref.listPreference.entryValues[pref.listPreference.valueIndex || 0])) : undefined;
                                const editVal = pref.editTextPreference ? pref.editTextPreference.value : undefined;
                                const checkVal = pref.checkBoxPreference ? pref.checkBoxPreference.value : undefined;
                                const def = listVal !== undefined ? listVal : (editVal !== undefined ? editVal : (checkVal !== undefined ? checkVal : pref.value));
                                if (def !== undefined && def !== null && def !== "") return def;
                            }
                        }
                    }
                } catch (_) {}
            }

            if (typeof key === 'string' && key.toLowerCase().includes("baseurl")) {
                if (typeof globalThis.__mangayomi_source_baseUrl === 'string' && globalThis.__mangayomi_source_baseUrl) {
                    return globalThis.__mangayomi_source_baseUrl;
                }
                let meta = globalThis.__mangayomi_source_meta;
                if (typeof meta === 'string') {
                    try { meta = JSON.parse(meta); } catch(_) { meta = null; }
                }
                if (meta && meta.baseUrl) return meta.baseUrl;
            }
            return null;
        }
        getString(key, def) {
            const val = this.get(key);
            return val !== null && val !== undefined ? String(val) : def;
        }
        setString(key, val) {
            globalThis.__dokuha_storage_set(_getStorageKey(key), String(val));
        }
    };

    // Paperback v0.8 & v0.9 Host Environment Shims
    globalThis.Sources = {};
    globalThis.window = globalThis;
    globalThis.window.Sources = globalThis.Sources;

    // Paperback v0.8 App Factory Object
    globalThis.App = {
        createRequestManager: (info) => ({
            requestsPerSecond: info?.requestsPerSecond || 5,
            schedule: async (req) => {
                const res = await globalThis.dokuha.fetch(req.url, {
                    method: req.method || 'GET',
                    headers: req.headers || {},
                    body: req.data
                });
                return {
                    status: res.status,
                    headers: res.headers,
                    data: res.body,
                    cookies: []
                };
            }
        }),
        createSourceStateManager: () => ({
            store: async (k, v) => globalThis.__dokuha_storage_set(_getStorageKey(k), JSON.stringify(v)),
            retrieve: async (k) => {
                const v = globalThis.__dokuha_storage_get(_getStorageKey(k));
                return v ? JSON.parse(v) : null;
            },
            keychain: {
                store: async (k, v) => globalThis.__dokuha_secure_set(_getStorageKey(k), JSON.stringify(v)),
                retrieve: async (k) => {
                    const v = globalThis.__dokuha_secure_get(_getStorageKey(k));
                    return v ? JSON.parse(v) : null;
                }
            }
        }),
        createRequest: (obj) => obj,
        createSourceManga: (obj) => obj,
        createChapter: (obj) => obj,
        createChapterDetails: (obj) => obj,
        createHomeSection: (obj) => obj,
        createPagedResults: (obj) => obj
    };

    // Paperback v0.9 Application Namespace
    globalThis.Application = {
        scheduleRequest: async (req) => {
            const res = await globalThis.dokuha.fetch(req.url, {
                method: req.method || 'GET',
                headers: req.headers || {},
                body: req.data
            });
            return [{ status: res.status, headers: res.headers }, res.body];
        },
        getState: (k) => {
            const v = globalThis.__dokuha_storage_get(_getStorageKey(k));
            return v ? JSON.parse(v) : undefined;
        },
        setState: (v, k) => globalThis.__dokuha_storage_set(_getStorageKey(k), JSON.stringify(v)),
        getSecureState: (k) => {
            const v = globalThis.__dokuha_secure_get(_getStorageKey(k));
            return v ? JSON.parse(v) : undefined;
        },
        setSecureState: (v, k) => globalThis.__dokuha_secure_set(_getStorageKey(k), JSON.stringify(v))
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
        val cleaned = str.filter { !it.isWhitespace() }
            .replace('-', '+')
            .replace('_', '/')
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
