package moe.via.browser.feature.home.ui.customize

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import moe.via.browser.R
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.insets.NativePageInsets
import moe.via.browser.ui.components.toolbar.NativePageToolbar
import moe.via.browser.ui.theme.palette
import java.util.WeakHashMap
import kotlin.math.abs

/** Original q.xml owns geometry; the page stack supplies the existing Activity window. */
internal class CustomizeWorkspace(activity: Activity, selected: (DesignPage) -> Unit,
    back: () -> Unit, setHomepage: () -> Unit, applyInsets: Boolean) {
    val toolbar = NativePageToolbar(activity, activity.getString(R.string.ref_home_design_title), back).apply {
        setBackgroundColor(WORKSPACE); setContentColor(Color.WHITE)
    }
    private val content = LayoutInflater.from(activity).inflate(R.layout.home_customize, null)
    val previewHost: FrameLayout = content.findViewById(R.id.home_design_preview)
    val toolsHost: FrameLayout = content.findViewById(R.id.home_design_tools)
    val tabs: RadioGroup = content.findViewById(R.id.home_design_tabs)
    private val hint: View = content.findViewById(R.id.home_design_hint)
    private val surface = if (activity.palette.night) 0xCC000000.toInt() else 0xEBFFFFFF.toInt()
    private val radius = activity.dp(18).toFloat()
    private val taps = WeakHashMap<View, Long>()
    val root = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL; setBackgroundColor(WORKSPACE)
        addView(toolbar, LinearLayout.LayoutParams(-1, -2)); addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
    }
    init {
        NativeTypography.tree(content)
        tabs.background = rounded(); toolsHost.background = rounded(); tabs.clipToPadding = true
        if (applyInsets) NativePageInsets(root, null).bottom(tabs)
        for ((id, page) in PAGE_IDS) content.findViewById<RadioButton>(id).setOnClickListener { view -> click(view) { selected(page) } }
        content.findViewById<View>(R.id.home_design_set_home).setOnClickListener { view -> click(view, setHomepage) }
    }
    private fun click(view: View, action: () -> Unit) {
        val previous = taps[view]; val now = SystemClock.elapsedRealtime()
        if (previous == null || abs(now - previous) > 300) { taps[view] = now; action() }
    }
    private fun rounded() = GradientDrawable().apply {
        setColor(surface); cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
    }
    fun showPage(page: DesignPage?) {
        if (page == null) {
            if (toolsHost.visibility == View.VISIBLE) toolsHost.animate().translationY(toolsHost.height.toFloat())
                .setDuration(200).setInterpolator(PathInterpolator(.2f, .2f, .8f, .8f)).withStartAction(null)
                .withEndAction { tabs.background = rounded(); toolsHost.visibility = View.GONE }.start()
        } else if (toolsHost.visibility == View.GONE) {
            toolsHost.animate().translationY(0f).setDuration(200).setInterpolator(PathInterpolator(.2f, .2f, .8f, .8f))
                .withStartAction { toolsHost.visibility = View.VISIBLE }
                .withEndAction { tabs.setBackgroundColor(surface) }.start()
        }
    }
    fun clearSelection() = tabs.clearCheck()
    fun homepageHint(show: Boolean) { hint.visibility = if (show) View.VISIBLE else View.GONE }
    fun dismissHomepageHint() {
        hint.animate().alpha(0f).translationY(-100f).setDuration(240)
            .withEndAction { hint.visibility = View.GONE }.start()
    }
    companion object {
        const val WORKSPACE = -14540254
        val PAGE_IDS = linkedMapOf(R.id.home_design_logo to DesignPage.LOGO, R.id.home_design_search to DesignPage.SEARCH,
            R.id.home_design_background to DesignPage.BACKGROUND, R.id.home_design_favorites to DesignPage.FAVORITES,
            R.id.home_design_advanced to DesignPage.ADVANCED)
    }
}
