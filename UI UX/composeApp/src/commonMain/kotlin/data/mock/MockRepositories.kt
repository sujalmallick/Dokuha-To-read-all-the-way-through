package data.mock

import core.source.MangaStatus
import core.source.NovelContent
import core.source.SourceChapter
import core.source.SourceManga
import core.source.SourcePage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

data class MockExtension(
    val id: String,
    val name: String,
    val version: String,
    val lang: String,
    val isInstalled: Boolean,
    val hasUpdate: Boolean,
    val iconUrl: String? = null
)

data class MockUpdateItem(
    val manga: SourceManga,
    val chapter: SourceChapter,
    val isDownloaded: Boolean = false
)

data class MockUpdateGroup(
    val title: String,
    val items: List<MockUpdateItem>
)

data class MockDownloadItem(
    val id: String,
    val manga: SourceManga,
    val chapter: SourceChapter,
    val progress: Float, // 0f..1f
    val downloadedBytes: Long,
    val totalBytes: Long,
    val etaMinutes: Int,
    val isPaused: Boolean = false
)

data class MockStorageInfo(
    val totalGb: Float = 64f,
    val usedGb: Float = 2.4f,
    val mangaMb: Float = 1600f,
    val manhwaMb: Float = 512f,
    val novelsMb: Float = 208f,
    val othersMb: Float = 136f
)

enum class BrowseSortOrder(val displayName: String) {
    POPULARITY("Popularity"),
    LATEST("Latest"),
    ALPHABETICAL("A-Z")
}

data class BrowseFilters(
    val genres: Set<String> = emptySet(),
    val status: MangaStatus? = null,
    val isNovel: Boolean? = null,
    val sortBy: BrowseSortOrder = BrowseSortOrder.POPULARITY
)

val AvailableBrowseGenres = listOf(
    "Action",
    "Adventure",
    "Cyberpunk",
    "Dark Fantasy",
    "Drama",
    "Fantasy",
    "Martial Arts",
    "Mystery",
    "Psychological",
    "Reincarnation",
    "Romance",
    "Sci-Fi",
    "Supernatural"
)

interface PanelRepository {
    fun getLibrary(category: String): Flow<List<SourceManga>>
    fun getContinueReading(): Flow<Pair<SourceManga, SourceChapter>?>
    fun getMangaDetails(url: String): Flow<SourceManga?>
    fun getChapters(mangaUrl: String): Flow<List<SourceChapter>>
    fun getPages(chapterUrl: String): Flow<List<SourcePage>>
    fun getNovelContent(chapterUrl: String): Flow<NovelContent?>
    fun getTrending(): Flow<List<SourceManga>>
    fun getSources(): Flow<List<MockSourceInfo>>
    fun getExtensions(): Flow<List<MockExtension>>
    fun getUpdates(): Flow<List<MockUpdateGroup>>
    fun getDownloads(): Flow<List<MockDownloadItem>>
    fun getStorageInfo(): Flow<MockStorageInfo>
    fun searchManga(
        query: String = "",
        sourceId: String? = null,
        filters: BrowseFilters = BrowseFilters()
    ): Flow<List<SourceManga>>
    fun getSourceCatalogue(
        sourceId: String,
        page: Int = 1,
        query: String = "",
        filters: BrowseFilters = BrowseFilters()
    ): Flow<List<SourceManga>>

    fun registerSource(source: core.source.ContentSource) {}
    fun unregisterSource(sourceId: Long) {}
    fun getRegisteredSources(): List<core.source.ContentSource> = emptyList()
}

data class MockSourceInfo(
    val id: String,
    val name: String,
    val type: String, // Manga, Manhwa, Novel
    val languages: List<String>,
    val iconUrl: String? = null
)

class FakePanelRepository : PanelRepository {

    private val lastHorizon = SourceManga(
        url = "manga/the-last-horizon",
        title = "The Last Horizon",
        author = "A. V. Frost",
        artist = "Studio Horizon",
        description = "In a world where the sky has split, a young man follows the last light to uncover the truth behind a forgotten era...",
        genres = listOf("Action", "Fantasy", "Adventure"),
        status = MangaStatus.ONGOING,
        thumbnailUrl = "horizon_cover",
        isNovel = false
    )

