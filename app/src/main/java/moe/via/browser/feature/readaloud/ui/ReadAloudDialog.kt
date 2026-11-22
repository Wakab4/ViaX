package moe.via.browser.feature.readaloud.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import moe.via.browser.ui.message.BrowserMessages

import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.AbsListView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.feature.readaloud.ReadAloudCommands
import moe.via.browser.feature.readaloud.ReadAloudManager
import moe.via.browser.feature.readaloud.ReadAloudTask
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.dialog.DialogLayoutOwner
import moe.via.browser.ui.theme.palette

/** Paragraphs and transport only. Speech and notification ownership remains in the feature manager. */
class ReadAloudDialog(private val activity: Activity) :
    Dialog(activity, R.style.Theme_ViaBrowser_Dialog), DialogLayoutOwner {
    private val manager = ReadAloudManager.get(activity)
    private val handler = Handler(Looper.getMainLooper())
    private val adapter = ReadAloudParagraphAdapter(activity)
    private val root = FrameLayout(activity)
    private val list = ListView(activity)
    private val controls = LinearLayout(activity)
    private val empty = NativeTypography.text(activity)
    private val speed = ReadAloudSpeedControl(activity, manager::setRate)
    private val play = icon(R.drawable.ref_tts_play, "恢复") { togglePlayback() }
    private var task: ReadAloudTask? = null
    private var pendingIndex = -1
    private var followAfter = 0L
    private var observing = false
    private val observer: (ReadAloudTask?) -> Unit = ::updateTask
    private val commitJump = Runnable {
        val current = task
        val target = pendingIndex
        pendingIndex = -1
        if (current != null && current.segments.isNotEmpty() && target >= 0) {
            followAfter = 0L
            ReadAloudCommands.play(activity, current.id, target.coerceIn(0, current.segments.lastIndex))
        }
    }

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        buildContent()
        setContentView(root)
        setCanceledOnTouchOutside(true)
        window?.setDimAmount(.4f)
    }

    override fun onStart() {
        super.onStart()
        updateGeometry()
        @Suppress("DEPRECATION")
        window?.decorView?.systemUiVisibility = activity.window.decorView.systemUiVisibility
        if (!observing) {
            observing = true
            manager.observe(observer)
        }
    }

    override fun onStop() {
        if (observing) {
            manager.removeObserver(observer)
            observing = false
        }
        handler.removeCallbacksAndMessages(null)
        pendingIndex = -1
        super.onStop()
    }

    override fun updateGeometry() {
        val metrics = activity.resources.displayMetrics
        val side = minOf(metrics.widthPixels, metrics.heightPixels) * 4 / 5
        window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { gravity = Gravity.CENTER }
            setLayout(minOf(side, activity.dp(450)), minOf(side, activity.dp(600)))
        }
    }

    private fun buildContent() {
        root.clipToOutline = true
        empty.apply {
            text = "空空如也…"
            gravity = Gravity.CENTER
            textSize = 22f
            setTextColor(activity.palette.secondary)
            visibility = View.GONE
        }
        root.addView(empty, FrameLayout.LayoutParams(-1, -1))
        list.apply {
            adapter = this@ReadAloudDialog.adapter
            divider = null
            dividerHeight = 0
            setPadding(0, dp(16), 0, dp(16))
            clipToPadding = false
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            setSelector(R.drawable.ref_tts_feedback)
            setOnItemClickListener { _, _, position, _ ->
                task?.let { ReadAloudCommands.play(activity, it.id, position) }
            }
            setOnItemLongClickListener { _, _, position, _ ->
                copyParagraph(position)
                true
            }
            setOnScrollListener(object : AbsListView.OnScrollListener {
                override fun onScroll(view: AbsListView, firstVisibleItem: Int,
                    visibleItemCount: Int, totalItemCount: Int) = Unit
                override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                    if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) followAfter = -1L
                    else if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE && followAfter == -1L)
                        followAfter = SystemClock.elapsedRealtime() + 3000L
                }
            })
        }
        root.addView(list, FrameLayout.LayoutParams(-1, -1).apply { bottomMargin = activity.dp(48) })
        controls.apply { orientation = LinearLayout.HORIZONTAL }
        val settings = icon(R.drawable.ref_tts_settings, "设置", ::openSystemSettings)
        val previous = icon(R.drawable.ref_tts_previous, "倒带") { queueJump(-1) }
        val next = icon(R.drawable.ref_tts_next, "快进") { queueJump(1) }
        controls.addView(settings, controlCell())
        controls.addView(speed.slider, LinearLayout.LayoutParams(0, -1, 4f))
        controls.addView(previous, controlCell())
        controls.addView(play, controlCell())
        controls.addView(next, controlCell())
        controls.addView(speed.label, controlCell())
        speed.install(controls, listOf(settings, previous, play, next))
        speed.setRate(manager.rate)
        root.addView(controls, FrameLayout.LayoutParams(-1, activity.dp(48), Gravity.BOTTOM))
    }

    private fun updateTask(value: ReadAloudTask?) {
        val old = task
        task = value
        val emptyTask = value == null || value.segments.isEmpty()
        empty.visibility = if (emptyTask) View.VISIBLE else View.GONE
        controls.visibility = if (emptyTask) View.GONE else View.VISIBLE
        list.visibility = if (emptyTask) View.GONE else View.VISIBLE
        speed.setRate(manager.rate)
        if (old?.id != value?.id) {
            handler.removeCallbacks(commitJump)
            pendingIndex = -1
            followAfter = 0L
        }
        val changedContent = old?.id != value?.id || old?.segments !== value?.segments
        if (changedContent) adapter.replace(value?.segments.orEmpty())
        val playing = value != null && value.segmentIndex in value.segments.indices && value.playing
        play.setImageResource(if (playing) R.drawable.ref_tts_pause else R.drawable.ref_tts_play)
        play.contentDescription = if (playing) "暂停" else "恢复"
        if (value != null && pendingIndex < 0) highlight(value.segmentIndex, !changedContent)
    }

    private fun highlight(index: Int, smooth: Boolean) {
        val changed = adapter.currentIndex != index
        adapter.select(index, list)
        if ((!changed && smooth) || index !in 0 until adapter.count || followAfter < 0L ||
            SystemClock.elapsedRealtime() <= followAfter) return
        list.post {
            if (!isShowing || adapter.currentIndex != index) return@post
            if (smooth) list.smoothScrollToPosition(index) else list.setSelection(index)
        }
    }

    private fun queueJump(direction: Int) {
        if (task?.segments.isNullOrEmpty()) return
        if (pendingIndex < 0) pendingIndex = adapter.currentIndex
        pendingIndex = (pendingIndex + direction).coerceAtLeast(0)
        highlight(pendingIndex, true)
        handler.removeCallbacks(commitJump)
        handler.postDelayed(commitJump, 300L)
    }

    private fun togglePlayback() {
        val current = task ?: return
        if (current.playing) ReadAloudCommands.pause(activity)
        else ReadAloudCommands.play(activity, current.id)
    }

    private fun openSystemSettings() {
        if (task?.playing == true) ReadAloudCommands.pause(activity)
        try {
            activity.startActivity(Intent("com.android.settings.TTS_SETTINGS")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: android.content.ActivityNotFoundException) {
            BrowserMessages.show(activity, "操作失败")
        } catch (_: SecurityException) {
            BrowserMessages.show(activity, "操作失败")
        }
    }

    private fun copyParagraph(position: Int) {
        val paragraph = task?.segments?.getOrNull(position) ?: return
        followAfter = SystemClock.elapsedRealtime() + 3000L
        activity.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText(null, paragraph))
        BrowserMessages.show(activity, "已复制文本")
    }

    private fun icon(resource: Int, description: String, action: () -> Unit) = ImageView(activity).apply {
        setImageResource(resource)
        imageTintList = ColorStateList.valueOf(activity.palette.icon)
        setPadding(dp(13), dp(13), dp(13), dp(13))
        scaleType = ImageView.ScaleType.FIT_CENTER
        setBackgroundResource(R.drawable.ref_tts_feedback)
        contentDescription = description
        isFocusable = true
        setOnClickListener { action() }
    }

    private fun controlCell() = LinearLayout.LayoutParams(0, -1, 1f)
}
