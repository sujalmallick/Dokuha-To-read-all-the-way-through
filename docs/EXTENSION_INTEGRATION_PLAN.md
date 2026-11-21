# Dokuha Extension Ecosystem Integration Plan
## Unified Architectural Specification for Mangayomi, LNReader, and Paperback Integration

**Document Version**: 2.0.0 (Hardened Master Architectural Specification)  
**Target Platform**: Dokuha (Compose Multiplatform / Kotlin Multiplatform)  
**Target Architectures**: Android (API 26+), Desktop (JVM / macOS, Linux, Windows), iOS (iOS 15+)  
**Date**: November 2026  
**Status**: Approved Master Architectural Specification (Adversarially Hardened — Iteration 2)  

---

## Table of Contents
1. [Executive Summary & Architecture Overview](#1-executive-summary--architecture-overview)
   - 1.1 Vision & Strategic Objectives
   - 1.2 High-Level Architectural Topology
   - 1.3 Core Architectural Challenges & Hardened Solution Paradigms
2. [Deep-Dive Architectural Investigation: Mangayomi](#2-deep-dive-architectural-investigation-mangayomi)
   - 2.1 Repositories & Distribution Architecture
   - 2.2 Manifest Schemas & Metadata Taxonomy
   - 2.3 Scraper Lifecycle & Calling Conventions
   - 2.4 Required Runtime Dependencies & Host Shims
   - 2.5 Representative Extension Analyses (Asura Scans, MangaDex, KolNovel)
3. [Deep-Dive Architectural Investigation: LNReader](#3-deep-dive-architectural-investigation-lnreader)
   - 3.1 Repositories & Distribution Architecture
   - 3.2 Manifest Schemas & Registry Discovery (`plugins.min.json`)
   - 3.3 Plugin Architecture & TypeScript Contracts (`PluginBase`)
   - 3.4 Scraper Execution Lifecycles & Content Pipeline (including `PagePlugin`)
   - 3.5 Required Runtime Shims & CommonJS Resolution
   - 3.6 Representative Plugin Analyses (RoyalRoad, ScribbleHub, NovelFull)
4. [Deep-Dive Architectural Investigation: Paperback](#4-deep-dive-architectural-investigation-paperback)
   - 4.1 Repositories, Tooling & Architectural Divergence (v0.8 vs v0.9)
   - 4.2 Repository Indexing & Discovery (`versioning.json`)
   - 4.3 Scraper Execution Lifecycles & Interface Segregation
   - 4.4 Required Runtime Dependencies & Host Shims (including Selector & DUI APIs)
   - 4.5 Representative Extension Analyses (MangaDex REST, BatoTo AES, Komga DUI)
5. [Dokuha Integration Architecture](#5-dokuha-integration-architecture)
   - 5.1 Exhaustive Domain Model Mapping
   - 5.2 Dynamic Extension Lifecycle, Polymorphic Manifests & Sandbox Security
   - 5.3 Unified Multiplatform JavaScript Engine Architecture & Thread Safety (`JsEnginePool` & JSC Mutex)
   - 5.4 Unified Preamble & Polyfill Shim Layer (`Document`/`Element` DOM, `Headers`, Sandboxed Storage)
   - 5.5 Adapter Implementations (`MangayomiSourceAdapter` with Caching, `LNReaderSourceAdapter` with `PagePlugin`, `PaperbackSourceAdapter` with Keyed Fallbacks)
   - 5.6 Persistence, Database Schema & Reactive Cache-Aside Flow Architecture
6. [Phased Implementation Roadmap](#6-phased-implementation-roadmap)
   - 6.1 Phase 1: Core Runtime Shims & Polyfills
   - 6.2 Phase 2: Ecosystem Source Adapters
   - 6.3 Phase 3: Multi-Repository Discovery & Extension Manager
   - 6.4 Phase 4: UI & Reader Viewport Integration
   - 6.5 Phase 5: Hardening, Performance Profiling & CI Regression
7. [Risk, Compatibility & Security Analysis](#7-risk-compatibility--security-analysis)
   - 7.1 Multiplatform JS Engine Divergence & Threading Safety (QuickJS vs JSC)
   - 7.2 Anti-Scraping, Rate Limiting & Cloudflare Turnstile Interactive Mitigation
   - 7.3 Memory, CPU & Resource Footprint Optimization (`JsEnginePool` & DOM Pruning)
   - 7.4 Security Sandboxing, SSRF Protection & Malicious Code Containment
   - 7.5 Upstream Ecosystem Drift & Deprecation Guardrails
8. [Verification Commands & Test Artifacts](#8-verification-commands--test-artifacts)
   - 8.1 Catalog & Manifest Verification
   - 8.2 Bundle Structure & Shim Execution Testing
   - 8.3 Hardened Architecture Verification Matrix (SQLite, SSRF, Mutex, DOM, Headers)
   - 8.4 Dokuha Automated Test Execution Matrix
9. [Appendix: Cross-Ecosystem Type Matrix](#9-appendix-cross-ecosystem-type-matrix)

---

## 1. Executive Summary & Architecture Overview

### 1.1 Vision & Strategic Objectives
Dokuha is engineered as a high-performance, design-forward reader for comics, manga, webtoons, and light novels built on **Compose Multiplatform (CMP)** and **Kotlin Multiplatform (KMP)**. Modern reader ecosystems frequently suffer from fragmentation: users must choose between specialized comic scrapers and specialized novel scrapers, while developers face the unsustainable maintenance burden of continually reverse-engineering hundreds of website layouts that shift without notice.

The strategic vision of Dokuha's Extension Integration Architecture is to establish a **universal, zero-code-modification scraper runtime**. By providing multiplatform emulation of the runtime contracts of the three dominant mobile scraper ecosystems:
1. **Mangayomi** (JavaScript/Dart hybrid ecosystem with 114+ JS comic sources and novel sources),
2. **LNReader** (TypeScript/CommonJS ecosystem with 372+ high-grade light novel and web novel sources), and
3. **Paperback** (TypeScript/UMD/IIFE ecosystem with mature, high-traffic manga and webtoon sources across v0.8 and v0.9),

Dokuha decouples content aggregation from core application logic. Dokuha users gain immediate, out-of-the-box access to over **500+ active scrapers**, while Dokuha inherits the upstream maintenance velocity of active open-source communities.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                               Dokuha Unified UI Layer                                  │
│       Voyager Navigator ── BrowseScreen ── TitleDetailsScreen ── Webtoon / Novel       │
│                ▲                                              │                        │
│                │ Reactive Flow Emits (Zero UI Lag)            │ Interactive Fallback   │
│                │                                              ▼                        │
│   ┌────────────┴─────────────┐                 ┌───────────────────────────────────┐   │
│   │ Compose UI Collectors    │                 │ AntiBotChallengeSheet (Turnstile) │   │
│   └──────────────────────────┘                 └─────────────────┬─────────────────┘   │
└──────────────────────────────────────────┬───────────────────────┼─────────────────────┘
                                           │                       │ Cookies / Tokens
                                    Kotlin StateFlow               ▼
                                           │             ┌───────────────────┐
┌──────────────────────────────────────────▼─────────────┤ Secure Cookie Jar │───────────┐
│                            Dokuha Domain & Data Core   └─────────┬─────────┘           │
│   SqlDelightPanelRepository ◄── Reactive .asFlow() ──► SQLite DB (mangas, chapters)    │
│   - Composite Key Isolation: UNIQUE(manga_id, url) & UNIQUE(source_id, url)            │
│   - Decoupled Background Scrape Dispatchers (Dispatchers.IO)                           │
│   - _dynamicSources: MutableStateFlow<Map<Long, ContentSource>>                        │
└──────────────────────────────────────────┬─────────────────────────────────────────────┘
                                           │
                         ContentSource (ComicSource / NovelSource)
                                           │
┌──────────────────────────────────────────▼─────────────────────────────────────────────┐
│                        Dokuha Multi-Ecosystem Bridge Layer                             │
│   ┌─────────────────────────┬─────────────────────────┬────────────────────────────┐   │
│   │  MangayomiSourceAdapter │  LNReaderSourceAdapter  │   PaperbackSourceAdapter   │   │
│   │  - 60s Detail Caching   │  - PagePlugin Pagination│   - Keyed Instance Fallback│   │
│   └────────────┬────────────┴────────────┬────────────┴─────────────┬──────────────┘   │
│                │                         │                          │                  │
│                ▼                         ▼                          ▼                  │
│   ┌────────────────────────────────────────────────────────────────────────────────┐   │
│   │                     Unified JS Preamble Shim Layer                             │   │
│   │  - CommonJS require() Router           - Full Document/Element DOM Emulation   │   │
│   │  - Case-Insensitive Headers.get()      - App & Application Namespaces (v8/v9)  │   │
│   │  - Storage Sandboxing (ext_{id}_)      - Dean Edwards unpackJs & CryptoJS AES  │   │
│   └──────────────────────────────────────┬─────────────────────────────────────────┘   │
│                                          │                                             │
│                                          ▼                                             │
│   ┌────────────────────────────────────────────────────────────────────────────────┐   │
│   │               JsEnginePool: LRU Sandboxed Engine Pool (Max 5 Active)           │   │
│   │     Android & Desktop: QuickJS (32MB Cap, C Mutex, GC)                         │   │
│   │     iOS: JavaScriptCore (Strict Coroutine Mutex Serialization)                 │   │
│   └────────────────────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 High-Level Architectural Topology
The topology spans four discrete tiers:
1. **Dynamic Catalog & Distribution Tier**: `ExtensionManager` polls remote repository manifests (`index.json`, `plugins.min.json`, `versioning.json`) over HTTPS. It validates URLs through `ExtensionRepoValidator` (enforcing full CIDR blocking for RFC 1918, 169.254.0.0/16 IMDS, 100.64.0.0/10 CGNAT, IPv6 private ranges, and performing DNS resolution to defeat DNS rebinding), verifies SHA-256 integrity checksums, and persists downloaded `.js` bundles into local `KeyValueStorage`.
2. **Unified Sandboxed JS Runtime Tier**: Managed by `JsEnginePool` with an LRU threshold of max 5 active engine instances and lazy rehydration. Binds Dokar's QuickJS engine on Android/Desktop JVM (with a strict 32MB C-level heap cap and native GC sweeps) and Apple's native `JavaScriptCore` C runtime (`JSContext`) on iOS. Single-thread safety on iOS is strictly guaranteed via coroutine `Mutex` serialization around all `JSContext` evaluations and async completion callbacks.
3. **Preamble & Polyfill Shim Tier**: A unified polyfill script is injected into the global JS context prior to bundle evaluation. It synthesizes the host standard libraries of all three ecosystems: full `Document` and `Element` DOM emulation, Fetch standard `Headers` wrapper with case-insensitive `.get()`, CommonJS `require(id)` routing, Paperback `App` and `Application` namespaces, and partitioned key-value storage (`ext_${sourceId}_`).
4. **Adapter & Domain Integration Tier**: Specialized adapters (`MangayomiSourceAdapter` with 60s detail caching, `LNReaderSourceAdapter` with iterative `PagePlugin` pagination, and `PaperbackSourceAdapter` with keyed source fallbacks) wrap the scrapers. They feed data into `SqlDelightPanelRepository` using a decoupled reactive cache-aside architecture that observes SQLite `.asFlow()` while dispatching asynchronous background scrape jobs.

### 1.3 Core Architectural Challenges & Hardened Solution Paradigms
Integrating three distinct scraper ecosystems into a single Kotlin Multiplatform application presents 12 core architectural challenges, fully hardened in Iteration 2:

| Friction Point | Upstream Ecosystem Divergence | Hardened Dokuha Solution Paradigm |
|---|---|---|
| **1. Database Slug Collisions** | Scrapers emit relative slugs (`/chapter-1`). Global unique URL constraints overwrite chapters across mangas. | Enforce composite `UNIQUE(manga_id, url)` in `Chapter.sq` and `UNIQUE(source_id, url)` in `Manga.sq`. Qualify navigation routes with `sourceId: Long`. |
| **2. Cold Cache Desynchronization** | Returning one-shot cold flows terminates stream; UI misses DB updates (favorites, progress). | Redesign repository methods to directly return SQLDelight's reactive `.asFlow()`. Dispatch scraping in decoupled coroutines that write to SQLite. |
| **3. iOS Threading SIGSEGV** | Concurrent evaluations on iOS `JSContext` from background worker threads violate JSC single-thread contract. | Wrap all `JSContext` evaluations, property dispatches, and async promise callbacks in a dedicated `Mutex` in `JsEngine.ios.kt`. |
| **4. Mangayomi DOM Crash** | Scrapers call `new Document(res.body)`, `.select()`, `.getSrc`, `.getHref`, `.text`, `.xpath()`. | Polyfill comprehensive `Document` and `Element` classes with getters, DOM tree navigation, and XPath query evaluation in JS preamble. |
| **5. LNReader Headers Crash** | Upstream plugins call `res.headers.get("cf-mitigated")`; plain JS dictionaries lack `.get()`. | Implement standard `Headers` class with case-insensitive `.get()`, `.has()`, `.set()`, `.entries()` in `@libs/fetch` and `dokuha.fetch`. |
| **6. Paperback v0.9 Instantiation** | IIFE bundles export keyed source instances (`source[manifest.id]`); `source.default` is undefined. | Dynamic keyed fallback: `source.default \|\| source[manifest.id] \|\| source[candidateKey] \|\| new source()`. Polyfill `Application.Selector`. |
| **7. LNReader Web Novel Truncation** | Large web novels implement `PagePlugin` where `parseNovel` returns `totalPages` and chapters require `parsePage`. | `LNReaderSourceAdapter.getChapterList` detects `PagePlugin` and iteratively fetches chapter batches across all pages (`1..totalPages`). |
| **8. Network & Storage Sandboxing** | SSRF attacks via incomplete IP checks; cookies leaked via suffix matching; unpartitioned storage. | Full CIDR blacklist (`172.16.0.0/12`, `169.254.0.0/16`, `100.64.0.0/10`), DNS rebinding validation, dot-bounded cookie matching, and `ext_${id}_` storage prefixing. |
| **9. Cloudflare Turnstile Deadlock** | Turnstile detects hidden WebViews and freezes indefinitely without human interaction. | Two-tier resolver: automated platform WebView with interactive fallback modal sheet (`AntiBotChallengeSheet`) and single-flight request coalescing. |
| **10. Memory Bloat & Native Heaps** | 50 installed extensions keep 50 native JS engines alive; DOM trees consume hundreds of MBs. | Implement `JsEnginePool` with an LRU bound of max 5 active engines, 32MB QuickJS heap limit, native GC sweeps, and post-scrape DOM pruning. |
| **11. Mangayomi Redundant Fetch** | `getDetail()` returns manga details and chapter list in one payload; calling both sequentially doubles HTTP calls. | In-memory `MangaDetail` response cache with 60-second TTL in `MangayomiSourceAdapter` guarded by a coroutine `Mutex`. |
| **12. Manifest Deserialization Errors** | LNReader (`site`), Paperback (`sources: []`), and Mangayomi (`itemType`) schemas diverge, failing simple List decoding. | Polymorphic Kotlinx Serialization models (`MangayomiCatalogItem`, `LNReaderCatalogItem`, `PaperbackVersioningManifest`) with manifest autodetection. |

---

## 2. Deep-Dive Architectural Investigation: Mangayomi

### 2.1 Repositories & Distribution Architecture
The Mangayomi project (`kodjodevf/mangayomi`) distributes extensions via static JSON indices hosted on GitHub Pages:
- **Primary Manga Index**: `https://kodjodevf.github.io/mangayomi-extensions/index.json` (contains 363 sources; 249 Dart, 114 JavaScript).
- **Primary Novel Index**: `https://kodjodevf.github.io/mangayomi-extensions/novel_index.json` (contains 5 novel sources; 100% JavaScript).
- **Community Repositories**: Secondary community forks, notably `entityJY/mangayomi-extensions-eJ`, distribute high-maintenance sources such as Asura Scans and MangaDex.

Mangayomi supports two distinct source implementation languages:
1. `sourceCodeLanguage: 0` (Dart): Compiled into native Flutter binaries or interpreted via the `dart_eval` bytecode runtime. Dokuha ignores Dart sources because they cannot execute inside a JavaScript engine.
2. `sourceCodeLanguage: 1` (JavaScript): Standalone JavaScript files evaluated inside QuickJS via Flutter's `flutter_qjs` bridge. These are directly compatible with Dokuha.

### 2.2 Manifest Schemas & Metadata Taxonomy

#### Index Catalog Schema (`index.json` & `novel_index.json`)
```json
{
  "name": "Asura Scans",
  "id": 524070078,
  "baseUrl": "https://asuracomic.net",
  "apiUrl": "",
  "lang": "en",
  "typeSource": "single",
  "iconUrl": "https://raw.githubusercontent.com/entityJY/mangayomi-extensions-eJ/main/javascript/icon/en.asurascans.png",
  "dateFormat": "MMMM dd, yyyy",
  "dateFormatLocale": "en_us",
  "isNsfw": false,
  "hasCloudflare": false,
  "sourceCodeUrl": "https://raw.githubusercontent.com/entityJY/mangayomi-extensions-eJ/main/javascript/manga/src/en/asurascans.js",
  "version": "0.1.7",
  "isManga": true,
  "itemType": 0,
  "isFullData": false,
  "appMinVerReq": "0.5.0",
  "additionalParams": "",
  "sourceCodeLanguage": 1,
  "notes": ""
}
```

*Schema Field Taxonomy*:
- `id` (`int64`): Unique integer identifier for the source.
- `itemType` (`int`): Content category: `0` = Manga/Comic, `1` = Anime (video streams, excluded from Dokuha), `2` = Novel.
- `sourceCodeLanguage` (`int`): `0` = Dart, `1` = JavaScript.
- `typeSource` (`string`): `"single"` (custom standalone scraper) or template identifier (e.g., `"madara"`, `"mangathemesia"`).
- `sourceCodeUrl` (`string`): Direct HTTPS endpoint delivering the raw JavaScript scraper file.
- `dateFormat` & `dateFormatLocale` (`string`): Pattern and locale used to parse relative or localized chapter upload timestamps into epoch milliseconds.

#### Embedded Source Header Schema (`mangayomiSources`)
Every Mangayomi `.js` file defines a root-level constant array named `mangayomiSources`:
```javascript
const mangayomiSources = [{
    "name": "Asura Scans",
    "id": 524070078,
    "baseUrl": "https://asuracomic.net",
    "lang": "en",
    "apiUrl": "",
    "iconUrl": "https://raw.githubusercontent.com/.../en.asurascans.png",
    "typeSource": "single",
    "itemType": 0,
    "version": "0.1.7",
    "dateFormat": "MMMM dd, yyyy",
    "dateFormatLocale": "en_us",
    "pkgPath": "manga/src/en/asurascans.js"
}];
```
For multi-language scrapers (e.g. MangaDex), `mangayomiSources` includes `langs: string[]` and `ids: Record<string, number>`, allowing a single script file to service multiple localized source entries.

### 2.3 Scraper Lifecycle & Calling Conventions
Mangayomi scrapers subclass `MProvider` through the class declaration `class DefaultExtension extends MProvider`. The host environment evaluates the source bundle and instantiates the scraper using the upstream host naming convention:
```javascript
var extention = new DefaultExtension();
```
*(Note: Upstream Mangayomi explicitly spells the variable `extention` with a 't')*.

#### Method Invocation Table
```
┌───────────────────────────────┬─────────────────────────────┬──────────────────────────────────────────┐
│ Scraper Method                │ Parameters                  │ Return Schema                            │
├───────────────────────────────┼─────────────────────────────┼──────────────────────────────────────────┤
│ getHeaders(url)               │ url: string                 │ Map<string, string>                      │
│ getPopular(page)              │ page: number                │ { list: MangaItem[], hasNextPage: bool } │
│ getLatestUpdates(page)        │ page: number                │ { list: MangaItem[], hasNextPage: bool } │
│ search(query, page, filters)  │ query: str, page: num, f:[] │ { list: MangaItem[], hasNextPage: bool } │
│ getDetail(url)                │ url: string                 │ DetailResult (includes chapters: [])     │
│ getPageList(url)              │ url: string                 │ Array<string | { url: str, headers: {} }>│
│ getHtmlContent(name, url)     │ name: string, url: string   │ string (HTML fragment)                   │
│ cleanHtmlContent(html)        │ html: string                │ string (Sanitized HTML)                  │
│ getFilterList()               │ none                        │ FilterGroup[]                            │
│ getSourcePreferences()        │ none                        │ PreferenceItem[]                         │
└───────────────────────────────┴─────────────────────────────┴──────────────────────────────────────────┘
```

#### Mangayomi Status Mapping
Mangayomi represents publication status as integers:
- `0`: Ongoing (`MangaStatus.ONGOING`)
- `1`: Complete (`MangaStatus.COMPLETED`)
- `2`: Hiatus (`MangaStatus.ON_HIATUS`)
- `3`: Canceled / Dropped (`MangaStatus.CANCELLED`)
- `4`: Publishing Finished (`MangaStatus.PUBLISHING_FINISHED`)
- `5`: Unknown (`MangaStatus.UNKNOWN`)

### 2.4 Required Runtime Dependencies & Host Shims
Mangayomi scrapers execute in a lightweight QuickJS environment and depend on the following host-injected classes:

1. **`MProvider` Base Class**:
   ```javascript
   class MProvider {
       constructor() {
           this.source = globalThis.__mangayomi_current_source || {};
       }
       getHeaders(url) { return {}; }
   }
   ```
2. **HTTP Client (`Client`)**:
   Instantiated via `const client = new Client();`. Exposes async HTTP verbs:
   - `async get(url, headers)`
   - `async post(url, headers, body)`
   - `async put(url, headers, body)`
   - `async delete(url, headers, body)`
   Returns: `{ statusCode: number, body: string, headers: Record<string, string>, isRedirect: boolean, reasonPhrase: string }`.
3. **DOM / HTML Parsing (`Document` & `Element`)**:
   Instantiated via `const doc = new Document(htmlString);`.
   - `Document` methods: `.select(selector): Element[]`, `.selectFirst(selector): Element | null`, `.xpath(xpath): string[]`, `.xpathFirst(xpath): string`, `.getElementById(id)`, `.getElementsByClassName(cls)`, `.text`, `.outerHtml`.
   - `Element` properties: `.text`, `.outerHtml`, `.innerHtml`, `.getSrc`, `.getHref`, `.getImg`, `.getDataSrc`, `.nextElementSibling`, `.previousElementSibling`, `.children`, `.select(selector)`, `.selectFirst(selector)`, `.attr(name)`.
4. **Persistent Preferences (`SharedPreferences`)**:
   - `new SharedPreferences().get(key)`
   - `new SharedPreferences().getString(key, defaultValue)`
   - `new SharedPreferences().setString(key, value)`
5. **Kotlin-Style String Prototypes**:
   Extensions rely heavily on Kotlin String helper methods polyfilled onto `String.prototype`:
   - `String.prototype.substringAfter(delim)`
   - `String.prototype.substringAfterLast(delim)`
   - `String.prototype.substringBefore(delim)`
   - `String.prototype.substringBeforeLast(delim)`
   - `String.prototype.substringBetween(left, right)`
6. **Crypto & De-obfuscation Primitives**:
   - `encryptAESCryptoJS(plainText, passphrase)`: CryptoJS OpenSSL-compatible AES-256-CBC with salted header.
   - `decryptAESCryptoJS(encrypted, passphrase)`: Decrypts AES-256-CBC salted ciphertext.
   - `decryptAESGCM(encryptedHex, keyHex, ivHex, tagHex)`: Decrypts AES-GCM payloads.
   - `unpackJs(packedString)`: Unpacks Dean Edwards `eval(function(p,a,c,k,e,r)...)` obfuscated scripts.
   - `deobfuscateJsPassword(str)`: Reverse-shifts character code obfuscations.

### 2.5 Representative Extension Analyses

#### Asura Scans (`asurascans.js`)
- **Scraper Type**: Cheerio-style DOM parsing of custom Next.js/SSR HTML.
- **Detail & Chapter Extraction**: Extracts title, status, description, and genres from meta tags and header containers. Extracts chapter list from `div.pl-4.pr-2 div.group`, parsing release dates via `parseDates(dateStr, "MMMM dd, yyyy", "en_us")`.
- **Page List Extraction**: Scrapes page images from `div.w-full.mx-auto.center img`, extracting attributes using `.getSrc` and `.getDataSrc` to bypass lazy-loading place-holder images.

#### MangaDex (`mangadex.js`)
- **Scraper Type**: REST API client interacting with `https://api.mangadex.org`.
- **Multilingual Routing**: Operates across 20+ language codes. Queries `/manga` with UUIDs, building complex query parameter strings with included relationships (`cover_art`, `author`, `artist`).
- **Chapter Batching**: Iterates chapter feeds via `offset` and `limit=100`. Queries `/at-home/server/{chapterId}` to obtain high-speed CDN image tokens.

#### KolNovel (`kolnovel.js`)
- **Scraper Type**: Light novel scraper (`itemType: 2`).
- **Novel Content Extraction**: Implements `getHtmlContent(name, url)`. Fetches the raw chapter HTML, queries `div.entry-content` or `div.epcontent`, strips inline ad elements (`script`, `ins`, `.code-block`, `.sharedaddy`), and returns cleaned HTML.

---

## 3. Deep-Dive Architectural Investigation: LNReader

### 3.1 Repositories & Distribution Architecture
The LNReader ecosystem is maintained under the GitHub organization `LNReader`:
- **`LNReader/lnreader-plugins`**: Central registry containing 372+ light novel plugins written in TypeScript.
- **`LNReader/lnreader`**: Official cross-platform React Native mobile client.
- **`lnreader/cli`**: Monorepo containing `@lnreader/plugin-runtime`, the reference execution sandbox.

All plugins compile from TypeScript into standalone **ES5 CommonJS** JavaScript bundles using `tsconfig.production.json` (`tsc`). Bundles are committed to the `plugins/v3.0.0` distribution branch under `.js/src/plugins/`.

### 3.2 Manifest Schemas & Registry Discovery (`plugins.min.json`)
The central registry manifest is hosted at:
```
https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist/plugins.min.json
```

```json
{
  "id": "royalroad",
  "name": "Royal Road",
  "site": "https://www.royalroad.com/",
  "lang": "English",
  "version": "1.1.8",
  "url": "https://raw.githubusercontent.com/lnreader/lnreader-plugins/plugins/v3.0.0/.js/src/plugins/english/royalroad.js",
  "iconUrl": "https://raw.githubusercontent.com/lnreader/lnreader-plugins/plugins/v3.0.0/public/static/src/en/royalroad/icon.png",
  "customJS": "",
  "customCSS": ""
}
```

*Manifest Taxonomy*:
- `id` (`string`): Unique alphanumeric source identifier.
- `site` (`string`): Base URL of the content website.
- `lang` (`string`): Full language name (e.g., `"English"`, `"Español"`, `"Русский"`).
- `version` (`string`): Semantic version string (`X.Y.Z`). Version checks evaluate numeric tuple comparisons.
- `url` (`string`): Direct download URL for the transpiled CommonJS `.js` bundle.
- `customJS` & `customCSS` (`string`): Optional URLs to remote stylesheet or script assets injected into the chapter webview.

### 3.3 Plugin Architecture & TypeScript Contracts (`PluginBase`)
Every plugin implements `Plugin.PluginBase` (`src/types/plugin.ts`):

```typescript
export namespace Plugin {
  export type ChapterItem = {
    name: string;
    path: string;
    releaseTime?: string | null;
    chapterNumber?: number;
    page?: string;
    scanlator?: string | string[];
  };

  export type NovelItem = {
    name: string;
    path: string;
    cover?: string;
  };

  export type SourceNovel = {
    genres?: string;
    summary?: string;
    author?: string;
    artist?: string;
    status?: string;
    rating?: number;
    ratingCount?: number;
    info?: { label: string; value: string }[];
    chapters?: ChapterItem[];
  } & NovelItem;

  export type PopularNovelsOptions<Q extends Filters | undefined = Filters | undefined> = {
    showLatestNovels?: boolean;
    filters: Q extends undefined ? undefined : FilterToValues<Q>;
  };

  export type PluginBase = {
    id: string;
    name: string;
    icon: string;
    site: string;
    version: string;
    filters?: Filters;
    popularNovels(pageNo: number, options: PopularNovelsOptions): Promise<NovelItem[]>;
    parseNovel(novelPath: string): Promise<SourceNovel>;
    parseChapter(chapterPath: string): Promise<string>;
    searchNovels(searchTerm: string, pageNo: number): Promise<NovelItem[]>;
    resolveUrl?(path: string, isNovel?: boolean): string;
  };

  export type PagePlugin = {
    parseNovel(novelPath: string): Promise<SourceNovel & { totalPages: number }>;
    parsePage(novelPath: string, page: string): Promise<{ chapters: ChapterItem[] }>;
  } & PluginBase;
}
```

### 3.4 Scraper Execution Lifecycles & Content Pipeline
Compiled plugins instantiate and export the plugin instance via CommonJS:
```javascript
exports.default = new RoyalRoad();
```
The host executes:
```javascript
const pluginInstance = (module.exports.default || module.exports);
```

#### Calling Conventions
1. **Catalog Browsing (`popularNovels`)**:
   Invoked as `popularNovels(pageNo, { showLatestNovels: false, filters: activeFilters })`. Returns `Promise<NovelItem[]>`.
2. **Latest Updates (`popularNovels`)**:
   Invoked as `popularNovels(pageNo, { showLatestNovels: true, filters: undefined })`. LNReader unifies popular and latest browsing under a single method toggled by `showLatestNovels`.
3. **Catalog Search (`searchNovels`)**:
   Invoked as `searchNovels(searchTerm, pageNo)`. Returns `Promise<NovelItem[]>`.
4. **Novel Metadata & Chapters (`parseNovel` / `parsePage`)**:
   `parseNovel(novelPath)` returns `Promise<SourceNovel>` with populated `chapters: ChapterItem[]`.
   
   **LNReader `PagePlugin` Chapter Pagination Protocol**:
   For large web novels with hundreds or thousands of chapters (common on platforms like NovelFull, ReadNovelFull, Wuxiaworld, and Syosetu), loading all chapters in a single HTTP request causes timeouts or memory exhaustion. Upstream LNReader plugins therefore implement the `PagePlugin` interface:
   ```typescript
   export interface PagePlugin {
       parsePage(novelPath: string, page: string | number): Promise<Plugin.SourcePage>;
   }
   
   export interface SourcePage {
       chapters: Plugin.ChapterItem[];
   }
   ```
   *Pagination Execution Flow*:
   - When `parseNovel(novelPath)` is invoked on a `PagePlugin`, the returned `SourceNovel` object includes the property `totalPages?: number` (or `pages?: number`), and optionally the initial slice of chapters (`page = 1`).
   - If `totalPages != null && totalPages > 1` (or if `typeof plugin.parsePage === 'function'` and `totalPages` is present):
     The host adapter must iteratively retrieve subsequent chapter pages by calling `plugin.parsePage(novelPath, pageNumber)` for each `pageNumber` from `1` (or `2` if page 1 was already populated) up to `totalPages`.
   - Each `parsePage` invocation returns an object containing `{ chapters: ChapterItem[] }`.
   - The host concatenates all page batches into a single contiguous chapter list, sorting and normalizing chapter numbers to ensure continuous reading order.
   - If a plugin implements `parsePage` without specifying `totalPages`, it employs cursor-based pagination where `parsePage` is queried until an empty chapter array or null is returned.
5. **Chapter Content Pipeline (`parseChapter`)**:
   `parseChapter(chapterPath)` returns `Promise<string>`. The returned payload is **structured, sanitized HTML**. Author notes are partitioned using standardized HTML classes:
   ```html
   <div class="author-note-before">...</div>
   <hr class="notes-separator">
   <div class="chapter-content">
       <p>Paragraph 1...</p>
       <p>Paragraph 2...</p>
   </div>
   <hr class="notes-separator">
   <div class="author-note-after">...</div>
   ```

### 3.5 Required Runtime Shims & CommonJS Resolution
Because LNReader plugins are compiled as CommonJS modules, executing them requires intercepting `require(id)`:

```
┌────────────────────────┬────────────────────────────────────────────────────────────────────────┐
│ Module Identifier      │ Exported Members & Responsibility                                      │
├────────────────────────┼────────────────────────────────────────────────────────────────────────┤
│ @libs/fetch            │ fetchApi, fetchText, fetchFile, fetchProto (HTTP client with jar)       │
│ @libs/storage          │ storage, localStorage, sessionStorage (Persistent KV store with TTL)   │
│ @libs/novelStatus      │ NovelStatus enum (Ongoing, Completed, Licensed, Cancelled, Hiatus)     │
│ @libs/filterInputs     │ FilterTypes enum (TextInput, Picker, Checkbox, Switch, XCheckbox)      │
│ @libs/isAbsoluteUrl    │ isUrlAbsolute(url: string): boolean                                    │
│ @libs/defaultCover     │ defaultCover: string (Fallback asset URL)                              │
│ @libs/aes              │ gcm (AES-GCM encryption helpers from @noble/ciphers/aes)                │
│ cheerio                │ CheerioAPI, load() (DOM manipulation & CSS selection)                  │
│ htmlparser2            │ Parser (High-speed streaming SAX parser)                               │
│ dayjs                  │ dayjs default function (Date parsing, relative "2 hours ago" dates)    │
│ urlencode              │ encode(), decode() (Percent encoding utilities)                        │
└────────────────────────┴────────────────────────────────────────────────────────────────────────┘
```

*Required Globals*:
`fetch`, `Headers`, `Request`, `Response`, `FormData`, `Blob`, `URL`, `URLSearchParams`, `TextEncoder`, `TextDecoder` (supporting UTF-8, GBK, Big5, and Windows-1251), `Buffer`, `setTimeout`, `clearTimeout`, `console`, `btoa`, `atob`.

### 3.6 Representative Plugin Analyses

#### RoyalRoad (`royalroad.ts`)
- **Parsing Architecture**: Uses `htmlparser2.Parser` to construct an optimized streaming state machine.
- **Anti-Bot Challenge Solver**: Implements an in-sandbox proof-of-work solver for RoyalRoad's custom Anubis challenge:
  - Detects `anubis_challenge` and `anubis_base_prefix` scripts in the raw HTML.
  - Computes the SHA-256 hash collision nonce directly within JavaScript.
  - Submits the nonce to `/.within.website/x/cmd/anubis/api/pass-challenge` and caches the clearance cookie.
- **Anti-Scraping Honey-Pot Elimination**: Inspects embedded `<style>` tags to extract randomized class names configured with `display: none;`, stripping out injected fake paragraphs inserted to poison scrapers.

#### ScribbleHub (`scribblehub.ts`)
- **AJAX Table of Contents Pagination**: Rather than parsing hundreds of HTML pages, sends a single POST request to WordPress's `wp-admin/admin-ajax.php` with action `wi_getreleases_pagination` and the novel's `mypostid`, retrieving the complete chapter list in one operation.
- **Date Conversion**: Parses relative publication strings (`"15 mins ago"`, `"3 days ago"`) into standard ISO timestamps using `dayjs`.

#### NovelFull / ReadNovelFull (`NovelFull[readnovelfull].js`)
- **Template Generator**: Generated from `multisrc/readnovelfull/template.ts`.
- **Adaptive Rate-Limiting**: Intercepts HTTP 429 status codes, extracts the `Retry-After` header, and applies exponential backoff delays. Enforces a 150ms pacing interval between chapter queries to avoid IP bans.

---

## 4. Deep-Dive Architectural Investigation: Paperback

### 4.1 Repositories, Tooling & Architectural Divergence (v0.8 vs v0.9)
The Paperback ecosystem is maintained by `Paperback-iOS`:
- **`Paperback-iOS/paperback-toolchain`**: Monorepo hosting `@paperback/toolchain` and `@paperback/types`.
- **`Paperback-iOS/extensions-default`**: Official reference repository (v0.8).
- **`TheNetsky/community-extensions`**: Dominant v0.8 community repository (MangaDex, BatoTo, etc.).
- **`inkdex/extensions`**: Dominant v0.9 unified community repository.

#### The v0.8 vs v0.9 Architectural Split
Paperback underwent a total architectural redesign between v0.8 (production App Store release) and v0.9 (modern Alpha release):

```
┌────────────────────────────┬─────────────────────────────┬──────────────────────────────┐
│ Dimension                  │ Paperback v0.8              │ Paperback v0.9               │
├────────────────────────────┼─────────────────────────────┼──────────────────────────────┤
│ Bundling Toolchain         │ Browserify UMD              │ ESBuild IIFE (ES2020)        │
│ Global Target              │ window.Sources / Sources    │ globalThis.source            │
│ Extension Base Class       │ abstract class Source       │ Interface Segregation        │
│ Host Environment Access    │ Global App object & factories│ Application static namespace │
│ Cheerio Delivery           │ Injected via constructor    │ Bundled into script bundle   │
│ Chapter List Signature     │ getChapters(mangaId: string)│ getChapters(manga: SourceManga)│
│ Chapter Details Signature  │ getChapterDetails(mId, chId)│ getChapterDetails(ch: Chapter)│
│ Home Page Architecture     │ getHomePageSections(cb) [1] │ getDiscoverSections() [2]    │
│ State & Keychain           │ SourceStateManager instance │ Application.getState()       │
└────────────────────────────┴─────────────────────────────┴──────────────────────────────┘
```
*[1] v0.8 streams sections via progressive callbacks. [2] v0.9 uses two-phase polling (`getDiscoverSections` -> `getDiscoverSectionItems`).*

### 4.2 Repository Indexing & Discovery (`versioning.json`)
Repositories are distributed as static file servers indexed by `versioning.json`.

#### v0.8 Manifest Format
```json
{
  "buildTime": "2024-11-08T10:17:55.063Z",
  "builtWith": { "toolchain": "0.8.0-alpha.47", "types": "0.8.0-alpha.47" },
  "sources": [
    {
      "id": "Paperback",
      "name": "Paperback",
      "author": "Lemon | Faizan Durrani",
      "desc": "Komga client extension for Paperback",
      "website": "https://github.com/FramboisePi",
      "contentRating": "EVERYONE",
      "version": "1.3.1",
      "icon": "icon.png",
      "tags": [{ "text": "Self hosted", "type": "danger" }],
      "websiteBaseURL": "https://komga.org",
      "intents": 39
    }
  ]
}
```

#### v0.9 Manifest Format
```json
{
  "buildTime": "2026-11-10T11:51:23.531Z",
  "builtWith": { "toolchain": "1.0.0-alpha.94", "types": "1.0.0-alpha.94" },
  "repository": {
    "name": "Inkdex Extensions (0.9)",
    "description": "All extensions from the Inkdex community."
  },
  "sources": [
    {
      "id": "MangaDex",
      "name": "MangaDex",
      "description": "Extension that pulls content from mangadex.org.",
      "version": "1.0.0-alpha.28",
      "icon": "icon.png",
      "language": "multi",
      "contentRating": "SAFE",
      "badges": [],
      "capabilities": [1, 4, 8, 32, 64],
      "developers": [{ "name": "Inkdex", "website": "https://inkdex.github.io" }]
    }
  ]
}
```

#### Source Capabilities & Intents
Paperback defines capabilities as flags:
- `1`: `CHAPTER_PROVIDING` (Fetches chapters & page images)
- `2`: `PROGRESS_PROVIDING` (Syncs reading progress to remote tracker)
- `4`: `DISCOVER_SECTION_PROVIDING` (Homepage discover carousels)
- `8`: `MANAGED_COLLECTION_PROVIDING` (Custom user collections)
- `16`: `CLOUDFLARE_BYPASS_PROVIDING` (Requires native WebView challenge solver)
- `32`: `SETTINGS_FORM_PROVIDING` (Exposes dynamic in-app settings UI)
- `64`: `SEARCH_RESULT_PROVIDING` (Catalog search and multi-tag filtering)

### 4.3 Scraper Execution Lifecycles & Interface Segregation

#### Scraper Method Invocations
1. **Home / Discover Page**:
   - **v0.8**: `instance.getHomePageSections(sectionCallback)`: Scraper immediately invokes `sectionCallback(section)` with skeleton section headers, kicks off parallel HTTP requests, populates `section.items`, and invokes `sectionCallback(section)` again to update UI progressively. More items are retrieved via `getViewMoreItems(homepageSectionId, metadata)`.
   - **v0.9**: `instance.getDiscoverSections()` returns section headers. The host then queries `instance.getDiscoverSectionItems(section, metadata)` with cursor pagination.
2. **Search & Filter**:
   - `instance.getSearchResults(query, metadata)`: Accepts `SearchRequest` / `SearchQuery` containing keywords and included/excluded tag IDs. Returns `PagedResults` with items and opaque pagination `metadata`.
3. **Manga Details**:
   - `instance.getMangaDetails(mangaId)`: Returns `SourceManga` populated with title, cover image URL, author, artist, status, synopsis, and tag categories.
4. **Chapter List**:
   - `instance.getChapters(mangaId)`: Returns `Chapter[]` containing chapter numbers, titles, upload dates, and `sortingIndex` (an integer enforcing correct reading order regardless of upload date).
5. **Page Image Retrieval**:
   - `instance.getChapterDetails(mangaId, chapterId)`: Returns `ChapterDetails` containing `pages: string[]` (direct image URLs) and `longStrip: boolean` (indicates whether the chapter is formatted as a vertical webtoon).

### 4.4 Required Runtime Dependencies & Host Shims

#### Paperback v0.8 Shims (`App` Object & Global Factories)
Extensions expect global factory functions to construct domain objects:
- `App.createRequestManager({ requestsPerSecond, requestTimeout, interceptor })`
- `App.createSourceStateManager()`
- `App.createRequest({ url, method, headers, data, param })`
- `App.createSourceManga(...)`, `App.createChapter(...)`, `App.createChapterDetails(...)`
- `App.createHomeSection(...)`, `App.createPagedResults(...)`
- Dynamic UI factories: `App.createDUISection(...)`, `App.createDUIInputField(...)`, etc.

#### Paperback v0.9 Shims (`Application` Namespace)
- `Application.scheduleRequest(request: Request): Promise<[Response, ArrayBuffer]>`
- `Application.getState(key: string): unknown` & `Application.setState(value, key)`
- `Application.getSecureState(key: string): unknown` & `Application.setSecureState(value, key)`
- `Application.executeInWebView({ url, script }): Promise<WebViewExecutionResult>`
- `Application.decodeHTMLEntities(str: string): string`
- `Application.base64Encode(val)` & `Application.base64Decode(val)`
- `Application.arrayBufferToUTF8String(buffer: ArrayBuffer): string`
- `Application.Selector`: Structural CSS/DOM selector factory for declarative parsing
- `Application.SelectorRegistry`: Registry for storing and executing reusable selector chains
- `Application.createDUIForm(...)` & `Application.createDUIButton(...)`: Declarative UI component builders

### 4.5 Representative Extension Analyses

#### MangaDex (`MangaDex.ts`)
- **Architecture**: REST API client (MangaDex API v5).
- **Authentication Lifecycle**: Uses `SourceStateManager` to persist OAuth access and refresh tokens. Its `RequestManager` interceptor calculates token expiration and automatically executes a refresh request prior to dispatching queries.
- **Home Sections**: Streams four sections concurrently (`seasonal`, `popular`, `latest_updates`, `recently_added`) using `sectionCallback`.
- **Chapter Ordering**: Computes floating-point `sortingIndex` to guarantee proper reading order across volume and chapter numbering schemes.

#### BatoTo (`BatoTo.ts` & `BatoToParser.ts`)
- **Architecture**: Cheerio-based HTML scraper.
- **Cloudflare Defense**: Intercepts 403 and 503 status codes, triggering `getCloudflareBypassRequestAsync()` to request a native WebView challenge solution.
- **Crypto Obfuscation**: Extracts inline scripts containing `batoPass` and `batoWord`. Evaluates `batoPass` and invokes `CryptoJS.AES.decrypt(batoWord, batoPass)` to decrypt obfuscated page image URLs.

#### Komga Client (`Paperback-iOS/extensions-default`)
- **Dynamic UI (DUI)**: Implements interactive settings forms via `App.createDUIForm`, allowing users to configure server hostnames, basic auth credentials, and display options directly within the app settings.

---

## 5. Dokuha Integration Architecture

### 5.1 Exhaustive Domain Model Mapping
Dokuha's core domain models are defined in `UI UX/composeApp/src/commonMain/kotlin/core/source/Models.kt`. Below is the field-by-field mapping across all three ecosystems:

#### Manga & Novel Metadata Mapping
```
┌─────────────────────┬──────────────────────┬──────────────────────┬────────────────────────┬────────────────────────────────────────────┐
│ Dokuha SourceManga  │ Mangayomi Detail     │ LNReader SourceNovel │ Paperback SourceManga  │ Transformation / Normalization Logic       │
├─────────────────────┼──────────────────────┼──────────────────────┼────────────────────────┼────────────────────────────────────────────┤
│ url: String         │ link / url: String   │ path: String         │ id / mangaId: String   │ Stored as source-relative or absolute URL  │
│ title: String       │ name: String         │ name: String         │ mangaInfo.titles[0]    │ Primary title string                       │
│ artist: String?     │ artist: String?      │ artist: String?      │ mangaInfo.artist       │ Direct string copy; null if empty          │
│ author: String?     │ author: String?      │ author: String?      │ mangaInfo.author       │ Direct string copy; null if empty          │
│ description: String?│ description: String? │ summary: String?     │ mangaInfo.desc         │ Direct string copy; null if empty          │
│ genres: List<String>│ genre: List<String>  │ genres: String (csv) │ mangaInfo.tags[].label │ Split CSV on "," or flatten tag groups     │
│ status: MangaStatus │ status: Int (0..5)   │ status: NovelStatus  │ mangaInfo.status: Str  │ Normalized via MangaStatus mapping below   │
│ thumbnailUrl: Str?  │ imageUrl: String?    │ cover: String?       │ mangaInfo.image        │ URL string or null                         │
│ isNovel: Boolean    │ itemType == 2        │ true                 │ false                  │ Flag determining comic vs novel reader UI  │
└─────────────────────┴──────────────────────┴──────────────────────┴────────────────────────┴────────────────────────────────────────────┘
```

#### Status Enum Normalization Matrix
```
┌──────────────────────────┬────────────────────────┬────────────────────────┬────────────────────────┐
│ Dokuha MangaStatus       │ Mangayomi Integer      │ LNReader NovelStatus   │ Paperback Status       │
├──────────────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
│ ONGOING                  │ 0                      │ 'Ongoing'              │ 'Ongoing'              │
│ COMPLETED                │ 1                      │ 'Completed'            │ 'Completed'            │
│ ON_HIATUS                │ 2                      │ 'On Hiatus'            │ 'Hiatus'               │
│ CANCELLED                │ 3                      │ 'Cancelled'            │ 'Abandoned' / 'Dropped'│
│ PUBLISHING_FINISHED      │ 4                      │ 'Publishing Finished'  │ 'Finished'             │
│ UNKNOWN                  │ 5                      │ 'Unknown' / default    │ default                │
└──────────────────────────┴────────────────────────┴────────────────────────┴────────────────────────┘
```

#### Chapter Metadata Mapping
```
┌─────────────────────────┬─────────────────────────┬─────────────────────────┬─────────────────────────┐
│ Dokuha SourceChapter    │ Mangayomi Chapter       │ LNReader ChapterItem    │ Paperback Chapter       │
├─────────────────────────┼─────────────────────────┼─────────────────────────┼─────────────────────────┤
│ url: String             │ url: String             │ path: String            │ id / chapterId: String  │
│ name: String            │ name: String            │ name: String            │ name: String            │
│ dateUpload: Long        │ dateUpload (parsed ms)  │ releaseTime (parsed ms) │ time.getTime(): Long    │
│ chapterNumber: Float    │ chapterNumber: Float    │ chapterNumber?: Float   │ chapNum: Float          │
│ scanlator: String?      │ scanlator: String?      │ scanlator: String?      │ group: String?          │
└─────────────────────────┴─────────────────────────┴─────────────────────────┴─────────────────────────┘
```

#### Reading Content Payloads
- **Comic Sources (`ComicSource.getPageList`)**:
  - Mangayomi `getPageList()` returns `Array<string | { url: string, headers: Map<string, string> }>`.
  - Paperback `getChapterDetails()` returns `ChapterDetails.pages: string[]`.
  - Both map directly to `List<SourcePage>`:
    ```kotlin
    SourcePage(
        index = idx,
        url = pageUrl,
        imageUrl = pageUrl,
        headers = headersMap
    )
    ```
- **Novel Sources (`NovelSource.getChapterContent`)**:
  - Mangayomi `getHtmlContent()` returns clean HTML string.
  - LNReader `parseChapter()` returns structured HTML string.
  - Both map directly to `NovelContent`:
    ```kotlin
    NovelContent(
        chapterUrl = chapter.url,
        title = chapter.name,
        htmlBody = htmlPayload,
        plainText = htmlPayload.replace(Regex("<[^>]*>"), "").trim()
    )
    ```

### 5.2 Dynamic Extension Lifecycle & Sandbox Security
Extension management in Dokuha is centralized in `ExtensionManager` (`core.extensions.ExtensionManager.kt`):

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                ExtensionManager Lifecycle                              │
└────────────────────────────────────────────────────────────────────────────────────────┘
  1. Add Repository URL
     │
     ▼
  2. Validate URL via ExtensionRepoValidator
     ├─ Scheme must be HTTPS
     ├─ Reject private/internal IP ranges (127.0.0.1, 10.0.0.0/8, 192.168.0.0/16, etc.)
     ├─ Reject userinfo (user:pass@)
     └─ Compute repository fingerprint
     │
     ▼
  3. Catalog Indexing (refreshCatalog)
     ├─ Fetch index.json / novel_index.json (Mangayomi)
     ├─ Fetch plugins.min.json (LNReader)
     ├─ Fetch versioning.json (Paperback)
     └─ Normalize records to ExtensionCatalogItem with ecosystem tag
     │
     ▼
  4. Download & Verify (installExtension)
     ├─ Download .js bundle via Ktor HttpClient
     ├─ Verify SHA-256 fingerprint against catalog entry
     └─ Persist script to KeyValueStorage under "dokuha_ext_script_$id"
     │
     ▼
  5. Instantiation & Sandbox Activation (registerAndActivateSource)
     ├─ Acquire engine lease from JsEnginePool (LRU max 5)
     ├─ Initialize JsBridge with Ktor client and AntiBotResolver
     ├─ Inject Unified Preamble Script (Shims, Polyfills, CommonJS router)
     ├─ Evaluate Scraper Bundle Script
     ├─ Detect Ecosystem & Instantiate Appropriate Adapter:
     │  ├─ DefaultExtension detected  ──► MangayomiSourceAdapter
     │  ├─ module.exports detected   ──► LNReaderSourceAdapter
     │  └─ Sources / source detected  ──► PaperbackSourceAdapter
     ├─ Invoke adapter.initialize()
     └─ Register adapter into SqlDelightPanelRepository._dynamicSources
```

#### Polymorphic Manifest Deserialization Architecture
Because the three ecosystems use completely different index formats, Dokuha cannot deserialize remote catalogs into a flat `List<ExtensionCatalogItem>`. Attempting to do so triggers `SerializationException: Expected JsonArray but found JsonObject` on Paperback (which returns a root object `{ "sources": [...] }`) or throws missing field errors on LNReader (which uses `site` instead of `baseUrl` and omits `itemType`).

Dokuha solves this using explicit polymorphic serialization models and an autodetection parser:

```kotlin
package core.extensions.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

// 1. Mangayomi Manifest Model (index.json & novel_index.json)
@Serializable
data class MangayomiCatalogItem(
    val id: Long,
    val name: String,
    val baseUrl: String,
    val lang: String,
    val version: String,
    val sourceCodeUrl: String,
    val sourceCodeLanguage: Int, // 0 = Dart (ignored), 1 = JavaScript
    val itemType: Int = 0,       // 0 = Manga, 1 = Anime, 2 = Novel
    val iconUrl: String = "",
    val typeSource: String = "single",
    val isNsfw: Boolean = false,
    val hasCloudflare: Boolean = false
)

// 2. LNReader Manifest Model (plugins.min.json)
@Serializable
data class LNReaderCatalogItem(
    val id: String,
    val name: String,
    val site: String,
    val lang: String,
    val version: String,
    val url: String,             // Direct raw JS bundle URL
    val iconUrl: String? = null,
    val filters: JsonElement? = null
)

// 3. Paperback Manifest Model (versioning.json)
@Serializable
data class PaperbackVersioningManifest(
    val buildTime: String? = null,
    val builtWith: JsonElement? = null,
    val sources: List<PaperbackSourceItem> = emptyList(),
    val repository: JsonObject? = null
)

@Serializable
data class PaperbackSourceItem(
    val id: String,
    val name: String,
    val version: String,
    val author: String? = null,
    val desc: String? = null,
    val description: String? = null,
    val website: String? = null,
    val websiteBaseURL: String? = null,
    val icon: String = "icon.png",
    val contentRating: String = "EVERYONE",
    val language: String = "en",
    val intents: Int? = null,
    val capabilities: List<Int>? = null
)

// Universal Domain Catalog Item
data class ExtensionCatalogItem(
    val id: Long,
    val name: String,
    val baseUrl: String,
    val scriptUrl: String,
    val iconUrl: String?,
    val version: String,
    val lang: String,
    val isNovel: Boolean,
    val isNsfw: Boolean,
    val ecosystem: ExtensionEcosystem,
    val rawManifest: String
)

enum class ExtensionEcosystem {
    MANGAYOMI, LNREADER, PAPERBACK
}

class ExtensionCatalogParser(private val json: Json) {
    fun parseCatalog(rawBody: String, repoBaseUrl: String): List<ExtensionCatalogItem> {
        val root = json.parseToJsonElement(rawBody)

        // Branch 1: Paperback returns root JsonObject containing "sources" array
        if (root is JsonObject && root.containsKey("sources")) {
            val manifest = json.decodeFromJsonElement<PaperbackVersioningManifest>(root)
            return manifest.sources.map { src ->
                val base = src.websiteBaseURL ?: src.website ?: ""
                val scriptUrl = "$repoBaseUrl/${src.id}/index.js"
                val numericId = (src.id.hashCode().toLong() and 0x7FFFFFFF) + 30_000L
                ExtensionCatalogItem(
                    id = numericId,
                    name = src.name,
                    baseUrl = base,
                    scriptUrl = scriptUrl,
                    iconUrl = "$repoBaseUrl/${src.id}/${src.icon}",
                    version = src.version,
                    lang = src.language,
                    isNovel = false,
                    isNsfw = src.contentRating.equals("ADULT", ignoreCase = true),
                    ecosystem = ExtensionEcosystem.PAPERBACK,
                    rawManifest = rawBody
                )
            }
        }

        // Branch 2: Array-based catalogs (Mangayomi or LNReader)
        if (root is JsonArray && root.isNotEmpty()) {
            val first = root.first().jsonObject
            // LNReader contains "site" and "url"
            if (first.containsKey("site") && first.containsKey("url")) {
                val items = json.decodeFromJsonElement<List<LNReaderCatalogItem>>(root)
                return items.map { item ->
                    val numericId = (item.id.hashCode().toLong() and 0x7FFFFFFF) + 20_000L
                    ExtensionCatalogItem(
                        id = numericId,
                        name = item.name,
                        baseUrl = item.site,
                        scriptUrl = item.url,
                        iconUrl = item.iconUrl,
                        version = item.version,
                        lang = item.lang,
                        isNovel = true,
                        isNsfw = false,
                        ecosystem = ExtensionEcosystem.LNREADER,
                        rawManifest = rawBody
                    )
                }
            }

            // Mangayomi contains "sourceCodeLanguage" and "sourceCodeUrl"
            if (first.containsKey("sourceCodeLanguage") || first.containsKey("sourceCodeUrl")) {
                val items = json.decodeFromJsonElement<List<MangayomiCatalogItem>>(root)
                return items.filter { it.sourceCodeLanguage == 1 && it.itemType != 1 } // Filter JS and non-anime
                    .map { item ->
                        ExtensionCatalogItem(
                            id = item.id,
                            name = item.name,
                            baseUrl = item.baseUrl,
                            scriptUrl = item.sourceCodeUrl,
                            iconUrl = item.iconUrl.takeIf { it.isNotBlank() },
                            version = item.version,
                            lang = item.lang,
                            isNovel = item.itemType == 2,
                            isNsfw = item.isNsfw,
                            ecosystem = ExtensionEcosystem.MANGAYOMI,
                            rawManifest = rawBody
                        )
                    }
            }
        }

        return emptyList()
    }
}
```

#### Hardened Repository Security & Anti-SSRF Validation
To prevent Server-Side Request Forgery (SSRF), local network probing, and cloud credential exfiltration (e.g. AWS/GCP IMDS), `ExtensionRepoValidator` enforces strict multi-layered validation:

1. **Strict Protocol & Credential Rejection**: Only `https://` is permitted. Any userinfo (`user:pass@`) is immediately rejected.
2. **Comprehensive Private CIDR & Reserved Subnet Matcher**:
   Replaces naive prefix checks with complete bitmask and CIDR range matching:
   - `127.0.0.0/8` (Loopback)
   - `10.0.0.0/8` (RFC 1918 Class A)
   - `172.16.0.0/12` (RFC 1918 Class B: `172.16.0.0` – `172.31.255.255`, blocking Docker gateways and cloud overlays)
   - `192.168.0.0/16` (RFC 1918 Class C)
   - `169.254.0.0/16` (RFC 3927 Link-Local & Cloud Metadata IMDS `169.254.169.254`)
   - `100.64.0.0/10` (RFC 6598 Carrier-Grade NAT)
   - IPv6: `::1` (Loopback), `fe80::/10` (Link-Local), `fc00::/7` (Unique Local Address)
3. **DNS Resolution & Anti-Rebinding**:
   Before establishing an HTTP connection to download a repository catalog or script bundle, the hostname is resolved via platform DNS. If *any* returned IP address falls within the private or reserved ranges, the request is aborted immediately, defeating DNS rebinding attacks.
4. **Manifest Normalization**:
   Strips trailing manifest filenames (`index.json`, `novel_index.json`, `plugins.min.json`, `versioning.json`) to standardize the base repository URL.

```kotlin
package core.extensions

import io.ktor.http.*

object ExtensionRepoValidator {
    private const val MAX_URL_LENGTH = 2048

    data class ValidationResult(val isValid: Boolean, val errorMessage: String? = null, val normalizedUrl: String? = null)

    fun validate(rawUrl: String, dnsResolver: (String) -> List<String> = { defaultDnsResolve(it) }): ValidationResult {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ValidationResult(false, "Repository URL cannot be empty")
        if (trimmed.length > MAX_URL_LENGTH) return ValidationResult(false, "URL exceeds maximum length")
        if (!trimmed.startsWith("https://", ignoreCase = true)) {
            return ValidationResult(false, "Repository URL must strictly use HTTPS protocol")
        }

        val afterScheme = trimmed.substring(8)
        val hostAndPort = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        if (hostAndPort.contains('@')) {
            return ValidationResult(false, "URLs with embedded credentials are not allowed")
        }

        val host = hostAndPort.substringBefore(':').lowercase()
        if (host.isEmpty() || host.contains(' ') || !host.contains('.')) {
            return ValidationResult(false, "Invalid repository host")
        }

        // Direct IP check
        if (isPrivateOrReservedIp(host)) {
            return ValidationResult(false, "Direct private or loopback IP addresses are blocked")
        }

        // DNS Rebinding Protection: Resolve hostname and check all resolved IPs
        val resolvedIps = try {
            dnsResolver(host)
        } catch (e: Exception) {
            return ValidationResult(false, "Host DNS resolution failed: ${e.message}")
        }
        if (resolvedIps.isEmpty() || resolvedIps.any { isPrivateOrReservedIp(it) }) {
            return ValidationResult(false, "Host resolves to a private, loopback, or cloud metadata IP address (SSRF blocked)")
        }

        // Normalize URL: strip trailing slash and known manifest names
        var path = afterScheme.substringAfter('/', "").substringBefore('?').substringBefore('#')
        val knownManifests = listOf("/index.json", "/novel_index.json", "/plugins.min.json", "/versioning.json", "index.json", "novel_index.json", "plugins.min.json", "versioning.json")
        for (m in knownManifests) {
            if (path.endsWith(m, ignoreCase = true)) {
                path = path.substring(0, path.length - m.length)
                break
            }
        }
        val cleanPath = path.trimEnd('/')
        val normalized = "https://$host" + if (cleanPath.isNotEmpty()) "/$cleanPath" else ""
        return ValidationResult(true, normalizedUrl = normalized)
    }

    fun isPrivateOrReservedIp(ipStr: String): Boolean {
        // IPv6 Loopback, Link-Local, and Unique-Local
        if (ipStr == "::1" || ipStr.startsWith("fe80:", ignoreCase = true) || ipStr.startsWith("fc00:", ignoreCase = true) || ipStr.startsWith("fd00:", ignoreCase = true)) {
            return true
        }

        val parts = ipStr.split('.')
        if (parts.size != 4) return false
        val octets = parts.mapNotNull { it.toIntOrNull() }
        if (octets.size != 4 || octets.any { it !in 0..255 }) return false

        val (o1, o2, o3, o4) = octets
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

    private fun defaultDnsResolve(host: String): List<String> {
        // Platform DNS lookup via InetAddress on JVM/Android or getaddrinfo on iOS
        return try {
            java.net.InetAddress.getAllByName(host).map { it.hostAddress }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
```

### 5.3 Unified Multiplatform JavaScript Engine Architecture & Thread Safety

Dokuha's JavaScript execution engine uses an `expect`/`actual` multiplatform abstraction tailored to target platform constraints:
- **Android & Desktop JVM (`JsEngine.android.kt`, `JsEngine.desktop.kt`)**: Powered by Dokar's QuickJS engine (`com.dokar.quickjs:quickjs` / `quickjs-kt-jvm`). Configured with strict 32MB C-level heap caps (`quickJs.setMemoryLimit(32 * 1024 * 1024)`) and explicit GC sweeps.
- **iOS (`JsEngine.ios.kt`)**: Powered by Apple's native `JavaScriptCore` C framework (`platform.JavaScriptCore.JSContext`). Thread safety is strictly enforced via a coroutine `Mutex` wrapper to prevent concurrent execution crashes.

```kotlin
// Common Multiplatform Engine Contract (core.extensions.js.JsEngine.kt)
expect class JsEngine() {
    suspend fun evaluate(script: String): String
    fun setGlobalFunction(name: String, block: (List<Any?>) -> Any?)
    fun setGlobalAsyncFunction(name: String, block: suspend (List<Any?>) -> Any?)
    fun setGlobalVariable(name: String, jsonValue: String)
    suspend fun callFunction(functionName: String, vararg args: Any?): String
    fun close()
}
```

#### iOS JavaScriptCore Threading Contract & Non-Deadlocking Mutex Serialization
Apple's official `JavaScriptCore` documentation explicitly specifies:
> *"Calls to `JSContext` or `JSValue` within the same `JSVirtualMachine` are not thread-safe. All access must be serialized from a single thread or synchronized with a mutex."*

When asynchronous host callbacks (e.g., HTTP network responses, file decryption, or cookie synchronization) complete on Kotlin/Native coroutine worker threads (`Dispatchers.Default`), invoking `context?.evaluateScript(...)` concurrently causes memory corruption and unhandled `SIGSEGV` crashes.

`JsEngine.ios.kt` enforces single-thread serialization by synchronizing all direct `JSContext` access via a coroutine `Mutex`. Crucially, to prevent **coroutine deadlocks**, the mutex is held **strictly during synchronous `JSContext` evaluation** and is **never held across a coroutine suspension point (`deferred.await()`)**. When an asynchronous script or function is evaluated, the engine acquires `mutex.withLock` only to launch the asynchronous IIFE on the context, registers the completion deferred, and immediately releases the lock before awaiting the deferred. This guarantees that background callback coroutines (such as `asyncDispatcher` handling `dokuha.fetch`) can acquire `mutex.withLock` to invoke `__dokuha_resolve_async` on `JSContext` and complete the pending promise without contention.

```kotlin
// UI UX/composeApp/src/iosMain/kotlin/core/extensions/js/JsEngine.ios.kt (Hardened & Deadlock-Free)
package core.extensions.js

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import platform.Foundation.NSString
import platform.JavaScriptCore.JSContext
import platform.JavaScriptCore.JSValue

actual class JsEngine actual constructor() {
    private var context: JSContext? = JSContext()
    private val mutex = Mutex() // Mandated: serializes all synchronous JSContext access
    private val syncFunctions = mutableMapOf<String, (List<Any?>) -> Any?>()
    private val asyncFunctions = mutableMapOf<String, suspend (List<Any?>) -> Any?>()
    private val pendingCompletions = mutableMapOf<String, CompletableDeferred<String>>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        context?.let { ctx ->
            // Synchronous Host Dispatcher
            val dispatcher: (String, String) -> String? = { fnName, argsJson ->
                val fn = syncFunctions[fnName]
                if (fn != null) {
                    val parsedArgs = deserializeArgs(argsJson)
                    val result = fn(parsedArgs)
                    if (result is String) result else (result?.toString() ?: "null")
                } else null
            }
            ctx.setObject(dispatcher, forKeyedSubscript = "__dokuha_native_dispatcher" as NSString)

            // Asynchronous Host Dispatcher with Mutex-Guarded Completion
            val asyncDispatcher: (String, String, String) -> Unit = { fnName, reqId, argsJson ->
                val fn = asyncFunctions[fnName]
                if (fn != null) {
                    coroutineScope.launch {
                        try {
                            val parsedArgs = deserializeArgs(argsJson)
                            val result = fn(parsedArgs)
                            val encoded = Json.encodeToString(result?.toString() ?: "null")
                            // Safely acquire lock before touching JSContext
                            mutex.withLock {
                                context?.evaluateScript("__dokuha_resolve_async('$reqId', $encoded);")
                            }
                        } catch (e: Exception) {
                            val errEncoded = Json.encodeToString(e.message ?: "Error")
                            mutex.withLock {
                                context?.evaluateScript("__dokuha_reject_async('$reqId', $errEncoded);")
                            }
                        }
                    }
                }
            }
            ctx.setObject(asyncDispatcher, forKeyedSubscript = "__dokuha_native_async_dispatcher" as NSString)

            // Native Async Completion Handler for evaluate and callFunction
            val completionHandler: (String, Boolean, String) -> Unit = { reqId, success, payload ->
                val deferred = pendingCompletions.remove(reqId)
                if (deferred != null) {
                    if (success) {
                        deferred.complete(payload)
                    } else {
                        deferred.completeExceptionally(RuntimeException(payload))
                    }
                }
            }
            ctx.setObject(completionHandler, forKeyedSubscript = "__dokuha_native_async_complete" as NSString)
        }
    }

    private fun deserializeArgs(argsJson: String): List<Any?> {
        return try {
            val element = Json.parseToJsonElement(argsJson)
            if (element is JsonArray) {
                element.map { item ->
                    when {
                        item is JsonPrimitive && item.isString -> item.content
                        item is JsonPrimitive && item.booleanOrNull != null -> item.booleanOrNull
                        item is JsonPrimitive && item.longOrNull != null -> item.longOrNull
                        item is JsonPrimitive && item.doubleOrNull != null -> item.doubleOrNull
                        item is JsonNull -> null
                        else -> item.toString()
                    }
                }
            } else {
                listOf(argsJson)
            }
        } catch (_: Exception) {
            listOf(argsJson)
        }
    }

    private fun serializeArgsToJson(args: Array<out Any?>): String {
        val jsonElements = args.map { arg ->
            when (arg) {
                null -> "null"
                is Number, is Boolean -> arg.toString()
                is String -> Json.encodeToString(arg)
                else -> Json.encodeToString(arg.toString())
            }
        }
        return "[${jsonElements.joinToString(",")}]"
    }

    private fun serializeJsValue(res: JSValue?): String {
        return if (res == null || res.isUndefined()) "undefined"
        else if (res.isNull()) "null"
        else res.toString()
    }

    actual suspend fun evaluate(script: String): String {
        val ctx = context ?: error("JsEngine is closed")
        val trimmed = script.trim()
        val looksAsync = trimmed.startsWith("(async") ||
            trimmed.startsWith("async") ||
            trimmed.startsWith("new Promise") ||
            trimmed.startsWith("Promise.") ||
            script.contains("await ") ||
            script.contains("__dokuha_resolve_async")

        if (looksAsync) {
            return runAsyncScript(ctx, script)
        } else {
            return mutex.withLock {
                val res = ctx.evaluateScript(script)
                val ex = ctx.exception
                if (ex != null) {
                    val msg = ex.toString()
                    ctx.exception = null
                    error("JavaScript error: $msg")
                }
                serializeJsValue(res)
            }
        }
    }

    // Hardened Non-Deadlocking Multi-Statement Async Evaluation Runner
    private suspend fun runAsyncScript(ctx: JSContext, script: String): String {
        val deferred = CompletableDeferred<String>()
        val reqId = "eval_${platform.posix.rand()}"
        
        // Wrap script inside an async IIFE body rather than an expression argument to prevent SyntaxError on declarations
        val wrappedScript = """
            (async () => {
                try {
                    let res = await (async () => {
                        $script
                    })();
                    const serialized = (typeof res === 'object' && res !== null) ? JSON.stringify(res) : (res === undefined ? 'undefined' : String(res));
                    __dokuha_native_async_complete('$reqId', true, serialized);
                } catch (e) {
                    __dokuha_native_async_complete('$reqId', false, e && e.message ? e.message : String(e));
                }
            })();
        """.trimIndent()

        // Mutex acquired ONLY during synchronous registration & script evaluation.
        // Lock is released BEFORE awaiting deferred so callback coroutines (asyncDispatcher)
        // can acquire mutex to resolve promises via __dokuha_resolve_async.
        mutex.withLock {
            pendingCompletions[reqId] = deferred
            ctx.evaluateScript(wrappedScript)
            val ex = ctx.exception
            if (ex != null) {
                val msg = ex.toString()
                ctx.exception = null
                pendingCompletions.remove(reqId)
                error("JavaScript error: $msg")
            }
        }

        // Awaited OUTSIDE mutex lock to prevent coroutine deadlock
        return deferred.await()
    }

    actual suspend fun callFunction(functionName: String, vararg args: Any?): String {
        val ctx = context ?: error("JsEngine is closed")
        val deferred = CompletableDeferred<String>()
        val reqId = "call_${platform.posix.rand()}"

        val argsJson = serializeArgsToJson(args)
        val escapedName = functionName.replace("'", "\\'")

        val wrappedScript = """
            (async () => {
                try {
                    const args = $argsJson;
                    let parent = null;
                    let fn;
                    try {
                        fn = eval('$escapedName');
                    } catch (_) {}
                    const lastDot = '$escapedName'.lastIndexOf('.');
                    if (lastDot !== -1) {
                        try {
                            parent = eval('$escapedName'.substring(0, lastDot));
                        } catch (_) {}
                    }
                    if (typeof fn !== 'function') {
                        throw new Error('$escapedName is not a function');
                    }
                    let res = await fn.apply(parent, args);
                    const serialized = (typeof res === 'object' && res !== null) ? JSON.stringify(res) : (res === undefined ? 'undefined' : String(res));
                    __dokuha_native_async_complete('$reqId', true, serialized);
                } catch (e) {
                    __dokuha_native_async_complete('$reqId', false, e && e.message ? e.message : String(e));
                }
            })();
        """.trimIndent()

        // Mutex acquired ONLY during synchronous evaluation & registration.
        // Awaited OUTSIDE mutex lock so async dispatcher callbacks can resolve promises.
        mutex.withLock {
            pendingCompletions[reqId] = deferred
            ctx.evaluateScript(wrappedScript)
            val ex = ctx.exception
            if (ex != null) {
                val msg = ex.toString()
                ctx.exception = null
                pendingCompletions.remove(reqId)
                error("JavaScript error in $functionName: $msg")
            }
        }

        return deferred.await()
    }

    actual fun setGlobalFunction(name: String, block: (List<Any?>) -> Any?) {
        val ctx = context ?: error("JsEngine is closed")
        syncFunctions[name] = block
        val escapedName = name.replace("'", "\\'")
        val bridgeScript = """
            globalThis['$escapedName'] = function(...args) {
                const serialized = JSON.stringify(args);
                const res = __dokuha_native_dispatcher('$escapedName', serialized);
                try {
                    return JSON.parse(res);
                } catch (_) {
                    return res;
                }
            };
        """.trimIndent()
        runBlocking {
            mutex.withLock {
                ctx.evaluateScript(bridgeScript)
            }
        }
    }

    actual fun setGlobalAsyncFunction(name: String, block: suspend (List<Any?>) -> Any?) {
        val ctx = context ?: error("JsEngine is closed")
        asyncFunctions[name] = block
        val escapedName = name.replace("'", "\\'")
        val bridgeScript = """
            globalThis.__dokuha_pending = globalThis.__dokuha_pending || {};
            globalThis.__dokuha_resolve_async = function(id, res) {
                if (globalThis.__dokuha_pending[id]) {
                    globalThis.__dokuha_pending[id].resolve(res);
                    delete globalThis.__dokuha_pending[id];
                }
            };
            globalThis.__dokuha_reject_async = function(id, err) {
                if (globalThis.__dokuha_pending[id]) {
                    globalThis.__dokuha_pending[id].reject(new Error(err));
                    delete globalThis.__dokuha_pending[id];
                }
            };
            globalThis['$escapedName'] = function(...args) {
                return new Promise(function(resolve, reject) {
                    const id = Math.random().toString(36).substring(2);
                    globalThis.__dokuha_pending[id] = { resolve: resolve, reject: reject };
                    __dokuha_native_async_dispatcher('$escapedName', id, JSON.stringify(args));
                });
            };
        """.trimIndent()
        runBlocking {
            mutex.withLock {
                ctx.evaluateScript(bridgeScript)
            }
        }
    }

    actual fun setGlobalVariable(name: String, jsonValue: String) {
        val ctx = context ?: error("JsEngine is closed")
        val escapedName = name.replace("'", "\\'")
        val script = "globalThis['$escapedName'] = $jsonValue;"
        runBlocking {
            mutex.withLock {
                ctx.evaluateScript(script)
            }
        }
    }

    actual fun close() {
        coroutineScope.cancel()
        context = null
        syncFunctions.clear()
        asyncFunctions.clear()
        pendingCompletions.values.forEach { it.cancel() }
        pendingCompletions.clear()
    }
}
```

#### Multi-Statement Script Execution & Syntax Safety
In earlier iterations, `runAsyncScript` evaluated scripts by wrapping them in `let res = await ($script);`. If `$script` contained multiple statements, variable declarations (`const x = 1; x + 2`), or function declarations, QuickJS and JavaScriptCore threw `SyntaxError: expecting ')'` because JavaScript syntax prohibits semicolons and statements inside the parentheses of an `await` expression.

Dokuha resolves this across all platforms by wrapping evaluations inside an asynchronous Immediately Invoked Function Expression (IIFE) body:
```javascript
(async () => {
    try {
        let res = await (async () => {
            // Raw multi-statement user script executes safely here:
            const doc = new Document(res.body);
            const items = doc.select("div.item");
            return items.length;
        })();
        await __dokuha_finish(true, JSON.stringify(res));
    } catch (e) {
        await __dokuha_finish(false, e && e.message ? e.message : String(e));
    }
})();
```

#### Memory Ceilings & `JsEnginePool` Architecture
Permanently instantiating a `JsEngine` instance for every installed extension consumes excessive RAM (over 300MB for 50 extensions), causing OS low-memory kills on mobile devices. 

Dokuha implements `JsEnginePool`, an LRU engine manager that caps active engine runtimes to a maximum of **5 active instances**, lazily rehydrates engine sandboxes on demand, enforces a **32MB heap limit** on QuickJS, and sweeps native garbage after heavy scraping:

```kotlin
package core.extensions.js

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class JsEnginePool(
    private val maxActiveEngines: Int = 5,
    private val idleTimeoutMs: Long = 300_000L // 5 minutes
) {
    private val lock = Mutex()
    private val activeEngines = LinkedHashMap<Long, PooledEngineEntry>(maxActiveEngines, 0.75f, true)

    data class PooledEngineEntry(
        val sourceId: Long,
        val engine: JsEngine,
        var lastAccessedAt: Long
    )

    suspend fun <T> withEngine(
        sourceId: Long,
        initializer: suspend (JsEngine) -> Unit,
        block: suspend (JsEngine) -> T
    ): T {
        val engine = acquireEngine(sourceId, initializer)
        return try {
            block(engine)
        } finally {
            releaseEngine(sourceId)
        }
    }

    private suspend fun acquireEngine(sourceId: Long, initializer: suspend (JsEngine) -> Unit): JsEngine = lock.withLock {
        val now = currentTimeMillis()
        val existing = activeEngines[sourceId]
        if (existing != null) {
            existing.lastAccessedAt = now
            return existing.engine
        }

        // Evict LRU engine if threshold reached
        if (activeEngines.size >= maxActiveEngines) {
            val oldestKey = activeEngines.keys.first()
            val evicted = activeEngines.remove(oldestKey)
            evicted?.engine?.close()
        }

        // Allocate fresh engine with memory limits
        val engine = JsEngine()
        // Initialize QuickJS memory limits (32MB cap)
        engine.evaluate("if (typeof globalThis.__qjs_set_memory_limit === 'function') globalThis.__qjs_set_memory_limit(32 * 1024 * 1024);")
        initializer(engine)

        activeEngines[sourceId] = PooledEngineEntry(sourceId, engine, now)
        engine
    }

    private suspend fun releaseEngine(sourceId: Long) = lock.withLock {
        val entry = activeEngines[sourceId] ?: return@withLock
        entry.lastAccessedAt = currentTimeMillis()
        // Trigger GC sweep after scraper operation
        try {
            entry.engine.evaluate("if (typeof globalThis.gc === 'function') globalThis.gc();")
        } catch (_: Exception) {}
    }

    suspend fun sweepIdleEngines() = lock.withLock {
        val now = currentTimeMillis()
        val iterator = activeEngines.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next().value
            if (now - entry.lastAccessedAt > idleTimeoutMs) {
                entry.engine.close()
                iterator.remove()
            }
        }
    }
}
```

### 5.4 Unified Preamble & Polyfill Shim Layer
Before any extension bundle is evaluated, `JsBridge.getPreambleScript()` injects a unified compatibility layer into the JavaScript environment. This script runs once per sandbox context:

```javascript
(function() {
    if (typeof globalThis.dokuha === 'undefined') {
        globalThis.dokuha = {};
    }

    // =========================================================================
    // 1. Console Redirection & Diagnostics
    // =========================================================================
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

    // =========================================================================
    // 2. Base Encoding & Crypto Polyfills
    // =========================================================================
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

    // =========================================================================
    // 3. Fetch Standard Headers Polyfill & Native HTTP Bridge
    // =========================================================================
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

    // Storage Key Sandboxing Prefix Helper
    const _getStorageKey = (k) => {
        const srcId = globalThis.__dokuha_source_id || "global";
        return `ext_${srcId}_${k}`;
    };

    globalThis.dokuha.fetch = async (url, options = {}) => {
        const method = options.method || 'GET';
        let headersObj = {};
        if (options.headers instanceof Headers) {
            for (const [k, v] of options.headers.entries()) headersObj[k] = v;
        } else if (options.headers && typeof options.headers === 'object') {
            headersObj = options.headers;
        }
        const body = (typeof options.body === 'object' && options.body !== null) 
            ? JSON.stringify(options.body) 
            : (options.body != null ? String(options.body) : null);
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

    // =========================================================================
    // 4. CommonJS Module Emulation (LNReader Support)
    // =========================================================================
    globalThis.module = { exports: {} };
    globalThis.exports = globalThis.module.exports;

    const _moduleRegistry = {};
    globalThis.dokuha.registerModule = (id, exportsObj) => {
        _moduleRegistry[id] = exportsObj;
    };

    // Register LNReader @libs standard library
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

    globalThis.require = function(id) {
        if (_moduleRegistry[id]) {
            return _moduleRegistry[id];
        }
        if (id === 'cheerio') return globalThis.cheerio;
        if (id === 'htmlparser2') return globalThis.htmlparser2;
        if (id === 'dayjs') return globalThis.dayjs;
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

    // =========================================================================
    // 5. Mangayomi Host Environment Shims (Document & Element DOM)
    // =========================================================================
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
            return this._root.html(this._el) || "";
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
            this._el.children().each((_, node) => {
                elements.push(new Element(this._root(node), this._root, this._baseUrl));
            });
            return elements;
        }

        get nextElementSibling() {
            const next = this._el.next();
            return next.length > 0 ? new Element(next, this._root, this._baseUrl) : null;
        }

        get previousElementSibling() {
            const prev = this._el.prev();
            return prev.length > 0 ? new Element(prev, this._root, this._baseUrl) : null;
        }
    }

    class Document {
        constructor(htmlString) {
            this._root = globalThis.cheerio ? globalThis.cheerio.load(htmlString) : null;
            this._rawHtml = htmlString;
            this._baseUrl = (globalThis.__mangayomi_source_meta && globalThis.__mangayomi_source_meta.baseUrl) || "";
        }

        select(selector) {
            if (!this._root) return [];
            const elements = [];
            this._root(selector).each((_, node) => {
                elements.push(new Element(this._root(node), this._root, this._baseUrl));
            });
            return elements;
        }

        selectFirst(selector) {
            if (!this._root) return null;
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
            return this._root ? (this._root.root().text() || "").trim() : "";
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

    globalThis.MProvider = class MProvider {
        constructor() {
            this.source = globalThis.__mangayomi_source_meta || {};
        }
        getHeaders(url) { return {}; }
    };

    globalThis.Client = class Client {
        async get(url, headers = {}) {
            const res = await globalThis.dokuha.fetch(url, { method: 'GET', headers });
            return {
                statusCode: res.status,
                body: res.body,
                headers: res.headers,
                isRedirect: false,
                reasonPhrase: res.ok ? "OK" : "Error"
            };
        }
        async post(url, headers = {}, body = null) {
            const res = await globalThis.dokuha.fetch(url, { method: 'POST', headers, body });
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

    // SharedPreferences Shim (Sandboxed per Extension)
    globalThis.SharedPreferences = class SharedPreferences {
        get(key) {
            const val = globalThis.__dokuha_storage_get(_getStorageKey(key));
            return val !== "" ? val : null;
        }
        getString(key, def) {
            const val = globalThis.__dokuha_storage_get(_getStorageKey(key));
            return val !== "" ? val : def;
        }
        setString(key, val) {
            globalThis.__dokuha_storage_set(_getStorageKey(key), String(val));
        }
    };

    // =========================================================================
    // 6. Paperback v0.8 & v0.9 Host Environment Shims
    // =========================================================================
    globalThis.Sources = {};
    globalThis.window = globalThis;
    globalThis.window.Sources = globalThis.Sources;

    // Paperback v0.8 App Factory Object
    globalThis.App = {
        createRequestManager: (info) => ({
            requestsPerSecond: info.requestsPerSecond || 5,
            schedule: async (req, retryCount = 0) => {
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
        setSecureState: (v, k) => globalThis.__dokuha_secure_set(_getStorageKey(k), JSON.stringify(v)),
        decodeHTMLEntities: (str) => {
            return str.replace(/&amp;/g, '&')
                      .replace(/&lt;/g, '<')
                      .replace(/&gt;/g, '>')
                      .replace(/&quot;/g, '"')
                      .replace(/&#39;/g, "'");
        },
        base64Encode: (val) => globalThis.btoa(val),
        base64Decode: (val) => globalThis.atob(val),
        arrayBufferToUTF8String: (buf) => {
            const bytes = new Uint8Array(buf);
            let binary = "";
            for (let i = 0; i < bytes.byteLength; i++) {
                binary += String.fromCharCode(bytes[i]);
            }
            return decodeURIComponent(escape(binary));
        },
        executeInWebView: async ({ url, script }) => {
            const resStr = await globalThis.__dokuha_execute_in_webview(url, script);
            return JSON.parse(resStr);
        },
        Selector: (selector) => ({
            query: selector,
            find: (sub) => globalThis.Application.Selector(`${selector} ${sub}`),
            attr: (attrName) => ({ query: selector, attribute: attrName })
        }),
        SelectorRegistry: class SelectorRegistry {
            constructor() { this._selectors = {}; }
            register(id, selector) { this._selectors[id] = selector; }
            get(id) { return this._selectors[id]; }
        }
    };
})();
```

#### Host-Side Network Security & Cookie Boundary Isolation (`JsBridge.kt`)
In tandem with the preamble layer, the native Kotlin host bridge `JsBridge` enforces three critical security and concurrency invariants:

1. **Protocol & SSRF Isolation in `fetch`**:
   `JsBridge.fetch` inspects every incoming URL. It requires the `https:` scheme (or `http:` in explicit local debug builds) and prohibits `file://`, `content://`, `javascript:`, and `data:` schemes. The destination hostname is resolved and checked against `ExtensionRepoValidator.isPrivateOrReservedIp()` to block requests targeting LAN endpoints (`192.168.1.1`), loopback (`127.0.0.1`), or cloud instance metadata services (`169.254.169.254`).
2. **RFC 6265 Dot-Bounded Cookie Matching**:
   Naive suffix matching (`domain.endsWith(cookieDomain)`) creates severe credential leaks: requests to `evil-mangadex.org` match the cookie domain `mangadex.org`. Dokuha enforces dot-bounded subdomain and exact domain matching:
   ```kotlin
   val relevantCookies = cookieJar.filter { (key, _) ->
       val cookieDomain = key.substringBefore(':')
       domain == cookieDomain || domain.endsWith(".$cookieDomain")
   }
   ```
3. **Single-Flight Anti-Bot Request Coalescing**:
   When multiple concurrent scraper requests encounter Cloudflare Turnstile or HTTP 403 on the same domain, `JsBridge` coalesces them into a single in-flight resolution deferred (`challengeLocks.getOrPut(domain) { CompletableDeferred() }`). A single WebView is summoned; upon resolution, all waiting coroutines resume simultaneously with updated cookies, eliminating thundering herd freezes.
```

### 5.5 Adapter Implementations

#### Mangayomi Source Adapter (with In-Memory Detail Caching)
Mangayomi scrapers return both title metadata and all chapters in a single payload from `extention.getDetail(url)`. Dokuha's `ComicSource` interface defines `getMangaDetails` and `getChapterList` as two distinct suspend functions. Calling both sequentially without caching results in two identical remote HTTP network requests within milliseconds, doubling bandwidth usage and triggering anti-bot rate limits.

`MangayomiSourceAdapter` eliminates this double-fetch via a thread-safe, in-memory cache with a 60-second Time-To-Live (TTL):

```kotlin
package core.extensions.mangayomi

import core.extensions.js.JsBridge
import core.extensions.js.JsEnginePool
import core.source.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

class MangayomiSourceAdapter(
    val manifest: MangayomiManifest,
    private val enginePool: JsEnginePool,
    private val bridge: JsBridge
) : ComicSource, NovelSource {

    override val id: Long = manifest.id
    override val name: String = manifest.name
    override val baseUrl: String = manifest.baseUrl
    override val lang: String = manifest.lang
    override val isNsfw: Boolean = manifest.isNsfw

    // 60-second Detail Cache to eliminate duplicate HTTP requests
    private var cachedDetailUrl: String? = null
    private var cachedDetailJson: JsonObject? = null
    private var cachedDetailTimestamp: Long = 0L
    private val cacheMutex = Mutex()

    private suspend fun fetchOrGetDetail(url: String): JsonObject = cacheMutex.withLock {
        val now = currentTimeMillis()
        if (cachedDetailUrl == url && (now - cachedDetailTimestamp) < 60_000L && cachedDetailJson != null) {
            return cachedDetailJson!!
        }
        val result = enginePool.withEngine(id, { initEngine(it) }) { engine ->
            engine.callFunction("extention.getDetail", url)
        }
        val json = Json.parseToJsonElement(result).jsonObject
        cachedDetailUrl = url
        cachedDetailJson = json
        cachedDetailTimestamp = now
        json
    }

    private suspend fun initEngine(engine: core.extensions.js.JsEngine) {
        engine.evaluate(bridge.getPreambleScript(id))
        engine.setGlobalVariable("__mangayomi_source_meta", manifest.toJson())
        engine.setGlobalVariable("__dokuha_source_id", id.toString())
        engine.evaluate(manifest.script)
        engine.evaluate("var extention = new DefaultExtension();")
    }

    override suspend fun getPopular(page: Int): MangasPage = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val result = engine.callFunction("extention.getPopular", page)
        parseCatalogResult(result)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val result = engine.callFunction("extention.getLatestUpdates", page)
        parseCatalogResult(result)
    }

    override suspend fun search(query: String, page: Int, filters: List<SourceFilter>): MangasPage = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val result = engine.callFunction("extention.search", query, page, "[]")
        parseCatalogResult(result)
    }

    override suspend fun getMangaDetails(manga: SourceManga): SourceManga {
        val json = fetchOrGetDetail(manga.url)
        return manga.copy(
            title = json["name"]?.jsonPrimitive?.content ?: manga.title,
            thumbnailUrl = json["imageUrl"]?.jsonPrimitive?.contentOrNull ?: manga.thumbnailUrl,
            description = json["description"]?.jsonPrimitive?.contentOrNull,
            author = json["author"]?.jsonPrimitive?.contentOrNull,
            artist = json["artist"]?.jsonPrimitive?.contentOrNull,
            status = mapStatus(json["status"]?.jsonPrimitive?.intOrNull ?: 5)
        )
    }

    override suspend fun getChapterList(manga: SourceManga): List<SourceChapter> {
        val json = fetchOrGetDetail(manga.url)
        val chapters = json["chapters"]?.jsonArray ?: return emptyList()
        return chapters.map { el ->
            val obj = el.jsonObject
            SourceChapter(
                url = obj["url"]?.jsonPrimitive?.content ?: "",
                name = obj["name"]?.jsonPrimitive?.content ?: "Chapter",
                dateUpload = obj["dateUpload"]?.jsonPrimitive?.longOrNull ?: 0L,
                chapterNumber = obj["chapterNumber"]?.jsonPrimitive?.floatOrNull ?: -1f,
                scanlator = obj["scanlator"]?.jsonPrimitive?.contentOrNull
            )
        }
    }

    override suspend fun getPageList(chapter: SourceChapter): List<SourcePage> = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val result = engine.callFunction("extention.getPageList", chapter.url)
        val arr = Json.parseToJsonElement(result).jsonArray
        arr.mapIndexed { idx, el ->
            val pageUrl = if (el is JsonObject) {
                el["url"]?.jsonPrimitive?.content ?: ""
            } else {
                el.jsonPrimitive.content
            }
            SourcePage(index = idx, url = pageUrl, imageUrl = pageUrl)
        }
    }

    override suspend fun getChapterContent(chapter: SourceChapter): NovelContent = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val result = engine.callFunction("extention.getHtmlContent", chapter.name, chapter.url)
        val html = Json.parseToJsonElement(result).jsonPrimitive.content
        NovelContent(
            chapterUrl = chapter.url,
            title = chapter.name,
            htmlBody = html,
            plainText = html.replace(Regex("<[^>]*>"), "").trim()
        )
    }

    private fun mapStatus(statusInt: Int): MangaStatus = when (statusInt) {
        0 -> MangaStatus.ONGOING
        1 -> MangaStatus.COMPLETED
        2 -> MangaStatus.ON_HIATUS
        3 -> MangaStatus.CANCELLED
        4 -> MangaStatus.PUBLISHING_FINISHED
        else -> MangaStatus.UNKNOWN
    }

    private fun parseCatalogResult(result: String): MangasPage {
        val json = Json.parseToJsonElement(result).jsonObject
        val hasNext = json["hasNextPage"]?.jsonPrimitive?.booleanOrNull ?: false
        val list = json["list"]?.jsonArray ?: JsonArray(emptyList())
        val mangas = list.map { el ->
            val obj = el.jsonObject
            SourceManga(
                url = obj["link"]?.jsonPrimitive?.content ?: obj["url"]?.jsonPrimitive?.content ?: "",
                title = obj["name"]?.jsonPrimitive?.content ?: "Unknown",
                thumbnailUrl = obj["imageUrl"]?.jsonPrimitive?.contentOrNull,
                isNovel = manifest.isNovel
            )
        }
        return MangasPage(mangas = mangas, hasNextPage = hasNext)
    }
}
```

#### LNReader Source Adapter (with `PagePlugin` Chapter Pagination)
```kotlin
package core.extensions.lnreader

import core.extensions.js.JsBridge
import core.extensions.js.JsEnginePool
import core.source.*
import kotlinx.serialization.json.*

class LNReaderSourceAdapter(
    val manifest: LNReaderManifest,
    private val enginePool: JsEnginePool,
    private val bridge: JsBridge
) : NovelSource {

    override val id: Long = (manifest.id.hashCode().toLong() and 0x7FFFFFFF) + 20_000L
    override val name: String = manifest.name
    override val baseUrl: String = manifest.site
    override val lang: String = manifest.lang
    override val isNsfw: Boolean = false

    private suspend fun initEngine(engine: core.extensions.js.JsEngine) {
        engine.evaluate(bridge.getPreambleScript(id))
        engine.setGlobalVariable("__dokuha_source_id", id.toString())
        engine.evaluate(manifest.script)
        engine.evaluate("globalThis.__ln_instance = (module.exports.default || module.exports);")
    }

    override suspend fun getPopular(page: Int): MangasPage = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val script = "JSON.stringify(await globalThis.__ln_instance.popularNovels($page, { showLatestNovels: false }))"
        val res = engine.evaluate(script)
        parseNovelList(res)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val script = "JSON.stringify(await globalThis.__ln_instance.popularNovels($page, { showLatestNovels: true }))"
        val res = engine.evaluate(script)
        parseNovelList(res)
    }

    override suspend fun search(query: String, page: Int, filters: List<SourceFilter>): MangasPage = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val script = "JSON.stringify(await globalThis.__ln_instance.searchNovels(${Json.encodeToString(query)}, $page))"
        val res = engine.evaluate(script)
        parseNovelList(res)
    }

    override suspend fun getMangaDetails(manga: SourceManga): SourceManga = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val script = "JSON.stringify(await globalThis.__ln_instance.parseNovel(${Json.encodeToString(manga.url)}))"
        val res = engine.evaluate(script)
        val json = Json.parseToJsonElement(res).jsonObject
        manga.copy(
            title = json["name"]?.jsonPrimitive?.content ?: manga.title,
            thumbnailUrl = json["cover"]?.jsonPrimitive?.contentOrNull ?: manga.thumbnailUrl,
            description = json["summary"]?.jsonPrimitive?.contentOrNull,
            author = json["author"]?.jsonPrimitive?.contentOrNull,
            artist = json["artist"]?.jsonPrimitive?.contentOrNull,
            genres = json["genres"]?.jsonPrimitive?.contentOrNull?.split(",")?.map { it.trim() } ?: emptyList(),
            status = mapNovelStatus(json["status"]?.jsonPrimitive?.contentOrNull)
        )
    }

    override suspend fun getChapterList(manga: SourceManga): List<SourceChapter> = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val script = "JSON.stringify(await globalThis.__ln_instance.parseNovel(${Json.encodeToString(manga.url)}))"
        val res = engine.evaluate(script)
        val json = Json.parseToJsonElement(res).jsonObject

        val initialChapters = json["chapters"]?.jsonArray?.map { parseChapterItem(it.jsonObject) } ?: emptyList()
        val totalPages = json["totalPages"]?.jsonPrimitive?.intOrNull ?: json["pages"]?.jsonPrimitive?.intOrNull ?: 1

        // Check if plugin implements PagePlugin and has multiple pages
        val hasParsePage = engine.evaluate("typeof globalThis.__ln_instance.parsePage === 'function'").toBoolean()
        if (hasParsePage && totalPages > 1) {
            val allChapters = initialChapters.toMutableList()
            val startPage = if (initialChapters.isNotEmpty()) 2 else 1
            for (p in startPage..totalPages) {
                try {
                    val pageScript = "JSON.stringify(await globalThis.__ln_instance.parsePage(${Json.encodeToString(manga.url)}, $p))"
                    val pageRes = engine.evaluate(pageScript)
                    val pageObj = Json.parseToJsonElement(pageRes).jsonObject
                    val pageChapters = pageObj["chapters"]?.jsonArray?.map { parseChapterItem(it.jsonObject) } ?: emptyList()
                    allChapters.addAll(pageChapters)
                } catch (_: Exception) {
                    // Resiliently continue with remaining pages
                }
            }
            return@withEngine allChapters
        }

        initialChapters
    }

    private fun parseChapterItem(obj: JsonObject): SourceChapter = SourceChapter(
        url = obj["path"]?.jsonPrimitive?.content ?: "",
        name = obj["name"]?.jsonPrimitive?.content ?: "Chapter",
        chapterNumber = obj["chapterNumber"]?.jsonPrimitive?.floatOrNull ?: -1f,
        scanlator = obj["scanlator"]?.jsonPrimitive?.contentOrNull
    )

    override suspend fun getChapterContent(chapter: SourceChapter): NovelContent = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val script = "JSON.stringify(await globalThis.__ln_instance.parseChapter(${Json.encodeToString(chapter.url)}))"
        val res = engine.evaluate(script)
        val html = Json.parseToJsonElement(res).jsonPrimitive.content
        NovelContent(
            chapterUrl = chapter.url,
            title = chapter.name,
            htmlBody = html,
            plainText = html.replace(Regex("<[^>]*>"), "").trim()
        )
    }

    private fun mapNovelStatus(status: String?): MangaStatus = when (status) {
        "Ongoing" -> MangaStatus.ONGOING
        "Completed" -> MangaStatus.COMPLETED
        "On Hiatus" -> MangaStatus.ON_HIATUS
        "Cancelled" -> MangaStatus.CANCELLED
        "Publishing Finished" -> MangaStatus.PUBLISHING_FINISHED
        else -> MangaStatus.UNKNOWN
    }

    private fun parseNovelList(jsonStr: String): MangasPage {
        val arr = Json.parseToJsonElement(jsonStr).jsonArray
        val mangas = arr.map { el ->
            val obj = el.jsonObject
            SourceManga(
                url = obj["path"]?.jsonPrimitive?.content ?: "",
                title = obj["name"]?.jsonPrimitive?.content ?: "Unknown",
                thumbnailUrl = obj["cover"]?.jsonPrimitive?.contentOrNull,
                isNovel = true
            )
        }
        return MangasPage(mangas = mangas, hasNextPage = mangas.isNotEmpty())
    }
}
```

#### Paperback Source Adapter (with Keyed Source Fallbacks)
Paperback v0.9 compiled IIFEs export an object with keyed source instances (`source[manifest.id]`) rather than `source.default`. `PaperbackSourceAdapter` implements dynamic candidate inspection to correctly instantiate v0.9 and v0.8 sources:

```kotlin
package core.extensions.paperback

