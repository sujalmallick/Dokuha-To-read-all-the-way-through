package core.platform

import androidx.compose.runtime.Composable

interface TtsListener {
    fun onParagraphStart(index: Int)
    fun onComplete()
    fun onError(message: String)
}

expect class PlatformTts {
    fun speak(paragraphs: List<String>, startIndex: Int = 0, listener: TtsListener)
    fun pause()
    fun resume()
    fun stop()
    fun setSpeed(speed: Float)
    fun isSpeaking(): Boolean
    fun release()
}

@Composable
expect fun rememberPlatformTts(): PlatformTts
