package moe.pure.browser.feature.home.ui.favorites

import android.animation.ValueAnimator
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import moe.pure.browser.R
import moe.pure.browser.ui.components.dp
import android.os.Build
import moe.pure.browser.ui.theme.palette
import kotlin.math.abs

/** qa.e1 is an overlay Fragment in the existing window; it does not create or blur a Dialog. */
class HomeFavoritesPanelFragment : Fragment() {
    private val state get() = ViewModelProvider(this)[HomeFavoritesPanelState::class.java]
    private var pager: ViewPager2? = null
    private var previousSoftInput = 0
    private var heightAnimation: ValueAnimator? = null
    private var heightChange: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previousSoftInput = requireActivity().window.attributes.softInputMode
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FrameLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(-1, -1)
            setBackgroundColor(0x40808080)
            setOnClickListener { parentFragmentManager.popBackStack() }
            addView(ViewPager2(context).also { pager = it; it.id = R.id.home_favorite_pages },
                FrameLayout.LayoutParams(-1, context.dp(300), Gravity.BOTTOM))
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        state.passedCount = arguments?.getInt("count", 0) ?: 0
        val pages = checkNotNull(pager)
        resize(Resources.getSystem().configuration)
        pages.setOnClickListener { }
        pages.isUserInputEnabled = false
        pages.adapter = object : FragmentStateAdapter(childFragmentManager, lifecycle) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment =
                if (position == 0) HomeFavoritesGridFragment() else HomeFavoriteEditFragment()
        }
        pages.setCurrentItem(1, false)
        pages.setCurrentItem(0, false)
        val radius = requireContext().dp(18).toFloat()
        pages.background = GradientDrawable().apply {
            setColor(if (requireContext().palette.night) 0xCC000000.toInt() else 0xEBFFFFFF.toInt())
            cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
        }
        state.page.observe(viewLifecycleOwner) { pages.setCurrentItem(it, true) }
        state.close.observe(viewLifecycleOwner) {
            changed()
            parentFragmentManager.popBackStack()
        }
        ViewCompat.setOnApplyWindowInsetsListener(pages) { _, insets ->
            if (Build.VERSION.SDK_INT >= 29) {
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                pages.setPadding(bars.left, 0, bars.right, bars.bottom)
            }
            insets
        }
    }

    private fun resize(configuration: Configuration) {
        val pages = pager ?: return
        heightChange?.let(pages::removeCallbacks)
        heightAnimation?.cancel()
        val size = NativeFavoriteGeometry.height(requireContext(), configuration, arguments?.getInt("height", 0) ?: 0)
        val animate = size.requested != size.target && abs(size.target - size.requested) <= requireContext().dp(96)
        pages.layoutParams = pages.layoutParams.apply { height = if (animate) size.requested else size.target }
        if (animate) {
            val change = Runnable {
                heightAnimation = ValueAnimator.ofInt(size.requested, size.target).apply {
                    duration = (abs(size.requested - size.target) / 512f * 100f + 80f).toLong()
                    interpolator = PathInterpolator(.2f, .2f, .8f, .8f)
                    addUpdateListener { pages.layoutParams = pages.layoutParams.apply { height = it.animatedValue as Int } }
                    start()
                }
            }
            heightChange = change
            pages.postDelayed(change, 100)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        arguments?.clear()
        super.onConfigurationChanged(newConfig)
        resize(newConfig)
    }

    override fun onResume() { super.onResume(); view?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO }
    override fun onPause() { view?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS; super.onPause() }
    override fun onDestroyView() {
        heightChange?.let { pager?.removeCallbacks(it) }; heightChange = null
        heightAnimation?.cancel(); heightAnimation = null
        pager = null
        super.onDestroyView()
    }
    override fun onDestroy() {
        changed()
        if (previousSoftInput != 0) requireActivity().window.setSoftInputMode(previousSoftInput)
        super.onDestroy()
    }
    private fun changed() { parentFragmentManager.setFragmentResult(CHANGED, Bundle()) }

    companion object {
        private const val TAG = "home_favorite_panel"
        const val CHANGED = "favoriteChanged"

        fun show(activity: FragmentActivity, count: Int, height: Int) {
            val panel = HomeFavoritesPanelFragment().apply {
                arguments = Bundle().apply { putInt("count", count); putInt("height", height) }
            }
            activity.supportFragmentManager.beginTransaction().setReorderingAllowed(true)
                .setCustomAnimations(R.anim.ref_favorite_enter, R.anim.ref_favorite_hold,
                    R.anim.ref_favorite_hold, R.anim.ref_favorite_exit)
                .add(android.R.id.content, panel, TAG).addToBackStack(TAG).commit()
        }

        fun close(activity: FragmentActivity): Boolean {
            val manager = activity.supportFragmentManager
            if (manager.findFragmentByTag(TAG) == null) return false
            manager.popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            return true
        }
    }
}