    private val soloLeveling = SourceManga(
        url = "manga/solo-leveling",
        title = "Solo Leveling",
        description = "10 years ago, after 'the Gate' that connected the real world with the monster world opened, some ordinary people received powers.",
        genres = listOf("Action", "Fantasy", "Supernatural"),
        status = MangaStatus.COMPLETED,
        thumbnailUrl = "solo_leveling_cover"
    )

    private val auroraBlade = SourceManga(
        url = "manga/aurora-blade",
        title = "Aurora Blade",
        genres = listOf("Fantasy", "Action"),
        status = MangaStatus.ONGOING,
        thumbnailUrl = "aurora_cover"
    )

    private val windBeneath = SourceManga(
        url = "manga/wind-beneath",
        title = "Wind Beneath",
        genres = listOf("Martial Arts", "Adventure"),
        status = MangaStatus.ONGOING,
        thumbnailUrl = "wind_cover"
    )

    private val shadowCrown = SourceManga(
        url = "manga/shadow-crown",
        title = "Shadow Crown",
        genres = listOf("Dark Fantasy", "Action"),
        status = MangaStatus.ONGOING,
        thumbnailUrl = "shadow_cover"
    )

    private val neonRequiem = SourceManga(
        url = "manga/neon-requiem",
        title = "Neon Requiem",
        genres = listOf("Sci-Fi", "Cyberpunk"),
        status = MangaStatus.ONGOING,
        thumbnailUrl = "neon_cover"
    )

    private val frostbound = SourceManga(
        url = "manga/frostbound",
        title = "Frostbound",
        genres = listOf("Fantasy", "Mystery"),
        status = MangaStatus.ONGOING,
        thumbnailUrl = "frostbound_cover"
    )

    private val velvetNight = SourceManga(
        url = "manga/velvet-night",
        title = "Velvet Night",
        genres = listOf("Romance", "Drama"),
        status = MangaStatus.ONGOING,
        thumbnailUrl = "velvet_cover"
    )

    private val crimsonOath = SourceManga(
        url = "manga/crimson-oath",
        title = "Crimson Oath",
        genres = listOf("Action", "Supernatural"),
        status = MangaStatus.ONGOING,
        thumbnailUrl = "crimson_cover"
    )

    private val silentWinter = SourceManga(
        url = "manga/silent-winter",
        title = "Silent Winter",
        genres = listOf("Mystery", "Psychological"),
        status = MangaStatus.ONGOING,
        thumbnailUrl = "silent_winter_cover"
    )

    private val tbate = SourceManga(
        url = "novel/tbate",
        title = "The Beginning After the End",
        author = "TurtleMe",
        genres = listOf("Action", "Fantasy", "Reincarnation"),
        status = MangaStatus.ONGOING,
        thumbnailUrl = "tbate_cover",
        isNovel = true
    )

    private val shadowSlave = SourceManga(
        url = "novel/shadow-slave",
        title = "Shadow Slave",
        author = "Guiltythree",
        genres = listOf("Dark Fantasy", "Adventure", "Supernatural"),
        status = MangaStatus.ONGOING,
        thumbnailUrl = "shadow_slave_cover",
        isNovel = true
    )

    private val omniscientReader = SourceManga(
        url = "manga/orv",
        title = "Omniscient Reader's Viewpoint",
        author = "sing N song",
        artist = "Sleepy-C",
        genres = listOf("Action", "Fantasy", "Supernatural", "Psychological"),
        status = MangaStatus.ONGOING,
        thumbnailUrl = "orv_cover",
        isNovel = false
    )

    private val allMangaList = listOf(
        lastHorizon,
        soloLeveling,
        auroraBlade,
        windBeneath,
        shadowCrown,
        neonRequiem,
        frostbound,
        velvetNight,
        crimsonOath,
        silentWinter,
        tbate,
        shadowSlave,
        omniscientReader
    )

