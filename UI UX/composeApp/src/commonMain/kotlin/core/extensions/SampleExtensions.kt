package core.extensions

import core.extensions.js.ExtensionCatalogItem
import core.extensions.js.ExtensionType
import core.extensions.model.ExtensionEcosystem

/**
 * Built-in reference extensions used for demonstration, out-of-the-box readiness,
 * and verification of the JavaScript Extension Runner across Android, iOS, and Desktop.
 */
object SampleExtensions {

    val ASURA_SCANS_BUNDLE = """
        const manifest = {
            id: "asurascans",
            name: "Asura Scans",
            version: "1.2.0",
            lang: "en",
            baseUrl: "https://asurascans.com",
            type: "COMIC",
            isNsfw: false
        };

        const URL_RANDOM_PART = /-[a-z0-9]{8}$/i;
        const OLD_FORMAT_MANGA_REGEX = /^\/manga\/(?:\d+-)?([^/]+)\/?$/;
        const PAGE_TOKEN_REGEX = /pageToken\*=\*"([^"]+)"/;
        const PER_PAGE_LIMIT = 20;

        const slugMap = new Map();

        function cleanSlug(url) {
            if (!url) return "";
            let clean = url.trim();
            if (clean.includes("/series/")) {
                clean = clean.split("/series/")[1];
            } else if (clean.includes("/comics/")) {
                clean = clean.split("/comics/")[1];
            } else if (clean.includes("/manga/")) {
                const match = clean.match(OLD_FORMAT_MANGA_REGEX);
                clean = match ? match[1] : clean.split("/manga/")[1];
            }
            return clean.split("/")[0].split("?")[0].replace(/\/+$/, "");
        }

        function getCachedSlug(slug) {
            if (!slug) return slug;
            return slugMap.get(slug) || slugMap.get(slug.replace(URL_RANDOM_PART, "")) || slug;
        }

        function setCachedSlug(requestedSlug, canonicalSlug) {
            if (!requestedSlug || !canonicalSlug) return;
            const cleanReq = cleanSlug(requestedSlug);
            const cleanCanon = cleanSlug(canonicalSlug);
            if (cleanReq && cleanCanon) {
                slugMap.set(cleanReq, cleanCanon);
                const baseSlug = cleanReq.replace(URL_RANDOM_PART, "");
                if (baseSlug) slugMap.set(baseSlug, cleanCanon);
            }
        }

        function unwrapAstro(val) {
            if (Array.isArray(val)) {
                if (val.length === 0) return null;
                if (val.length === 1 && typeof val[0] === 'number') return null;
                if (val.length === 2 && typeof val[0] === 'number') {
                    return unwrapAstro(val[1]);
                }
                return val.map(unwrapAstro);
            }
            if (val !== null && typeof val === 'object') {
                const obj = {};
                for (const k of Object.keys(val)) {
                    obj[k] = unwrapAstro(val[k]);
                }
                return obj;
            }
            return val;
        }

        function decodeHtmlEntities(str) {
            if (!str) return "";
            return str
                .replace(/&quot;/g, '"')
                .replace(/&amp;/g, '&')
                .replace(/&#39;/g, "'")
                .replace(/&lt;/g, '<')
                .replace(/&gt;/g, '>');
        }

        function extractAstroData(html, key) {
            if (!html) return null;
            try {
                if (typeof dokuha !== 'undefined' && typeof dokuha.parseHtml === 'function') {
                    const $ = dokuha.parseHtml(html);
                    const island = $(`[props*="${'$'}{key}"]`).first();
                    const raw = island.attr("props");
                    if (raw) {
                        return unwrapAstro(JSON.parse(decodeHtmlEntities(raw)));
                    }
                } else if (typeof Document !== 'undefined') {
                    const doc = new Document(html);
                    const el = doc.selectFirst(`[props*="${'$'}{key}"]`);
                    if (el) {
                        const raw = el.attr("props");
                        if (raw) return unwrapAstro(JSON.parse(decodeHtmlEntities(raw)));
                    }
                }
            } catch (_) {}

            try {
                const regex = new RegExp(`<astro-island[^>]*props="([^"]*${'$'}{key}[^"]*)"`, 'i');
                const match = html.match(regex);
                if (match && match[1]) {
                    const decoded = decodeHtmlEntities(match[1]);
                    return unwrapAstro(JSON.parse(decoded));
                }
            } catch (_) {}

            return null;
        }

        function formatBookmarks(count) {
            if (typeof count !== 'number') return null;
            if (count >= 1000000) return (count / 1000000).toFixed(1) + "M";
            if (count >= 1000) return (count / 1000).toFixed(1) + "K";
            return String(count);
        }

        function parseDescription(data) {
            const parts = [];
            const meta = [];
            if (data.popularityRank) meta.push(`Rank: #${'$'}{data.popularityRank}`);
            if (typeof data.rating === 'number') meta.push(`Rating: ${'$'}{data.rating.toFixed(2)}`);
            if (typeof data.bookmarkCount === 'number') {
                const formatted = formatBookmarks(data.bookmarkCount);
                if (formatted) meta.push(`Bookmarks: ${'$'}{formatted}`);
            }
            if (meta.length > 0) parts.push(meta.join(" • "));

            if (data.description) {
                const clean = decodeHtmlEntities(data.description)
                    .replace(/<[^>]*>/g, "")
                    .trim();
                if (clean) parts.push(clean);
            }

            if (data.alternativeTitles) {
                const rawAlt = data.alternativeTitles.includes("•")
                    ? data.alternativeTitles.split("•")
                    : data.alternativeTitles.split(",");
                const altList = rawAlt.map(s => s.trim()).filter(Boolean);
                if (altList.length > 0) {
                    parts.push("Alternative Titles:\n" + altList.map(t => `- ${'$'}{t}`).join("\n"));
                }
            }

            return parts.join("\n\n");
        }

        function buildGenres(data) {
            const list = [];
            if (data.type) {
                const capType = data.type.charAt(0).toUpperCase() + data.type.slice(1);
                list.push(capType);
            }
            if (Array.isArray(data.genres)) {
                data.genres.forEach(g => {
                    const name = typeof g === 'object' && g !== null ? (g.name || g.slug) : String(g);
                    if (name) list.push(name);
                });
            }
            return list;
        }

        function mapStatus(statusStr) {
            if (!statusStr) return "UNKNOWN";
            const s = String(statusStr).toLowerCase();
            if (s === "ongoing") return "ONGOING";
            if (s === "completed") return "COMPLETED";
            if (s === "hiatus") return "ON_HIATUS";
            if (s === "dropped" || s === "axed" || s === "cancel" || s === "cancelled") return "CANCELLED";
            return "UNKNOWN";
        }

        function formatChapterNumber(num) {
            const parsed = parseFloat(num);
            if (isNaN(parsed)) return "0";
            return Number.isInteger(parsed) ? String(parsed) : String(parsed);
        }

        const extension = {
            baseUrl: "https://asurascans.com",
            apiUrl: "https://api.asurascans.com/api",
            _lastRequestTime: 0,

            _headers(customHeaders = {}) {
                return {
                    "Referer": "https://asurascans.com/",
                    "Origin": "https://asurascans.com",
                    "Accept": "application/json, text/plain, */*",
                    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                    ...customHeaders
                };
            },

            async _throttle(url) {
                if (url && (url.includes("/covers/") || url.includes("/_astro/") || url.endsWith(".webp") || url.endsWith(".jpg") || url.endsWith(".png"))) {
                    return;
                }
                const now = Date.now();
                const elapsed = now - (this._lastRequestTime || 0);
                const minInterval = 50;
                if (elapsed < minInterval) {
                    const delay = minInterval - elapsed;
                    this._lastRequestTime = now + delay;
                    if (typeof dokuha !== 'undefined' && typeof dokuha.sleep === 'function') {
                        await dokuha.sleep(delay);
                    } else if (typeof __dokuha_sleep === 'function') {
                        await __dokuha_sleep(delay);
                    } else if (typeof setTimeout === 'function') {
                        await new Promise(resolve => setTimeout(resolve, delay));
                    }
                } else {
                    this._lastRequestTime = now;
                }
            },

            async _fetch(url, options = {}) {
                await this._throttle(url);
                const headers = this._headers(options.headers || {});
                if (typeof dokuha !== 'undefined' && typeof dokuha.fetch === 'function') {
                    return await dokuha.fetch(url, { ...options, headers });
                }
                if (typeof fetch === 'function') {
                    return await fetch(url, { ...options, headers });
                }
                const res = await new Client().get(url, headers);
                return {
                    ok: res.statusCode >= 200 && res.statusCode < 300,
                    status: res.statusCode,
                    async json() { return typeof res.body === 'string' ? JSON.parse(res.body) : res.body; },
                    async text() { return res.body || ''; }
                };
            },

            async getPopular(page = 1) {
                const limit = PER_PAGE_LIMIT;
                const offset = Math.max(0, (page - 1) * limit);
                const url = `${'$'}{this.apiUrl}/series?offset=${'$'}{offset}&limit=${'$'}{limit}&order=popular`;

                const res = await this._fetch(url);
                if (!res.ok) return { mangas: [], hasNextPage: false };

                const data = await res.json();
                const items = (data && data.data) || [];
                const meta = (data && data.meta) || {};

                const mangas = items.map(item => {
                    const publicUrl = item.public_url || `/comics/${'$'}{item.slug}`;
                    if (item.slug && item.public_url) {
                        setCachedSlug(item.slug, item.public_url);
                    }
                    return {
                        url: publicUrl,
                        title: item.title || "Untitled",
                        thumbnailUrl: item.coverUrl || item.cover || "",
                        status: "UNKNOWN",
                        genres: [],
                        isNovel: false
                    };
                });

                const hasNext = Boolean(meta.has_more ?? (items.length >= limit));
                return { mangas, hasNextPage: hasNext };
            },

            async getLatestUpdates(page = 1) {
                return this.getPopular(page);
            },

            async search(query, page = 1) {
                const pop = await this.getPopular(page);
                const q = (query || "").toLowerCase();
                const matched = pop.mangas.filter(m => m.title.toLowerCase().includes(q));
                return { mangas: matched, hasNextPage: false };
            },

            async getDetails(url) {
                const clean = cleanSlug(url);
                const resolvedSlug = getCachedSlug(clean);
                let targetUrl = url.startsWith("http")
                    ? url
                    : `${'$'}{this.baseUrl}/comics/${'$'}{resolvedSlug}`;

                let res = await this._fetch(targetUrl);

                if (!res.ok && URL_RANDOM_PART.test(targetUrl)) {
                    const fallbackUrl = targetUrl.replace(URL_RANDOM_PART, "");
                    const retryRes = await this._fetch(fallbackUrl);
                    if (retryRes.ok) {
                        res = retryRes;
                        targetUrl = fallbackUrl;
                    }
                }

                if (!res.ok) {
                    return {
                        url: url,
                        title: "Unknown",
                        thumbnailUrl: "",
                        description: "",
                        author: "",
                        artist: "",
                        genres: [],
                        status: "UNKNOWN",
                        isNovel: false
                    };
                }

                const html = await res.text();
                const data = extractAstroData(html, "title");
                const publicUrlData = extractAstroData(html, "publicUrl");

                let canonicalUrl = targetUrl;
                const extractedPublicUrl = (publicUrlData && (publicUrlData.publicUrl || publicUrlData.public_url)) ||
                    (data && (data.publicUrl || data.public_url));

                if (extractedPublicUrl) {
                    canonicalUrl = extractedPublicUrl.startsWith("http")
                        ? extractedPublicUrl
                        : `${'$'}{this.baseUrl}${'$'}{extractedPublicUrl.startsWith("/") ? "" : "/"}${'$'}{extractedPublicUrl}`;
                    const canonicalSlug = cleanSlug(canonicalUrl);
                    if (canonicalSlug) {
                        setCachedSlug(clean, canonicalSlug);
                    }
                } else if (URL_RANDOM_PART.test(clean)) {
                    setCachedSlug(clean, cleanSlug(targetUrl));
                }

                const returnUrl = canonicalUrl.startsWith("http")
                    ? canonicalUrl.replace(this.baseUrl, "")
                    : canonicalUrl;

                if (data) {
                    const genres = buildGenres(data);
                    const desc = parseDescription(data);

                    return {
                        url: returnUrl.startsWith("/") ? returnUrl : `/${'$'}{returnUrl}`,
                        title: data.title || "Untitled",
                        thumbnailUrl: data.coverUrl || data.cover || "",
                        author: data.author || "",
                        artist: data.artist || "",
                        description: desc,
                        genres: genres,
                        status: mapStatus(data.status),
                        isNovel: false
                    };
                }

                return {
                    url: returnUrl.startsWith("/") ? returnUrl : `/${'$'}{returnUrl}`,
                    title: "Unknown",
                    thumbnailUrl: "",
                    description: "",
                    author: "",
                    artist: "",
                    genres: [],
                    status: "UNKNOWN",
                    isNovel: false
                };
            },

            async getChapters(url, options = {}) {
                const clean = cleanSlug(url);
                const resolvedSlug = getCachedSlug(clean);
                let targetUrl = url.startsWith("http")
                    ? url
                    : `${'$'}{this.baseUrl}/comics/${'$'}{resolvedSlug}`;

                let res = await this._fetch(targetUrl);

                if (!res.ok && URL_RANDOM_PART.test(targetUrl)) {
                    const fallbackUrl = targetUrl.replace(URL_RANDOM_PART, "");
                    const retryRes = await this._fetch(fallbackUrl);
                    if (retryRes.ok) {
                        res = retryRes;
                        targetUrl = fallbackUrl;
                    }
                }

                if (!res.ok) return [];

                const html = await res.text();
                const data = extractAstroData(html, "chapters");
                const rawChapters = (data && data.chapters) ? data.chapters : [];

                const publicUrlData = extractAstroData(html, "publicUrl");
                const extractedPublicUrl = (publicUrlData && (publicUrlData.publicUrl || publicUrlData.public_url)) ||
                    (data && (data.publicUrl || data.public_url));
                if (extractedPublicUrl) {
                    setCachedSlug(clean, cleanSlug(extractedPublicUrl));
                }

                const currentSlug = getCachedSlug(clean) || clean;
                const now = Date.now();
                const hidePremium = Boolean(options && (options.hidePremium || options.pref_hide_premium_chapters));

                const chapters = [];
                for (const ch of rawChapters) {
                    const isPremium = Boolean(ch.is_premium || ch.is_locked);
                    const isEarlyAccess = Boolean(ch.early_access_until && new Date(ch.early_access_until).getTime() > now);
                    const isLocked = isPremium || isEarlyAccess;

                    if (hidePremium && isLocked) {
                        continue;
                    }

                    const numFloat = parseFloat(ch.number) || 0;
                    const numStr = formatChapterNumber(numFloat);
                    const chapterSlug = ch.series_slug || currentSlug;
                    const chapterUrl = `/comics/${'$'}{chapterSlug}/chapter/${'$'}{numStr}`;

                    let name = "";
                    if (isLocked) name += "[Locked] ";
                    name += `Chapter ${'$'}{numStr}`;
                    if (ch.title && ch.title.trim().length > 0) {
                        name += `: ${'$'}{ch.title.trim()}`;
                    }

                    const dateUpload = ch.created_at ? (new Date(ch.created_at).getTime() || 0) : 0;

                    chapters.push({
                        url: chapterUrl,
                        name: name,
                        chapterNumber: numFloat,
                        dateUpload: dateUpload,
                        scanlator: isLocked ? "[Early Access]" : "Asura Scans",
                        isLocked: isLocked
                    });
                }

                return chapters;
            },

            async getPages(chapterUrl) {
                let fullUrl = chapterUrl.startsWith("http") ? chapterUrl : `${'$'}{this.baseUrl}${'$'}{chapterUrl.startsWith("/") ? "" : "/"}${'$'}{chapterUrl}`;
                let res = await this._fetch(fullUrl);

                if (!res.ok && URL_RANDOM_PART.test(fullUrl)) {
                    const fallbackUrl = fullUrl.replace(URL_RANDOM_PART, "");
                    const retryRes = await this._fetch(fallbackUrl);
                    if (retryRes.ok) {
                        res = retryRes;
                        fullUrl = fallbackUrl;
                    }
                }

                if (!res.ok) return [];

                const html = await res.text();
                const data = extractAstroData(html, "pages");
                let pages = (data && data.pages) ? data.pages : [];

                if (pages.length === 0) {
                    try {
                        const pageTokenMatch = html.match(PAGE_TOKEN_REGEX) || html.match(/"pageToken":\s*"([^"]+)"/);
                        const pageToken = pageTokenMatch ? pageTokenMatch[1] : "asura-reader-2026";

                        const pathSegments = fullUrl.replace(this.baseUrl, "").split("/").filter(Boolean);
                        const comicsIdx = pathSegments.indexOf("comics");
                        const mangaSlug = comicsIdx !== -1 && pathSegments[comicsIdx + 1] ? pathSegments[comicsIdx + 1] : "";
                        const chapterIdx = pathSegments.indexOf("chapter");
                        const chapterNumber = chapterIdx !== -1 && pathSegments[chapterIdx + 1] ? pathSegments[chapterIdx + 1] : "";

                        if (mangaSlug && chapterNumber) {
                            const apiUrl = `${'$'}{this.apiUrl}/series/${'$'}{mangaSlug}/chapters/${'$'}{chapterNumber}`;
                            const headers = { "X-Page-Token": pageToken };

                            let accessToken = "";
                            try {
                                if (typeof dokuha !== 'undefined' && typeof dokuha.getCookies === 'function') {
                                    const cookies = dokuha.getCookies(this.baseUrl) || "";
                                    const m = cookies.match(/access_token=([^;]+)/);
                                    if (m) accessToken = m[1];
                                }
                                if (!accessToken && typeof dokuha !== 'undefined' && dokuha.storage && typeof dokuha.storage.getString === 'function') {
                                    accessToken = dokuha.storage.getString("access_token", "");
                                }
                            } catch (_) {}

                            if (accessToken) {
                                headers["Authorization"] = `Bearer ${'$'}{accessToken}`;
                            }

                            const apiRes = await this._fetch(apiUrl, { headers });
                            if (apiRes.ok) {
                                const apiJson = await apiRes.json();
                                if (apiJson && apiJson.data && apiJson.data.chapter && Array.isArray(apiJson.data.chapter.pages)) {
                                    pages = apiJson.data.chapter.pages;
                                }
                            }
                        }
                    } catch (_) {}
                }

                if (pages.length === 0) {
                    const match = html.match(/"pages":\s*(\[[^\]]+\])/);
                    if (match && match[1]) {
                        try {
                            pages = JSON.parse(match[1]);
                        } catch (_) {}
                    }
                }

                return pages.map((p, idx) => {
                    const rawImg = typeof p === "string" ? p : (p.url || p.imageUrl || "");
                    let finalUrl = rawImg;
                    const hasTiles = p && Array.isArray(p.tiles) && p.tiles.length > 0;
                    let tileData = null;
                    if (hasTiles) {
                        tileData = {
                            tiles: p.tiles,
                            tileCols: p.tile_cols || p.tileCols || 4,
                            tileRows: p.tile_rows || p.tileRows || 5
                        };
                        finalUrl = `${'$'}{rawImg}#${'$'}{encodeURIComponent(JSON.stringify(tileData))}`;
                    }
                    const item = {
                        index: idx,
                        url: finalUrl,
                        imageUrl: finalUrl
                    };
                    if (tileData) {
                        item.tiles = tileData.tiles;
                        item.tileCols = tileData.tileCols;
                        item.tileRows = tileData.tileRows;
                    }
                    return item;
                });
            }
        };
    """.trimIndent()

