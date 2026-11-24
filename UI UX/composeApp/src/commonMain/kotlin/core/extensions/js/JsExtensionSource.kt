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
            bridge.registerFunctions(engine, id)
            engine.evaluate(bridge.getPreambleScript(id))
        }
        engine.evaluate(manifest.script)
    }

    override suspend fun getPopular(page: Int): MangasPage {
        val result = engine.callFunction("extension.getPopular", page)
        core.logging.AppLogger.d("JsExtensionSource", "getPopular result: $result")
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
        core.logging.AppLogger.d("JsExtensionSource", "getPageList called with chapter.url=${chapter.url}")
        return try {
            val result = engine.callFunction("extension.getPages", chapter.url)
            core.logging.AppLogger.d("JsExtensionSource", "extension.getPages returned raw result length: ${result.length}")
            val pages = parsePageList(result)
            core.logging.AppLogger.d("JsExtensionSource", "parsePageList parsed ${pages.size} pages")
            pages
        } catch (e: Exception) {
            core.logging.AppLogger.e("JsExtensionSource", "Error calling extension.getPages: ${e.message}", e)
            emptyList()
        }
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
            isNovel = isNovel,
            sourceId = this.id
        )
    }

    private fun parseMangaDetails(jsonStr: String, fallback: SourceManga): SourceManga {
        return try {
            val obj = json.parseToJsonElement(jsonStr).jsonObject
            val parsed = parseSourceManga(obj)
            parsed.copy(
                url = if (parsed.url.isNotEmpty()) parsed.url else fallback.url,
                title = if (parsed.title.isNotBlank() && parsed.title != "Untitled" && parsed.title != "Unknown") parsed.title else fallback.title,
                thumbnailUrl = parsed.thumbnailUrl ?: fallback.thumbnailUrl,
                sourceId = if (parsed.sourceId != 0L) parsed.sourceId else (if (fallback.sourceId != 0L) fallback.sourceId else this.id)
            )
        } catch (_: Exception) {
            fallback.copy(sourceId = if (fallback.sourceId != 0L) fallback.sourceId else this.id)
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
        } catch (e: Exception) {
            println("[JsExtensionSource] Error in parsePageList: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parsePageList(jsonStr: String): List<SourcePage> {
        return try {
            val element = json.parseToJsonElement(jsonStr)
            val array = if (element is JsonArray) element else element.jsonObject["pages"]?.jsonArray ?: JsonArray(emptyList())
            array.mapIndexed { idx, item ->
                val obj = item.jsonObject
                val headersMap = obj["headers"]?.let { h ->
                    if (h is JsonObject) {
                        h.mapValues { it.value.jsonPrimitive.content }
                    } else null
                } ?: mapOf(
                    "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
                    "Referer" to manifest.baseUrl
                )
                SourcePage(
                    index = obj["index"]?.jsonPrimitive?.floatOrNull?.toInt() ?: (idx + 1),
                    url = obj["url"]?.jsonPrimitive?.contentOrNull ?: "",
                    imageUrl = obj["imageUrl"]?.jsonPrimitive?.contentOrNull,
                    headers = headersMap
                )
            }
        } catch (e: Exception) {
            println("[JsExtensionSource] Error in parsePageList: ${e.message}")
            e.printStackTrace()
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
