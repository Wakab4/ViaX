package moe.via.browser.feature.ai.ui.settings

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.theme.skin.NativeSkinIcons
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.via.browser.ui.editor.NativePasswordVisibility
import moe.via.browser.ui.theme.palette

internal class AiBackendFields(activity: Activity, add: () -> Unit, openManagement: () -> Unit) {
    val root = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(12), 0, 0) }
    val title = AiLineField(activity, R.string.ref_search_page_title_hint, 524289, 10)
    val url = AiLineField(activity, R.string.ref_search_page_address_hint, 16, 10)
    val key = AiFields.editor(activity, R.string.ref_ai_page_key, 129).apply {
        setLines(1); minLines = 1; maxLines = 1; imeOptions = EditorInfo.IME_ACTION_NEXT; setAutofillHints(View.AUTOFILL_HINT_PASSWORD)
        setPaddingRelative(paddingStart, dp(10), paddingEnd, dp(10)); setBackgroundResource(R.drawable.ref_ai_field_underline); NativePasswordVisibility.attach(this)
    }
    val pendingModel = AiLineField(activity, R.string.ref_ai_page_model, 524289, 10)
    val management = NativeTypography.text(activity).apply {
        val caption = SpannableString(activity.getString(R.string.ref_ai_page_management)); caption.setSpan(UnderlineSpan(), 0, caption.length, 0); text = caption
        setTextColor(activity.palette.secondary); setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.settings_detail_size).toFloat())
        visibility = View.GONE; setOnClickListener { openManagement() }
    }
    val models = RecyclerView(activity).apply { layoutManager = LinearLayoutManager(activity); NativeRecyclerEdgeEffect.install(this) }
    init {
        root.addView(title, AiFields.margin(activity)); root.addView(url, AiFields.margin(activity)); root.addView(key, AiFields.margin(activity)); root.addView(management, AiFields.margin(activity))
        root.addView(NativeTypography.text(activity).apply {
            setText(R.string.ref_ai_page_model); setTextColor(activity.palette.accent); typeface = Typeface.DEFAULT_BOLD; textDirection = View.TEXT_DIRECTION_LOCALE
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.settings_detail_size).toFloat()); setPaddingRelative(dp(16), dp(12), dp(16), dp(4))
        }, LinearLayout.LayoutParams(-1, -2))
        val row = LinearLayout(activity)
        row.addView(pendingModel, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = activity.dp(16) })
        row.addView(ImageView(activity).apply {
            setImageDrawable(NativeSkinIcons(activity).resolve(R.drawable.ref_ic_add, "ic_add") ?: activity.getDrawable(R.drawable.ref_ic_add))
            imageTintList = ColorStateList.valueOf(activity.palette.icon); setPaddingRelative(dp(13), 0, dp(13), 0); contentDescription = activity.getString(R.string.ref_ai_page_add_model)
            setBackgroundResource(R.drawable.dialog_button_feedback); setOnClickListener { add() }
        }, LinearLayout.LayoutParams(activity.dp(48), activity.dp(40)).apply { marginEnd = activity.dp(6); gravity = Gravity.CENTER_VERTICAL })
        root.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = activity.dp(6) }); root.addView(models, LinearLayout.LayoutParams(-1, -1))
        pendingModel.editor.imeOptions = EditorInfo.IME_ACTION_GO
        pendingModel.editor.setOnKeyListener { _, code, event -> if (code != KeyEvent.KEYCODE_ENTER) false else { if (event.action == KeyEvent.ACTION_UP) add(); true } }
    }
    fun editors() = listOf(title.editor, url.editor, key, pendingModel.editor)
    fun values() = editors().map { it.text.toString().trim { char -> char <= ' ' } }
    fun close() { models.stopScroll(); models.itemAnimator?.endAnimations(); (models.edgeEffectFactory as? NativeRecyclerEdgeEffect)?.close(); models.adapter = null }
}
