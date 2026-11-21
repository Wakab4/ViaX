package moe.pure.browser.feature.settings.host

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import moe.pure.browser.feature.settings.actions.SettingsRuntimeActions
import moe.pure.browser.feature.settings.pages.SettingsPageFragment
import moe.pure.browser.feature.settings.ui.SettingsCommand
import moe.pure.browser.ui.theme.ThemedActivity

/** The window owns runtime operations; this Fragment owns their document/permission result channels. */
class SettingsRuntimeFragment : Fragment() {
    private var actions: SettingsRuntimeActions? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = requireActivity() as ThemedActivity
        actions = SettingsRuntimeActions(window, ::refresh,
            { intent, code -> startActivityForResult(intent, code) },
            { permissions, code -> requestPermissions(permissions, code) })
            .also { it.restore(savedInstanceState?.getBundle("backup")) }
    }
    private fun refresh() {
        val window = activity as? ThemedActivity ?: return
        (window.currentRecordStack?.top as? SettingsPageFragment)?.refreshSettings()
    }
    internal fun perform(command: SettingsCommand) = requireNotNull(actions).perform(command)
    internal fun customize() = requireNotNull(actions).customize()
    override fun onResume() { super.onResume(); actions?.resume() }
    override fun onPause() { actions?.pause(); super.onPause() }
    @Deprecated("Framework scoped document results")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (actions?.result(requestCode, resultCode, data) != true) super.onActivityResult(requestCode, resultCode, data)
    }
    @Deprecated("Framework scoped permission results")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (actions?.permissions(requestCode, grantResults) != true) super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    override fun onSaveInstanceState(outState: Bundle) { actions?.save(outState); super.onSaveInstanceState(outState) }
    override fun onDestroy() { actions?.close(); actions = null; super.onDestroy() }
    companion object {
        private const val TAG = "settings-window-runtime"
        internal fun ensure(window: ThemedActivity): SettingsRuntimeFragment {
            val manager = window.supportFragmentManager
            val existing = manager.findFragmentByTag(TAG) as? SettingsRuntimeFragment
            if (existing != null) return existing
            return SettingsRuntimeFragment().also {
                manager.beginTransaction().add(it, TAG).commitNow()
            }
        }
        internal fun of(window: ThemedActivity) = window.supportFragmentManager.findFragmentByTag(TAG) as SettingsRuntimeFragment
    }
}
