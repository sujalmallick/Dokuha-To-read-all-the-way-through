package core.logging

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoreCoroutineSafetyTest {

    @Test
    fun testLaunchSafe_surfacesNonCancellationErrorViaCallback() = runBlocking {
        var capturedError: Throwable? = null
        val done = CompletableDeferred<Unit>()

        val job = launchSafe(
            tag = "TestTag",
            onError = { err ->
                capturedError = err
                done.complete(Unit)
            }
        ) {
            throw IllegalStateException("sensitive-url-or-token")
        }

        done.await()
        job.join()

        assertTrue(capturedError is IllegalStateException)
        // Ensure AppLogger does not log raw exception message (PII/URL safety)
        val lastLog = AppLogger.logs.value.lastOrNull { it.tag == "TestTag" }
        assertTrue(lastLog != null)
        assertEquals("Action failed (IllegalStateException)", lastLog.message)
        assertFalse(lastLog.message.contains("sensitive-url-or-token"))
    }

    @Test
    fun testLaunchSafe_rethrowsCancellationWithoutCallingOnError() = runBlocking {
        var onErrorCalled = false

        val job = launchSafe(
            tag = "CancelTestTag",
            onError = { onErrorCalled = true }
        ) {
            throw CancellationException("cancelled")
        }

        job.join()
        assertTrue(job.isCancelled)
        assertFalse(onErrorCalled)
    }
}
