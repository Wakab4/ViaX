package moe.via.browser.ui.pages

import android.view.View
import androidx.fragment.app.Fragment

/** o8.a applies the same accessibility visibility to ordinary and Record child pages. */
abstract class NativePageFragment : Fragment() {
    override fun onPause() { view?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS; super.onPause() }
    override fun onResume() { super.onResume(); view?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO }
}
