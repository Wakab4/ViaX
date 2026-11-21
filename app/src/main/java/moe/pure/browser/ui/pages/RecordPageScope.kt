package moe.pure.browser.ui.pages

import androidx.fragment.app.Fragment
import moe.pure.browser.ui.theme.ThemedActivity

/** Nested content panels own their navigation, rather than sending their pages into the activity stack. */
internal interface RecordPageStackOwner { val recordPageStack: PageStackController? }

internal object RecordPageScope {
    fun of(fragment: Fragment): PageStackController? {
        var parent = fragment.parentFragment
        while (parent != null) {
            if (parent is RecordPageStackOwner) return parent.recordPageStack
            parent = parent.parentFragment
        }
        return (fragment.activity as? ThemedActivity)?.pageStack
    }
}
