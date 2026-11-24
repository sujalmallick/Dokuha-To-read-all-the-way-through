package core.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

actual class PlatformTts(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isInitialized = false
    private var currentParagraphs: List<String> = emptyList()
    private var currentIndex = 0
    private var currentSpeed = 1.0f
    private var activeListener: TtsListener? = null
    private var isPaused = false

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    // Listen for headphone disconnect
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }
    private var isReceiverRegistered = false

    init {
        try {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            context.registerReceiver(noisyReceiver, filter)
            isReceiverRegistered = true
        } catch (_: Exception) {}
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.getDefault()
            tts?.setSpeechRate(currentSpeed)
            setupProgressListener()
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val index = utteranceId?.toIntOrNull() ?: currentIndex
                activeListener?.onParagraphStart(index)
            }

            override fun onDone(utteranceId: String?) {
                val index = utteranceId?.toIntOrNull() ?: currentIndex
                val next = index + 1
                if (next < currentParagraphs.size && !isPaused) {
                    currentIndex = next
                    speakCurrent()
                } else if (next >= currentParagraphs.size) {
                    activeListener?.onComplete()
                    abandonAudioFocus()
                }
            }

            override fun onError(utteranceId: String?) {
                activeListener?.onError("TTS playback error")
            }
        })
    }

    actual fun speak(paragraphs: List<String>, startIndex: Int, listener: TtsListener) {
        currentParagraphs = paragraphs
        currentIndex = startIndex.coerceIn(0, (paragraphs.size - 1).coerceAtLeast(0))
        activeListener = listener
        isPaused = false
        requestAudioFocus()
        speakCurrent()
    }

    private fun speakCurrent() {
        if (!isInitialized || currentIndex >= currentParagraphs.size) return
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

        val params = Bundle()
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, params, currentIndex.toString())
    }

    actual fun pause() {
        isPaused = true
        tts?.stop()
    }

    actual fun resume() {
        if (isPaused && currentIndex < currentParagraphs.size) {
            isPaused = false
            requestAudioFocus()
            speakCurrent()
        }
    }

    actual fun stop() {
        isPaused = false
        tts?.stop()
        abandonAudioFocus()
    }

    actual fun setSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0.5f, 2.5f)
        tts?.setSpeechRate(currentSpeed)
    }

    actual fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true && !isPaused
    }

    actual fun release() {
        stop()
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(noisyReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
        tts?.shutdown()
        tts = null
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes)
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        pause()
                    }
                }
                .build()
            audioFocusRequest = focusRequest
            audioManager?.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        pause()
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
    }
}

@Composable
actual fun rememberPlatformTts(): PlatformTts {
    val context = LocalContext.current.applicationContext
    val platformTts = remember(context) { PlatformTts(context) }
    DisposableEffect(platformTts) {
        onDispose {
            platformTts.release()
        }
    }
    return platformTts
}