    val MANGADEX_BUNDLE = """
        const manifest = {
            id: "mangadex",
            name: "MangaDex",
            version: "1.0.2",
            lang: "en",
            baseUrl: "https://mangadex.org",
            type: "COMIC",
            iconUrl: "https://mangadex.org/favicon.ico",
            isNsfw: false
        };

        const extension = {
            baseUrl: "https://mangadex.org",
            apiUrl: "https://api.mangadex.org",
            coverBaseUrl: "https://uploads.mangadex.org/covers",

            _headers() {
                return {
                    "User-Agent": "Dokuha/1.0.0",
                    "Accept": "application/json"
                };
            },

            _extractCover(relationships, mangaId) {
                if (!relationships || !Array.isArray(relationships)) return null;
                const coverRel = relationships.find(r => r.type === "cover_art");
                if (coverRel && coverRel.attributes && coverRel.attributes.fileName) {
                    return `${'$'}{this.coverBaseUrl}/${'$'}{mangaId}/${'$'}{coverRel.attributes.fileName}.256.jpg`;
                }
                return null;
            },

            _extractTitle(attributes) {
                if (!attributes || !attributes.title) return "Untitled";
                const titleObj = attributes.title;
                return titleObj.en || titleObj["ja-ro"] || titleObj["ko-ro"] || titleObj["zh-ro"] || Object.values(titleObj)[0] || "Untitled";
            },

            _extractDescription(attributes) {
                if (!attributes || !attributes.description) return "";
                const descObj = attributes.description;
                return descObj.en || Object.values(descObj)[0] || "";
            },

            _extractAuthor(relationships, type) {
                if (!relationships || !Array.isArray(relationships)) return null;
                const rel = relationships.find(r => r.type === type);
                return (rel && rel.attributes && rel.attributes.name) ? rel.attributes.name : null;
            },

            _extractGenres(attributes) {
                if (!attributes || !attributes.tags) return [];
                return attributes.tags
                    .filter(t => t.attributes && t.attributes.name)
                    .map(t => t.attributes.name.en || Object.values(t.attributes.name)[0]);
            },

            _mapManga(item) {
                const id = item.id;
                const attr = item.attributes || {};
                const coverUrl = this._extractCover(item.relationships, id);
                const title = this._extractTitle(attr);
                const author = this._extractAuthor(item.relationships, "author");
                const artist = this._extractAuthor(item.relationships, "artist");
                const description = this._extractDescription(attr);
                const genres = this._extractGenres(attr);

                let status = "UNKNOWN";
                if (attr.status === "ongoing") status = "ONGOING";
                else if (attr.status === "completed") status = "COMPLETED";
                else if (attr.status === "hiatus") status = "ON_HIATUS";
                else if (attr.status === "cancelled") status = "CANCELLED";

                return {
                    url: id,
                    title: title,
                    thumbnailUrl: coverUrl,
                    author: author,
                    artist: artist,
                    description: description,
                    genres: genres,
                    status: status,
                    isNovel: false
                };
            },

            async getPopular(page = 1) {
                const limit = 20;
                const offset = Math.max(0, (page - 1) * limit);
                const url = `${'$'}{this.apiUrl}/manga?limit=${'$'}{limit}&offset=${'$'}{offset}&includes[]=cover_art&includes[]=author&includes[]=artist&order[followedCount]=desc&contentRating[]=safe&contentRating[]=suggestive`;

                const res = await (typeof dokuha !== 'undefined' ? dokuha.fetch(url, { headers: this._headers() }) : fetch(url, { headers: this._headers() }));
                const data = await res.json();
                const mangas = (data.data || []).map(item => this._mapManga(item));
                const total = data.total || 0;
                const hasNext = offset + limit < total;
                return { mangas: mangas, hasNextPage: hasNext };
            },

            async getLatestUpdates(page = 1) {
                return this.getPopular(page);
            },

            async search(query, page = 1) {
                const limit = 20;
                const offset = Math.max(0, (page - 1) * limit);
                let url = `${'$'}{this.apiUrl}/manga?limit=${'$'}{limit}&offset=${'$'}{offset}&includes[]=cover_art&includes[]=author&includes[]=artist&contentRating[]=safe&contentRating[]=suggestive`;
                if (query && query.trim().length > 0) {
                    url += `&title=${'$'}{encodeURIComponent(query.trim())}`;
                } else {
                    url += `&order[followedCount]=desc`;
                }

                const res = await (typeof dokuha !== 'undefined' ? dokuha.fetch(url, { headers: this._headers() }) : fetch(url, { headers: this._headers() }));
                const data = await res.json();
                const mangas = (data.data || []).map(item => this._mapManga(item));
                const total = data.total || 0;
                const hasNext = offset + limit < total;
                return { mangas: mangas, hasNextPage: hasNext };
            },

            async getDetails(url) {
                const id = url.replace("https://mangadex.org/title/", "").replace("https://api.mangadex.org/manga/", "").split("/")[0];
                const fetchUrl = `${'$'}{this.apiUrl}/manga/${'$'}{id}?includes[]=cover_art&includes[]=author&includes[]=artist`;

                const res = await (typeof dokuha !== 'undefined' ? dokuha.fetch(fetchUrl, { headers: this._headers() }) : fetch(fetchUrl, { headers: this._headers() }));
                const json = await res.json();
                if (json.data) {
                    return this._mapManga(json.data);
                }
                return { url: id, title: "Unknown", isNovel: false };
            },

            async getChapters(url) {
                const id = url.replace("https://mangadex.org/title/", "").replace("https://api.mangadex.org/manga/", "").split("/")[0];
                const ratings = "&contentRating[]=safe&contentRating[]=suggestive&contentRating[]=erotica&contentRating[]=pornographic";
                const fetchUrlEn = `${'$'}{this.apiUrl}/manga/${'$'}{id}/feed?limit=500&translatedLanguage[]=en&order[chapter]=desc${'$'}{ratings}`;

                const resEn = await (typeof dokuha !== 'undefined' ? dokuha.fetch(fetchUrlEn, { headers: this._headers() }) : fetch(fetchUrlEn, { headers: this._headers() }));
                const jsonEn = await resEn.json();
                let chaptersData = jsonEn.data || [];

                const hasHostedEn = chaptersData.some(ch => !ch.attributes || !ch.attributes.externalUrl);
                if (!hasHostedEn) {
                    const fetchUrlAll = `${'$'}{this.apiUrl}/manga/${'$'}{id}/feed?limit=500&order[chapter]=desc${'$'}{ratings}`;
                    const resAll = await (typeof dokuha !== 'undefined' ? dokuha.fetch(fetchUrlAll, { headers: this._headers() }) : fetch(fetchUrlAll, { headers: this._headers() }));
                    const jsonAll = await resAll.json();
                    const allData = jsonAll.data || [];

                    const existingIds = {};
                    for (let i = 0; i < chaptersData.length; i++) {
                        existingIds[chaptersData[i].id] = true;
                    }
                    for (let i = 0; i < allData.length; i++) {
                        if (!existingIds[allData[i].id]) {
                            chaptersData.push(allData[i]);
                            existingIds[allData[i].id] = true;
                        }
                    }
                }

                return chaptersData.map(ch => {
                    const attr = ch.attributes || {};
                    const chNum = parseFloat(attr.chapter) || 0;
                    const extUrl = attr.externalUrl;
                    const lang = attr.translatedLanguage;
                    const title = attr.title ? ` - ${'$'}{attr.title}` : "";

                    let prefix = "";
                    if (lang && lang !== "en") {
                        prefix = `[${'$'}{lang.toUpperCase()}] `;
                    }
                    if (extUrl) {
                        try {
                            const host = extUrl.split("/")[2].replace("www.", "");
                            prefix = `[${'$'}{host}] ` + prefix;
                        } catch (_) {
                            prefix = `[External] ` + prefix;
                        }
                    }

                    const name = attr.chapter ? `${'$'}{prefix}Chapter ${'$'}{attr.chapter}${'$'}{title}` : (attr.title ? `${'$'}{prefix}${'$'}{attr.title}` : `${'$'}{prefix}Oneshot`);
                    const dateUpload = attr.publishAt ? new Date(attr.publishAt).getTime() : 0;

                    return {
                        url: extUrl || ch.id,
                        name: name,
                        chapterNumber: chNum,
                        dateUpload: dateUpload,
                        scanlator: null
                    };
                });
            },

            async getPages(chapterUrl) {
                if (chapterUrl.startsWith("http://") || chapterUrl.startsWith("https://")) {
                    return [];
                }

                const id = chapterUrl.split("/").pop();
                const fetchUrl = `${'$'}{this.apiUrl}/at-home/server/${'$'}{id}`;

                const res = await (typeof dokuha !== 'undefined' ? dokuha.fetch(fetchUrl, { headers: this._headers() }) : fetch(fetchUrl, { headers: this._headers() }));
                const json = await res.json();

                if (json.result !== "ok" || !json.baseUrl || !json.chapter) {
                    return [];
                }

                const baseUrl = json.baseUrl;
                const hash = json.chapter.hash;
                const files = json.chapter.data || [];

                return files.map((file, idx) => ({
                    index: idx + 1,
                    url: `${'$'}{baseUrl}/data/${'$'}{hash}/${'$'}{file}`,
                    imageUrl: `${'$'}{baseUrl}/data/${'$'}{hash}/${'$'}{file}`,
                    headers: {
                        "User-Agent": "Dokuha/1.0.0 (https://github.com/RitwikGupta-0501/dokuha)",
                        "Referer": "https://mangadex.org"
                    }
                }));
            }
        };
    """.trimIndent()

