package core.extensions.lnreader

import core.extensions.js.JsBridge
import core.extensions.js.JsEnginePool
import core.source.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

@Serializable
data class LNReaderManifest(
    val id: String,
    val name: String,
    val site: String,
    val lang: String,
    val version: String,
    val script: String
)

/**
 * Adapter integrating LNReader TypeScript/CommonJS plugins into Dokuha's [NovelSource] contract.
 * Supports PagePlugin chapter pagination and cleans textual chapter content.
 */
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
        bridge.registerFunctions(engine, id)
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
            status = mapNovelStatus(json["status"]?.jsonPrimitive?.contentOrNull),
            genres = json["genres"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: manga.genres,
            isNovel = true
        )
    }

    override suspend fun getChapterList(manga: SourceManga): List<SourceChapter> = enginePool.withEngine(id, { initEngine(it) }) { engine ->
        val script = "JSON.stringify(await globalThis.__ln_instance.parseNovel(${Json.encodeToString(manga.url)}))"
        val res = engine.evaluate(script)
        val json = Json.parseToJsonElement(res).jsonObject
        val baseChapters = json["chapters"]?.jsonArray?.map { parseChapterObj(it.jsonObject) } ?: emptyList()

        // PagePlugin multi-page iteration if supported (for 1,000+ chapter web novels)
        val isPagePlugin = engine.evaluate("typeof globalThis.__ln_instance.parsePage === 'function'").toBoolean()
        if (isPagePlugin) {
            val allChapters = baseChapters.toMutableList()
            var page = 2
            var hasMore = true
            while (hasMore && page <= 50) { // Safety ceiling of 50 pages (approx 5,000 chapters)
                val pageScript = "JSON.stringify(await globalThis.__ln_instance.parsePage(${Json.encodeToString(manga.url)}, $page))"
                val pageRes = try {
                    engine.evaluate(pageScript)
                } catch (_: Exception) {
                    "[]"
                }
                val pageArr = Json.parseToJsonElement(pageRes).jsonArray
                if (pageArr.isEmpty()) {
                    hasMore = false
                } else {
                    allChapters.addAll(pageArr.map { parseChapterObj(it.jsonObject) })
                    page++
                }
            }
            allChapters
        } else {
            baseChapters
        }
    }

    private fun parseChapterObj(obj: JsonObject): SourceChapter = SourceChapter(
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

    private fun mapNovelStatus(status: String?): MangaStatus = when (status?.lowercase()) {
        "ongoing" -> MangaStatus.ONGOING
        "completed" -> MangaStatus.COMPLETED
        "on hiatus", "hiatus" -> MangaStatus.ON_HIATUS
        "cancelled" -> MangaStatus.CANCELLED
        "publishing finished" -> MangaStatus.PUBLISHING_FINISHED
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