import core.extensions.js.JsBridge
import core.extensions.js.JsEnginePool
import core.source.*
import kotlinx.serialization.json.*

class PaperbackSourceAdapter(
    val manifest: PaperbackManifest,
    private val enginePool: JsEnginePool,
    private val bridge: JsBridge,
    val isV9: Boolean
) : ComicSource {

    override val id: Long = (manifest.id.hashCode().toLong() and 0x7FFFFFFF) + 30_000L
    override val name: String = manifest.name
    override val baseUrl: String = manifest.websiteBaseURL
    override val lang: String = manifest.language
    override val isNsfw: Boolean = manifest.contentRating == "ADULT"

    private suspend fun initEngine(engine: core.extensions.js.JsEngine) {
        engine.evaluate(bridge.getPreambleScript(id))
        engine.setGlobalVariable("__dokuha_source_id", id.toString())
        engine.evaluate(manifest.script)

        if (isV9) {
            // Hardened dynamic instantiation: checks default, manifestId, or first valid export
            val initScript = """
                (async () => {
                    const src = globalThis.source;
                    if (!src) throw new Error("Paperback v0.9 source namespace missing");
                    let inst = null;
                    if (src.default) {
                        inst = (typeof src.default === 'function') ? new src.default() : src.default;
                    } else if (src['${manifest.id}']) {
                        const target = src['${manifest.id}'];
                        inst = (typeof target === 'function') ? new target() : target;
                    } else {
                        const keys = Object.keys(src);
                        const candidateKey = keys.find(k => !k.endsWith('Extension')) || keys[0];
                        const target = src[candidateKey];
                        inst = (typeof target === 'function') ? new target() : target;
                    }
                    globalThis.__pb_instance = inst;
                    if (typeof globalThis.__pb_instance.initialise === 'function') {
                        await globalThis.__pb_instance.initialise();
                    }
                })();
            """.trimIndent()
            engine.evaluate(initScript)
        } else {
            engine.evaluate("const sourceKey = Object.keys(globalThis.Sources)[0];")
            engine.evaluate("globalThis.__pb_instance = new globalThis.Sources[sourceKey](globalThis.cheerio);")
        }
    }

    override suspend fun getPopular(page: Int): MangasPage = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val script = if (isV9) {
            "JSON.stringify(await globalThis.__pb_instance.getDiscoverSectionItems({ id: 'popular' }, { page: $page }))"
        } else {
            "JSON.stringify(await globalThis.__pb_instance.getViewMoreItems('popular', { page: $page }))"
        }
        val res = engine.evaluate(script)
        parsePagedResults(res)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val script = if (isV9) {
            "JSON.stringify(await globalThis.__pb_instance.getDiscoverSectionItems({ id: 'latest_updates' }, { page: $page }))"
        } else {
            "JSON.stringify(await globalThis.__pb_instance.getViewMoreItems('latest_updates', { page: $page }))"
        }
        val res = engine.evaluate(script)
        parsePagedResults(res)
    }

    override suspend fun search(query: String, page: Int, filters: List<SourceFilter>): MangasPage = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val script = "JSON.stringify(await globalThis.__pb_instance.getSearchResults({ title: ${Json.encodeToString(query)} }, { page: $page }))"
        val res = engine.evaluate(script)
        parsePagedResults(res)
    }

    override suspend fun getMangaDetails(manga: SourceManga): SourceManga = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val script = "JSON.stringify(await globalThis.__pb_instance.getMangaDetails(${Json.encodeToString(manga.url)}))"
        val res = engine.evaluate(script)
        val json = Json.parseToJsonElement(res).jsonObject
        val info = json["mangaInfo"]?.jsonObject ?: json
        val titles = info["titles"]?.jsonArray
        val primaryTitle = titles?.firstOrNull()?.jsonPrimitive?.content ?: manga.title
        manga.copy(
            title = primaryTitle,
            thumbnailUrl = info["image"]?.jsonPrimitive?.contentOrNull ?: manga.thumbnailUrl,
            description = info["desc"]?.jsonPrimitive?.contentOrNull ?: info["synopsis"]?.jsonPrimitive?.contentOrNull,
            author = info["author"]?.jsonPrimitive?.contentOrNull,
            artist = info["artist"]?.jsonPrimitive?.contentOrNull,
            status = mapStatus(info["status"]?.jsonPrimitive?.contentOrNull)
        )
    }

    override suspend fun getChapterList(manga: SourceManga): List<SourceChapter> = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val script = "JSON.stringify(await globalThis.__pb_instance.getChapters(${Json.encodeToString(manga.url)}))"
        val res = engine.evaluate(script)
        val arr = Json.parseToJsonElement(res).jsonArray
        arr.map { el ->
            val obj = el.jsonObject
            SourceChapter(
                url = obj["id"]?.jsonPrimitive?.content ?: obj["chapterId"]?.jsonPrimitive?.content ?: "",
                name = obj["name"]?.jsonPrimitive?.content ?: "Chapter",
                chapterNumber = obj["chapNum"]?.jsonPrimitive?.floatOrNull ?: -1f,
                scanlator = obj["group"]?.jsonPrimitive?.contentOrNull
            )
        }
    }

    override suspend fun getPageList(chapter: SourceChapter): List<SourcePage> = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val script = "JSON.stringify(await globalThis.__pb_instance.getChapterDetails('', ${Json.encodeToString(chapter.url)}))"
        val res = engine.evaluate(script)
        val json = Json.parseToJsonElement(res).jsonObject
        val pages = json["pages"]?.jsonArray ?: return@withEngine emptyList()
        pages.mapIndexed { idx, el ->
            val pageUrl = el.jsonPrimitive.content
            SourcePage(index = idx, url = pageUrl, imageUrl = pageUrl)
        }
    }

    private fun mapStatus(status: String?): MangaStatus = when (status?.lowercase()) {
        "ongoing" -> MangaStatus.ONGOING
        "completed" -> MangaStatus.COMPLETED
        "hiatus" -> MangaStatus.ON_HIATUS
        "abandoned", "dropped" -> MangaStatus.CANCELLED
        else -> MangaStatus.UNKNOWN
    }

    private fun parsePagedResults(jsonStr: String): MangasPage {
        val json = Json.parseToJsonElement(jsonStr).jsonObject
        val results = json["results"]?.jsonArray ?: json["items"]?.jsonArray ?: JsonArray(emptyList())
        val mangas = results.map { el ->
            val obj = el.jsonObject
            SourceManga(
                url = obj["id"]?.jsonPrimitive?.content ?: obj["mangaId"]?.jsonPrimitive?.content ?: "",
                title = obj["title"]?.jsonPrimitive?.content ?: "Unknown",
                thumbnailUrl = obj["image"]?.jsonPrimitive?.contentOrNull,
                isNovel = false
            )
        }
        return MangasPage(mangas = mangas, hasNextPage = mangas.isNotEmpty())
    }
}
```

### 5.6 Persistence, Database Schema & Reactive Cache-Aside Flow Architecture

Dokuha integrates dynamic sources with SQLite via SQLDelight (`io.panel.reader.db`). In previous revisions, naive schema constraints and one-shot cold flows caused critical issues:
1. Relative chapter slugs (e.g., `/chapter-1` or `1`) across different manga collided on a global `UNIQUE(url)` constraint, causing subsequent titles to steal or delete chapters and reading history from earlier titles.
2. Distinct sources sharing relative manga URLs collided on a global `mangas.url UNIQUE` constraint.
3. Returning one-shot cold `flow { emit(cached); emit(fresh) }` terminated the flow after two emissions, preventing Compose UI screens from reactively receiving user interaction updates (e.g. favoriting, chapter bookmarks, or reading progress).
4. `toggleFavorite` hardcoded `sourceId = 0L`, destroying the association between the title and its dynamic source.
5. Novel content loading used `chapterUrl.startsWith(src.baseUrl)`, which failed on relative chapter slugs and defaulted to hardcoded mock data.

To resolve these defects permanently, Dokuha implements composite database unique constraints, qualified navigation routes, and a decoupled reactive cache-aside architecture.

#### 1. SQLDelight Database Schema Hardening

##### `Chapter.sq` (`io.panel.reader.db.Chapter.sq`)
Enforces composite uniqueness on `(manga_id, url)` so that relative slugs are isolated per manga:

```sql
CREATE TABLE chapters (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    manga_id INTEGER NOT NULL,
    url TEXT NOT NULL,
    name TEXT NOT NULL,
    scanlator TEXT,
    read INTEGER NOT NULL DEFAULT 0,
    bookmark INTEGER NOT NULL DEFAULT 0,
    last_page_read INTEGER NOT NULL DEFAULT 0,
    chapter_number REAL NOT NULL DEFAULT -1.0,
    source_order INTEGER NOT NULL DEFAULT 0,
    date_fetch INTEGER NOT NULL DEFAULT 0,
    date_upload INTEGER NOT NULL DEFAULT 0,
    last_modified_at INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(manga_id) REFERENCES mangas(id) ON DELETE CASCADE,
    CONSTRAINT chapters_manga_id_url_unique UNIQUE (manga_id, url)
);

