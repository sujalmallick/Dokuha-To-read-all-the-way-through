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
}
