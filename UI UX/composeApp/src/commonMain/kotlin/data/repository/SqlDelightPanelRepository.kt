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
import core.source.BrowseFilters
import core.source.BrowseSortOrder
import core.source.SourceInfo
import core.source.SourceManga
import core.source.SourcePage
import core.source.StorageInfo
import data.mock.HistoryGroup
import data.mock.HistoryItem
import data.mock.LibraryMangaStats
import data.mock.MockDownloadItem
import data.mock.MockExtension
import data.mock.MockSourceInfo
import data.mock.MockStorageInfo
import data.mock.MockUpdateGroup
import data.mock.MockUpdateItem
import data.mock.PanelRepository
import data.backup.BackupEngine
import io.panel.reader.db.Chapters
import io.panel.reader.db.GetContinueReading
import io.panel.reader.db.Mangas
import io.panel.reader.db.PanelDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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

    override fun getLibraryStats(): Flow<Map<String, LibraryMangaStats>> {
        return database.mangaQueries.getLibraryMangaStats()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.associate { row ->
                    row.manga_url to LibraryMangaStats(
                        mangaUrl = row.manga_url,
                        totalChapters = row.total_chapters.toInt(),
                        readChapters = (row.read_chapters ?: 0L).toInt(),
                        lastReadAt = row.last_read_at ?: 0L,
                        dateAdded = row.date_added
                    )
                }
            }
    }

    override fun getContinueReading(): Flow<Pair<SourceManga, SourceChapter>?> {
        return database.historyQueries.getContinueReading()
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toPair() }
    }

    private fun matchSourceById(registered: Map<Long, ContentSource>, sourceId: String): ContentSource? {
        return registered[sourceId.toLongOrNull()] ?: registered.values.firstOrNull {
            it.id.toString() == sourceId ||
            it.name.equals(sourceId, ignoreCase = true) ||
            (it as? core.extensions.js.JsExtensionSource)?.manifest?.id.equals(sourceId, ignoreCase = true) ||
            (it as? core.extensions.mangayomi.MangayomiSourceAdapter)?.manifest?.id?.toString() == sourceId ||
            (it as? core.extensions.paperback.PaperbackSourceAdapter)?.manifest?.id.equals(sourceId, ignoreCase = true) ||
            (it as? core.extensions.lnreader.LNReaderSourceAdapter)?.manifest?.id.equals(sourceId, ignoreCase = true)
        }
    }

    override fun resolveSource(
        sourceId: String?,
        mangaUrl: String?,
        chapterUrl: String?
    ): ContentSource? {
        val registered = _dynamicSources.value
        if (registered.isEmpty()) return null

        // Explicit routing takes priority when sourceId is known.
        if (!sourceId.isNullOrBlank() && sourceId != "0") {
            return matchSourceById(registered, sourceId)
        }

        // Fallback resolution only when sourceId is missing/0L.
        return chapterUrl?.takeIf { it.isNotBlank() }?.let { findMatchingDynamicSource(it, null) }
            ?: mangaUrl?.takeIf { it.isNotBlank() }?.let { findMatchingDynamicSource(it, null) }
    }

    private fun findMatchingDynamicSource(url: String, sourceId: String?): ContentSource? {
        val registered = _dynamicSources.value
        if (registered.isEmpty()) return null

        // 1. Strict direct routing via explicit sourceId parameter
        if (!sourceId.isNullOrBlank() && sourceId != "0") {
            return matchSourceById(registered, sourceId)
        }

        // 2. Routing via existing database record
        val mangaRecord = database.mangaQueries.getMangaByUrl(url).executeAsOneOrNull()
        if (mangaRecord != null) {
            val fromDb = registered[mangaRecord.source_id]
            if (fromDb != null) return fromDb
        }

        // 3. Routing via chapter record if url is a chapter URL
        val chapterRecord = database.chapterQueries.getChapterByUrl(url).executeAsOneOrNull()
        if (chapterRecord != null) {
            val manga = database.mangaQueries.getMangaById(chapterRecord.manga_id).executeAsOneOrNull()
            if (manga != null) {
                val fromDb = registered[manga.source_id]
                if (fromDb != null) return fromDb
            }
        }

        // 4. Exact prefix match against source baseUrl
        val fromBaseUrl = registered.values.firstOrNull { src ->
            url.startsWith(src.baseUrl)
        }
        if (fromBaseUrl != null) return fromBaseUrl

        // 5. Host/domain match if url is absolute
        val fromDomain = registered.values.firstOrNull { src ->
            val host = src.baseUrl.removePrefix("https://").removePrefix("http://").split("/").firstOrNull() ?: ""
            host.isNotEmpty() && url.contains(host)
        }
        if (fromDomain != null) return fromDomain

        // 6. Single installed source fallback
        if (registered.size == 1) {
            return registered.values.first()
        }

        return null
    }

    override fun getMangaDetails(url: String, sourceId: String?, fallbackTitle: String): Flow<SourceManga?> {
        val mangaRecord = database.mangaQueries.getMangaByUrl(url).executeAsOneOrNull()
        val matchingDynamic = findMatchingDynamicSource(url, sourceId)

        if (matchingDynamic != null) {
            return flow {
                val cached = mangaRecord?.toSourceManga()
                if (cached != null) {
                    emit(cached)
                } else if (fallbackTitle.isNotBlank()) {
                    emit(SourceManga(url = url, title = fallbackTitle, sourceId = matchingDynamic.id))
                }

                try {
                    val fresh = matchingDynamic.getMangaDetails(
                        cached ?: SourceManga(url = url, title = fallbackTitle, sourceId = matchingDynamic.id)
                    )
                    val effectiveFresh = if (fresh.title.isBlank() && fallbackTitle.isNotBlank()) {
                        fresh.copy(title = fallbackTitle, sourceId = matchingDynamic.id)
                    } else {
                        if (fresh.sourceId == 0L) fresh.copy(sourceId = matchingDynamic.id) else fresh
                    }
                    insertOrUpdateMangaInternal(effectiveFresh, sourceId = matchingDynamic.id, forceFavorite = null)
                    emit(effectiveFresh)
                } catch (e: Exception) {
                    println("[SqlDelightPanelRepository] Error getting details for $url from source ${matchingDynamic.name}: ${e.message}")
                    if (cached == null && fallbackTitle.isBlank()) emit(null)
                }
            }.flowOn(Dispatchers.Default)
        }

        return database.mangaQueries.getMangaByUrl(url)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toSourceManga() ?: if (fallbackTitle.isNotBlank()) SourceManga(url = url, title = fallbackTitle) else null }
    }

    override fun getChapters(mangaUrl: String, sourceId: String?): Flow<List<SourceChapter>> {
        val mangaRecord = database.mangaQueries.getMangaByUrl(mangaUrl).executeAsOneOrNull()
        val matchingDynamic = findMatchingDynamicSource(mangaUrl, sourceId)

        if (matchingDynamic != null) {
            return flow {
                val cached = mangaRecord?.let {
                    database.chapterQueries.getChaptersByMangaId(it.id).executeAsList().map { ch -> ch.toSourceChapter() }
                } ?: database.chapterQueries.getChaptersByMangaUrl(mangaUrl).executeAsList().map { it.toSourceChapter() }
                if (cached.isNotEmpty()) emit(cached)

                try {
                    val fresh = matchingDynamic.getChapterList(SourceManga(url = mangaUrl, title = "", sourceId = matchingDynamic.id))
                    val mangaId = mangaRecord?.id
                        ?: insertOrUpdateMangaInternal(SourceManga(url = mangaUrl, title = "", sourceId = matchingDynamic.id), matchingDynamic.id, forceFavorite = null)

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
                    val updated = database.chapterQueries.getChaptersByMangaId(mangaId).executeAsList().map { it.toSourceChapter() }
                    emit(updated)
                } catch (e: Exception) {
                    println("[SqlDelightPanelRepository] Error getting chapters for $mangaUrl from source ${matchingDynamic.name}: ${e.message}")
                    if (cached.isEmpty()) emit(emptyList())
                }
            }.flowOn(Dispatchers.Default)
        }

        return database.chapterQueries.getChaptersByMangaUrl(mangaUrl)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toSourceChapter() } }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getPages(chapterUrl: String, sourceId: String?): Flow<List<SourcePage>> {
        val downloadedPages = downloadStorage?.getDownloadedPages(chapterUrl)
        if (!downloadedPages.isNullOrEmpty()) {
            return flowOf(downloadedPages)
        }

        return _dynamicSources
            .filter { it.isNotEmpty() }
            .flatMapLatest { _ ->
                flow {
                    val matching = findMatchingDynamicSource(chapterUrl, sourceId) as? ComicSource
                    if (matching != null) {
                        val pages = try {
                            matching.getPageList(SourceChapter(url = chapterUrl, name = ""))
                        } catch (e: Exception) {
                            println("[SqlDelightPanelRepository] Exception in getPageList for $chapterUrl: ${e.message}")
                            e.printStackTrace()
                            emptyList()
                        }
                        emit(pages)
                    } else {
                        emit(emptyList())
                    }
                }
            }.flowOn(Dispatchers.Default)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getNovelContent(chapterUrl: String, sourceId: String?): Flow<NovelContent?> {
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

        return _dynamicSources
            .filter { it.isNotEmpty() }
            .flatMapLatest { _ ->
                flow {
                    val matching = findMatchingDynamicSource(chapterUrl, sourceId) as? NovelSource
                    if (matching != null) {
                        val content = try {
                            matching.getChapterContent(SourceChapter(url = chapterUrl, name = ""))
                        } catch (_: Exception) {
                            null
                        }
                        emit(content)
                    } else {
                        emit(null)
                    }
                }
            }.flowOn(Dispatchers.Default)
    }

    override fun getAdaptation(manga: SourceManga): Flow<SourceManga?> {
        val cleanTitle = manga.title
            .replace(Regex("""(?i)\s*(\(Novel\)|\(Manhwa\)|\(Manga\)|\(Webtoon\)|\[Novel\]|\[Comic\])"""), "")
            .trim()
        val targetIsNovel = !manga.isNovel
        val matches = runCatching {
            database.mangaQueries.searchMangaByTitle(cleanTitle).executeAsList()
        }.getOrDefault(emptyList())
        val counterpart = matches.firstOrNull { it.is_novel == (if (targetIsNovel) 1L else 0L) && it.url != manga.url }?.toSourceManga()
        return flowOf(counterpart)
    }

    override fun getTrending(): Flow<List<SourceManga>> {
        val sources = _dynamicSources.value.values.toList()
        if (sources.isNotEmpty()) {
            return flow {
                val results = mutableListOf<SourceManga>()
                for (src in sources) {
                    try {
                        val popular = src.getPopular(1).mangas.take(5).map { manga ->
                            if (manga.sourceId == 0L) manga.copy(sourceId = src.id) else manga
                        }
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
                        isNovel = row.manga_is_novel == 1L,
                        sourceId = row.manga_source_id
                    )
                    val chapter = SourceChapter(
                        url = row.chapter_url,
                        name = row.chapter_name,
                        chapterNumber = row.chapter_number.toFloat()
                    )
                    val item = HistoryItem(
                        id = row.history_id,
                        manga = manga,
                        chapter = chapter,
                        readAt = row.read_at,
                        lastPageRead = row.last_page_read.toInt()
                    )
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

    override fun getRecentlyReadManga(limit: Long): Flow<List<HistoryItem>> {
        return database.historyQueries.getRecentlyReadManga(limit = limit)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map { row ->
                    val manga = SourceManga(
                        url = row.manga_url,
                        title = row.manga_title,
                        thumbnailUrl = row.manga_thumbnail,
                        author = row.manga_author,
                        artist = row.manga_artist,
                        description = row.manga_description,
                        genres = row.manga_genres?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
                        status = try { MangaStatus.valueOf(row.manga_status) } catch (_: Exception) { MangaStatus.UNKNOWN },
                        isNovel = row.manga_is_novel == 1L,
                        sourceId = row.manga_source_id
                    )
                    val chapter = SourceChapter(
                        url = row.chapter_url,
                        name = row.chapter_name,
                        dateUpload = row.chapter_date_upload,
                        chapterNumber = row.chapter_number.toFloat()
                    )
                    HistoryItem(
                        id = 0L,
                        manga = manga,
                        chapter = chapter,
                        readAt = row.last_read_at ?: 0L,
                        lastPageRead = row.chapter_last_page_read.toInt()
                    )
                }
            }
    }

    override suspend fun deleteHistoryItem(historyId: Long) {
        withContext(Dispatchers.Default) {
            database.historyQueries.deleteHistoryById(historyId)
        }
    }

    override suspend fun clearAllHistory() {
        withContext(Dispatchers.Default) {
            database.historyQueries.clearAllHistory()
        }
    }

    override fun getDownloads(): Flow<List<MockDownloadItem>> {
        return (downloadManager?.queue ?: flowOf(emptyList())).map { list ->
            list.map { job ->
                MockDownloadItem(
                    id = job.id,
                    manga = job.manga,
                    chapter = job.chapter,
                    progress = job.progress,
                    downloadedBytes = job.downloadedBytes,
                    totalBytes = job.totalBytes,
                    etaMinutes = if (job.etaSeconds > 0) (job.etaSeconds + 59) / 60 else 0,
                    isPaused = job.isPaused
                )
            }
        }
    }

    override fun getStorageInfo(): Flow<MockStorageInfo> {
        return downloadManager?.storageInfo ?: flowOf(MockStorageInfo())
    }

    override fun enqueueDownload(manga: SourceManga, chapter: SourceChapter) {
        val sid = manga.sourceId.takeIf { it != 0L }?.toString()
        val source = resolveSource(sid, manga.url, chapter.url)
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

    // ── Backup / Restore ────────────────────────────────────────────────────

    override suspend fun exportLibraryJson(): String =
        BackupEngine.exportJson(this, nowMs = currentTimeMillis())

    override suspend fun importLibraryJson(json: String): Int =
        BackupEngine.importJson(json, this)

    override fun searchManga(
        query: String,
        sourceId: String?,
        filters: BrowseFilters
    ): Flow<List<SourceManga>> {
        val matchingDynamic = findMatchingDynamicSource(query, sourceId)
        if (matchingDynamic != null) {
            return flow {
                val results = try {
                    matchingDynamic.search(query, 1, emptyList()).mangas.map { manga ->
                        if (manga.sourceId == 0L) manga.copy(sourceId = matchingDynamic.id) else manga
                    }
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
                coroutineScope {
                    val deferreds = sources.map { src ->
                        async(Dispatchers.Default) {
                            try {
                                withTimeoutOrNull(8000L) {
                                    src.search(query, 1, emptyList()).mangas.map { manga ->
                                        if (manga.sourceId == 0L) manga.copy(sourceId = src.id) else manga
                                    }
                                } ?: emptyList()
                            } catch (_: Exception) {
                                emptyList()
                            }
                        }
                    }
                    deferreds.awaitAll().forEach { remoteResults.addAll(it) }
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
        val matchingDynamic = findMatchingDynamicSource("", sourceId)
        if (matchingDynamic != null) {
            return flow {
                val results: List<SourceManga> = try {
                    val pageResult = if (query.isNotBlank()) {
                        matchingDynamic.search(query, page, emptyList()).mangas
                    } else if (filters.sortBy == BrowseSortOrder.LATEST) {
                        matchingDynamic.getLatestUpdates(page).mangas
                    } else {
                        matchingDynamic.getPopular(page).mangas
                    }
                    println("[getSourceCatalogue] source $sourceId returned ${pageResult.size} mangas")
                    pageResult.map { manga ->
                        if (manga.sourceId == 0L) manga.copy(sourceId = matchingDynamic.id) else manga
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    println("[getSourceCatalogue] Error for source $sourceId: ${e.message}")
                    emptyList()
                }
                emit(results)
            }.flowOn(Dispatchers.Default)
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
            val preservedSourceId = existing?.source_id?.takeIf { it != 0L } ?: manga.sourceId
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
        isNovel = is_novel == 1L,
        sourceId = source_id
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
            isNovel = manga_is_novel == 1L,
            sourceId = manga_source_id
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
