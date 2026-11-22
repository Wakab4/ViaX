package moe.via.browser.feature.ai.text

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.view.View
import io.noties.markwon.LinkResolver
import moe.via.browser.BrowserActivity
import moe.via.browser.R
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.ui.theme.ThemedActivity

/** pa.c -> b0.T/P asks before opening a Markdown link in the browser shell. */
internal object AiMarkdownLinkResolver : LinkResolver {
    override fun resolve(view: View, link: String) {
        if (link.isEmpty()) return
        val activity = view.context as ThemedActivity
        activity.dialogScope.show(BrowserDialog(activity, activity.getString(R.string.ref_ai_link_open), link)
            .positive(activity.getString(android.R.string.ok)) {
                activity.startActivity(Intent(activity, BrowserActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse(link)).putExtra("intent", "mark.via.BROWSER")); true
            }.negative(activity.getString(android.R.string.cancel)).neutral(activity.getString(android.R.string.copy)) {
                activity.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, link))
                BrowserMessages.show(activity, activity.getString(R.string.ref_ai_link_copied)); false
            })
    }
}
