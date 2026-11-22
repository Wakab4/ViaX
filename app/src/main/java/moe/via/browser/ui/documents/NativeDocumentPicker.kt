package moe.via.browser.ui.documents

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import moe.via.browser.R
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.core.logging.NativeLog

/** p8.f/x5.c keep a temporary callback and start OpenDocument in Download. */
internal class NativeDocumentPicker(private val fragment: Fragment) {
    private var picked: ((Uri?) -> Unit)? = null
    private val launcher = fragment.registerForActivityResult(object : ActivityResultContracts.OpenDocument() {
        override fun createIntent(context: Context, input: Array<String>): Intent = super.createIntent(context, input)
            .putExtra("android.provider.extra.INITIAL_URI", Uri.parse("content://com.android.externalstorage.documents/document/primary:Download"))
    }) { uri ->
        val callback = picked
        if (callback != null) { callback(uri); picked = null }
    }
    fun open(types: Array<String>, callback: (Uri?) -> Unit) {
        if (types.isEmpty()) return
        picked = callback
        try { launcher.launch(types) }
        catch (error: ActivityNotFoundException) {
            NativeLog.warn(null, error)
            val context = fragment.requireContext()
            BrowserMessages.show(context, context.getString(R.string.ref_document_picker_error))
        }
    }
}
