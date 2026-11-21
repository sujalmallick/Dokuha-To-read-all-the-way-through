package core.extensions.js

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AntiBotChallengeManagerTest {

    @Test
    fun testSingleFlightChallengeCoalescing() = runBlocking {
        val manager = AntiBotChallengeManager()

        // 3 concurrent requests to the same domain
        val deferred1 = async { manager.resolveChallenge("https://asuracomic.net/series/1", "UserAgentA") }
        val deferred2 = async { manager.resolveChallenge("https://asuracomic.net/series/2", "UserAgentB") }
        val deferred3 = async { manager.resolveChallenge("https://asuracomic.net/series/3", "UserAgentC") }

        // Give coroutines time to start
        kotlinx.coroutines.yield()

        val challenge = manager.activeChallenge.value
        assertNotNull(challenge, "There should be an active challenge request")
        assertEquals("asuracomic.net", challenge.domain)

        // Complete the single challenge
        challenge.onCompleted(
            AntiBotResolutionResult(
                success = true,
                cookies = mapOf("cf_clearance" to "verified_token_123")
            )
        )

        // All 3 requests should resolve with the same cookies
        val res1 = deferred1.await()
        val res2 = deferred2.await()
        val res3 = deferred3.await()

        assertTrue(res1.success)
        assertEquals("verified_token_123", res1.cookies["cf_clearance"])

        assertTrue(res2.success)
        assertEquals("verified_token_123", res2.cookies["cf_clearance"])

        assertTrue(res3.success)
        assertEquals("verified_token_123", res3.cookies["cf_clearance"])

        // Active challenge state should be cleared
        assertNull(manager.activeChallenge.value)
    }

    @Test
    fun testChallengeUserDismissal() = runBlocking {
        val manager = AntiBotChallengeManager()

        val deferred = async { manager.resolveChallenge("https://example.com/protected", "UserAgent") }
        kotlinx.coroutines.yield()

        val challenge = manager.activeChallenge.value
        assertNotNull(challenge)

        // User dismisses / cancels
        challenge.onDismiss()

        val result = deferred.await()
        assertFalse(result.success)
        assertTrue(result.errorMessage?.contains("dismissed", ignoreCase = true) == true)
        assertNull(manager.activeChallenge.value)
    }

    @Test
    fun testDistinctDomainsTriggerDistinctChallengesSequentially() = runBlocking {
        val manager = AntiBotChallengeManager()

        // First domain
        val deferred1 = async { manager.resolveChallenge("https://domain-a.com/page", "UA") }
        kotlinx.coroutines.yield()
        val ch1 = manager.activeChallenge.value
        assertNotNull(ch1)
        assertEquals("domain-a.com", ch1.domain)
        ch1.onCompleted(AntiBotResolutionResult(success = true))
        assertTrue(deferred1.await().success)

        // Second domain
        val deferred2 = async { manager.resolveChallenge("https://domain-b.com/page", "UA") }
        kotlinx.coroutines.yield()
        val ch2 = manager.activeChallenge.value
        assertNotNull(ch2)
        assertEquals("domain-b.com", ch2.domain)
        ch2.onCompleted(AntiBotResolutionResult(success = true))
        assertTrue(deferred2.await().success)
    }

    @Test
    fun testSilentBackgroundResolutionSuccessDoesNotPromptUser() = runBlocking {
        val mockSolver = object : HeadlessSolver {
            override suspend fun solve(url: String, userAgent: String, timeoutMs: Long): HeadlessSolverResult {
                return HeadlessSolverResult(
                    success = true,
                    cookies = mapOf("cf_clearance" to "silent_token_999"),
                    userAgent = "MockAndroidUA"
                )
            }
        }

        val manager = AntiBotChallengeManager(headlessSolver = mockSolver)

        val result = manager.resolveChallenge("https://asurascans.com/comics/test", "UA")

        assertTrue(result.success, "Silent resolution should succeed")
        assertEquals("silent_token_999", result.cookies["cf_clearance"])
        assertEquals("MockAndroidUA", result.userAgent)
        assertNull(manager.activeChallenge.value, "Zero popups should be shown for silent resolution")
    }

    @Test
    fun testNonInteractiveChallengeFailureDoesNotPromptUser() = runBlocking {
        val mockSolver = object : HeadlessSolver {
            override suspend fun solve(url: String, userAgent: String, timeoutMs: Long): HeadlessSolverResult {
                return HeadlessSolverResult(
                    success = false,
                    isInteractiveChallengeRequired = false,
                    errorMessage = "Automated check failed without interactive challenge"
                )
            }
        }

        val manager = AntiBotChallengeManager(headlessSolver = mockSolver)

        val result = manager.resolveChallenge("https://asurascans.com/comics/test", "UA")

        assertFalse(result.success)
        assertNull(manager.activeChallenge.value, "Popup must NOT be shown when no interactive challenge is present")
    }

    @Test
    fun testInteractiveTurnstileChallengePromptsUser() = runBlocking {
        val mockSolver = object : HeadlessSolver {
            override suspend fun solve(url: String, userAgent: String, timeoutMs: Long): HeadlessSolverResult {
                return HeadlessSolverResult(
                    success = false,
                    isInteractiveChallengeRequired = true,
                    errorMessage = "Turnstile checkbox detected"
                )
            }
        }

        val manager = AntiBotChallengeManager(headlessSolver = mockSolver)

        val deferred = async { manager.resolveChallenge("https://protected-site.com/entry", "UA") }
        kotlinx.coroutines.yield()

        val challenge = manager.activeChallenge.value
        assertNotNull(challenge, "Active challenge popup SHOULD be presented for Turnstile checkbox")
        assertEquals("protected-site.com", challenge.domain)

        challenge.onCompleted(
            AntiBotResolutionResult(
                success = true,
                cookies = mapOf("cf_clearance" to "turnstile_solved_token")
            )
        )

        val result = deferred.await()
        assertTrue(result.success)
        assertEquals("turnstile_solved_token", result.cookies["cf_clearance"])
        assertNull(manager.activeChallenge.value)
    }
}