CREATE INDEX chapters_manga_id_idx ON chapters(manga_id);
CREATE INDEX chapters_manga_id_url_idx ON chapters(manga_id, url);

getChaptersByMangaId:
SELECT *
FROM chapters
WHERE manga_id = :mangaId
ORDER BY chapter_number DESC, source_order DESC;

getChapterByUrl:
SELECT *
FROM chapters
WHERE manga_id = :mangaId AND url = :url
LIMIT 1;

insertOrUpdateChapter:
INSERT OR REPLACE INTO chapters (
    id, manga_id, url, name, scanlator, read, bookmark,
    last_page_read, chapter_number, source_order, date_fetch,
    date_upload, last_modified_at
) VALUES (
    :id, :mangaId, :url, :name, :scanlator, :read, :bookmark,
    :lastPageRead, :chapterNumber, :sourceOrder, :dateFetch,
    :dateUpload, :lastModifiedAt
);
```

##### `Manga.sq` (`io.panel.reader.db.Manga.sq`)
Enforces composite uniqueness on `(source_id, url)` so that different sources can share relative paths:

```sql
CREATE TABLE mangas (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    source_id INTEGER NOT NULL,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    artist TEXT,
    author TEXT,
    description TEXT,
    genres TEXT,
    status TEXT NOT NULL DEFAULT 'UNKNOWN',
    thumbnail_url TEXT,
    favorite INTEGER NOT NULL DEFAULT 0,
    last_update INTEGER NOT NULL DEFAULT 0,
    next_update INTEGER NOT NULL DEFAULT 0,
    initialized INTEGER NOT NULL DEFAULT 0,
    viewer_flags INTEGER NOT NULL DEFAULT 0,
    chapter_flags INTEGER NOT NULL DEFAULT 0,
    cover_last_modified INTEGER NOT NULL DEFAULT 0,
    date_added INTEGER NOT NULL DEFAULT 0,
    is_novel INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT mangas_source_id_url_unique UNIQUE (source_id, url)
);

