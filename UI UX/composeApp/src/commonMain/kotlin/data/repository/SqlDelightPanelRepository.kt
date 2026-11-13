package data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import core.download.DownloadManager
import core.download.DownloadStorage
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
import data.mock.BrowseSortOrder
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Production local persistent repository backed by SQLite via CashApp SQLDelight.
 * Replaces FakePanelRepository with real database queries, reactive Flows, and
 * seamless integration with dynamically registered extensions.
 */
class SqlDelightPanelRepository(
    private val database: PanelDatabase,
    private val downloadManager: DownloadManager? = null,
    private val downloadStorage: DownloadStorage? = null
) : PanelRepository {

    private val _dynamicSources = MutableStateFlow<Map<Long, ContentSource>>(emptyMap())
    val dynamicSources: StateFlow<Map<Long, ContentSource>> = _dynamicSources.asStateFlow()

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
        val mangaRecord = database.mangaQueries.getMangaByUrl(url).executeAsOneOrNull()
        val sourceFromDb = mangaRecord?.let { _dynamicSources.value[it.source_id] }
        val matchingDynamic = sourceFromDb
            ?: _dynamicSources.value.values.firstOrNull { src ->
                url.startsWith(src.baseUrl) || (url.length == 36 && url.contains("-"))
            }
            ?: _dynamicSources.value.values.firstOrNull()

        if (matchingDynamic != null) {
            return flow {
                val cached = mangaRecord?.toSourceManga()
                if (cached != null) emit(cached)

                try {
                    val fresh = matchingDynamic.getMangaDetails(SourceManga(url = url, title = ""))
                    insertOrUpdateMangaInternal(fresh, sourceId = matchingDynamic.id, forceFavorite = null)
                    emit(fresh)
                } catch (_: Exception) {
                    if (cached == null) emit(null)
                }
            }.flowOn(Dispatchers.Default)
        }

        return database.mangaQueries.getMangaByUrl(url)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toSourceManga() }
    }

    override fun getChapters(mangaUrl: String): Flow<List<SourceChapter>> {
        val mangaRecord = database.mangaQueries.getMangaByUrl(mangaUrl).executeAsOneOrNull()
        val sourceFromDb = mangaRecord?.let { _dynamicSources.value[it.source_id] }
        val matchingDynamic = sourceFromDb
            ?: _dynamicSources.value.values.firstOrNull { src ->
                mangaUrl.startsWith(src.baseUrl) || (mangaUrl.length == 36 && mangaUrl.contains("-"))
            }
            ?: _dynamicSources.value.values.firstOrNull()

        if (matchingDynamic != null) {
            return flow {
                val cached = database.chapterQueries.getChaptersByMangaUrl(mangaUrl).executeAsList().map { it.toSourceChapter() }
                if (cached.isNotEmpty()) emit(cached)

                try {
                    val fresh = matchingDynamic.getChapterList(SourceManga(url = mangaUrl, title = ""))
                    val mangaId = mangaRecord?.id
                        ?: insertOrUpdateMangaInternal(SourceManga(url = mangaUrl, title = ""), matchingDynamic.id, forceFavorite = null)

                    database.transaction {
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
                    }
                    val updated = database.chapterQueries.getChaptersByMangaUrl(mangaUrl).executeAsList().map { it.toSourceChapter() }
                    emit(updated)
                } catch (_: Exception) {
                    if (cached.isEmpty()) emit(emptyList())
                }
            }.flowOn(Dispatchers.Default)
        }

        return database.chapterQueries.getChaptersByMangaUrl(mangaUrl)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toSourceChapter() } }
    }

    override fun getPages(chapterUrl: String): Flow<List<SourcePage>> {
        val downloadedPages = downloadStorage?.getDownloadedPages(chapterUrl)
        if (!downloadedPages.isNullOrEmpty()) {
            return flowOf(downloadedPages)
        }

        val chapterRecord = database.chapterQueries.getChapterByUrl(chapterUrl).executeAsOneOrNull()
        val sourceFromDb = chapterRecord?.let { ch ->
            database.mangaQueries.getMangaById(ch.manga_id).executeAsOneOrNull()?.let { manga ->
                _dynamicSources.value[manga.source_id]
            }
        }

        val matchingDynamic = (sourceFromDb as? ComicSource)
            ?: _dynamicSources.value.values.filterIsInstance<ComicSource>().firstOrNull { src ->
                chapterUrl.startsWith(src.baseUrl) || (chapterUrl.length == 36 && chapterUrl.contains("-"))
            }
            ?: _dynamicSources.value.values.filterIsInstance<ComicSource>().firstOrNull()

        if (matchingDynamic != null) {
            return flow {
                val pages = try {
                    matchingDynamic.getPageList(SourceChapter(url = chapterUrl, name = ""))
                } catch (_: Exception) {
                    emptyList()
                }
                emit(pages)
            }.flowOn(Dispatchers.Default)
        }

        return flowOf(emptyList())
    }

    override fun getNovelContent(chapterUrl: String): Flow<NovelContent?> {
        val downloadedNovel = downloadStorage?.getDownloadedNovel(chapterUrl)
        if (downloadedNovel != null) {
            return flowOf(
                NovelContent(
                    chapterUrl = chapterUrl,
                    title = downloadedNovel.first,
                    htmlBody = downloadedNovel.second,
                    plainText = downloadedNovel.second
                )
            )
        }

        val chapterRecord = database.chapterQueries.getChapterByUrl(chapterUrl).executeAsOneOrNull()
        val sourceFromDb = chapterRecord?.let { ch ->
            database.mangaQueries.getMangaById(ch.manga_id).executeAsOneOrNull()?.let { manga ->
                _dynamicSources.value[manga.source_id]
            }
        }

        val matchingDynamic = (sourceFromDb as? NovelSource)
            ?: _dynamicSources.value.values.filterIsInstance<NovelSource>().firstOrNull { src ->
                chapterUrl.startsWith(src.baseUrl)
            }
            ?: _dynamicSources.value.values.filterIsInstance<NovelSource>().firstOrNull()

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
        val sources = _dynamicSources.value.values.toList()
        if (sources.isNotEmpty()) {
            return flow {
                val results = mutableListOf<SourceManga>()
                for (src in sources) {
                    try {
                        val popular = src.getPopular(1).mangas.take(5)
                        results.addAll(popular)
                    } catch (_: Exception) {
                    }
                }
                if (results.isNotEmpty()) {
                    emit(results.distinctBy { it.url })
                    return@flow
                }
                val local = database.mangaQueries.getLibrary().executeAsList().take(3).map { it.toSourceManga() }
                emit(local)
            }
        }
        return database.mangaQueries.getLibrary()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.take(3).map { it.toSourceManga() } }
    }

    override fun getSources(): Flow<List<MockSourceInfo>> {
        return _dynamicSources.map { map ->
            map.values.map { src ->
                val type = if (src is NovelSource && src !is ComicSource) "Novel" else "Manga"
                val iconUrl = when (src) {
                    is core.extensions.js.JsExtensionSource -> src.manifest.iconUrl
                    is core.extensions.mangayomi.MangayomiSourceAdapter -> src.manifest.iconUrl
                    is core.extensions.lnreader.LNReaderSourceAdapter -> src.manifest.iconUrl
                    is core.extensions.paperback.PaperbackSourceAdapter -> src.manifest.iconUrl
                    else -> null
                }
                MockSourceInfo(
                    id = (src as? core.extensions.js.JsExtensionSource)?.manifest?.id ?: src.id.toString(),
                    name = src.name,
                    type = type,
                    languages = listOf(src.lang.uppercase()),
                    iconUrl = iconUrl
                )
            }
        }
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
        return downloadManager?.queue ?: flowOf(emptyList())
    }

    override fun getStorageInfo(): Flow<MockStorageInfo> {
        return downloadManager?.storageInfo ?: flowOf(MockStorageInfo())
    }

    override fun enqueueDownload(manga: SourceManga, chapter: SourceChapter) {
        val source = _dynamicSources.value.values.firstOrNull { src ->
            chapter.url.startsWith(src.baseUrl) ||
            manga.url.startsWith(src.baseUrl) ||
            (chapter.url.length == 36 && chapter.url.contains("-"))
        }
        downloadManager?.enqueueDownload(manga, chapter, source)
    }

    override fun toggleDownloadPause(chapterUrl: String) {
        downloadManager?.togglePause(chapterUrl)
    }

    override fun cancelDownload(chapterUrl: String) {
        downloadManager?.cancelDownload(chapterUrl)
    }

    override fun isChapterDownloaded(chapterUrl: String): Boolean {
        return downloadStorage?.isChapterDownloaded(chapterUrl) ?: false
    }

    override fun searchManga(
        query: String,
        sourceId: String?,
        filters: BrowseFilters
    ): Flow<List<SourceManga>> {
        val matchingDynamic = _dynamicSources.value.values.firstOrNull {
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

        // Global search across all registered dynamic sources + local library
        val sources = _dynamicSources.value.values.toList()
        if (sources.isNotEmpty()) {
            return flow {
                val remoteResults = mutableListOf<SourceManga>()
                for (src in sources) {
                    try {
                        val res = src.search(query, 1, emptyList()).mangas
                        remoteResults.addAll(res)
                    } catch (_: Exception) {
                    }
                }
                val localResults = try {
                    database.mangaQueries.getLibrary().executeAsList()
                        .map { it.toSourceManga() }
                        .filter { it.title.contains(query, ignoreCase = true) }
                } catch (_: Exception) {
                    emptyList()
                }
                val combined = (remoteResults + localResults).distinctBy { it.url }
                emit(combined)
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
        val matchingDynamic = _dynamicSources.value.values.firstOrNull {
            it.id.toString() == sourceId ||
            it.name.equals(sourceId, ignoreCase = true) ||
            (it as? core.extensions.js.JsExtensionSource)?.manifest?.id.equals(sourceId, ignoreCase = true)
        }
        if (matchingDynamic != null) {
            return flow {
                val results = try {
                    if (query.isNotBlank()) {
                        matchingDynamic.search(query, page, emptyList()).mangas
                    } else if (filters.sortBy == BrowseSortOrder.LATEST) {
                        matchingDynamic.getLatestUpdates(page).mangas
                    } else {
                        matchingDynamic.getPopular(page).mangas
                    }
                } catch (e: Exception) {
                    println("[getSourceCatalogue] Error for source $sourceId: ${e.message}")
                    emptyList()
                }
                emit(results)
            }
        }
        return flowOf(emptyList())
    }

    override fun registerSource(source: ContentSource) {
        _dynamicSources.value = _dynamicSources.value + (source.id to source)
    }

    override fun unregisterSource(sourceId: Long) {
        _dynamicSources.value = _dynamicSources.value - sourceId
    }

    override fun getRegisteredSources(): List<ContentSource> = _dynamicSources.value.values.toList()

    override suspend fun toggleFavorite(manga: SourceManga, isFavorite: Boolean) {
        withContext(Dispatchers.Default) {
            val existing = database.mangaQueries.getMangaByUrl(manga.url).executeAsOneOrNull()
            val preservedSourceId = existing?.source_id ?: 0L
            insertOrUpdateMangaInternal(manga, sourceId = preservedSourceId, forceFavorite = isFavorite)
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
