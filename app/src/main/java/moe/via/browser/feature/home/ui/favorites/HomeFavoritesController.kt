package moe.via.browser.feature.home.ui.favorites

import android.net.Uri
import moe.via.browser.R
import moe.via.browser.browser.ui.host.BrowserRootFragment
import moe.via.browser.feature.internalpages.BrowserInternalPages
import moe.via.browser.feature.home.data.HomePreferences
import moe.via.browser.feature.home.data.NativeHomeFavorites
import moe.via.browser.browser.session.BrowserTab
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.ui.theme.ThemedActivity

/** s6 entry/result handling; each form owns its data work, photo result and lifecycle. */
class HomeFavoritesController(private val activity: ThemedActivity, private val actions: Actions) {
    interface Actions {
        fun current(): BrowserTab?
        fun changed()
        fun panelHeight(topCss: Int): Int
    }
    private val repository = NativeHomeFavorites.get(activity)
    private var disposed = false

    fun contains(url: String): Boolean = !disposed && repository.find(url) != null

    fun add(url: String, title: String?) {
        val root = BrowserRootFragment.find(activity) ?: return
        if (disposed || !root.isAdded || root.isHidden) return
        val manager = activity.supportFragmentManager
        manager.setFragmentResultListener(HomeFavoriteDialogFragment.RESULT, activity) { _, result ->
            if (result.getInt("result_id", -1) >= 0) {
                HomePreferences(activity).markDirty(html = true, style = false)
                if (BrowserInternalPages.kind(activity, actions.current()?.liveView?.url) == 1) actions.changed()
                else BrowserMessages.show(activity, R.string.ref_favorite_form_added)
            }
            manager.clearFragmentResultListener(HomeFavoriteDialogFragment.RESULT)
        }
        HomeFavoriteDialogFragment.create(url, title).show(root.childFragmentManager, HomeFavoriteDialogFragment.TAG)
    }

    fun delete(url: String) {
        repository.deleteUrl(url)
        HomePreferences(activity).markDirty(html = true, style = false)
        actions.changed()
    }

    fun manage() {
        if (disposed) return
        val manager = activity.supportFragmentManager
        manager.setFragmentResultListener(HomeFavoritesPanelFragment.CHANGED, activity) { _, _ ->
            actions.changed(); manager.clearFragmentResultListener(HomeFavoritesPanelFragment.CHANGED)
        }
        val view = actions.current()?.liveView
        if (view == null) HomeFavoritesPanelFragment.show(activity, 0, 0)
        else view.evaluateJavascript("javascript:(function(){var a=document.getElementsByClassName(\"box\"),b=0,c=a.length;if(0<c){var d=a[0].getBoundingClientRect().top;for(i=0;i<c;i++)if(a[i].getBoundingClientRect().top==d)b++;else break;return (d>0?d<<6:0)|b}return b})();") { raw ->
            if (disposed) return@evaluateJavascript
            val packed = if (raw.isNotEmpty() && android.text.TextUtils.isDigitsOnly(raw)) raw.toIntOrNull() ?: 0 else 0
            val top = packed shr 6
            HomeFavoritesPanelFragment.show(activity, packed and 63, if (top > 0) actions.panelHeight(top) else 0)
        }
    }

    fun back(): Boolean = HomeFavoritesPanelFragment.close(activity)
    fun close() {
        (BrowserRootFragment.find(activity)?.childFragmentManager?.findFragmentByTag(HomeFavoriteDialogFragment.TAG) as? HomeFavoriteDialogFragment)?.dismiss()
        HomeFavoritesPanelFragment.close(activity)
    }
    fun dispose() { disposed = true }

    companion object {
        fun unwrap(url: String): String = runCatching {
            val uri = Uri.parse(url)
            if (uri.scheme in listOf("v", "via") && uri.host == "error" && uri.path == "/jump") uri.getQueryParameter("url") ?: url else url
        }.getOrDefault(url)
    }
}
