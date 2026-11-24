package core.logging

import core.platform.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.FileSystem
import okio.Path
import okio.buffer

/**
 * Multiplatform persistent logger.
 * Maintains an in-memory ring buffer (up to [MAX_MEMORY_LOGS] entries) for immediate UI display
 * and asynchronously appends entries to rolling on-disk log files capped at [MAX_LOG_FILE_SIZE_BYTES].
 */
object AppLogger {
    private const val MAX_MEMORY_LOGS = 500
    private const val MAX_LOG_FILE_SIZE_BYTES = 1_500_000L // ~1.5 MB

    private val scope = CoroutineScope(Dispatchers.Default)
    private val mutex = Mutex()
    private val fileSystem = FileSystem.SYSTEM

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val pendingDiskQueue = mutableListOf<String>()
    private var isFlushing = false

    fun v(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.VERBOSE, tag, message, throwable)
    fun d(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.DEBUG, tag, message, throwable)
    fun i(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.INFO, tag, message, throwable)
    fun w(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.WARN, tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, message, throwable)

    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val entry = LogEntry(
            timestamp = currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable?.stackTraceToString()
        )

        // 1. Update in-memory ring buffer
        _logs.update { list ->
            val updated = list + entry
            if (updated.size > MAX_MEMORY_LOGS) {
                updated.drop(updated.size - MAX_MEMORY_LOGS)
            } else {
                updated
            }
        }

        // 2. Queue for background file persistence
        val formatted = entry.toFormattedString()
        scope.launch {
            mutex.withLock {
                pendingDiskQueue.add(formatted)
                if (!isFlushing) {
                    isFlushing = true
                    flushPendingQueueLocked()
                }
            }
        }
    }

    private fun flushPendingQueueLocked() {
        if (pendingDiskQueue.isEmpty()) {
            isFlushing = false
            return
        }

        val entriesToWrite = pendingDiskQueue.toList()
        pendingDiskQueue.clear()

        try {
            val logDir = PlatformLogger.getLogDirectory()
            if (!fileSystem.exists(logDir)) {
                fileSystem.createDirectories(logDir)
            }
            val activeLogFile = logDir / "dokuha.log"

            // Check size and rotate if necessary
            if (fileSystem.exists(activeLogFile)) {
                val metadata = fileSystem.metadataOrNull(activeLogFile)
                if (metadata != null && (metadata.size ?: 0L) > MAX_LOG_FILE_SIZE_BYTES) {
                    val rotatedFile = logDir / "dokuha.1.log"
                    if (fileSystem.exists(rotatedFile)) {
                        fileSystem.delete(rotatedFile)
                    }
                    fileSystem.atomicMove(activeLogFile, rotatedFile)
                }
            }

            // Append entries
            fileSystem.appendingSink(activeLogFile).buffer().use { sink ->
                for (line in entriesToWrite) {
                    sink.writeUtf8(line)
                    sink.writeUtf8("\n")
                }
            }
        } catch (_: Throwable) {
            // Avoid recursion if disk write fails
        } finally {
            if (pendingDiskQueue.isNotEmpty()) {
                flushPendingQueueLocked()
            } else {
                isFlushing = false
            }
        }
    }

    fun getLogFilePath(): Path {
        return PlatformLogger.getLogDirectory() / "dokuha.log"
    }

    fun shareLogs() {
        val path = getLogFilePath()
        PlatformLogger.shareLogFile(path)
    }

    fun clear() {
        _logs.value = emptyList()
        scope.launch {
            mutex.withLock {
                pendingDiskQueue.clear()
                try {
                    val logDir = PlatformLogger.getLogDirectory()
                    val activeLog = logDir / "dokuha.log"
                    val rotatedLog = logDir / "dokuha.1.log"
                    if (fileSystem.exists(activeLog)) fileSystem.delete(activeLog)
                    if (fileSystem.exists(rotatedLog)) fileSystem.delete(rotatedLog)
                } catch (_: Throwable) {
                }
            }
        }
    }
}
