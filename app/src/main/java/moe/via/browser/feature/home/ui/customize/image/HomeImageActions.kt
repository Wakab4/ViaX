package moe.via.browser.feature.home.ui.customize.image

import android.net.Uri
import androidx.fragment.app.Fragment
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.home.background.NativeBackgroundFiles
import moe.via.browser.feature.home.ui.customize.HomeCustomizeState
import moe.via.browser.ui.tasks.NativeLifecycleJobs

/** e9.u/i bind one nullable copy to the View scope, then apply its result on main. */
internal class HomeImageActions(private val owner: Fragment, private val model: HomeCustomizeState,
    private val refreshLogo: () -> Unit, private val backgroundImported: (Int) -> Unit) : AutoCloseable {
    private val jobs = NativeLifecycleJobs { owner.viewLifecycleOwner.lifecycle }

    fun importImage(source: Uri?, logo: Boolean) {
        val context = owner.requireContext()
        jobs.maybe({ HomeImageImporter.copy(context, source, logo) }, failed = { NativeLog.error(null, it) }) { path ->
            if (logo) applyLogo(path) else applyBackground(path)
        }
    }
    private fun applyLogo(path: String) {
        model.preferences.setImageLogo(path)
        model.preferences.markDirty(html = true, style = false)
        model.logoDesign.apply { width = 80; height = 80; radius = 100 }
        refreshLogo(); model.cacheCleared(); model.saveLogo()
    }
    private fun applyBackground(path: String) {
        val color = HomeImageImporter.backgroundColor(path)
        model.preferences.setBackgroundImageResult(path, color)
        model.backgroundDesign.overrideLight = false
        if (color == null) model.backgroundDesign.opacity = 0
        model.saveBackground(notifyDocument = false)
        prepareBackground(if (color == null) null else path)
        model.preferences.markDirty()
        model.documentChanged()
        backgroundImported(if (color == null) 0 else 1)
    }
    /** z8.h.b + e9.i.f3/Z2 + h.d are unbound IO, with the attachment check before posting. */
    private fun prepareBackground(path: String?) {
        BrowserExecutors.io.execute {
            try {
                NativeBackgroundFiles.clear(owner.requireContext())
                if (path != null) NativeBackgroundFiles.prepare(owner.requireContext(), path)
                if (owner.isAdded && !owner.isDetached) owner.requireActivity().runOnUiThread { model.backgroundChanged() }
            } catch (error: Exception) { NativeLog.warn(null, error) }
        }
    }
    override fun close() { jobs.close() }
}