    private val sourceMangaMap = mapOf(
        "1" to listOf(lastHorizon, soloLeveling, auroraBlade, velvetNight, silentWinter, tbate),
        "2" to listOf(soloLeveling, windBeneath, shadowCrown, crimsonOath, neonRequiem, omniscientReader),
        "3" to listOf(lastHorizon, windBeneath, frostbound, auroraBlade, shadowCrown, shadowSlave)
    )

    private val dynamicSources = mutableMapOf<Long, core.source.ContentSource>()

    override fun registerSource(source: core.source.ContentSource) {
        dynamicSources[source.id] = source
    }

    override fun unregisterSource(sourceId: Long) {
        dynamicSources.remove(sourceId)
    }

    override fun getRegisteredSources(): List<core.source.ContentSource> = dynamicSources.values.toList()

    override fun getLibrary(category: String): Flow<List<SourceManga>> {
        val list = listOf(
            soloLeveling,
            auroraBlade,
            windBeneath,
            shadowCrown,
            neonRequiem,
            frostbound
        )
        return flowOf(list)
    }

    override fun getContinueReading(): Flow<Pair<SourceManga, SourceChapter>?> {
        val chapter = SourceChapter(
            url = "manga/the-last-horizon/ch-210",
            name = "Ch. 210",
            chapterNumber = 210f,
            dateUpload = 1718409600000L
        )
        return flowOf(Pair(lastHorizon, chapter))
    }

    override fun getMangaDetails(url: String): Flow<SourceManga?> {
        val matchingDynamic = dynamicSources.values.firstOrNull { src ->
            url.startsWith(src.baseUrl)
        }
        if (matchingDynamic != null) {
            return kotlinx.coroutines.flow.flow {
                val details = try {
                    matchingDynamic.getMangaDetails(SourceManga(url = url, title = ""))
                } catch (_: Exception) {
                    null
                }
                emit(details)
            }
        }
        return flowOf(allMangaList.find { it.url == url } ?: lastHorizon)
    }

    override fun getChapters(mangaUrl: String): Flow<List<SourceChapter>> {
        val matchingDynamic = dynamicSources.values.firstOrNull { src ->
            mangaUrl.startsWith(src.baseUrl)
        }
        if (matchingDynamic != null) {
            return kotlinx.coroutines.flow.flow {
                val chapters = try {
                    matchingDynamic.getChapterList(SourceManga(url = mangaUrl, title = ""))
                } catch (_: Exception) {
                    emptyList()
                }
                emit(chapters)
            }
        }
        val chapters = listOf(
            SourceChapter(
                url = "ch-210",
                name = "Ch. 210",
                chapterNumber = 210f,
                dateUpload = 1718409600000L // Jun 15, 2024
            ),
            SourceChapter(
                url = "ch-209",
                name = "Ch. 209",
                chapterNumber = 209f,
                dateUpload = 1717804800000L // Jun 8, 2024
            ),
            SourceChapter(
                url = "ch-208",
                name = "Ch. 208",
                chapterNumber = 208f,
                dateUpload = 1717200000000L // Jun 1, 2024
            ),
            SourceChapter(
                url = "ch-207",
                name = "Ch. 207",
                chapterNumber = 207f,
                dateUpload = 1716595200000L
            )
        )
        return flowOf(chapters)
    }

    override fun getPages(chapterUrl: String): Flow<List<SourcePage>> {
        val matchingDynamic = dynamicSources.values.filterIsInstance<core.source.ComicSource>().firstOrNull { src ->
            chapterUrl.startsWith(src.baseUrl)
        }
        if (matchingDynamic != null) {
            return kotlinx.coroutines.flow.flow {
                val pages = try {
                    matchingDynamic.getPageList(SourceChapter(url = chapterUrl, name = ""))
                } catch (_: Exception) {
                    emptyList()
                }
                emit(pages)
            }
        }
        val pages = (1..6).map { i ->
            SourcePage(
                index = i,
                url = "page_$i",
                imageUrl = "webtoon_panel_$i"
            )
        }
        return flowOf(pages)
    }

