/**
 * Asura Scans Extension for Dokuha
 * Ported from Keiyoushi's Kotlin implementation (v2.0.0).
 * Features:
 * - Official JSON API integration (https://api.asurascans.com/api)
 * - True infinite catalogue pagination with meta.has_more
 * - Astro v7 island hydration unwrap (unwrapAstro) for metadata and chapters
 * - Dual-contract export: Dokuha native (manifest + extension) & Mangayomi (DefaultExtension)
 */

function unwrapAstro(val) {
    if (Array.isArray(val)) {
        if (val.length === 0) return null;
        if (val.length === 2 && typeof val[0] === 'number') {
            return unwrapAstro(val[1]);
        }
        return val.map(unwrapAstro);
    }
    if (val !== null && typeof val === 'object') {
        const obj = {};
        for (const k in val) {
            if (Object.prototype.hasOwnProperty.call(val, k)) {
                obj[k] = unwrapAstro(val[k]);
            }
        }
        return obj;
    }
    return val;
}

function extractAstroData(html, key) {
    if (!html) return null;
    try {
        const doc = new Document(html);
        const el = doc.selectFirst(`[props*="${key}"]`);
        if (el) {
            const raw = el.attr("props");
            if (raw) {
                return unwrapAstro(JSON.parse(raw));
            }
        }
    } catch (_) {}

    // Fallback regex search
    try {
        const regex = new RegExp(`<astro-island[^>]*props="([^"]*${key}[^"]*)"`, 'i');
        const match = html.match(regex);
        if (match && match[1]) {
            const decoded = match[1].replace(/&quot;/g, '"').replace(/&amp;/g, '&');
            return unwrapAstro(JSON.parse(decoded));
        }
    } catch (_) {}

    return null;
}

function mapStatus(statusStr) {
    if (!statusStr) return "UNKNOWN";
    const s = String(statusStr).toLowerCase();
    if (s.includes("ongoing")) return "ONGOING";
    if (s.includes("complete")) return "COMPLETED";
    if (s.includes("hiatus")) return "ON_HIATUS";
    if (s.includes("dropped") || s.includes("cancel") || s.includes("axed")) return "CANCELLED";
    return "UNKNOWN";
}

const API_BASE = "https://api.asurascans.com/api";
const WEB_BASE = "https://asurascans.com";

const DEFAULT_HEADERS = {
    "Referer": "https://asurascans.com/",
    "Origin": "https://asurascans.com",
    "Accept": "application/json, text/plain, */*",
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
};

// =========================================================================
// Dokuha Native Contract
// =========================================================================

const manifest = {
    id: "asurascans",
    name: "Asura Scans",
    version: "2.0.0",
    lang: "en",
    baseUrl: WEB_BASE,
    apiUrl: API_BASE,
    type: "COMIC",
    isNsfw: false
};

