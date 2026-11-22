package moe.via.browser.feature.search.ui

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.widget.EditText

/** Keyboard backspace at offset zero removes a temporary engine before consuming input text. */
internal class SearchAddressEditText(context: Context) : EditText(context) {
    init { moe.via.browser.ui.theme.fonts.NativeTypography.bind(this) }
    var deleteAtStart: (() -> Unit)? = null

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val connection = super.onCreateInputConnection(outAttrs) ?: return null
        return object : InputConnectionWrapper(connection, false) {
            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean =
                if (atStart(beforeLength)) true else super.deleteSurroundingText(beforeLength, afterLength)

            override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean =
                if (atStart(beforeLength)) true else super.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
        }
    }

    private fun atStart(length: Int): Boolean {
        val action = deleteAtStart ?: return false
        if (length <= 0 || selectionStart != 0 || selectionEnd != 0) return false
        post { action() }
        return true
    }
}
