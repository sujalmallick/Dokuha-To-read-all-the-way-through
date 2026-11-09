package data.tracker

import core.platform.BuildEnvironment
import core.storage.SecureStorage
import data.profile.TrackerAccount
import data.profile.TrackerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository interface for managing manga/novel tracking services (AniList, MyAnimeList, Kitsu).
 */
interface TrackerRepository {
    /**
     * Current tracking account states.
     */
    val trackers: StateFlow<List<TrackerAccount>>

    /**
     * True if running in a debug build where test UI stubs are allowed.
     * Strictly false in release builds.
     */
    val isDebugStubEnabled: Boolean

    /**
     * Debug-only stub to toggle connection state for UI testing.
     * In release builds, this is strictly a NO-OP and will never connect fake accounts.
     */
    fun toggleDebugConnection(service: TrackerService)

    /**
     * Disconnects a tracking service and removes any associated auth credentials.
     */
    fun disconnect(service: TrackerService)
}

/**
 * Production implementation of TrackerRepository.
 *
 * Rules:
 * - Real OAuth flows (PKCE / redirect callbacks) are not implemented yet.
 * - Stubs are allowed ONLY in debug builds (isDebugStubEnabled == true).
 * - In release builds, fake accounts must NEVER show as "Connected" (isConnected is always false).
 */
class ProductionTrackerRepository(
    private val secureStorage: SecureStorage? = null,
    private val isDebug: Boolean = BuildEnvironment.isDebug
) : TrackerRepository {

    override val isDebugStubEnabled: Boolean = isDebug

    private val initialTrackers: List<TrackerAccount> = listOf(
        TrackerAccount(TrackerService.ANILIST, isConnected = false, username = null),
        TrackerAccount(TrackerService.MY_ANIME_LIST, isConnected = false, username = null),
        TrackerAccount(TrackerService.KITSU, isConnected = false, username = null)
    )

    private val _trackers = MutableStateFlow(initialTrackers)
    override val trackers: StateFlow<List<TrackerAccount>> = _trackers.asStateFlow()

    override fun toggleDebugConnection(service: TrackerService) {
        // Hard security invariant: completely disabled in release builds
        if (!isDebugStubEnabled) {
            return
        }

        val updated = _trackers.value.map { account ->
            if (account.service == service) {
                val nextConnected = !account.isConnected
                account.copy(
                    isConnected = nextConnected,
                    username = if (nextConnected) "dev_user_${service.name.lowercase()}" else null
                )
            } else {
                account
            }
        }
        _trackers.value = updated
    }

    override fun disconnect(service: TrackerService) {
        secureStorage?.removeSecret("tracker_token_${service.name.lowercase()}")

        val updated = _trackers.value.map { account ->
            if (account.service == service) {
                account.copy(isConnected = false, username = null)
            } else {
                account
            }
        }
        _trackers.value = updated
    }
}
