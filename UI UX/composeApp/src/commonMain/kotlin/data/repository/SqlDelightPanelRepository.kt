package data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import core.platform.currentTimeMillis
import core.source.ComicSource
import core.source.ContentSource
import core.source.MangaStatus
import core.source.NovelContent
import core.source.NovelSource
import core.source.SourceChapter
import core.source.SourceManga
import core.source.SourcePage
import data.mock.BrowseFilters
import data.mock.HistoryGroup
import data.mock.HistoryItem
import data.mock.MockDownloadItem
import data.mock.MockExtension
import data.mock.MockSourceInfo
import data.mock.MockStorageInfo
import data.mock.MockUpdateGroup
import data.mock.MockUpdateItem
import data.mock.PanelRepository
import io.panel.reader.db.Chapters
import io.panel.reader.db.GetContinueReading
import io.panel.reader.db.Mangas
import io.panel.reader.db.PanelDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Production local persistent repository backed by SQLite via CashApp SQLDelight.
 * Replaces FakePanelRepository with real database queries, reactive Flows, and
 * seamless integration with dynamically registered extensions.
 */
class SqlDelightPanelRepository(
    private val database: PanelDatabase
) : PanelRepository {

    private val dynamicSources = mutableMapOf<Long, ContentSource>()

    init {
        purgeFakeStarterDataIfPresent()
    }

    private fun purgeFakeStarterDataIfPresent() {
        val fakeUrls = listOf(
            "manga/solo-leveling",
            "manga/aurora-blade",
            "manga/wind-beneath",
            "manga/shadow-crown",
            "manga/neon-requiem",
            "manga/the-last-horizon"
        )
        database.transaction {
            for (fakeUrl in fakeUrls) {
                val existing = database.mangaQueries.getMangaByUrl(fakeUrl).executeAsOneOrNull()
                if (existing != null) {
                    val chapters = database.chapterQueries.getChaptersByMangaId(existing.id).executeAsList()
                    for (ch in chapters) {
                        database.historyQueries.deleteHistoryByChapterId(ch.id)
                    }
                    database.chapterQueries.deleteChaptersByMangaId(existing.id)
                    database.mangaQueries.deleteByUrl(fakeUrl)
                }
            }
        }
    }

    private fun insertOrUpdateMangaInternal(
        manga: SourceManga,
        sourceId: Long = 0L,
        forceFavorite: Boolean? = null
    ): Long {
        val existing = database.mangaQueries.getMangaByUrl(manga.url).executeAsOneOrNull()
        val fav = when (forceFavorite) {
            true -> 1L
            false -> 0L
            null -> existing?.favorite ?: 0L
        }
        val dateAdded = if (fav == 1L) {
            if (existing?.date_added != null && existing.date_added > 0L) existing.date_added else currentTimeMillis()
        } else 0L

        database.mangaQueries.insertOrUpdateManga(
            id = existing?.id,
            sourceId = sourceId,
            url = manga.url,
            title = manga.title,
            artist = manga.artist,
            author = manga.author,
            description = manga.description,
            genres = manga.genres.joinToString(","),
            status = manga.status.name,
            thumbnailUrl = manga.thumbnailUrl,
            favorite = fav,
            lastUpdate = currentTimeMillis(),
            nextUpdate = 0L,
            initialized = 1L,
            viewerFlags = 0L,
            chapterFlags = 0L,
            coverLastModified = 0L,
            dateAdded = dateAdded,
            isNovel = if (manga.isNovel) 1L else 0L
        )

        return database.mangaQueries.getMangaByUrl(manga.url).executeAsOne().id
    }

    override fun getLibrary(category: String): Flow<List<SourceManga>> {
        return database.mangaQueries.getLibrary()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toSourceManga() } }
    }

    override fun getContinueReading(): Flow<Pair<SourceManga, SourceChapter>?> {
        return database.historyQueries.getContinueReading()
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toPair() }
    }

    override fun getMangaDetails(url: String): Flow<SourceManga?> {
        val matchingDynamic = dynamicSources.values.firstOrNull { src ->
            url.startsWith(src.baseUrl) || (url.length == 36 && url.contains("-")) // UUID or base URL match
        }

        if (matchingDynamic != null) {
            return flow {
                val cached = database.mangaQueries.getMangaByUrl(url).executeAsOneOrNull()?.toSourceManga()
                if (cached != null) emit(cached)

                try {
                    val fresh = matchingDynamic.getMangaDetails(SourceManga(url = url, title = ""))
                    insertOrUpdateMangaInternal(fresh, sourceId = matchingDynamic.id, forceFavorite = null)
                    emit(fresh)
                } catch (_: Exception) {
                    if (cached == null) emit(null)
                }
            }
        }

        return database.mangaQueries.getMangaByUrl(url)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toSourceManga() }
    }

    override fun getChapters(mangaUrl: String): Flow<List<SourceChapter>> {
        val matchingDynamic = dynamicSources.values.firstOrNull { src ->
            mangaUrl.startsWith(src.baseUrl) || (mangaUrl.length == 36 && mangaUrl.contains("-"))
        }

        if (matchingDynamic != null) {
            return flow {
                val cached = database.chapterQueries.getChaptersByMangaUrl(mangaUrl).executeAsList().map { it.toSourceChapter() }
                if (cached.isNotEmpty()) emit(cached)

                try {
                    val fresh = matchingDynamic.getChapterList(SourceManga(url = mangaUrl, title = ""))
                    val mangaId = database.mangaQueries.getMangaByUrl(mangaUrl).executeAsOneOrNull()?.id
                        ?: insertOrUpdateMangaInternal(SourceManga(url = mangaUrl, title = ""), matchingDynamic.id, forceFavorite = null)

                    for ((idx, ch) in fresh.withIndex()) {
                        val existingCh = database.chapterQueries.getChapterByUrl(ch.url).executeAsOneOrNull()
                        database.chapterQueries.insertOrUpdateChapter(
                            id = existingCh?.id,
                            mangaId = mangaId,
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
                    val updated = database.chapterQueries.getChaptersByMangaUrl(mangaUrl).executeAsList().map { it.toSourceChapter() }
                    emit(updated)
                } catch (_: Exception) {
                    if (cached.isEmpty()) emit(emptyList())
                }
            }
        }

        return database.chapterQueries.getChaptersByMangaUrl(mangaUrl)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toSourceChapter() } }
    }

    override fun getPages(chapterUrl: String): Flow<List<SourcePage>> {
        val matchingDynamic = dynamicSources.values.filterIsInstance<ComicSource>().firstOrNull { src ->
            chapterUrl.startsWith(src.baseUrl) || (chapterUrl.length == 36 && chapterUrl.contains("-"))
        }

        if (matchingDynamic != null) {
            return flow {
                val pages = try {
                    matchingDynamic.getPageList(SourceChapter(url = chapterUrl, name = ""))
                } catch (_: Exception) {
                    emptyList()
                }
                emit(pages)
            }
        }

        val pages = (1..6).map { i ->
            SourcePage(index = i, url = "page_$i", imageUrl = "webtoon_panel_$i")
        }
        return flowOf(pages)
    }

    override fun getNovelContent(chapterUrl: String): Flow<NovelContent?> {
        val matchingDynamic = dynamicSources.values.filterIsInstance<NovelSource>().firstOrNull { src ->
            chapterUrl.startsWith(src.baseUrl)
        }

        if (matchingDynamic != null) {
            return flow {
                val content = try {
                    matchingDynamic.getChapterContent(SourceChapter(url = chapterUrl, name = ""))
                } catch (_: Exception) {
                    null
                }
                emit(content)
            }
        }

        return flowOf(
            NovelContent(
                chapterUrl = chapterUrl,
                title = "The Wind Rises",
                htmlBody = "<p>The morning light filtered through the leaves...</p>",
                plainText = "The morning light filtered through the leaves, casting moving shadows on the stone path."
            )
        )
    }

    override fun getTrending(): Flow<List<SourceManga>> {
        return database.mangaQueries.getLibrary()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.take(3).map { it.toSourceManga() } }
    }

    override fun getSources(): Flow<List<MockSourceInfo>> {
        val dynamicList = dynamicSources.values.map { src ->
            val type = if (src is NovelSource && src !is ComicSource) "Novel" else "Manga"
            MockSourceInfo(
                id = (src as? core.extensions.js.JsExtensionSource)?.manifest?.id ?: src.id.toString(),
                name = src.name,
                type = type,
                languages = listOf(src.lang.uppercase()),
                iconUrl = (src as? core.extensions.js.JsExtensionSource)?.manifest?.iconUrl
            )
        }
        return flowOf(dynamicList)
    }

    override fun getExtensions(): Flow<List<MockExtension>> {
        return flowOf(emptyList())
    }

    override fun getUpdates(): Flow<List<MockUpdateGroup>> {
        return flowOf(emptyList())
    }

    override fun getHistory(): Flow<List<HistoryGroup>> {
        return database.historyQueries.getRecentHistory(limit = 100L)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                if (list.isEmpty()) return@map emptyList()

                val now = currentTimeMillis()
                val oneDay = 24 * 60 * 60 * 1000L
                val twoDays = 48 * 60 * 60 * 1000L

                val todayItems = mutableListOf<HistoryItem>()
                val yesterdayItems = mutableListOf<HistoryItem>()
                val earlierItems = mutableListOf<HistoryItem>()

                for (row in list) {
                    val manga = SourceManga(
                        url = row.manga_url,
                        title = row.manga_title,
                        thumbnailUrl = row.manga_thumbnail,
                        isNovel = row.manga_is_novel == 1L
                    )
                    val chapter = SourceChapter(
                        url = row.chapter_url,
                        name = row.chapter_name,
                        chapterNumber = row.chapter_number.toFloat()
                    )
                    val item = HistoryItem(manga, chapter, row.read_at)
                    val diff = now - row.read_at
                    when {
                        diff < oneDay -> todayItems.add(item)
                        diff < twoDays -> yesterdayItems.add(item)
                        else -> earlierItems.add(item)
                    }
                }

                val groups = mutableListOf<HistoryGroup>()
                if (todayItems.isNotEmpty()) groups.add(HistoryGroup("Today", todayItems))
                if (yesterdayItems.isNotEmpty()) groups.add(HistoryGroup("Yesterday", yesterdayItems))
                if (earlierItems.isNotEmpty()) groups.add(HistoryGroup("Earlier", earlierItems))
                groups
            }
    }

    override fun getDownloads(): Flow<List<MockDownloadItem>> {
        return flowOf(emptyList())
    }

    override fun getStorageInfo(): Flow<MockStorageInfo> {
        return flowOf(MockStorageInfo())
    }

    override fun searchManga(
        query: String,
        sourceId: String?,
        filters: BrowseFilters
    ): Flow<List<SourceManga>> {
        val matchingDynamic = dynamicSources.values.firstOrNull {
            it.id.toString() == sourceId ||
            it.name.equals(sourceId, ignoreCase = true) ||
            (it as? core.extensions.js.JsExtensionSource)?.manifest?.id.equals(sourceId, ignoreCase = true)
        }
        if (matchingDynamic != null) {
            return flow {
                val results = try {
                    matchingDynamic.search(query, 1, emptyList()).mangas
                } catch (_: Exception) {
                    emptyList()
                }
                emit(results)
            }
        }

        return database.mangaQueries.getLibrary()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map { it.toSourceManga() }
                    .filter { it.title.contains(query, ignoreCase = true) }
            }
    }

    override fun getSourceCatalogue(
        sourceId: String,
        page: Int,
        query: String,
        filters: BrowseFilters
    ): Flow<List<SourceManga>> {
        val matchingDynamic = dynamicSources.values.firstOrNull {
            it.id.toString() == sourceId ||
            it.name.equals(sourceId, ignoreCase = true) ||
            (it as? core.extensions.js.JsExtensionSource)?.manifest?.id.equals(sourceId, ignoreCase = true)
        }
        if (matchingDynamic != null) {
            return flow {
                val results = try {
                    if (query.isBlank()) {
                        matchingDynamic.getPopular(page).mangas
                    } else {
                        matchingDynamic.search(query, page, emptyList()).mangas
                    }
                } catch (_: Exception) {
                    emptyList()
                }
                emit(results)
            }
        }
        return flowOf(emptyList())
    }

    override fun registerSource(source: ContentSource) {
        dynamicSources[source.id] = source
    }

    override fun unregisterSource(sourceId: Long) {
        dynamicSources.remove(sourceId)
    }

    override fun getRegisteredSources(): List<ContentSource> = dynamicSources.values.toList()

    override suspend fun toggleFavorite(manga: SourceManga, isFavorite: Boolean) {
        withContext(Dispatchers.Default) {
            insertOrUpdateMangaInternal(manga, sourceId = 0L, forceFavorite = isFavorite)
        }
    }

    override suspend fun updateChapterProgress(chapterUrl: String, page: Int, isRead: Boolean) {
        withContext(Dispatchers.Default) {
            database.chapterQueries.updateChapterProgress(
                read = if (isRead) 1L else 0L,
                lastPageRead = page.toLong(),
                lastModifiedAt = currentTimeMillis(),
                url = chapterUrl
            )
            val chapter = database.chapterQueries.getChapterByUrl(chapterUrl).executeAsOneOrNull()
            if (chapter != null) {
                database.historyQueries.upsertHistory(
                    chapterId = chapter.id,
                    readAt = currentTimeMillis(),
                    timeRead = 60L
                )
            }
        }
    }

    override suspend fun recordHistory(chapterUrl: String) {
        withContext(Dispatchers.Default) {
            val chapter = database.chapterQueries.getChapterByUrl(chapterUrl).executeAsOneOrNull()
            if (chapter != null) {
                database.historyQueries.upsertHistory(
                    chapterId = chapter.id,
                    readAt = currentTimeMillis(),
                    timeRead = 30L
                )
            }
        }
    }

    private fun Mangas.toSourceManga(): SourceManga = SourceManga(
        url = url,
        title = title,
        artist = artist,
        author = author,
        description = description,
        genres = genres?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
        status = try { MangaStatus.valueOf(status) } catch (_: Exception) { MangaStatus.UNKNOWN },
        thumbnailUrl = thumbnail_url,
        isNovel = is_novel == 1L
    )

    private fun Chapters.toSourceChapter(): SourceChapter = SourceChapter(
        url = url,
        name = name,
        dateUpload = date_upload,
        chapterNumber = chapter_number.toFloat(),
        scanlator = scanlator
    )

    private fun GetContinueReading.toPair(): Pair<SourceManga, SourceChapter> {
        val manga = SourceManga(
            url = manga_url,
            title = manga_title,
            artist = manga_artist,
            author = manga_author,
            description = manga_description,
            genres = manga_genres?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
            status = try { MangaStatus.valueOf(manga_status) } catch (_: Exception) { MangaStatus.UNKNOWN },
            thumbnailUrl = manga_thumbnail,
            isNovel = manga_is_novel == 1L
        )
        val chapter = SourceChapter(
            url = chapter_url,
            name = chapter_name,
            chapterNumber = chapter_number.toFloat(),
            dateUpload = chapter_date_upload
        )
        return Pair(manga, chapter)
    }
}
