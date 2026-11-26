package core.updates

import kotlinx.cinterop.ExperimentalForeignApi
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow

private const val BG_REFRESH_TASK_ID = "com.dokuha.reader.library.refresh"

@OptIn(ExperimentalForeignApi::class)
actual object LibraryUpdateScheduler {
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
}