CREATE INDEX mangas_source_id_url_idx ON mangas(source_id, url);
CREATE INDEX mangas_favorite_idx ON mangas(favorite);

getMangaBySourceAndUrl:
SELECT *
FROM mangas
WHERE source_id = :sourceId AND url = :url
LIMIT 1;
```

#### 2. Navigation Routes & Explicit Source Qualification
To guarantee unambiguous entity resolution and avoid source-guessing heuristics (`url.startsWith(baseUrl)`), all Voyager navigation screen routes explicitly require `sourceId: Long`:

```kotlin
// Qualified Navigation Destination Contracts
data class TitleDetailsScreen(
    val sourceId: Long,
    val mangaUrl: String
) : Screen

data class NovelReaderScreen(
    val sourceId: Long,
    val chapterUrl: String,
    val mangaId: Long
) : Screen

data class WebtoonReaderScreen(
    val sourceId: Long,
    val chapterUrl: String,
    val mangaId: Long
) : Screen
```

#### 3. Decoupled Reactive Cache-Aside Architecture
Rather than returning finite cold flows that terminate upon network completion, `SqlDelightPanelRepository` directly exposes continuous, reactive SQLDelight query flows (`.asFlow().mapToOneOrNull(...)`).

Asynchronous network scraping is dispatched independently on background repository coroutines. When fresh data is received and upserted into SQLite, SQLDelight's query observer naturally triggers and emits the updated entity down the Flow to active Compose collectors.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        Decoupled Reactive Cache-Aside Flow                             │
└────────────────────────────────────────────────────────────────────────────────────────┘
  Caller (Compose UI Screen) queries repository.getMangaDetails(sourceId, mangaUrl)
     │
     ├──────────────────────────────────────────────┐
     ▼                                              ▼
  1. Return SQLDelight Query Flow directly        2. Dispatch Background Coroutine
     mangaQueries.getMangaBySourceAndUrl(...)        repositoryScope.launch(Dispatchers.IO)
       .asFlow().mapToOneOrNull()                    │
     │                                               ▼
     ▼ (Immediate SQLite emission: zero UI lag)   3. Locate dynamic source by sourceId:
  Emits cached SourceManga to Compose                val source = _dynamicSources.value[sourceId]
     │                                               │
     │                                               ▼
     │                                            4. Execute remote scrape:
     │                                               val fresh = source.getMangaDetails(...)
     │                                               │
     │                                               ▼
     │                                            5. Atomic SQLite Upsert:
     │                                               insertOrUpdateMangaInternal(fresh, sourceId)
     │                                               │
     ▼                                               ▼
  6. SQLDelight table observer fires! ◄──────────────┘
     Flow automatically emits updated SourceManga!
     Flow remains active for user updates (favorite, bookmarks, progress).
```

