package core.extensions.paperback

import core.extensions.js.JsBridge
import core.extensions.js.JsEnginePool
import core.source.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

@Serializable
data class PaperbackManifest(
    val id: String,
    val name: String,
    val websiteBaseURL: String,
    val language: String = "en",
    val version: String,
    val contentRating: String = "EVERYONE",
    val script: String,
    val iconUrl: String? = null
)

/**
 * Adapter integrating Paperback v0.8 and v0.9 TypeScript/JavaScript scrapers into Dokuha's [ComicSource] contract.
 * Dynamically resolves export signatures (`source.default`, `source[manifest.id]`, or candidate key) without source modification.
 */
class PaperbackSourceAdapter(
    val manifest: PaperbackManifest,
    private val enginePool: JsEnginePool,
    private val bridge: JsBridge,
    val isV9: Boolean = false
) : ComicSource {

    override val id: Long = (manifest.id.hashCode().toLong() and 0x7FFFFFFF) + 30_000L
    override val name: String = manifest.name
    override val baseUrl: String = manifest.websiteBaseURL
    override val lang: String = manifest.language
    override val isNsfw: Boolean = manifest.contentRating.equals("ADULT", ignoreCase = true)

    private suspend fun initEngine(engine: core.extensions.js.JsEngine) {
        bridge.registerFunctions(engine, id)
        engine.evaluate(bridge.getPreambleScript(id))
        engine.setGlobalVariable("__dokuha_source_id", id.toString())
        engine.evaluate(manifest.script)

        val initScript = """
            (async () => {
                let inst = null;
                let mode = '$isV9' === 'true' ? 'v9' : 'v8';

                if (globalThis.source) {
                    mode = 'v9';
                    const src = globalThis.source;
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
                } else if (globalThis.Sources && Object.keys(globalThis.Sources).length > 0) {
                    mode = 'v8';
                    const sourceKey = Object.keys(globalThis.Sources)[0];
                    inst = new globalThis.Sources[sourceKey](globalThis.cheerio);
                } else if (globalThis.window && globalThis.window.Sources && Object.keys(globalThis.window.Sources).length > 0) {
                    mode = 'v8';
                    const sourceKey = Object.keys(globalThis.window.Sources)[0];
                    inst = new globalThis.window.Sources[sourceKey](globalThis.cheerio);
                } else {
                    throw new Error("Neither Paperback v0.9 nor v0.8 source namespace found");
                }
                globalThis.__pb_instance = inst;
                globalThis.__pb_mode = mode;
                if (typeof globalThis.__pb_instance.initialise === 'function') {
                    await globalThis.__pb_instance.initialise();
                }
            })()
        """.trimIndent()
        engine.evaluate(initScript)
    }

    override suspend fun getPopular(page: Int): MangasPage = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val script = """
            (async () => {
                if (globalThis.__pb_mode === 'v9' && typeof globalThis.__pb_instance.getDiscoverSectionItems === 'function') {
                    return JSON.stringify(await globalThis.__pb_instance.getDiscoverSectionItems({ id: 'popular' }, { page: $page }));
                } else if (typeof globalThis.__pb_instance.getViewMoreItems === 'function') {
                    return JSON.stringify(await globalThis.__pb_instance.getViewMoreItems('popular', { page: $page }));
                } else if (typeof globalThis.__pb_instance.getHomePageSections === 'function') {
                    const sections = await globalThis.__pb_instance.getHomePageSections();
                    const first = (sections && sections[0]) ? (sections[0].items || []) : [];
                    return JSON.stringify({ items: first });
                }
                return JSON.stringify({ items: [] });
            })()
        """.trimIndent()
        val res = engine.evaluate(script)
        parsePagedResults(res)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val script = """
            (async () => {
                if (globalThis.__pb_mode === 'v9' && typeof globalThis.__pb_instance.getDiscoverSectionItems === 'function') {
                    return JSON.stringify(await globalThis.__pb_instance.getDiscoverSectionItems({ id: 'latest_updates' }, { page: $page }));
                } else if (typeof globalThis.__pb_instance.getViewMoreItems === 'function') {
                    return JSON.stringify(await globalThis.__pb_instance.getViewMoreItems('latest_updates', { page: $page }));
                } else if (typeof globalThis.__pb_instance.getHomePageSections === 'function') {
                    const sections = await globalThis.__pb_instance.getHomePageSections();
                    const sec = (sections && sections.length > 1) ? sections[1] : (sections && sections[0]);
                    const items = sec ? (sec.items || []) : [];
                    return JSON.stringify({ items: items });
                }
                return JSON.stringify({ items: [] });
            })()
        """.trimIndent()
        val res = engine.evaluate(script)
        parsePagedResults(res)
    }

    override suspend fun search(query: String, page: Int, filters: List<SourceFilter>): MangasPage = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val script = """
            (async () => {
                if (typeof globalThis.__pb_instance.getSearchResults === 'function') {
                    return JSON.stringify(await globalThis.__pb_instance.getSearchResults({ title: ${Json.encodeToString(query)} }, { page: $page }));
                }
                return JSON.stringify({ items: [] });
            })()
        """.trimIndent()
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
            status = mapStatus(info["status"]?.jsonPrimitive?.contentOrNull),
            sourceId = this.id
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

    private fun parsePagedResults(jsonStr: String?): MangasPage {
        if (jsonStr.isNullOrBlank() || jsonStr == "null" || jsonStr == "undefined") {
            return MangasPage(mangas = emptyList(), hasNextPage = false)
        }
        val element = try {
            Json.parseToJsonElement(jsonStr)
        } catch (_: Exception) {
            return MangasPage(mangas = emptyList(), hasNextPage = false)
        }
        val results = when (element) {
            is JsonArray -> element
            is JsonObject -> element["results"]?.jsonArray
                ?: element["items"]?.jsonArray
                ?: element["pagedResults"]?.jsonObject?.get("results")?.jsonArray
                ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }
        val mangas = results.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val url = obj["id"]?.jsonPrimitive?.contentOrNull
                ?: obj["mangaId"]?.jsonPrimitive?.contentOrNull
                ?: return@mapNotNull null
            val title = obj["title"]?.jsonPrimitive?.contentOrNull
                ?: (obj["title"] as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull
                ?: "Unknown"
            val image = obj["image"]?.jsonPrimitive?.contentOrNull
            SourceManga(
                url = url,
                title = title,
                thumbnailUrl = image,
                isNovel = false,
                sourceId = this.id
            )
        }
        return MangasPage(mangas = mangas, hasNextPage = mangas.isNotEmpty())
    }
}
