package core.logging

enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR
}

data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: String? = null
) {
    fun toFormattedString(): String {
        val lvl = when (level) {
            LogLevel.VERBOSE -> "V"
            LogLevel.DEBUG -> "D"
            LogLevel.INFO -> "I"
            LogLevel.WARN -> "W"
            LogLevel.ERROR -> "E"
        }
        val base = "[$lvl][$tag] $message"
        return if (throwable != null) "$base\n$throwable" else base
    }
}