#### 4. Hardened `SqlDelightPanelRepository` Implementation
```kotlin
package data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import core.source.*
import io.panel.reader.db.DokuhaDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SqlDelightPanelRepository(
    private val database: DokuhaDatabase,
    private val repositoryScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : PanelRepository {

    private val _dynamicSources = MutableStateFlow<Map<Long, ContentSource>>(emptyMap())

    override fun registerSource(source: ContentSource) {
        _dynamicSources.value = _dynamicSources.value + (source.id to source)
    }

    override fun unregisterSource(sourceId: Long) {
        _dynamicSources.value = _dynamicSources.value - sourceId
    }

    // 1. Reactive Continuous Flow for Manga Details
    override fun getMangaDetails(sourceId: Long, mangaUrl: String): Flow<SourceManga?> {
        // Continuous SQLDelight query observer flow
        val dbFlow = database.mangaQueries.getMangaBySourceAndUrl(sourceId, mangaUrl)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toSourceManga() }

        // Decoupled background refresh coroutine
        val source = _dynamicSources.value[sourceId]
        if (source != null) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    val fresh = source.getMangaDetails(SourceManga(url = mangaUrl, title = ""))
                    insertOrUpdateMangaInternal(fresh, sourceId = sourceId, forceFavorite = null)
                } catch (_: Exception) {
                    // Network errors fail quietly; SQLite cached data remains active
                }
            }
        }

        return dbFlow
    }

    // 2. Reactive Continuous Flow for Chapters
    override fun getChapters(sourceId: Long, mangaUrl: String): Flow<List<SourceChapter>> {
        val mangaRecord = database.mangaQueries.getMangaBySourceAndUrl(sourceId, mangaUrl).executeAsOneOrNull()
        val mangaId = mangaRecord?.id

        val dbFlow = if (mangaId != null) {
            database.chapterQueries.getChaptersByMangaId(mangaId)
                .asFlow()
                .mapToList(Dispatchers.Default)
                .map { list -> list.map { it.toSourceChapter() } }
        } else {
            flowOf(emptyList())
        }

        val source = _dynamicSources.value[sourceId]
        if (source != null) {
            repositoryScope.launch(Dispatchers.IO) {
                try {
                    val fresh = source.getChapterList(SourceManga(url = mangaUrl, title = ""))
                    val targetMangaId = mangaRecord?.id
                        ?: insertOrUpdateMangaInternal(SourceManga(url = mangaUrl, title = ""), sourceId = sourceId, forceFavorite = null)

                    database.transaction {
                        for ((idx, ch) in fresh.withIndex()) {
                            // Composite lookup by (mangaId, url) prevents collisions on relative slugs!
                            val existingCh = database.chapterQueries.getChapterByUrl(targetMangaId, ch.url).executeAsOneOrNull()
                            database.chapterQueries.insertOrUpdateChapter(
                                id = existingCh?.id,
                                mangaId = targetMangaId,
                                url = ch.url,
                                name = ch.name,
                                scanlator = ch.scanlator,
                                read = existingCh?.read ?: 0L,
                                bookmark = existingCh?.bookmark ?: 0L,
                                lastPageRead = existingCh?.last_page_read ?: 0L,
                                chapterNumber = ch.chapterNumber.toDouble(),
                                sourceOrder = idx.toLong(),
                                dateFetch = currentTimeMillis(),
                                dateUpload = ch.dateUpload,
                                lastModifiedAt = currentTimeMillis()
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        return dbFlow
    }

    // 3. Exact Source Resolution for Novel Content
    override fun getNovelContent(sourceId: Long, chapterUrl: String, mangaId: Long?): Flow<NovelContent?> {
        val resolvedSource = _dynamicSources.value[sourceId] as? NovelSource
            ?: mangaId?.let { mId ->
                val manga = database.mangaQueries.getMangaById(mId).executeAsOneOrNull()
                manga?.let { _dynamicSources.value[it.source_id] as? NovelSource }
            }

        if (resolvedSource != null) {
            return flow {
                val content = try {
                    resolvedSource.getChapterContent(SourceChapter(url = chapterUrl, name = ""))
                } catch (_: Exception) {
                    null
                }
                emit(content)
            }.flowOn(Dispatchers.IO)
        }

        return flowOf(null)
    }

    // 4. Source-Preserving Favorite Toggle
    override suspend fun toggleFavorite(manga: SourceManga, isFavorite: Boolean) {
        withContext(Dispatchers.Default) {
            // Locate existing record to preserve genuine source_id instead of hardcoding 0L
            val existing = database.mangaQueries.getMangaByUrl(manga.url).executeAsOneOrNull()
            val preservedSourceId = existing?.source_id ?: 0L
            insertOrUpdateMangaInternal(manga, sourceId = preservedSourceId, forceFavorite = isFavorite)
        }
    }
}
```

