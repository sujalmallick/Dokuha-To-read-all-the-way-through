package core.logging

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Top-level CoroutineExceptionHandler that logs uncaught coroutine failures
 * without recording personal data or sensitive URLs/messages.
 */
val AppCoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    if (throwable is CancellationException) return@CoroutineExceptionHandler
    AppLogger.e(
        tag = "CoroutineSafety",
        message = "Unhandled coroutine exception: ${throwable::class.simpleName ?: "Throwable"}",
        throwable = null
    )
}

/**
 * Launches a coroutine attached to [AppCoroutineExceptionHandler], always rethrowing
 * [CancellationException] and surfacing non-cancellation failures via [onError].
 */
fun CoroutineScope.launchSafe(
    tag: String,
    onError: (Throwable) -> Unit,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return launch(AppCoroutineExceptionHandler) {
        try {
            block()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            AppLogger.e(
                tag = tag,
                message = "Action failed (${t::class.simpleName ?: "Error"})",
                throwable = null
            )
            onError(t)
        }
    }
}
