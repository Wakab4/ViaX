package moe.pure.browser.feature.readaloud

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import moe.pure.browser.core.logging.NativeLog as Log
import java.util.Locale
import java.util.UUID

enum class SpeechState { INITIALIZING, READY, SPEAKING, FAILED }
internal enum class SpeechOutcome { COMPLETED, ERROR, REJECTED }

/** All mutable state and callbacks belong to the main thread. */
internal class ReadAloudEngine(context: Context, private val changed: () -> Unit) {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var speech: TextToSpeech? = null
    private var generation = 0L
    private var pending: Request? = null
    private var active: Request? = null
    private var utterance: String? = null
    private var rate = 1f
    var state = SpeechState.INITIALIZING
        private set
    private data class Request(val generation: Long, val text: String, val finished: (SpeechOutcome) -> Unit)

    init {
        main.post {
            try {
                speech = TextToSpeech(app) { result -> main.post { initialized(result) } }
            } catch (error: Exception) {
                Log.error("ReadAloud", "Cannot initialize text to speech", error)
                initialized(TextToSpeech.ERROR)
            }
        }
    }

    private fun initialized(result: Int) {
        if (result != TextToSpeech.SUCCESS || speech == null) {
            setState(SpeechState.FAILED)
            val request = pending
            pending = null
            if (request?.generation == generation) request.finished(SpeechOutcome.REJECTED)
            return
        }
        speech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) { main.post { finished(id, SpeechOutcome.COMPLETED) } }
            @Deprecated("Platform compatibility callback")
            override fun onError(id: String?) { main.post { finished(id, SpeechOutcome.ERROR) } }
            override fun onError(id: String?, errorCode: Int) {
                Log.warn("ReadAloud", "Speech synthesis error: $errorCode")
                main.post { finished(id, SpeechOutcome.ERROR) }
            }
        })
        speech?.setSpeechRate(rate)
        val language = speech?.setLanguage(Locale.getDefault())
        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.warn("ReadAloud", "Language ${Locale.getDefault().toLanguageTag()} is not supported")
        }
        setState(SpeechState.READY)
        val request = pending
        pending = null
        if (request?.generation == generation) submit(request)
    }

    fun setRate(value: Float) {
        rate = ReadAloudPreferences.normalize(value)
        if (state == SpeechState.READY || state == SpeechState.SPEAKING) speech?.setSpeechRate(rate)
    }

    fun speak(text: String, finished: (SpeechOutcome) -> Unit): Boolean {
        stop()
        if (state == SpeechState.FAILED) return false
        val request = Request(generation, text, finished)
        if (state == SpeechState.INITIALIZING) {
            pending = request
            return true
        }
        return submit(request)
    }

    private fun submit(request: Request): Boolean {
        if (request.generation != generation) return false
        active = request
        utterance = "pure-${UUID.randomUUID()}"
        setState(SpeechState.SPEAKING)
        val result = runCatching { speech?.speak(request.text, TextToSpeech.QUEUE_FLUSH, null, utterance) }
            .getOrDefault(TextToSpeech.ERROR)
        if (result == TextToSpeech.SUCCESS) return true
        active = null
        utterance = null
        setState(SpeechState.READY)
        Log.warn("ReadAloud", "Text to speech rejected the current segment")
        request.finished(SpeechOutcome.REJECTED)
        return false
    }

    private fun finished(id: String?, outcome: SpeechOutcome) {
        val request = active ?: return
        if (id == null || id != utterance || request.generation != generation) return
        active = null
        utterance = null
        setState(SpeechState.READY)
        request.finished(outcome)
    }

    fun stop() {
        generation++
        pending = null
        active = null
        utterance = null
        if (state == SpeechState.SPEAKING) setState(SpeechState.READY)
        runCatching { speech?.stop() }
    }

    private fun setState(value: SpeechState) {
        if (state != value) { state = value; changed() }
    }
}
