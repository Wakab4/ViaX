package moe.via.browser.feature.readaloud

import android.content.Context
import android.os.Handler
import android.os.Looper

/** One in-memory article per process; pause keeps the current segment and completion keeps the task. */
class ReadAloudManager private constructor(context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private val preferences = ReadAloudPreferences(context)
    private val listeners = linkedSetOf<(ReadAloudTask?) -> Unit>()
    private var publishing = false
    private var generation = 0L
    @Volatile private var task: ReadAloudTask? = null
    private val engine = ReadAloudEngine(context, ::engineChanged).apply { setRate(preferences.rate) }
    val rate: Float get() = preferences.rate
    val engineState: SpeechState get() = engine.state
    var lastError: String? = null
        private set

    fun snapshot(): ReadAloudTask? = task

    fun observe(listener: (ReadAloudTask?) -> Unit) {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post { observe(listener) }; return }
        listeners.add(listener)
        listener(task)
    }

    fun removeObserver(listener: (ReadAloudTask?) -> Unit) {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post { removeObserver(listener) }; return }
        listeners.remove(listener)
    }

    fun replace(value: ReadAloudTask) {
        checkMain()
        generation++
        task = value.copy(segments = value.segments.toList(), playing = false)
        lastError = null
        engine.stop()
        publish()
    }

    fun play(taskId: String, segmentIndex: Int? = null): Boolean {
        checkMain()
        val current = task?.takeIf { it.id == taskId && it.segments.isNotEmpty() } ?: return false
        val index = segmentIndex ?: if (current.completed) 0 else current.segmentIndex
        if (index !in current.segments.indices) return false
        generation++
        lastError = null
        task = current.copy(segmentIndex = index, playing = true)
        speakCurrent(generation)
        return task?.playing == true
    }

    fun pause(taskId: String? = null): Boolean {
        checkMain()
        val current = task ?: run { engine.stop(); return false }
        if (taskId != null && current.id != taskId) return false
        generation++
        task = current.copy(playing = false)
        engine.stop()
        publish()
        return current.playing
    }

    fun stop() {
        checkMain()
        generation++
        task = null
        lastError = null
        engine.stop()
        publish()
    }

    fun setRate(value: Float) {
        checkMain()
        preferences.rate = value
        engine.setRate(rate)
        val current = task
        if (current?.playing == true) play(current.id, current.segmentIndex) else publish()
    }

    private fun speakCurrent(request: Long) {
        val current = task ?: return
        if (!current.hasSegment) { task = current.copy(playing = false); publish(); return }
        val accepted = engine.speak(current.segments[current.segmentIndex]) { outcome ->
            if (request != generation || task?.id != current.id || task?.segmentIndex != current.segmentIndex || task?.playing != true) return@speak
            if (outcome == SpeechOutcome.REJECTED) {
                lastError = if (engine.state == SpeechState.FAILED) "文字转语音引擎加载失败" else "无法朗读当前段落"
                task = current.copy(playing = false)
                publish()
            } else {
                task = current.copy(segmentIndex = current.segmentIndex + 1, playing = true)
                speakCurrent(request)
            }
        }
        if (!accepted && request == generation) {
            lastError = if (engine.state == SpeechState.FAILED) "文字转语音引擎加载失败" else "无法朗读当前段落"
            task = current.copy(playing = false)
        }
        publish()
    }

    private fun engineChanged() {
        if (engine.state == SpeechState.FAILED) {
            lastError = "文字转语音引擎加载失败"
            task = task?.copy(playing = false)
        }
        publish()
    }

    private fun publish() {
        if (publishing) return
        publishing = true
        main.post {
            publishing = false
            val current = task
            listeners.toList().forEach { if (it in listeners) it(current) }
        }
    }

    private fun checkMain() = check(Looper.myLooper() == Looper.getMainLooper()) { "Read-aloud commands must run on the main thread" }

    companion object {
        @Volatile private var instance: ReadAloudManager? = null
        fun get(context: Context): ReadAloudManager = instance ?: synchronized(this) {
            instance ?: ReadAloudManager(context.applicationContext).also { instance = it }
        }
    }
}
