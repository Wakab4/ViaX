package moe.via.browser.feature.useragent.ui

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import moe.via.browser.R
import moe.via.browser.feature.useragent.UserAgentText
import moe.via.browser.feature.useragent.data.UserAgentEditorOperation
import moe.via.browser.feature.useragent.data.UserAgentEditorOwners
import moe.via.browser.ui.components.BrowserMessage
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope

internal class UserAgentEditorController(private val activity: FragmentActivity, private val requestedId: Int, restored: Bundle?,
    private val done: (Int?) -> Unit) : AutoCloseable, UserAgentEditorOperation.Observer {
    private val content = UserAgentEditorView(activity)
    val view: android.view.View get() = content
    val title get() = activity.getString(if (requestedId > 0) R.string.ref_ua_edit else R.string.ref_ua_new)
    private val owners = ViewModelProvider(activity)[UserAgentEditorOwners::class.java]
    private val operation = owners.acquire(activity.applicationContext, requestedId, restored)
    private var renderedRevision = -1
    private val scope = DialogScope()
    private val messages = BrowserMessage(activity)
    private var prompt: BrowserDialog? = null
    private var closed = false
    private var active = false

    init {
        content.value.setOnKeyListener { _, code, event ->
            if (code != KeyEvent.KEYCODE_ENTER) false else {
                if (event.action == KeyEvent.ACTION_UP) save()
                true
            }
        }
        renderDraft()
        if (active) operation.bind(this)
    }
    private fun renderDraft() {
        val draft = operation.draft
        content.name.setText(draft.name); content.value.setText(draft.value)
        content.name.setSelection(draft.nameCursor.coerceIn(0, content.name.length()))
        content.value.setSelection(draft.valueCursor.coerceIn(0, content.value.length()))
        renderedRevision = operation.revision
    }
    override fun editorChanged() {
        if (closed || !active) return
        if (renderedRevision != operation.revision) renderDraft()
        operation.consume(this)?.let { done(it.id) }
    }
    private fun captureDraft() {
        // A paused window must not replace a read result delivered after its last render.
        if (renderedRevision != operation.revision) return
        operation.updateDraft(UserAgentEditorOperation.Draft(content.name.text.toString(), content.value.text.toString(),
            content.name.selectionStart, content.value.selectionStart))
    }
    private fun normalized() = UserAgentText.cleanName(content.name.text.toString()) to UserAgentText.cleanValue(content.value.text.toString())
    private fun unchanged(): Boolean {
        val (name, value) = normalized()
        val current = UserAgentText.fingerprint(name, value)
        return operation.baseline == null && current.isEmpty() || operation.baseline == current
    }
    fun save() {
        if (closed || operation.loading || operation.saving) return
        captureDraft()
        if (unchanged()) { done(null); return }
        val (name, value) = normalized()
        if (name.isEmpty()) { messages.show(activity.getString(R.string.ref_ua_empty_name)); return }
        if (value.isEmpty()) { messages.show(activity.getString(R.string.ref_ua_empty_value)); return }
        operation.save(name, value)
    }
    fun back(): Boolean {
        if (closed) return false
        if (operation.saving) return true
        prompt?.let { it.dismiss(); return true }
        if (unchanged()) { done(null); return true }
        val dialog = BrowserDialog(activity, activity.getString(R.string.ref_ua_unsaved_title),
            activity.getString(R.string.ref_ua_unsaved_message))
            .positive(activity.getString(R.string.ref_ua_save_exit)) { save(); true }
            .negative(activity.getString(R.string.ref_ua_discard)) { done(null); true }
        prompt = dialog; scope.show(dialog) { if (prompt === dialog) prompt = null }
        return true
    }
    fun saveState(): Bundle { captureDraft(); return operation.saveState() }
    fun resume() {
        if (!closed) {
            // Framework hierarchy restoration may contain an older draft than the retained operation.
            renderDraft(); active = true; operation.bind(this)
        }
    }
    fun pause() {
        if (!closed) captureDraft()
        active = false; operation.unbind(this)
        (activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(content.value.windowToken, 0)
    }
    override fun close() = close(retain = false)
    fun close(retain: Boolean) {
        if (closed) return
        pause(); closed = true; scope.dispose(); messages.close()
        content.value.setOnKeyListener(null)
        if (!retain) owners.release(operation)
    }
}