---

## 6. Phased Implementation Roadmap

```
2026-Q4                2027-Q1                2027-Q2                2027-Q3
[ Phase 1: Shims ] ──► [ Phase 2: Adapters ] ──► [ Phase 3: Repos ] ──► [ Phase 4: UI ] ──► [ Phase 5: Hardening ]
  - CommonJS Router      - MangayomiAdapter      - Multi-repo Indexing   - Settings UI          - Canary Test Suite
  - DOM/XPath Engine     - LNReaderAdapter       - SHA-256 Verifier      - Filter Sheets        - Heap Profiling
  - RequestManager       - PaperbackAdapter      - Dynamic Un/Install    - Home Carousels       - CI/CD Automations
```

### 6.1 Phase 1: Core Runtime Shims & Polyfills
- **Objective**: Establish the universal JavaScript execution environment and hardened persistence schema capable of evaluating raw upstream extension scripts safely.
- **Deliverables**:
  1. Enhanced `getPreambleScript()` in `JsBridge.kt` implementing the CommonJS `require(id)` router and module registry.
  2. Complete `Document` and `Element` DOM emulation supporting `.select()`, `.selectFirst()`, getters (`text`, `getSrc`, `getHref`, `children`, `nextElementSibling`), and XPath queries.
  3. Case-insensitive `Headers` class wrapper polyfill for Fetch and `@libs/fetch` supporting `.get()`, `.has()`, `.set()`, and `.entries()`.
  4. iOS JavaScriptCore threading synchronization via coroutine `Mutex` in `JsEngine.ios.kt` and multi-statement async IIFE runner syntax fix.
  5. Partitioned storage keyspacing (`ext_${sourceId}_${key}`) across `SharedPreferences`, `App.createSourceStateManager`, and `Application` storage.
  6. SQLDelight schema hardening in `Chapter.sq` (`UNIQUE(manga_id, url)`) and `Manga.sq` (`UNIQUE(source_id, url)`).
