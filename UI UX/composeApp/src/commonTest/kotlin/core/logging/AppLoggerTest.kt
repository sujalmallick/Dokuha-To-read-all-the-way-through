package core.logging

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppLoggerTest {

    @Test
    fun testAppLoggerRingBuffer() {
        AppLogger.clear()

        AppLogger.i("TestTag", "Info message")
        AppLogger.w("TestTag", "Warning message")
        AppLogger.e("TestTag", "Error message", RuntimeException("Sample exception"))

        val currentLogs = AppLogger.logs.value
        assertEquals(3, currentLogs.size)

        assertEquals(LogLevel.INFO, currentLogs[0].level)
        assertEquals("Info message", currentLogs[0].message)

        assertEquals(LogLevel.WARN, currentLogs[1].level)
        assertEquals("Warning message", currentLogs[1].message)

        assertEquals(LogLevel.ERROR, currentLogs[2].level)
        assertEquals("Error message", currentLogs[2].message)
        assertTrue(currentLogs[2].throwable?.contains("Sample exception") == true)

        AppLogger.clear()
        assertEquals(0, AppLogger.logs.value.size)
    }

    @Test
    fun testLogEntryFormattedString() {
        val entry = LogEntry(
            timestamp = 1000L,
            level = LogLevel.ERROR,
            tag = "Network",
            message = "Connection timeout",
            throwable = "java.net.SocketTimeoutException"
        )
        val formatted = entry.toFormattedString()
        assertTrue(formatted.contains("[1000][E][Network] Connection timeout"))
        assertTrue(formatted.contains("java.net.SocketTimeoutException"))
    }
}
