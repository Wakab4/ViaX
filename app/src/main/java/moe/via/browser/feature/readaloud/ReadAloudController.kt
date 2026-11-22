package moe.via.browser.feature.readaloud

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import moe.via.browser.feature.readaloud.ui.ReadAloudDialog
import moe.via.browser.feature.readaloud.ui.ReadAloudMiniBar
import moe.via.browser.browser.session.BrowserTab
import moe.via.browser.ui.components.BrowserMessage
import moe.via.browser.ui.theme.ThemedActivity
import java.util.UUID

/** Coordinates browser entry points; task lifetime and speech belong to the manager/service. */
class ReadAloudController(private val activity: ThemedActivity, private val parent: () -> FrameLayout) {
    private val manager = ReadAloudManager.get(activity)
    private val messages = BrowserMessage(activity)
    private var dialog: ReadAloudDialog? = null
    private var bar: ReadAloudMiniBar? = null
    private var taskId: String? = null
    private var barY = -1
    private var disposed = false
    private var epoch = 0L
    private val changed: (ReadAloudTask?) -> Unit = { task ->
        if (!disposed) {
            if (taskId != null && task?.id != taskId) taskId = null
            updateBar(task)
        }
    }

    init { manager.observe(changed) }

    fun start(tab: BrowserTab) {
        val view = tab.liveView ?: return
        if (!view.isShown) return
        if (!view.settings.javaScriptEnabled) { messages.show("仅在允许 JavaScript 运行时支持"); return }
        if (manager.engineState == SpeechState.FAILED) { messages.show("文字转语音引擎加载失败"); return }
        messages.show("稍等片刻…")
        val request = ++epoch
        val url = tab.url
        val title = tab.title
        view.evaluateJavascript(EXTRACT) { raw ->
            if (disposed || request != epoch || tab.liveView !== view || tab.url != url) return@evaluateJavascript
            val segments = ReadAloudText.parse(raw)
            if (segments.isEmpty()) { messages.show("无法朗读该网页"); return@evaluateJavascript }
            val task = ReadAloudTask(UUID.randomUUID().toString().replace("-", ""), url, title, segments)
            taskId = task.id
            manager.replace(task)
            if (ReadAloudCommands.play(activity, task.id)) messages.show("开始朗读", "查看", ::show)
            else messages.show("操作失败")
            updateBar(task)
        }
    }

    fun show() {
        if (disposed) return
        manager.snapshot()?.let { taskId = it.id; updateBar(it) }
        if (dialog?.isShowing == true) return
        dialog = activity.dialogScope.show(ReadAloudDialog(activity)) { dialog = null }
    }

    fun consume(intent: Intent): Boolean {
        if (intent.action != ACTION_SHOW && intent.dataString != INTERNAL_URL) return false
        intent.action = null
        if (intent.dataString == INTERNAL_URL) intent.data = null
        parent().post { if (!disposed) show() }
        return true
    }

    fun consume(address: String): Boolean {
        if (address.trim() != INTERNAL_URL) return false
        show(); return true
    }

    fun save(): Bundle = Bundle().apply {
        putString("id", taskId); putInt("y", bar?.translationY?.toInt() ?: barY)
    }

    fun restore(state: Bundle) {
        taskId = state.getString("id")?.takeIf { it == manager.snapshot()?.id }
        barY = state.getInt("y", -1)
        updateBar(manager.snapshot())
    }

    fun rebind() {
        removeBar(false)
        messages.close()
        updateBar(manager.snapshot())
        dialog?.updateGeometry()
    }

    private fun updateBar(task: ReadAloudTask?) {
        if (task == null || taskId == null || task.id != taskId) { removeBar(true); return }
        if (bar == null) {
            val target = parent()
            val created = ReadAloudMiniBar(activity, object : ReadAloudMiniBar.Actions {
                override fun panel() = show()
                override fun play() { taskId?.let { ReadAloudCommands.play(activity, it) } }
                override fun pause() { ReadAloudCommands.pause(activity, taskId) }
                override fun stop() { ReadAloudCommands.stop(activity); taskId = null; removeBar(true) }
            })
            target.addView(created, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START))
            created.translationY = if (barY >= 0) barY.toFloat() else target.height / 2f
            created.alpha = 0f
            created.animate().alpha(1f).setDuration(100).start()
            created.post { created.constrainPosition() }
            bar = created
        }
        bar?.setPlaying(task.playing)
    }

    private fun removeBar(animate: Boolean) {
        val existing = bar ?: return
        barY = existing.translationY.toInt(); bar = null
        val remove = { (existing.parent as? FrameLayout)?.removeView(existing); Unit }
        existing.animate().cancel()
        if (animate) existing.animate().alpha(0f).setDuration(100).withEndAction(remove).start() else remove()
    }

    fun dispose(retain: Boolean) {
        disposed = true; epoch++
        manager.removeObserver(changed)
        dialog?.dismiss(); dialog = null
        messages.close(); removeBar(false)
        if (!retain && taskId != null && manager.snapshot()?.id == taskId) ReadAloudCommands.stop(activity)
    }

    companion object {
        const val ACTION_SHOW = "moe.via.browser.READ_ALOUD"
        const val INTERNAL_URL = "v://readaloud"
        private const val EXTRACT = "(function(){try{var e=document.getElementsByClassName(\"via-reader-body\"),n=e.length>0?e[0].innerText:document.body.innerText;return JSON.stringify(n.split(\"\\n\"))}catch(e){}})();"
    }
}