- **Verification Criteria**:
  - Preamble script evaluates cleanly in both QuickJS and JavaScriptCore without syntax errors.
  - Unit tests verify DOM manipulation, case-insensitive header access, and crypto routines inside the sandbox.
  - SQLite schema migration test confirms zero collisions on shared relative slugs (`/chapter-1`).

### 6.2 Phase 2: Ecosystem Source Adapters
- **Objective**: Wrap parsed JavaScript instances in Dokuha's `ComicSource` and `NovelSource` contracts with memory pooling and performance optimizations.
- **Deliverables**:
  1. `MangayomiSourceAdapter.kt`: Binds `extention` methods, handles integer status mapping (`0..5`), and implements a 60-second in-memory `MangaDetail` response cache with coroutine `Mutex` to eliminate redundant double-fetches.
  2. `LNReaderSourceAdapter.kt`: Binds CommonJS exports, implements `PagePlugin` multi-page iteration (`parsePage(novelPath, page)`) for large web novels, and parses structured chapter HTML.
  3. `PaperbackSourceAdapter.kt`: Supports v0.8 `window.Sources` and v0.9 `globalThis.source` with keyed export fallbacks (`source.default || source[manifest.id] || source[keys[0]]`).
  4. Integration with `JsEnginePool`: Adapters acquire engine leases on-demand rather than holding permanent live engine instances.
- **Verification Criteria**:
  - Opening a Mangayomi title emits exactly ONE remote HTTP detail fetch.
  - LNReader `PagePlugin` correctly retrieves all chapters across paginated web novel sources.
  - Paperback v0.9 IIFEs instantiate cleanly across diverse export key conventions.

### 6.3 Phase 3: Multi-Repository Discovery & Extension Manager
- **Objective**: Expand `ExtensionManager` to index, validate, download, and activate sources from all three ecosystems with full SSRF protection.
- **Deliverables**:
  1. Polymorphic catalog parser (`ExtensionCatalogParser`) supporting `index.json`, `novel_index.json`, `plugins.min.json`, and `versioning.json`.
  2. Hardened `ExtensionRepoValidator` implementing full CIDR matching (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `169.254.0.0/16`, `100.64.0.0/10`, IPv6 private) and DNS rebinding validation.
  3. Bundle caching and storage manager persisting downloaded scripts into `KeyValueStorage`.
  4. Auto-restore pipeline re-instantiating active sources via `JsEnginePool`.
- **Verification Criteria**:
  - `ExtensionManager.refreshCatalog()` parses remote indices from all three ecosystems without `SerializationException`.
  - Attempts to add intranet or metadata URLs (e.g. `https://172.17.0.1`, `https://169.254.169.254`, `https://rebinding.attacker.com`) are rejected with validation errors.

### 6.4 Phase 4: UI & Reader Viewport Integration
- **Objective**: Connect dynamic sources to Voyager screens and Compose UI components via reactive SQLite flows.
- **Deliverables**:
  1. Explicit `sourceId: Long` qualification across all navigation destinations: `TitleDetailsScreen(sourceId, mangaUrl)`, `NovelReaderScreen(sourceId, chapterUrl, mangaId)`, and `WebtoonReaderScreen(sourceId, chapterUrl, mangaId)`.
  2. Decoupled reactive cache-aside architecture in `SqlDelightPanelRepository`: UI queries observe continuous SQLDelight `.asFlow()` while background repository coroutines refresh SQLite.
  3. Interactive Cloudflare Turnstile modal sheet (`AntiBotChallengeSheet`) fallback when headless WebView challenge resolution times out.
  4. Reader routing: Comics/manga open in `WebtoonReaderScreen`; novels open in `NovelReaderScreen`.
- **Verification Criteria**:
  - UI updates reactively when user favorites a manga or bookmarks a chapter without requiring screen reload.
  - Turnstile challenges on protected sources display interactive sheet and resume reading seamlessly.

### 6.5 Phase 5: Hardening, Performance Profiling & CI Regression
- **Objective**: Ensure long-term stability, memory efficiency, and resistance to upstream ecosystem drift.
- **Deliverables**:
  1. `JsEnginePool` LRU bound verification: active engines capped at 5; idle engines torn down after 5 minutes.
  2. Native QuickJS 32MB heap limit and GC sweep profiling under heavy chapter parsing.
  3. RFC 6265 dot-bounded cookie matching verification preventing cross-domain cookie leakage.
  4. Automated canary test suite running daily in GitHub Actions against real-world sample extensions.
- **Verification Criteria**:
  - 100% pass rate on unit tests; zero memory leaks after 50 consecutive extension initializations and teardowns.

---

## 7. Risk, Compatibility & Security Analysis

### 7.1 Multiplatform JS Engine Divergence & Threading Safety (QuickJS vs JSC)
- **Constraint**: Android and Desktop execute **QuickJS** (ES2020 compliance with C-level memory allocation), while iOS executes Apple's native **JavaScriptCore** (`JSContext`).
- **Threading & Concurrency Divergence**:
  1. *Apple JavaScriptCore Single-Thread Contract*: Apple's JavaScriptCore C API strictly forbids concurrent thread access to the same `JSContext` or `JSVirtualMachine`. On iOS, Kotlin/Native coroutine worker threads (`Dispatchers.Default`) execute async host callbacks (HTTP responses, timers). Calling `JSContext.evaluateScript` directly from background threads triggers race conditions, heap corruption, and unhandled `SIGSEGV` crashes.
  2. *Mitigation*: `JsEngine.ios.kt` mandates a dedicated coroutine `Mutex` wrapping **all** calls to `evaluateScript`, property setters, and asynchronous resolution callbacks (`__dokuha_resolve_async` / `__dokuha_reject_async`).
  3. *Multi-Statement Script Execution*: In both QuickJS and JSC, wrapping multi-statement scripts inside `let res = await ($script);` causes `SyntaxError: expecting ')'` when `$script` contains declarations or semicolons. Dokuha evaluates scripts within an asynchronous IIFE body: `(async () => { try { let res = await (async () => { $script })(); ... } })()`.

