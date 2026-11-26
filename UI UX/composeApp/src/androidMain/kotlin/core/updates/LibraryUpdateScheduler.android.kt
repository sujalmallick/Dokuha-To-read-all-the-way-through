package core.updates

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import data.mock.PanelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val LIBRARY_UPDATE_JOB_ID = 7042

actual object LibraryUpdateScheduler : KoinComponent {
    private val context: Context by inject()

    actual fun schedulePeriodicUpdates(intervalHours: Long) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
        val intervalMs = (intervalHours.coerceAtLeast(1L)) * 60L * 60L * 1000L
        val component = ComponentName(context, LibraryUpdateJobService::class.java)
        val jobInfo = JobInfo.Builder(LIBRARY_UPDATE_JOB_ID, component)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(intervalMs)
            .setPersisted(false)
            .build()
        scheduler.schedule(jobInfo)
    }

    actual fun cancel() {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
        scheduler.cancel(LIBRARY_UPDATE_JOB_ID)
    }
}

class LibraryUpdateJobService : JobService(), KoinComponent {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        val repository = runCatching { getKoin().getOrNull<PanelRepository>() }.getOrNull()
        if (repository == null) {
            jobFinished(params, false)
            return false
        }
        scope.launch {
            runCatching { repository.refreshLibraryUpdates() }
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = true
}
