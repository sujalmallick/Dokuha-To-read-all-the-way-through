package core.source

import kotlinx.serialization.Serializable

@Serializable
data class SourceManga(
    val url: String,
    val title: String,
    val artist: String? = null,
    val author: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val status: MangaStatus = MangaStatus.UNKNOWN,
    val thumbnailUrl: String? = null,
    val isNovel: Boolean = false,
    val sourceId: Long = 0L
)

@Serializable
data class SourceChapter(
    val url: String,
    val name: String,
    val dateUpload: Long = 0L,
    val chapterNumber: Float = -1f,
    val scanlator: String? = null
)

@Serializable
data class SourcePage(
    val index: Int,
    val url: String,
    val imageUrl: String? = null,
    val headers: Map<String, String> = emptyMap()
)

@Serializable
data class NovelContent(
    val chapterUrl: String,
    val title: String,
    val htmlBody: String,
    val plainText: String
)

enum class MangaStatus {
    UNKNOWN,
    ONGOING,
    COMPLETED,
    LICENSED,
    PUBLISHING_FINISHED,
    CANCELLED,
    ON_HIATUS
}

data class MangasPage(
    val mangas: List<SourceManga>,
    val hasNextPage: Boolean
)

sealed interface SourceFilter {
    val name: String
}

sealed interface ContentSource {
    val id: Long
    val name: String
    val baseUrl: String
    val lang: String
    val isNsfw: Boolean

    suspend fun getPopular(page: Int): MangasPage
    suspend fun getLatestUpdates(page: Int): MangasPage
    suspend fun search(query: String, page: Int, filters: List<SourceFilter> = emptyList()): MangasPage
    suspend fun getMangaDetails(manga: SourceManga): SourceManga
    suspend fun getChapterList(manga: SourceManga): List<SourceChapter>
}

interface ComicSource : ContentSource {
    suspend fun getPageList(chapter: SourceChapter): List<SourcePage>
    suspend fun getImageUrl(page: SourcePage): String = page.imageUrl ?: page.url
}

interface NovelSource : ContentSource {
    suspend fun getChapterContent(chapter: SourceChapter): NovelContent
}

data class StorageInfo(
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

data class SourceInfo(
    val id: String,
    val name: String,
    val type: String, // Manga, Manhwa, Novel
    val languages: List<String>,
    val iconUrl: String? = null
)
