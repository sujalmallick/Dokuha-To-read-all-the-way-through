package core.logging

import okio.Path

/**
 * Multiplatform contract for resolving application log directory and sharing log files.
 */
expect object PlatformLogger {
    fun getLogDirectory(): Path
    fun shareLogFile(filePath: Path)
}
