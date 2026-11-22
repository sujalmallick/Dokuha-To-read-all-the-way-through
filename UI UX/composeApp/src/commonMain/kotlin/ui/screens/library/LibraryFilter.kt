package ui.screens.library

import core.source.MangaStatus
import core.source.SourceManga
import data.mock.LibraryMangaStats

enum class MangaStatusFilter(val label: String) {
    ALL("All"),
    ONGOING("Ongoing"),
    COMPLETED("Completed")
}

enum class ContentTypeFilter(val label: String) {
    ALL("All"),
    MANGA("Manga"),
    MANHWA("Manhwa"),
    NOVEL("Novel")
}

enum class ReadStatusFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed")
}

enum class LibrarySort(val label: String) {
    TITLE("Alphabetical"),
    LAST_READ("Last Read"),
    TOTAL_CHAPTERS("Total Chapters"),
    DATE_ADDED("Date Added")
}

data class LibraryFilter(
    val status: MangaStatusFilter = MangaStatusFilter.ALL,
    val contentType: ContentTypeFilter = ContentTypeFilter.ALL,
    val readStatus: ReadStatusFilter = ReadStatusFilter.ALL,
    val downloadedOnly: Boolean = false,
    val sortBy: LibrarySort = LibrarySort.TITLE,
    val sortAscending: Boolean = true
) {
    val isActive: Boolean
        get() = status != MangaStatusFilter.ALL ||
                contentType != ContentTypeFilter.ALL ||
                readStatus != ReadStatusFilter.ALL ||
                downloadedOnly ||
                sortBy != LibrarySort.TITLE ||
                !sortAscending

    val activeFilterCount: Int
        get() {
            var count = 0
            if (status != MangaStatusFilter.ALL) count++
            if (contentType != ContentTypeFilter.ALL) count++
            if (readStatus != ReadStatusFilter.ALL) count++
            if (downloadedOnly) count++
            if (sortBy != LibrarySort.TITLE || !sortAscending) count++
            return count
        }
}

/**
 * Pure, testable filter and sorting function for library manga collections.
 */
fun filterAndSortLibrary(
    items: List<SourceManga>,
    filter: LibraryFilter,
    stats: Map<String, LibraryMangaStats>,
    isDownloaded: ((SourceManga) -> Boolean)? = null
): List<SourceManga> {
    return items.asSequence()
        .filter { manga ->
            when (filter.status) {
                MangaStatusFilter.ALL -> true
                MangaStatusFilter.ONGOING -> manga.status == MangaStatus.ONGOING
                MangaStatusFilter.COMPLETED -> manga.status == MangaStatus.COMPLETED || manga.status == MangaStatus.PUBLISHING_FINISHED
            }
        }
        .filter { manga ->
            when (filter.contentType) {
                ContentTypeFilter.ALL -> true
                ContentTypeFilter.NOVEL -> manga.isNovel
                ContentTypeFilter.MANHWA -> !manga.isNovel && (
                    manga.genres.any { it.contains("manhwa", ignoreCase = true) || it.contains("webtoon", ignoreCase = true) } ||
                    manga.title.contains("manhwa", ignoreCase = true) ||
                    manga.title.contains("webtoon", ignoreCase = true)
                )
                ContentTypeFilter.MANGA -> !manga.isNovel && !manga.genres.any {
                    it.contains("manhwa", ignoreCase = true) || it.contains("webtoon", ignoreCase = true)
                }
            }
        }
        .filter { manga ->
            val mangaStat = stats[manga.url]
            when (filter.readStatus) {
                ReadStatusFilter.ALL -> true
                ReadStatusFilter.UNREAD -> mangaStat?.isUnread ?: true
                ReadStatusFilter.IN_PROGRESS -> mangaStat?.isInProgress ?: false
                ReadStatusFilter.COMPLETED -> mangaStat?.isCompletedReading ?: false
            }
        }
        .filter { manga ->
            if (!filter.downloadedOnly) true
            else isDownloaded?.invoke(manga) ?: false
        }
        .toList()
        .let { list ->
            val sorted = when (filter.sortBy) {
                LibrarySort.TITLE -> list.sortedBy { it.title.lowercase() }
                LibrarySort.LAST_READ -> list.sortedBy { stats[it.url]?.lastReadAt ?: 0L }
                LibrarySort.TOTAL_CHAPTERS -> list.sortedBy { stats[it.url]?.totalChapters ?: 0 }
                LibrarySort.DATE_ADDED -> list.sortedBy { stats[it.url]?.dateAdded ?: 0L }
            }
            if (filter.sortAscending) sorted else sorted.reversed()
        }
}
