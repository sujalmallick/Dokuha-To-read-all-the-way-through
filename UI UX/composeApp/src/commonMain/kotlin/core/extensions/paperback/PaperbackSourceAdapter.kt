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
    val script: String
)

/**
 * Adapter integrating Paperback v0.8 and v0.9 TypeScript/JavaScript scrapers into Dokuha's [ComicSource] contract.
 * Dynamically resolves export signatures (`source.default`, `source[manifest.id]`, or candidate key) without source modification.
 */
class PaperbackSourceAdapter(
    val manifest: PaperbackManifest,
    private val enginePool: JsEnginePool,
    private val bridge: JsBridge,
    val isV9: Boolean = true
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

        if (isV9) {
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
                })()
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
