package core.extensions.js

import core.source.ComicSource
import core.source.MangaStatus
import core.source.MangasPage
import core.source.NovelContent
import core.source.NovelSource
import core.source.SourceChapter
import core.source.SourceFilter
import core.source.SourceManga
import core.source.SourcePage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Adapter that binds a JavaScript extension into Dokuha's ComicSource and NovelSource contracts.
 */
class JsExtensionSource(
    val manifest: JsExtensionManifest,
    private val engine: JsEngine,
    private val bridge: JsBridge? = null
) : ComicSource, NovelSource {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override val id: Long = (manifest.id.hashCode().toLong() and 0x7FFFFFFF) + 10_000L
    override val name: String = manifest.name
    override val baseUrl: String = manifest.baseUrl
    override val lang: String = manifest.lang
    override val isNsfw: Boolean = manifest.isNsfw

    suspend fun initialize() {
        if (bridge != null) {
            setupBridgeFunctions(engine, bridge)
            engine.evaluate(bridge.getPreambleScript())
        }
        engine.evaluate(manifest.script)
    }

    private fun setupBridgeFunctions(engine: JsEngine, bridge: JsBridge) {
        engine.setGlobalFunction("__dokuha_log") { args ->
            val lvl = args.getOrNull(0)?.toString() ?: "LOG"
            val msg = args.getOrNull(1)?.toString() ?: ""
            bridge.log(lvl, msg)
            null
        }
        engine.setGlobalFunction("__dokuha_btoa") { args ->
            val str = args.getOrNull(0)?.toString() ?: ""
            bridge.btoa(str)
        }
        engine.setGlobalFunction("__dokuha_atob") { args ->
            val str = args.getOrNull(0)?.toString() ?: ""
            bridge.atob(str)
        }
        engine.setGlobalFunction("__dokuha_sha256") { args ->
            val str = args.getOrNull(0)?.toString() ?: ""
            bridge.sha256(str)
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
            val res = bridge.fetch(url, method, headersMap, body)
            val resJson = buildJsonObject {
                put("status", res.status)
                put("ok", res.ok)
                putJsonObject("headers") {
                    res.headers.forEach { (k, v) -> put(k, v) }
                }
                put("body", res.body)
            }.toString()
            resJson
        }
    }

    override suspend fun getPopular(page: Int): MangasPage {
        val result = engine.callFunction("extension.getPopular", page)
        return parseMangasPage(result)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        return try {
            val result = engine.callFunction("extension.getLatestUpdates", page)
            parseMangasPage(result)
        } catch (_: Exception) {
            getPopular(page)
        }
    }

    override suspend fun search(query: String, page: Int, filters: List<SourceFilter>): MangasPage {
        return try {
            val result = engine.callFunction("extension.search", query, page)
            parseMangasPage(result)
        } catch (_: Exception) {
            val popular = getPopular(page)
            if (query.isBlank()) {
                popular
            } else {
                val filtered = popular.mangas.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.author?.contains(query, ignoreCase = true) == true
                }
                popular.copy(mangas = filtered)
            }
        }
    }

    override suspend fun getMangaDetails(manga: SourceManga): SourceManga {
        val result = engine.callFunction("extension.getDetails", manga.url)
        return parseMangaDetails(result, manga)
    }

    override suspend fun getChapterList(manga: SourceManga): List<SourceChapter> {
        val result = engine.callFunction("extension.getChapters", manga.url)
        return parseChapterList(result)
    }

    override suspend fun getPageList(chapter: SourceChapter): List<SourcePage> {
        val result = engine.callFunction("extension.getPages", chapter.url)
        return parsePageList(result)
    }

    override suspend fun getChapterContent(chapter: SourceChapter): NovelContent {
        val result = engine.callFunction("extension.getNovelContent", chapter.url)
        return parseNovelContent(result, chapter.url)
    }

    fun close() {
        engine.close()
    }

    private fun parseMangasPage(jsonStr: String): MangasPage {
        return try {
            val element = json.parseToJsonElement(jsonStr)
            if (element is JsonObject) {
                val mangasArray = element["mangas"]?.jsonArray ?: JsonArray(emptyList())
                val hasNext = element["hasNextPage"]?.jsonPrimitive?.booleanOrNull ?: false
                val mangas = mangasArray.map { parseSourceManga(it.jsonObject) }
                MangasPage(mangas = mangas, hasNextPage = hasNext)
            } else if (element is JsonArray) {
                val mangas = element.map { parseSourceManga(it.jsonObject) }
                MangasPage(mangas = mangas, hasNextPage = false)
            } else {
                MangasPage(emptyList(), false)
            }
        } catch (_: Exception) {
            MangasPage(emptyList(), false)
        }
    }

    private fun parseSourceManga(obj: JsonObject): SourceManga {
        val statusStr = obj["status"]?.jsonPrimitive?.contentOrNull?.uppercase() ?: "UNKNOWN"
        val status = try {
            MangaStatus.valueOf(statusStr)
        } catch (_: Exception) {
            MangaStatus.UNKNOWN
        }
        val genres = obj["genres"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        val isNovel = obj["isNovel"]?.jsonPrimitive?.booleanOrNull
            ?: (manifest.type == ExtensionType.NOVEL)

        return SourceManga(
            url = obj["url"]?.jsonPrimitive?.contentOrNull ?: "",
            title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "Untitled",
            artist = obj["artist"]?.jsonPrimitive?.contentOrNull,
            author = obj["author"]?.jsonPrimitive?.contentOrNull,
            description = obj["description"]?.jsonPrimitive?.contentOrNull,
            genres = genres,
            status = status,
            thumbnailUrl = obj["thumbnailUrl"]?.jsonPrimitive?.contentOrNull,
            isNovel = isNovel
        )
    }

    private fun parseMangaDetails(jsonStr: String, fallback: SourceManga): SourceManga {
        return try {
            val obj = json.parseToJsonElement(jsonStr).jsonObject
            val parsed = parseSourceManga(obj)
            parsed.copy(
                url = if (parsed.url.isNotEmpty()) parsed.url else fallback.url,
                title = if (parsed.title.isNotEmpty() && parsed.title != "Untitled") parsed.title else fallback.title,
                thumbnailUrl = parsed.thumbnailUrl ?: fallback.thumbnailUrl
            )
        } catch (_: Exception) {
            fallback
        }
    }

    private fun parseChapterList(jsonStr: String): List<SourceChapter> {
        return try {
            val element = json.parseToJsonElement(jsonStr)
            val array = if (element is JsonArray) element else element.jsonObject["chapters"]?.jsonArray ?: JsonArray(emptyList())
            array.mapNotNull { it as? JsonObject }.map { obj ->
                SourceChapter(
                    url = obj["url"]?.jsonPrimitive?.contentOrNull ?: "",
                    name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Chapter",
                    dateUpload = obj["dateUpload"]?.jsonPrimitive?.longOrNull ?: 0L,
                    chapterNumber = obj["chapterNumber"]?.jsonPrimitive?.floatOrNull ?: -1f,
                    scanlator = obj["scanlator"]?.jsonPrimitive?.contentOrNull
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parsePageList(jsonStr: String): List<SourcePage> {
        return try {
            val element = json.parseToJsonElement(jsonStr)
            val array = if (element is JsonArray) element else element.jsonObject["pages"]?.jsonArray ?: JsonArray(emptyList())
            array.mapIndexed { idx, item ->
                val obj = item.jsonObject
                SourcePage(
                    index = obj["index"]?.jsonPrimitive?.floatOrNull?.toInt() ?: (idx + 1),
                    url = obj["url"]?.jsonPrimitive?.contentOrNull ?: "",
                    imageUrl = obj["imageUrl"]?.jsonPrimitive?.contentOrNull
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseNovelContent(jsonStr: String, defaultUrl: String): NovelContent {
        return try {
            val obj = json.parseToJsonElement(jsonStr).jsonObject
            val body = obj["htmlBody"]?.jsonPrimitive?.contentOrNull ?: ""
            val plain = obj["plainText"]?.jsonPrimitive?.contentOrNull ?: body.replace(Regex("<[^>]*>"), "")
            NovelContent(
                chapterUrl = obj["chapterUrl"]?.jsonPrimitive?.contentOrNull ?: defaultUrl,
                title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "Chapter",
                htmlBody = body,
                plainText = plain
            )
        } catch (_: Exception) {
            NovelContent(defaultUrl, "Chapter", "", "")
        }
    }
}
