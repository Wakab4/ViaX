package moe.pure.browser.feature.useragent.data

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import moe.pure.browser.core.logging.NativeLog as Log
import moe.pure.browser.feature.useragent.UserAgentText
import java.lang.ref.WeakReference
import java.util.concurrent.Future

/** One editor UUID owns one real read/write, including its terminal result across configuration. */
internal class UserAgentEditorOperation(
    context: Context,
    val token: String,
    val requestedId: Int,
    restored: Bundle?
) : AutoCloseable {
    interface Observer { fun editorChanged() }
    data class Draft(val name: String = "", val value: String = "", val nameCursor: Int = 0, val valueCursor: Int = 0)
    data class Saved(val id: Int)

    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var observer = WeakReference<Observer>(null)
    private var task: Future<*>? = null
    private var closed = false
    private var terminal: Saved? = null
    var recordId = restored?.getInt("record_id") ?: 0
        private set
    var baseline: String? = restored?.getString("baseline")
        private set
    var draft = Draft(restored?.getString("name").orEmpty(), restored?.getString("value").orEmpty(),
        restored?.getInt("name_cursor") ?: 0, restored?.getInt("value_cursor") ?: 0)
        private set
    var revision = 0
        private set
    var loading = requestedId > 0
        private set
    var saving = false
        private set

    init {
        // A missing ViewModel is a new process: query the original record, never replay a saved write.
        if (loading) {
            val preserveDraft = restored != null && restored.getBoolean("loaded", restored.getInt("record_id") > 0)
            task = UserAgentOperations.run(app, { it.get(requestedId) }) { result ->
                if (!closed) {
                    loading = false; task = null
                    result.onFailure { Log.warn("UserAgents", "Unable to load user-agent editor", it) }
                    result.getOrNull()?.let { value ->
                        recordId = value.id
                        baseline = UserAgentText.fingerprint(value.name.takeIf(String::isNotEmpty), value.value)
                        if (!preserveDraft) {
                            draft = Draft(value.name, value.value.orEmpty())
                            revision++
                        }
                    }
                    changed()
                }
            }
        }
    }

    fun bind(value: Observer) { if (!closed) { observer = WeakReference(value); changed() } }
    fun unbind(value: Observer) { if (observer.get() === value) observer.clear() }
    fun updateDraft(value: Draft) { if (!closed) draft = value }

    fun save(name: String, value: String) {
        if (closed || loading || saving || terminal != null) return
        saving = true
        val id = recordId.takeIf { it > 0 }
        task = UserAgentOperations.run(app, { it.save(id, name, value) }) { result ->
            if (!closed) {
                saving = false; task = null
                result.onSuccess { recordId = it; terminal = Saved(it) }
                    .onFailure { Log.warn("UserAgents", "Unable to save user-agent", it) }
                changed()
            }
        }
    }

    fun consume(value: Observer): Saved? {
        if (closed || observer.get() !== value) return null
        return terminal.also { terminal = null }
    }

    fun saveState() = Bundle().apply {
        putString("owner_id", token); putInt("requested_id", requestedId)
        putInt("record_id", recordId); putString("baseline", baseline); putBoolean("loaded", !loading)
        putString("name", draft.name); putString("value", draft.value)
        putInt("name_cursor", draft.nameCursor); putInt("value_cursor", draft.valueCursor)
        // These are metadata only. Neither an in-flight Future nor its commit is recreated from Bundle.
        putBoolean("saving", saving)
    }

    private fun changed() {
        main.post { if (!closed) observer.get()?.editorChanged() }
    }

    override fun close() {
        if (closed) return
        closed = true; observer.clear(); task?.cancel(false); task = null; terminal = null
        main.removeCallbacksAndMessages(null)
    }
}