const extension = {
    async getPopular(page) {
        const offset = Math.max(0, (page - 1) * 20);
        const url = `${API_BASE}/series?offset=${offset}&limit=20&order=popular`;
        const res = await new Client().get(url, DEFAULT_HEADERS);
        return this.parseCatalogResponse(res.body);
    },

    async getLatestUpdates(page) {
        const offset = Math.max(0, (page - 1) * 20);
        const url = `${API_BASE}/series?offset=${offset}&limit=20&order=latest`;
        const res = await new Client().get(url, DEFAULT_HEADERS);
        return this.parseCatalogResponse(res.body);
    },

    async search(query, page, filters) {
        const offset = Math.max(0, (page - 1) * 20);
        const searchParam = query ? `&search=${encodeURIComponent(query)}` : "";
        const url = `${API_BASE}/series?offset=${offset}&limit=20${searchParam}`;
        const res = await new Client().get(url, DEFAULT_HEADERS);
        return this.parseCatalogResponse(res.body);
    },

    parseCatalogResponse(body) {
        try {
            const parsed = typeof body === "string" ? JSON.parse(body) : body;
            const data = (parsed && parsed.data) || [];
            const meta = (parsed && parsed.meta) || {};
            const mangas = data.map(item => ({
                url: item.public_url || `/comics/${item.slug}`,
                title: item.title || "Untitled",
                thumbnailUrl: item.coverUrl || item.cover || "",
                status: "UNKNOWN",
                genres: [],
                isNovel: false
            }));
            const hasNext = Boolean(meta.has_more ?? (data.length >= 20));
            return { mangas, hasNextPage: hasNext };
        } catch (_) {
            return { mangas: [], hasNextPage: false };
        }
    },

    async getDetails(url) {
        const fullUrl = url.startsWith("http") ? url : `${WEB_BASE}${url.startsWith("/") ? "" : "/"}${url}`;
        const res = await new Client().get(fullUrl, DEFAULT_HEADERS);
        const html = res.body || "";
        const data = extractAstroData(html, "title");

        if (data) {
            const genres = (data.genres || []).map(g => (typeof g === "object" ? g.name : String(g))).filter(Boolean);
            const cleanDesc = data.description
                ? data.description.replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/<[^>]*>/g, "").trim()
                : "";
            return {
                url: url,
                title: data.title || "Untitled",
                thumbnailUrl: data.coverUrl || data.cover || "",
                author: data.author || "",
                artist: data.artist || "",
                description: cleanDesc,
                genres: genres,
                status: mapStatus(data.status),
                isNovel: false
            };
        }

        // Fallback meta parsing from HTML document
        const doc = new Document(html);
        const ogTitle = doc.selectFirst("meta[property='og:title']")?.attr("content") || "Unknown";
        const ogImage = doc.selectFirst("meta[property='og:image']")?.attr("content") || "";
        const ogDesc = doc.selectFirst("meta[property='og:description']")?.attr("content") || "";

        return {
            url: url,
            title: ogTitle,
            thumbnailUrl: ogImage,
            description: ogDesc,
            author: "",
            artist: "",
            genres: [],
            status: "UNKNOWN",
            isNovel: false
        };
    },

    async getChapters(url) {
        const fullUrl = url.startsWith("http") ? url : `${WEB_BASE}${url.startsWith("/") ? "" : "/"}${url}`;
        const res = await new Client().get(fullUrl, DEFAULT_HEADERS);
        const html = res.body || "";
        const data = extractAstroData(html, "chapters");
        const rawChapters = (data && data.chapters) ? data.chapters : [];
        const slug = url.split("/").filter(Boolean).pop() || "";
        const now = Date.now();

        return rawChapters.map(ch => {
            const isLocked = ch.is_premium || (ch.early_access_until && new Date(ch.early_access_until).getTime() > now);
            const num = parseFloat(ch.number) || 0;
            const chapterSlug = ch.series_slug || slug;
            const chapterUrl = `/comics/${chapterSlug}/chapter/${num}`;
            const chapterName = ch.title ? `Chapter ${num}: ${ch.title}` : `Chapter ${num}`;
            const dateUpload = ch.created_at ? new Date(ch.created_at).getTime() : 0;

            return {
                url: chapterUrl,
                name: chapterName,
                chapterNumber: num,
                dateUpload: dateUpload,
                scanlator: isLocked ? "[Early Access]" : "Asura Scans"
            };
        });
    },

    async getPages(chapterUrl) {
        const fullUrl = chapterUrl.startsWith("http") ? chapterUrl : `${WEB_BASE}${chapterUrl.startsWith("/") ? "" : "/"}${chapterUrl}`;
        const res = await new Client().get(fullUrl, DEFAULT_HEADERS);
        const html = res.body || "";
        const data = extractAstroData(html, "pages");
        const pages = (data && data.pages) ? data.pages : [];

        if (pages.length > 0) {
            return pages.map((p, idx) => {
                const img = typeof p === "string" ? p : (p.url || p.imageUrl || "");
                return { index: idx, url: img, imageUrl: img };
            });
        }

        // Script regex fallback
        const match = html.match(/"pages":\s*(\[[^\]]+\])/);
        if (match && match[1]) {
            try {
                const fallbackList = JSON.parse(match[1]);
                return fallbackList.map((p, idx) => {
                    const img = typeof p === "string" ? p : (p.url || "");
                    return { index: idx, url: img, imageUrl: img };
                });
            } catch (_) {}
        }

        return [];
    }
};

// =========================================================================
// Mangayomi Ecosystem Compatibility Contract
// =========================================================================

const mangayomiSources = [{
    id: 524070078,
    name: "Asura Scans",
    lang: "en",
    baseUrl: WEB_BASE,
    apiUrl: API_BASE,
    iconUrl: "https://raw.githubusercontent.com/m2k3a/mangayomi-extensions/main/javascript/icon/en.asurascans.png",
    typeSource: "single",
    itemType: 0,
    version: "2.0.0",
    dateFormat: "",
    dateFormatLocale: "",
    pkgPath: "manga/src/en/asurascans.js"
}];

class DefaultExtension {
    async getPopular(page) {
        const res = await extension.getPopular(page);
        return {
            list: res.mangas.map(m => ({ name: m.title, imageUrl: m.thumbnailUrl, link: m.url })),
            hasNextPage: res.hasNextPage
        };
    }

    async getLatestUpdates(page) {
        const res = await extension.getLatestUpdates(page);
        return {
            list: res.mangas.map(m => ({ name: m.title, imageUrl: m.thumbnailUrl, link: m.url })),
            hasNextPage: res.hasNextPage
        };
    }

    async search(query, page, filters) {
        const res = await extension.search(query, page, filters);
        return {
            list: res.mangas.map(m => ({ name: m.title, imageUrl: m.thumbnailUrl, link: m.url })),
            hasNextPage: res.hasNextPage
        };
    }

    async getDetail(url) {
        const m = await extension.getDetails(url);
        const chapters = await extension.getChapters(url);
        return {
            name: m.title,
            imageUrl: m.thumbnailUrl,
            description: m.description,
            author: m.author,
            artist: m.artist,
            genre: m.genres,
            status: m.status === "ONGOING" ? 0 : (m.status === "COMPLETED" ? 1 : 5),
            chapters: chapters.map(ch => ({
                name: ch.name,
                url: ch.url,
                dateUpload: String(ch.dateUpload),
                chapterNumber: ch.chapterNumber
            }))
        };
    }

    async getPageList(url) {
        const pages = await extension.getPages(url);
        return pages.map(p => p.imageUrl);
    }
}