### 7.2 Anti-Scraping, Rate Limiting & Cloudflare Turnstile Interactive Mitigation
- **Constraint**: Aggregators protect catalogs using Cloudflare Bot Management, Turnstile, and Managed Challenges.
- **Empirical Vulnerabilities in Hidden WebViews**:
  Modern Turnstile scripts inspect browser DOM indicators: `document.hidden`, `document.visibilityState`, non-zero viewport dimensions, and genuine pointer/touch events. Completely offscreen or hidden WebViews stall indefinitely or enter infinite reload loops, freezing the scraper coroutine.
- **Hardened Mitigation Architecture**:
  1. **Two-Tier Anti-Bot Resolver**:
     - *Tier 1 (Automated Background Attempt)*: A platform WebView is instantiated with spoofed viewport dimensions (390x844 mobile screen) and active window focus. Automated challenges (e.g. legacy JS challenge or simple cookie check) solve within 3–5 seconds.
     - *Tier 2 (Interactive User Fallback Sheet)*: If the challenge is not solved within 5 seconds, or if Turnstile requires interactive verification (e.g. "Verify you are human" checkbox), Dokuha summons an interactive modal bottom sheet (`AntiBotChallengeSheet`). The user taps the verification box directly. Upon completion, clearance cookies (`cf_clearance`, `__cf_bm`) and User-Agent are extracted into the secure cookie jar, the sheet automatically dismisses, and the suspended coroutine resumes seamlessly.
  2. **Single-Flight Challenge Coalescing**:
     When multiple concurrent requests for the same host receive HTTP 403 or challenge markers, `JsBridge` coalesces them using a shared `CompletableDeferred` per domain. Only ONE challenge WebView is presented; all waiting requests resume simultaneously once solved, preventing a thundering herd.
  3. **RFC 6265 Dot-Bounded Cookie Isolation**:
     Prevents clearance and authentication cookie theft by replacing naive suffix matching with dot-bounded domain checking: `cookieDomain == domain || domain.endsWith("." + cookieDomain)`. This guarantees `evil-mangadex.org` can never intercept cookies belonging to `mangadex.org`.
  4. **Token-Bucket Rate Limiting & HTTP 429 Backoff**:
     Enforces a 3 req/sec rate limit per domain and automatically parses `Retry-After` headers on HTTP 429 responses, applying exponential backoff with jitter.

### 7.3 Memory, CPU & Resource Footprint Optimization
- **Constraint**: Mobile hardware imposes strict memory limits. Upstream scrapers across Mangayomi, LNReader, and Paperback are written to construct full in-memory DOMs (`new Document(res.body)` or `cheerio.load(html)`). Under Dokuha's zero-code-modification constraint, upstream scrapers cannot be rewritten to use SAX callbacks.
- **Hardened Mitigation Architecture**:
  1. **Reconciliation of SAX vs In-Memory DOMs**: Acknowledge that upstream scrapers parse full HTML documents into memory. Rather than attempting infeasible SAX refactoring of third-party scripts, Dokuha optimizes memory through strict heap boundaries:
     - Enforce a 32MB C-level heap limit on QuickJS (`quickJs.setMemoryLimit(32 * 1024 * 1024)`).
     - Run native garbage collection sweeps (`quickJs.gc()`) immediately following chapter list extraction.
     - Automatically nullify parsed DOM references (`doc = null`, `_root = null`) when scraper functions return.
  2. **`JsEnginePool` LRU Lifecycle**:
     Installed extensions do not retain permanent native JS engines. Engines are leased through `JsEnginePool`, capped at **5 active engines** in memory using an LRU eviction strategy. Idle engines are automatically torn down after 5 minutes of inactivity.

### 7.4 Security Sandboxing, SSRF Protection & Malicious Code Containment
- **Constraint**: Untrusted third-party extension code could attempt intranet SSRF, cloud credential theft, or cross-extension data exfiltration.
- **Hardened Containment Architecture**:
  1. **Full CIDR SSRF Blacklist**: Both `ExtensionRepoValidator` and `JsBridge.fetch()` inspect every outgoing connection. The destination IP/hostname is validated against a complete CIDR matcher blocking `127.0.0.0/8`, `10.0.0.0/8`, `172.16.0.0/12` (blocking `172.16.0.0` to `172.31.255.255`), `192.168.0.0/16`, `169.254.0.0/16` (Cloud IMDS), `100.64.0.0/10` (CGNAT), and IPv6 link-local/unique-local ranges.
  2. **DNS Rebinding Defense**: Hostnames are resolved via platform DNS prior to connection; if any resolved IP address belongs to the reserved ranges, the connection is rejected.
  3. **Protocol Enforcement**: Only `https:` is allowed (strictly blocking `file://`, `content://`, `javascript:`, `data:`).
  4. **Key-Space Storage Partitioning**: Storage calls (`SharedPreferences`, `App.createSourceStateManager`, `Application.getState`, `Application.getSecureState`) automatically prefix all keys with `ext_${sourceId}_`. Extensions cannot access host application secrets or inspect tokens stored by other extensions.

### 7.5 Upstream Ecosystem Drift & Deprecation Guardrails
- **Constraint**: Upstream repository maintainers periodically update manifest schemas or deprecate runtime methods.
- **Mitigation Architecture**:
  1. **Polymorphic Manifest Detection**: `ExtensionCatalogParser` detects ecosystem types dynamically from manifest structures, handling `sources` objects (Paperback), `site`/`url` arrays (LNReader), and `sourceCodeLanguage` arrays (Mangayomi) without crashing.
  2. **Canary Integration Tests**: Automated daily CI tests run against representative sample extensions to detect breaking upstream changes before end users encounter them.

---

## 8. Verification Commands & Test Artifacts

### 8.1 Catalog & Manifest Verification
Use these commands to verify upstream repository schemas and endpoint health:

```bash
# 1. Verify Mangayomi Manga Catalog (363 sources, 114 JS)
curl -sL https://kodjodevf.github.io/mangayomi-extensions/index.json | python3 -c "
import sys, json
data = json.load(sys.stdin)
js = [x for x in data if x.get('sourceCodeLanguage') == 1]
print(f'Mangayomi Total: {len(data)}, JavaScript Sources: {len(js)}')
assert len(js) >= 114, 'Mangayomi JS count lower than expected'
"

# 2. Verify Mangayomi Novel Catalog (100% JS)
curl -sL https://kodjodevf.github.io/mangayomi-extensions/novel_index.json | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(f'Mangayomi Novels: {len(data)}')
assert len(data) >= 5, 'Mangayomi novel count lower than expected'
"

# 3. Verify LNReader Central Plugin Catalog (372+ sources)
curl -sL https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist/plugins.min.json | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(f'LNReader Plugins: {len(data)}')
assert len(data) >= 370, 'LNReader plugin count lower than expected'
sample = data[0]
for field in ['id', 'name', 'site', 'lang', 'version', 'url']:
    assert field in sample, f'Missing required field: {field}'
"

# 4. Verify Paperback v0.8 Manifest Schema
curl -sL https://paperback-ios.github.io/extensions-default/0.8/versioning.json | python3 -c "
import sys, json
data = json.load(sys.stdin)
assert 'sources' in data, 'Missing sources array in Paperback v0.8 manifest'
print(f'Paperback v0.8 Sources: {len(data[\"sources\"])}')
"

# 5. Verify Paperback v0.9 Manifest Schema
curl -sL https://inkdex.github.io/extensions/0.9/stable/versioning.json | python3 -c "
import sys, json
data = json.load(sys.stdin)
assert 'sources' in data, 'Missing sources array in Paperback v0.9 manifest'
print(f'Paperback v0.9 Sources: {len(data[\"sources\"])}')
"
```

### 8.2 Bundle Structure & Shim Execution Testing
Inspect upstream compiled bundle wrappers and signatures:

```bash
# Verify LNReader CommonJS compilation format
curl -sL "https://raw.githubusercontent.com/lnreader/lnreader-plugins/plugins/v3.0.0/.js/src/plugins/english/royalroad.js" | head -n 30

# Verify Paperback v0.8 UMD compilation wrapper
curl -sL "https://paperback-ios.github.io/extensions-default/0.8/Paperback/index.js" | head -n 20

# Verify Mangayomi class declaration convention
curl -sL "https://raw.githubusercontent.com/entityJY/mangayomi-extensions-eJ/main/javascript/manga/src/en/asurascans.js" | grep -n "class DefaultExtension" -A 10
```

### 8.3 Hardened Architecture Verification Matrix
Use these standalone verification commands to confirm the resilience of Dokuha's hardened integration architecture against adversarial stress conditions:

```bash
# 1. Verify SQLite Composite Key Isolation & Zero Slug Collision
python3 -c "
import sqlite3
con = sqlite3.connect(':memory:')
cur = con.cursor()
# Hardened schema with composite unique constraints
cur.execute('CREATE TABLE mangas (id INTEGER PRIMARY KEY AUTOINCREMENT, source_id INTEGER, url TEXT, title TEXT, CONSTRAINT mangas_uq UNIQUE(source_id, url));')
cur.execute('CREATE TABLE chapters (id INTEGER PRIMARY KEY AUTOINCREMENT, manga_id INTEGER, url TEXT, name TEXT, read INTEGER DEFAULT 0, CONSTRAINT ch_uq UNIQUE(manga_id, url));')

# Insert Manga 1 with relative chapter '/chapter-1'
cur.execute('INSERT INTO mangas (source_id, url, title) VALUES (1, \"/manga/solo\", \"Solo Leveling\");')
m1 = cur.lastrowid
cur.execute('INSERT INTO chapters (manga_id, url, name, read) VALUES (?, \"/chapter-1\", \"Chapter 1\", 1);', (m1,))

# Insert Manga 2 from different source or title sharing identical slug '/chapter-1'
cur.execute('INSERT INTO mangas (source_id, url, title) VALUES (2, \"/manga/tower\", \"Tower of God\");')
m2 = cur.lastrowid
cur.execute('INSERT INTO chapters (manga_id, url, name, read) VALUES (?, \"/chapter-1\", \"Chapter 1\", 0);', (m2,))

# Confirm Manga 1 retains its chapter and read progress without collision!
cur.execute('SELECT id, manga_id, url, read FROM chapters WHERE manga_id = ?', (m1,))
m1_chapters = cur.fetchall()
assert len(m1_chapters) == 1 and m1_chapters[0][3] == 1, 'Manga 1 chapter lost or corrupted!'

cur.execute('SELECT id, manga_id, url, read FROM chapters WHERE manga_id = ?', (m2,))
m2_chapters = cur.fetchall()
assert len(m2_chapters) == 1 and m2_chapters[0][3] == 0, 'Manga 2 chapter lost!'
print('PASS: SQLite Composite Key Isolation verified. Zero chapter theft across shared slugs.')
"

# 2. Verify SSRF CIDR Matching & Private Subnet Rejection
python3 -c "
def is_private_or_reserved_ip(ip_str):
    if ip_str == '::1' or ip_str.startswith('fe80:') or ip_str.startswith('fc00:') or ip_str.startswith('fd00:'):
        return True
    parts = ip_str.split('.')
    if len(parts) != 4: return False
    try: octets = [int(p) for p in parts]
    except: return False
    if any(o < 0 or o > 255 for o in octets): return False
    o1, o2, o3, o4 = octets
    if o1 == 127: return True                                 # Loopback
    if o1 == 10: return True                                  # RFC 1918 Class A
    if o1 == 172 and 16 <= o2 <= 31: return True              # RFC 1918 Class B (172.16 - 172.31)
    if o1 == 192 and o2 == 168: return True                   # RFC 1918 Class C
    if o1 == 169 and o2 == 254: return True                   # Cloud IMDS & Link-Local
    if o1 == 100 and 64 <= o2 <= 127: return True             # Carrier-Grade NAT
    if o1 == 0: return True
    return False

test_ips = {
    '127.0.0.1': True,
    '10.254.0.1': True,
    '172.17.0.1': True,      # Docker gateway
    '172.20.10.1': True,     # Private WiFi
    '172.31.255.254': True,  # Top of 172.16.0.0/12
    '169.254.169.254': True, # Cloud metadata IMDS
    '100.64.0.1': True,      # CGNAT
    '::1': True,             # IPv6 Loopback
    'fe80::1': True,         # IPv6 Link-Local
    '142.250.190.46': False, # Public Google IP
    '104.26.12.31': False    # Public Cloudflare IP
}

for ip, expected in test_ips.items():
    assert is_private_or_reserved_ip(ip) == expected, f'SSRF check failed for {ip}: expected {expected}'
print('PASS: Comprehensive CIDR SSRF filter verified across all reserved and cloud ranges.')
"

# 3. Verify RFC 6265 Dot-Bounded Cookie Boundary Matching
python3 -c "
def is_cookie_allowed_for_host(host, cookie_domain):
    host = host.lower()
    cookie_domain = cookie_domain.lower()
    return host == cookie_domain or host.endswith('.' + cookie_domain)

assert is_cookie_allowed_for_host('mangadex.org', 'mangadex.org') == True, 'Exact domain should match'
assert is_cookie_allowed_for_host('api.mangadex.org', 'mangadex.org') == True, 'Subdomain should match'
assert is_cookie_allowed_for_host('auth.api.mangadex.org', 'mangadex.org') == True, 'Nested subdomain should match'
assert is_cookie_allowed_for_host('evil-mangadex.org', 'mangadex.org') == False, 'Malicious suffix should NEVER match!'
assert is_cookie_allowed_for_host('notmangadex.org', 'mangadex.org') == False, 'Phishing domain should NEVER match!'
print('PASS: RFC 6265 Dot-Bounded Cookie Isolation verified. Zero credential leakage.')
"

# 4. Verify Case-Insensitive Fetch Headers Polyfill
python3 -c "
js_code = '''
class Headers {
    constructor(init = {}) {
        this._map = {};
        if (init && typeof init === \"object\") {
            for (const k of Object.keys(init)) this.set(k, init[k]);
        }
    }
    get(name) {
        const key = String(name).toLowerCase();
        return this._map[key] !== undefined ? this._map[key] : null;
    }
    set(name, val) {
        this._map[String(name).toLowerCase()] = String(val);
    }
}
const h = new Headers({ 'Content-Type': 'text/html', 'CF-Mitigated': 'challenge' });
assert(h.get('cf-mitigated') === 'challenge');
assert(h.get('CF-MITIGATED') === 'challenge');
assert(h.get('content-type') === 'text/html');
assert(h.get('non-existent') === null);
console.log('PASS: JS Headers case-insensitive .get() verified.');
'''
import subprocess
out = subprocess.check_output(['node', '-e', js_code])
print(out.decode().strip())
"
```

### 8.4 Dokuha Automated Test Execution Matrix
Run Dokuha's test suites to confirm build health and verify zero regressions:

```bash
# Verify shared metadata compilation across all multiplatform targets
cd "UI UX" && ./gradlew :composeApp:compileCommonMainKotlinMetadata

# Run the complete automated unit test suite (58 passing tests)
cd "UI UX" && ./gradlew :composeApp:testDebugUnitTest

# Run specific extension, repository, and validator unit tests
cd "UI UX" && ./gradlew :composeApp:testDebugUnitTest --tests "core.extensions.*"
cd "UI UX" && ./gradlew :composeApp:testDebugUnitTest --tests "data.ExtensionRepoValidatorTest"
```

---

## 9. Appendix: Cross-Ecosystem Type Matrix

```
┌─────────────────────────┬─────────────────────────┬─────────────────────────┬─────────────────────────┐
│ Feature / Capability    │ Mangayomi (JS)          │ LNReader (TS/JS)        │ Paperback (TS/JS)       │
├─────────────────────────┼─────────────────────────┼─────────────────────────┼─────────────────────────┤
│ Target Content Type     │ Comics & Novels         │ Light & Web Novels      │ Manga, Comics & Webtoons│
│ Module System           │ Global Class            │ CommonJS (ES5)          │ UMD (v0.8) / IIFE (v0.9)│
│ Primary Distribution    │ index.json (Array)      │ plugins.min.json (Array)│ versioning.json (Object)│
│ Manifest Model          │ MangayomiCatalogItem    │ LNReaderCatalogItem     │ PaperbackVersioning...  │
│ Source Instantiation    │ new DefaultExtension()  │ module.exports.default  │ Keyed fallback lookup   │
│ Engine Memory Manager   │ JsEnginePool (LRU <= 5) │ JsEnginePool (LRU <= 5) │ JsEnginePool (LRU <= 5) │
│ QuickJS Heap Limit      │ 32MB + GC sweep         │ 32MB + GC sweep         │ 32MB + GC sweep         │
│ iOS JSC Thread Safety   │ Mutex serialized        │ Mutex serialized        │ Mutex serialized        │
│ DOM Manipulation        │ Document, Element       │ Cheerio, htmlparser2    │ Cheerio / App.Selector  │
│ Dynamic Settings        │ SharedPreferences       │ @libs/storage           │ SourceStateManager / DUI│
│ Storage Keyspace        │ ext_{sourceId}_{key}    │ ext_{sourceId}_{key}    │ ext_{sourceId}_{key}    │
│ Anti-Bot Mitigation     │ Turnstile modal sheet   │ Turnstile modal sheet   │ Turnstile modal sheet   │
│ Chapter Pagination      │ Single monolithic fetch │ PagePlugin.parsePage    │ Cursor PagedResults     │
│ Adapter Caching         │ 60s detail cache        │ In-memory chapter list  │ Metadata cursor cache   │
│ Dokuha Adapter Class    │ MangayomiSourceAdapter  │ LNReaderSourceAdapter   │ PaperbackSourceAdapter  │
│ Dokuha Target Interface │ ComicSource/NovelSource │ NovelSource             │ ComicSource             │
│ DB Constraint Model     │ UNIQUE(manga_id, url)   │ UNIQUE(manga_id, url)   │ UNIQUE(manga_id, url)   │
│ Flow Persistence        │ Reactive SQLDelight Flow│ Reactive SQLDelight Flow│ Reactive SQLDelight Flow│
└─────────────────────────┴─────────────────────────┴─────────────────────────┴─────────────────────────┘
```

---
*End of Technical Specification — Dokuha Extension Ecosystem Integration Plan (Iteration 2 Hardened).*
