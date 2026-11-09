package data.profile

import core.storage.KeyValueStorage
import data.tracker.ProductionTrackerRepository
import data.tracker.TrackerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Production persistent implementation of ProfileRepository backed by KeyValueStorage.
 *
 * Rules:
 * - Only user-entered/chosen profile fields (display name, avatar color) are persisted.
 * - Stats, weekly activity, and library summary are NOT persisted to KeyValueStorage;
 *   they will be populated from the database later.
 * - Tracker accounts and connection states are managed through TrackerRepository.
 *   In release builds, fake accounts never show as connected.
 */
class PersistentProfileRepository(
    private val storage: KeyValueStorage,
    private val trackerRepository: TrackerRepository = ProductionTrackerRepository()
) : ProfileRepository {

    companion object {
        private const val KEY_PROFILE_NAME = "profile_display_name"
        private const val KEY_AVATAR_COLOR = "profile_avatar_color_hex"
        private const val DEFAULT_NAME = "Sujal"
        private const val DEFAULT_AVATAR_COLOR = 0xFF007AFF
        private const val DEFAULT_MEMBER_SINCE = "Reading since March 2026"
    }

    private val initialName = storage.getString(KEY_PROFILE_NAME, DEFAULT_NAME)
    private val initialColor = storage.getLong(KEY_AVATAR_COLOR, DEFAULT_AVATAR_COLOR)

    private val _profile = MutableStateFlow(
        UserProfile(
            name = initialName,
            memberSince = DEFAULT_MEMBER_SINCE,
            avatarColorHex = initialColor,
            initials = computeInitials(initialName)
        )
    )
    override val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    // Stats, activity, and library summary are intentionally in-memory only (database-backed in future)
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
            DayReadingActivity("T", 60),
            DayReadingActivity("W", 30),
            DayReadingActivity("T", 75),
            DayReadingActivity("F", 90),
            DayReadingActivity("S", 120),
            DayReadingActivity("S", 40)
        )
    )
    override val weeklyActivity: StateFlow<List<DayReadingActivity>> = _weeklyActivity.asStateFlow()

    // Delegated to TrackerRepository (strictly honors debug vs release)
    override val trackers: StateFlow<List<TrackerAccount>> = trackerRepository.trackers

    private val _librarySummary = MutableStateFlow(
        LibrarySummary(
            readingCount = 14,
            completedCount = 18,
            planToReadCount = 5
        )
    )
    override val librarySummary: StateFlow<LibrarySummary> = _librarySummary.asStateFlow()

    override fun updateProfile(name: String, avatarColorHex: Long) {
        val cleanName = name.trim().ifEmpty { DEFAULT_NAME }
        val updated = _profile.value.copy(
            name = cleanName,
            avatarColorHex = avatarColorHex,
            initials = computeInitials(cleanName)
        )
        _profile.value = updated

        // Persist only user-entered fields
        storage.putString(KEY_PROFILE_NAME, cleanName)
        storage.putLong(KEY_AVATAR_COLOR, avatarColorHex)
    }

    override fun toggleTrackerConnection(service: TrackerService) {
        trackerRepository.toggleDebugConnection(service)
    }

    private fun computeInitials(name: String): String =
        name.trim().firstOrNull()?.uppercase() ?: "R"
}
