package data

import core.storage.InMemorySecureStorage
import data.profile.TrackerService
import data.tracker.ProductionTrackerRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackerRepositoryTest {

    @Test
    fun testReleaseModeNeverAllowsFakeAccountsToBeConnected() {
        // Enforce hard security rule: In release builds, stubs are disabled and fake accounts NEVER connect
        val secureStorage = InMemorySecureStorage()
        val repo = ProductionTrackerRepository(
            secureStorage = secureStorage,
            isDebug = false // Simulating release build
        )

        assertFalse(repo.isDebugStubEnabled)

        // Initial state: all disconnected
        val initialTrackers = repo.trackers.value
        assertEquals(3, initialTrackers.size)
        assertTrue(initialTrackers.all { !it.isConnected && it.username == null })

        // Attempting to toggle connection in release must be a complete NO-OP
        repo.toggleDebugConnection(TrackerService.ANILIST)
        repo.toggleDebugConnection(TrackerService.MY_ANIME_LIST)
        repo.toggleDebugConnection(TrackerService.KITSU)

        // Must still be disconnected!
        val afterAttempt = repo.trackers.value
        assertTrue(afterAttempt.all { !it.isConnected && it.username == null })
    }

    @Test
    fun testDebugModeAllowsStubTogglingForUiTesting() {
        val secureStorage = InMemorySecureStorage()
        val repo = ProductionTrackerRepository(
            secureStorage = secureStorage,
            isDebug = true // Simulating debug build
        )

        assertTrue(repo.isDebugStubEnabled)

        // Connect AniList in debug mode
        repo.toggleDebugConnection(TrackerService.ANILIST)

        val anilist = repo.trackers.value.first { it.service == TrackerService.ANILIST }
        assertTrue(anilist.isConnected)
        assertEquals("dev_user_anilist", anilist.username)

        // Disconnect
        repo.disconnect(TrackerService.ANILIST)
        val disconnectedAnilist = repo.trackers.value.first { it.service == TrackerService.ANILIST }
        assertFalse(disconnectedAnilist.isConnected)
        assertNull(disconnectedAnilist.username)
    }

    @Test
    fun testDisconnectClearsSecureToken() {
        val secureStorage = InMemorySecureStorage()
        val repo = ProductionTrackerRepository(
            secureStorage = secureStorage,
            isDebug = true
        )

        // Place a mock token in secure storage
        secureStorage.putSecret("tracker_token_anilist", "oauth_access_token_12345")
        assertEquals("oauth_access_token_12345", secureStorage.getSecret("tracker_token_anilist"))

        // Disconnect service
        repo.disconnect(TrackerService.ANILIST)

        // Token must be wiped from secure storage
        assertNull(secureStorage.getSecret("tracker_token_anilist"))
    }
}
