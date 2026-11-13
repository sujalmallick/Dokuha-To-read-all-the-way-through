package core.extensions.mangayomi

import core.extensions.js.JsBridge
import core.extensions.js.JsEnginePool
import core.platform.currentTimeMillis
import core.source.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class MangayomiManifest(
    val id: Long,
    val name: String,
    val baseUrl: String,
    val lang: String,
    val version: String,
    val isNovel: Boolean = false,
    val isNsfw: Boolean = false,
    val script: String
) {
    fun toJson(): String = buildJsonObject {
        put("id", id)
        put("name", name)
        put("baseUrl", baseUrl)
        put("lang", lang)
        put("version", version)
        put("isNovel", isNovel)
        put("isNsfw", isNsfw)
    }.toString()
}

/**
 * Adapter integrating Mangayomi JavaScript scrapers into Dokuha's [ComicSource] and [NovelSource] contracts.
 * Evaluates scripts within [JsEnginePool] without modifying upstream source code, and caches title details for 60s.
 */
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

    // 60-second in-memory Detail Cache to eliminate duplicate HTTP requests between details & chapters
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
        bridge.registerFunctions(engine, id)
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
        val html = try {
            Json.parseToJsonElement(result).jsonPrimitive.content
        } catch (_: Exception) {
            result
        }
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
