package moe.via.browser.feature.scan.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.app.Activity
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.view.Gravity
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.ui.theme.skin.NativeSkinIcons
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

@SuppressLint("ClickableViewAccessibility")
internal class ScannerPage(private val activity: Activity, back: () -> Unit, toggleTorch: () -> Unit,
    gallery: () -> Unit, zoom: (Float) -> Unit, retry: () -> Unit) {
    private val icons = NativeSkinIcons(activity)
    val root = FrameLayout(activity).apply { setBackgroundColor(Color.BLACK); isFocusableInTouchMode = true }
    val preview = ScannerPreview(activity)
    val finder = ScannerViewfinder(activity)
    private val overlay = FrameLayout(activity)
    private val header = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
    val status = NativeTypography.text(activity).apply {
        textSize = 16f; setTextColor(0xB2FFFFFF.toInt()); gravity = Gravity.CENTER
        setPadding(dp(16), 0, dp(16), 0); visibility = View.GONE
        setOnClickListener { retry() }
    }
    val torch = button(R.drawable.ref_scan_torch, activity.getString(R.string.ref_scan_page_torch), toggleTorch)
    val galleryButton = button(R.drawable.ref_scan_gallery, activity.getString(R.string.ref_scan_page_gallery), gallery)
    var frameSide = 0
        private set
    private var insetTop = 0
    private var insetBottom = 0
    private var insetRight = 0
    private val scale = ScaleGestureDetector(activity, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector) = true
        override fun onScale(detector: ScaleGestureDetector): Boolean { zoom(detector.scaleFactor); return true }
    })
    init {
        preview.setOnTouchListener { _, event -> scale.onTouchEvent(event); true }
        root.addView(preview, FrameLayout.LayoutParams(-1, -1))
        root.addView(finder, FrameLayout.LayoutParams(-1, -1))
        root.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        header.addView(ImageView(activity).apply {
            setImageDrawable(icons.resolve(R.drawable.ref_ic_back, "ic_back") ?: activity.getDrawable(R.drawable.ref_ic_back)); imageTintList = ColorStateList.valueOf(Color.WHITE)
            setPadding(dp(13), 0, dp(13), 0); contentDescription = activity.getString(R.string.ref_scan_page_back); setOnClickListener { back() }
        }, LinearLayout.LayoutParams(activity.dp(48), activity.dp(48)))
        header.addView(NativeTypography.text(activity).apply {
            setText(R.string.ref_scan_page_title); textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(0, activity.dp(48), 1f))
        overlay.addView(header, FrameLayout.LayoutParams(-1, activity.dp(48)))
        overlay.addView(status, FrameLayout.LayoutParams(-1, -1).apply { topMargin = activity.dp(54) })
        overlay.addView(torch, FrameLayout.LayoutParams(activity.dp(48), activity.dp(48)))
        overlay.addView(galleryButton, FrameLayout.LayoutParams(activity.dp(48), activity.dp(48)))
        overlay.setOnApplyWindowInsetsListener { _, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                insetTop = bars.top; insetBottom = bars.bottom; insetRight = bars.right
            } else {
                @Suppress("DEPRECATION")
                insetTop = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                insetBottom = insets.systemWindowInsetBottom
                @Suppress("DEPRECATION")
                insetRight = insets.systemWindowInsetRight
            }
            configure(activity.resources.configuration)
            insets
        }
        configure(activity.resources.configuration)
    }

    fun configure(configuration: Configuration) {
        val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val factor = if (landscape) .6 else .75
        frameSide = minOf((minOf(activity.dp(configuration.screenWidthDp), activity.dp(configuration.screenHeightDp)) * factor).toInt(), activity.dp(320))
        finder.frameSize(frameSide)
        header.layoutParams = FrameLayout.LayoutParams(-1, activity.dp(48)).apply { topMargin = insetTop }
        status.layoutParams = FrameLayout.LayoutParams(-1, -1).apply { topMargin = activity.dp(54) + insetTop; bottomMargin = insetBottom }
        val halfSeparation = frameSide / 2 - activity.dp(48) / 2
        if (landscape) {
            val balance = kotlin.math.abs(insetTop - insetBottom) / 2
            torch.layoutParams = FrameLayout.LayoutParams(activity.dp(48), activity.dp(48), Gravity.END or Gravity.CENTER_VERTICAL).apply {
                setMargins(0, 0, activity.dp(32) + insetRight, halfSeparation + balance)
            }
            galleryButton.layoutParams = FrameLayout.LayoutParams(activity.dp(48), activity.dp(48), Gravity.END or Gravity.CENTER_VERTICAL).apply {
                setMargins(0, halfSeparation - balance, activity.dp(32) + insetRight, 0)
            }
        } else {
            torch.layoutParams = FrameLayout.LayoutParams(activity.dp(48), activity.dp(48), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                setMargins(0, 0, halfSeparation, activity.dp(32) + insetBottom)
            }
            galleryButton.layoutParams = FrameLayout.LayoutParams(activity.dp(48), activity.dp(48), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                setMargins(halfSeparation, 0, 0, activity.dp(32) + insetBottom)
            }
        }
    }

    fun torch(on: Boolean, enabled: Boolean) {
        torch.isSelected = on; torch.isEnabled = enabled; torch.alpha = if (enabled) 1f else .5f
        torch.imageTintList = ColorStateList.valueOf(if (on) activity.palette.accent else Color.WHITE)
    }

    fun message(text: String?, enabled: Boolean = true) {
        status.text = text; status.isEnabled = enabled; status.visibility = if (text == null) View.GONE else View.VISIBLE
    }

    private fun button(icon: Int, description: String, action: () -> Unit) = ImageView(activity).apply {
        setImageDrawable(icons.resolve(icon, if (icon == R.drawable.ref_scan_torch) "ic_torch" else "ic_image") ?: activity.getDrawable(icon)); imageTintList = ColorStateList.valueOf(Color.WHITE)
        scaleType = ImageView.ScaleType.FIT_CENTER
        contentDescription = description; setPadding(dp(10), dp(10), dp(10), dp(10))
        background = StateListDrawable().apply {
            fun circle(fill: Int, stroke: Int) = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(fill); setStroke(activity.dp(1), stroke) }
            addState(intArrayOf(-android.R.attr.state_selected, -android.R.attr.state_pressed), circle(0x20FFFFFF, Color.TRANSPARENT))
            addState(intArrayOf(-android.R.attr.state_selected, android.R.attr.state_pressed), circle(0x20FFFFFF, 0x20FFFFFF))
            addState(intArrayOf(android.R.attr.state_selected, -android.R.attr.state_pressed), circle(Color.WHITE, Color.WHITE))
        }
        setOnClickListener { action() }
    }
}
