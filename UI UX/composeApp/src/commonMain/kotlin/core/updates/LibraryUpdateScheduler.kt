package core.updates

/**
 * Cross-platform periodic library chapter update scheduler.
 * Uses JobScheduler on Android and BGTaskScheduler on iOS.
 */
expect object LibraryUpdateScheduler {
    fun schedulePeriodicUpdates(intervalHours: Long = 12L)
    fun cancel()
}