    val DEFAULT_INSTALLED_BUNDLES = listOf(
        ASURA_SCANS_BUNDLE,
        MANGADEX_BUNDLE
    )

    val DEMO_CATALOG_ITEMS = listOf(
        ExtensionCatalogItem(
            id = "asurascans",
            name = "Asura Scans",
            version = "1.2.0",
            lang = "en",
            baseUrl = "https://asurascans.com",
            type = ExtensionType.COMIC,
            iconUrl = "https://raw.githubusercontent.com/m2k3a/mangayomi-extensions/main/javascript/icon/en.asurascans.png",
            pkg = "demo://asurascans",
            ecosystem = ExtensionEcosystem.CUSTOM
        ),
        ExtensionCatalogItem(
            id = "mangadex",
            name = "MangaDex",
            version = "1.0.2",
            lang = "en",
            baseUrl = "https://mangadex.org",
            type = ExtensionType.COMIC,
            iconUrl = "https://mangadex.org/favicon.ico",
            pkg = "demo://mangadex",
            ecosystem = ExtensionEcosystem.CUSTOM
        )
    )

    fun getDemoBundle(id: String): String? = when (id) {
        "asurascans", "ext-asura" -> ASURA_SCANS_BUNDLE
        "mangadex", "ext-mangadex" -> MANGADEX_BUNDLE
        else -> null
    }
}
