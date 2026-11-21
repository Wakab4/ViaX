package moe.pure.browser.feature.shortcut

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.dialog.DialogLayoutOwner
import moe.pure.browser.ui.theme.palette

class ShortcutEditor(private val activity: Activity, draft: ShortcutDraft, preview: Bitmap,
    pickImage: () -> Unit, private val approve: (ShortcutDraft) -> Unit) : Dialog(activity, R.style.Theme_PureBrowser_Dialog), DialogLayoutOwner {
    private var selectedIcon = draft.icon
    private var iconOrigin = draft.iconFromUrl
    private var approved = false
    private val legacy = ContextThemeWrapper(activity, if (activity.palette.night) android.R.style.Theme_Black else android.R.style.Theme_Light)
    private val body = LayoutInflater.from(legacy).inflate(R.layout.shortcut_editor, null).also { NativeTypography.tree(it) }
    private val name = body.findViewById<EditText>(R.id.shortcut_name)
    private val url = body.findViewById<EditText>(R.id.shortcut_url)
    private val icon = body.findViewById<ImageView>(R.id.shortcut_icon)
    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE); setContentView(body)
        body.findViewById<TextView>(R.id.shortcut_title).setTextColor(activity.palette.text)
        name.setText(draft.title); url.setText(draft.url)
        listOf(name, url).forEach { it.setTextColor(activity.palette.text); it.setHintTextColor(activity.palette.secondary) }
        icon.setImageBitmap(preview); icon.setOnClickListener { pickImage() }
        body.findViewById<TextView>(R.id.shortcut_cancel).setOnClickListener { cancel() }
        body.findViewById<TextView>(R.id.shortcut_accept).setOnClickListener {
            if (approved) return@setOnClickListener
            val empty = if (url.text.toString().trim().isEmpty()) url else if (name.text.isEmpty()) name else null
            if (empty != null) {
                empty.requestFocus(); empty.error = "不能为空"
                activity.getSystemService(InputMethodManager::class.java).showSoftInput(empty, InputMethodManager.SHOW_IMPLICIT)
            } else { approved = true; approve(snapshot().copy(url = url.text.toString().trim())); dismiss() }
        }
    }
    fun snapshot() = ShortcutDraft(url.text.toString(), name.text.toString(), selectedIcon, iconOrigin)
    fun updateIcon(token: String, bitmap: Bitmap) { selectedIcon = token; iconOrigin = null; icon.setImageBitmap(bitmap) }
    override fun show() { updateGeometry(); super.show(); updateGeometry() }
    override fun updateGeometry() {
        val metrics = activity.resources.displayMetrics
        window?.apply { setLayout(minOf(activity.dp(384), minOf(metrics.widthPixels, metrics.heightPixels) - activity.dp(72)), -2); setGravity(Gravity.CENTER) }
    }
}
