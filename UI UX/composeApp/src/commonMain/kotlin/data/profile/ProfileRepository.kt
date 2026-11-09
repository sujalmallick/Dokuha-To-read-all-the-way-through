package data.profile

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val name: String = "Sujal",
    val memberSince: String = "Reading since March 2026",
    val avatarColorHex: Long = 0xFF007AFF,
    val initials: String = "S"
)

data class ReadingStats(
    val chaptersRead: Int = 482,
    val titlesCount: Int = 37,
    val hoursRead: Float = 142.5f
)

data class DayReadingActivity(
    val dayLabel: String, // "M", "T", "W", "T", "F", "S", "S"
    val minutes: Int
)

enum class TrackerService(val displayName: String) {
    ANILIST("AniList"),
    MY_ANIME_LIST("MyAnimeList"),
    KITSU("Kitsu")
}

data class TrackerAccount(
    val service: TrackerService,
    val isConnected: Boolean,
    val username: String? = null
)

data class LibrarySummary(
    val readingCount: Int = 14,
    val completedCount: Int = 18,
    val planToReadCount: Int = 5
)

interface ProfileRepository {
    val profile: StateFlow<UserProfile>
    val stats: StateFlow<ReadingStats>
    val weeklyActivity: StateFlow<List<DayReadingActivity>>
    val trackers: StateFlow<List<TrackerAccount>>
    val librarySummary: StateFlow<LibrarySummary>

    fun updateProfile(name: String, avatarColorHex: Long)
    fun toggleTrackerConnection(service: TrackerService)
}

class FakeProfileRepository : ProfileRepository {

    private val _profile = MutableStateFlow(
        UserProfile(
            name = "Sujal",
            memberSince = "Reading since March 2026",
            avatarColorHex = 0xFF007AFF,
            initials = "S"
        )
    )
    override val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _stats = MutableStateFlow(
        ReadingStats(
            chaptersRead = 482,
            titlesCount = 37,
            hoursRead = 142.5f
        )
    )
    override val stats: StateFlow<ReadingStats> = _stats.asStateFlow()

    private val _weeklyActivity = MutableStateFlow(
        listOf(
            DayReadingActivity("M", 45),
            DayReadingActivity("T", 90),
            DayReadingActivity("W", 30),
            DayReadingActivity("T", 120),
            DayReadingActivity("F", 60),
            DayReadingActivity("S", 150),
            DayReadingActivity("S", 85)
        )
    )
    override val weeklyActivity: StateFlow<List<DayReadingActivity>> = _weeklyActivity.asStateFlow()

    private val _trackers = MutableStateFlow(
        listOf(
            TrackerAccount(
                service = TrackerService.ANILIST,
                isConnected = true,
                username = "sujal_reads"
            ),
            TrackerAccount(
                service = TrackerService.MY_ANIME_LIST,
                isConnected = false,
                username = null
            ),
            TrackerAccount(
                service = TrackerService.KITSU,
                isConnected = false,
                username = null
            )
        )
    )
    override val trackers: StateFlow<List<TrackerAccount>> = _trackers.asStateFlow()

    private val _librarySummary = MutableStateFlow(
        LibrarySummary(
            readingCount = 14,
            completedCount = 18,
            planToReadCount = 5
        )
    )
    override val librarySummary: StateFlow<LibrarySummary> = _librarySummary.asStateFlow()

    override fun updateProfile(name: String, avatarColorHex: Long) {
        val cleanName = name.trim().ifEmpty { _profile.value.name }
        val initials = cleanName
            .split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifEmpty { "P" }

        _profile.value = _profile.value.copy(
            name = cleanName,
            avatarColorHex = avatarColorHex,
            initials = initials
        )
    }

    override fun toggleTrackerConnection(service: TrackerService) {
        _trackers.value = _trackers.value.map { account ->
            if (account.service == service) {
                val newConnected = !account.isConnected
                val username = if (newConnected) "sujal_${service.name.lowercase()}" else null
                account.copy(isConnected = newConnected, username = username)
            } else {
                account
            }
        }
    }
}
