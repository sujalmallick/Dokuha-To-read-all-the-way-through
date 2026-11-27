package core.updates

import core.di.appModule
import data.mock.PanelRepository
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow

private const val BG_REFRESH_TASK_ID = "com.dokuha.reader.library.refresh"

@OptIn(ExperimentalForeignApi::class)
actual object LibraryUpdateScheduler {
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    actual fun schedulePeriodicUpdates(intervalHours: Long) {
        val request = BGAppRefreshTaskRequest(identifier = BG_REFRESH_TASK_ID).apply {
            earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(
                (intervalHours.coerceAtLeast(1L) * 3600L).toDouble()
            )
        }
        runCatching {
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error = null)
        }
    }

    actual fun cancel() {
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(BG_REFRESH_TASK_ID)
    }

    /**
     * Executes a background library refresh for iOS `BGAppRefreshTask`, ensuring Koin is
     * initialized when launched in the background. Returns a cancellation callback for `expirationHandler`.
     */
    fun performBackgroundRefresh(
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ): () -> Unit {
        val koin = KoinPlatformTools.defaultContext().getOrNull() ?: runCatching {
            startKoin { modules(appModule) }.koin
        }.getOrNull()

        val repository = koin?.getOrNull<PanelRepository>()
        if (repository == null) {
            onFailure()
            return {}
        }

        val job = backgroundScope.launch {
            val result = runCatching { repository.refreshLibraryUpdates() }
            if (result.isSuccess) {
                onSuccess()
            } else {
                onFailure()
            }
        }
        return { job.cancel() }
    }
}
