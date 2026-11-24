package core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.darwin.NSObject

actual class PlatformTts {
    private val synthesizer = AVSpeechSynthesizer()
    private var currentParagraphs: List<String> = emptyList()
    private var currentIndex = 0
    private var currentSpeed = 1.0f
    private var activeListener: TtsListener? = null
    private var isPaused = false

    private val delegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didStartSpeechUtterance: AVSpeechUtterance) {
            activeListener?.onParagraphStart(currentIndex)
        }

        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didFinishSpeechUtterance: AVSpeechUtterance) {
            val next = currentIndex + 1
            if (next < currentParagraphs.size && !isPaused) {
                currentIndex = next
                speakCurrent()
            } else if (next >= currentParagraphs.size) {
                activeListener?.onComplete()
            }
        }

        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didCancelSpeechUtterance: AVSpeechUtterance) {
            // Cancelled
        }
    }

    init {
        try {
            synthesizer.delegate = delegate
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        } catch (_: Exception) {}
    }

    actual fun speak(paragraphs: List<String>, startIndex: Int, listener: TtsListener) {
        currentParagraphs = paragraphs
        currentIndex = startIndex.coerceIn(0, (paragraphs.size - 1).coerceAtLeast(0))
        activeListener = listener
        isPaused = false
        speakCurrent()
    }

    private fun speakCurrent() {
        if (currentIndex >= currentParagraphs.size) return
        val raw = currentParagraphs[currentIndex]
        val clean = NovelTextSanitizer.cleanForSpeech(raw)
        if (clean.isBlank()) {
            val next = currentIndex + 1
            if (next < currentParagraphs.size) {
                currentIndex = next
                speakCurrent()
            } else {
                activeListener?.onComplete()
            }
            return
        }

        val utterance = AVSpeechUtterance(string = clean)
        val rate = (0.5f * currentSpeed).coerceIn(0.1f, 1.0f)
        utterance.rate = rate
        synthesizer.speakUtterance(utterance)
    }

    actual fun pause() {
        isPaused = true
        synthesizer.pauseSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    actual fun resume() {
        if (isPaused) {
            isPaused = false
            if (synthesizer.isPaused()) {
                synthesizer.continueSpeaking()
            } else {
                speakCurrent()
            }
        }
    }

    actual fun stop() {
        isPaused = false
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    actual fun setSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0.5f, 2.5f)
    }

    actual fun isSpeaking(): Boolean {
        return synthesizer.isSpeaking() && !isPaused
    }

    actual fun release() {
        stop()
    }
}

@Composable
actual fun rememberPlatformTts(): PlatformTts {
    val platformTts = remember { PlatformTts() }
    DisposableEffect(platformTts) {
        onDispose {
            platformTts.release()
        }
    }
    return platformTts
}
