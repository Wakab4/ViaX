package moe.pure.browser.feature.reader.ui

import androidx.fragment.app.Fragment
import moe.pure.browser.R
import moe.pure.browser.feature.reader.ReaderPreferences
import moe.pure.browser.ui.editor.NativeTextEditorFragment
import moe.pure.browser.ui.pages.RecordPageScope

/** hb.o5 and c8.s6 share hb.i1, but each caller owns its distinct post-save effect. */
internal object ReaderCssNavigation {
    fun open(owner: Fragment, saved: () -> Unit) {
        val preferences = ReaderPreferences(owner.requireContext())
        val manager = owner.parentFragmentManager
        manager.setFragmentResultListener(NativeTextEditorFragment.RESULT, owner) { _, result ->
            result.getString(NativeTextEditorFragment.TEXT)?.let { value -> preferences.customCss = value; saved() }
            manager.clearFragmentResultListener(NativeTextEditorFragment.RESULT)
        }
        val title = owner.getString(R.string.ref_reader_settings_css)
        val arguments = NativeTextEditorFragment.arguments(title, preferences.customCss, title, true)
        if (owner.isVisible && !owner.isRemoving) RecordPageScope.of(owner)?.push(NativeTextEditorFragment::class.java, arguments)
    }
}