    override fun getNovelContent(chapterUrl: String): Flow<NovelContent?> {
        val matchingDynamic = dynamicSources.values.filterIsInstance<core.source.NovelSource>().firstOrNull { src ->
            chapterUrl.startsWith(src.baseUrl)
        }
        if (matchingDynamic != null) {
            return kotlinx.coroutines.flow.flow {
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
                htmlBody = "<p>The morning light filtered through the leaves, casting moving shadows on the stone path...</p>",
                plainText = "The morning light filtered through the leaves, casting moving shadows on the stone path. Wei Wuxian adjusted his sword at his side and looked toward the distant mountains. A new journey awaited, and with it, countless unknowns.\n\n\"We'll set out at dawn,\" he said, turning to his companions. \"This time, we don't just fight the darkness—we understand it.\""
            )
        )
    }

    override fun getTrending(): Flow<List<SourceManga>> {
        return flowOf(listOf(velvetNight, crimsonOath, silentWinter))
    }

    override fun getSources(): Flow<List<MockSourceInfo>> {
        val baseSources = listOf(
            MockSourceInfo("1", "MangaDex", "Manga", listOf("EN", "JP")),
            MockSourceInfo("2", "Comick", "Manga", listOf("EN", "KR")),
            MockSourceInfo("3", "Asura Scans", "Manhwa", listOf("EN"))
        )
        val dynamicList = dynamicSources.values.map { src ->
            val type = if (src is core.source.NovelSource && src !is core.source.ComicSource) "Novel" else "Manga"
            MockSourceInfo(
                id = src.id.toString(),
                name = src.name,
                type = type,
                languages = listOf(src.lang.uppercase())
            )
        }
        return flowOf(baseSources + dynamicList)
    }

    override fun getExtensions(): Flow<List<MockExtension>> {
        return flowOf(
            listOf(
                MockExtension("1", "MangaDex", "v1.4.8", "EN", isInstalled = true, hasUpdate = false),
                MockExtension("2", "Comick", "v1.3.1", "All", isInstalled = true, hasUpdate = true),
                MockExtension("3", "Asura Scans", "v1.2.0", "EN", isInstalled = true, hasUpdate = false),
                MockExtension("4", "WebNovel", "v1.1.9", "EN", isInstalled = false, hasUpdate = true)
            )
        )
    }

    override fun getUpdates(): Flow<List<MockUpdateGroup>> {
        return flowOf(
            listOf(
                MockUpdateGroup(
                    title = "Today",
                    items = listOf(
                        MockUpdateItem(lastHorizon, SourceChapter("c210", "Ch. 210", chapterNumber = 210f)),
                        MockUpdateItem(neonRequiem, SourceChapter("c47", "Ch. 47", chapterNumber = 47f)),
                        MockUpdateItem(frostbound, SourceChapter("c12", "Ch. 12", chapterNumber = 12f))
                    )
                ),
                MockUpdateGroup(
                    title = "Yesterday",
                    items = listOf(
                        MockUpdateItem(shadowCrown, SourceChapter("c86", "Ch. 86", chapterNumber = 86f)),
                        MockUpdateItem(auroraBlade, SourceChapter("c24", "Ch. 24", chapterNumber = 24f))
                    )
                )
            )
        )
    }

    override fun getDownloads(): Flow<List<MockDownloadItem>> {
        return flowOf(
            listOf(
                MockDownloadItem(
                    id = "d1",
                    manga = lastHorizon,
                    chapter = SourceChapter("c210", "Ch. 210", chapterNumber = 210f),
                    progress = 0.68f,
                    downloadedBytes = 520L * 1024 * 1024,
                    totalBytes = 760L * 1024 * 1024,
                    etaMinutes = 2
                ),
                MockDownloadItem(
                    id = "d2",
                    manga = neonRequiem,
                    chapter = SourceChapter("c47", "Ch. 47", chapterNumber = 47f),
                    progress = 0.42f,
                    downloadedBytes = 310L * 1024 * 1024,
                    totalBytes = 730L * 1024 * 1024,
                    etaMinutes = 6
                ),
                MockDownloadItem(
                    id = "d3",
                    manga = silentWinter,
                    chapter = SourceChapter("c5", "Ch. 5", chapterNumber = 5f),
                    progress = 0.27f,
                    downloadedBytes = 180L * 1024 * 1024,
                    totalBytes = 660L * 1024 * 1024,
                    etaMinutes = 12
                )
            )
        )
    }

