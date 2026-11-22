package moe.via.browser.feature.useragent.data

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModel
import java.util.UUID

/** Only operation state survives a window change; no window or repository helper is retained. */
class UserAgentEditorOwners : ViewModel() {
    private val editors = mutableMapOf<String, UserAgentEditorOperation>()

    internal fun acquire(context: Context, requestedId: Int, restored: Bundle?): UserAgentEditorOperation {
        val restoredToken = restored?.getString("owner_id")
        if (restoredToken != null) editors[restoredToken]?.let {
            if (it.requestedId == requestedId) return it
        }
        val token = restoredToken?.takeIf { runCatching { UUID.fromString(it) }.isSuccess }
            ?: UUID.randomUUID().toString()
        return UserAgentEditorOperation(context.applicationContext, token, requestedId, restored).also {
            editors.put(token, it)?.close()
        }
    }

    internal fun release(operation: UserAgentEditorOperation) {
        if (editors[operation.token] === operation) editors.remove(operation.token)?.close()
    }

    override fun onCleared() {
        editors.values.forEach(UserAgentEditorOperation::close)
        editors.clear()
    }
}
