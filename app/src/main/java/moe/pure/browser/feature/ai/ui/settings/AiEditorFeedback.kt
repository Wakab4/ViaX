package moe.pure.browser.feature.ai.ui.settings

import android.widget.EditText
import moe.pure.browser.ui.editor.NativeFieldFeedback

internal object AiEditorFeedback {
    fun required(editor: EditText, shake: Boolean = true) {
        NativeFieldFeedback.required(editor, shake)
    }
}