    override fun getStorageInfo(): Flow<MockStorageInfo> {
        return flowOf(MockStorageInfo())
    }

    override fun searchManga(
        query: String,
        sourceId: String?,
        filters: BrowseFilters
    ): Flow<List<SourceManga>> {
        val dynamicSource = sourceId?.toLongOrNull()?.let { dynamicSources[it] }
        if (dynamicSource != null) {
            return kotlinx.coroutines.flow.flow {
                val pageResult = try {
                    if (query.isNotBlank()) {
                        dynamicSource.search(query, 1)
                    } else {
                        dynamicSource.getPopular(1)
                    }
                } catch (_: Exception) {
                    null
                }
                emit(pageResult?.mangas ?: emptyList())
            }
        }

        val baseList = if (!sourceId.isNullOrBlank()) {
            sourceMangaMap[sourceId] ?: allMangaList
        } else {
            allMangaList
        }

        val trimmedQuery = query.trim()

        var results = baseList.filter { manga ->
            val matchesQuery = if (trimmedQuery.isEmpty()) {
                true
            } else {
                manga.title.contains(trimmedQuery, ignoreCase = true) ||
                    (manga.author?.contains(trimmedQuery, ignoreCase = true) == true) ||
                    manga.genres.any { it.contains(trimmedQuery, ignoreCase = true) }
            }

            val matchesStatus = filters.status == null || manga.status == filters.status

            val matchesNovel = filters.isNovel == null || manga.isNovel == filters.isNovel

            val matchesGenres = filters.genres.isEmpty() || filters.genres.any { filterGenre ->
                manga.genres.any { it.equals(filterGenre, ignoreCase = true) }
            }

            matchesQuery && matchesStatus && matchesNovel && matchesGenres
        }

        results = when (filters.sortBy) {
            BrowseSortOrder.POPULARITY -> results
            BrowseSortOrder.LATEST -> results.reversed()
            BrowseSortOrder.ALPHABETICAL -> results.sortedBy { it.title.lowercase() }
        }

        return flowOf(results)
    }

    override fun getSourceCatalogue(
        sourceId: String,
        page: Int,
        query: String,
        filters: BrowseFilters
    ): Flow<List<SourceManga>> {
        val dynamicSource = sourceId.toLongOrNull()?.let { dynamicSources[it] }
        if (dynamicSource != null) {
            return kotlinx.coroutines.flow.flow {
                val pageResult = try {
                    if (query.isNotBlank()) {
                        dynamicSource.search(query, page)
                    } else {
                        dynamicSource.getPopular(page)
                    }
                } catch (_: Exception) {
                    null
                }
                emit(pageResult?.mangas ?: emptyList())
            }
        }

        val baseList = sourceMangaMap[sourceId] ?: allMangaList
        val trimmedQuery = query.trim()

        var results = baseList.filter { manga ->
            val matchesQuery = if (trimmedQuery.isEmpty()) {
                true
            } else {
                manga.title.contains(trimmedQuery, ignoreCase = true) ||
                    (manga.author?.contains(trimmedQuery, ignoreCase = true) == true) ||
                    manga.genres.any { it.contains(trimmedQuery, ignoreCase = true) }
            }

            val matchesStatus = filters.status == null || manga.status == filters.status

            val matchesNovel = filters.isNovel == null || manga.isNovel == filters.isNovel

            val matchesGenres = filters.genres.isEmpty() || filters.genres.any { filterGenre ->
                manga.genres.any { it.equals(filterGenre, ignoreCase = true) }
            }

            matchesQuery && matchesStatus && matchesNovel && matchesGenres
        }

        results = when (filters.sortBy) {
            BrowseSortOrder.POPULARITY -> results
            BrowseSortOrder.LATEST -> results.reversed()
            BrowseSortOrder.ALPHABETICAL -> results.sortedBy { it.title.lowercase() }
        }

        val pageSize = 4
        val startIndex = ((page - 1) * pageSize).coerceAtLeast(0)
        val pagedList = if (startIndex < results.size) {
            results.drop(startIndex).take(pageSize)
        } else {
            emptyList()
        }

        return flowOf(pagedList)
    }
}
