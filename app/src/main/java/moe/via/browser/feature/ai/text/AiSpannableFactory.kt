package moe.via.browser.feature.ai.text

import android.text.Spannable
import android.text.SpannableString

/** t6.d reuses an already mutable span buffer instead of copying pre-rendered text. */
internal object AiSpannableFactory : Spannable.Factory() {
    override fun newSpannable(source: CharSequence): Spannable = if (source is Spannable) source else SpannableString(source)
}
