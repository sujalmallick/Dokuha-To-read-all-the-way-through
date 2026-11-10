package data.profile

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val name: String = "Ritwik",
    val memberSince: String = "Member since November 2026",
    val avatarColorHex: Long = 0xFF38BDF8,
    val initials: String = "R"
)

data class ReadingStats(
    val chaptersRead: Int = 0,
    val titlesCount: Int = 0,
    val hoursRead: Float = 0.0f
)

data class DayReadingActivity(
    val dayLabel: String, // "M", "T", "W", "T", "F", "S", "S"
    val minutes: Int = 0
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
    val readingCount: Int = 0,
    val completedCount: Int = 0,
    val planToReadCount: Int = 0
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
            name = "Ritwik",
            memberSince = "Member since November 2026",
            avatarColorHex = 0xFF38BDF8,
            initials = "R"
        )
    )
    override val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _stats = MutableStateFlow(
        ReadingStats(
            chaptersRead = 0,
            titlesCount = 0,
            hoursRead = 0.0f
        )
    )
    override val stats: StateFlow<ReadingStats> = _stats.asStateFlow()

    private val _weeklyActivity = MutableStateFlow(
        listOf(
            DayReadingActivity("M", 0),
            DayReadingActivity("T", 0),
            DayReadingActivity("W", 0),
            DayReadingActivity("T", 0),
            DayReadingActivity("F", 0),
            DayReadingActivity("S", 0),
            DayReadingActivity("S", 0)
        )
    )
    override val weeklyActivity: StateFlow<List<DayReadingActivity>> = _weeklyActivity.asStateFlow()

    private val _trackers = MutableStateFlow(
        listOf(
            TrackerAccount(
                service = TrackerService.ANILIST,
                isConnected = false,
                username = null
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
            readingCount = 0,
            completedCount = 0,
            planToReadCount = 0
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
