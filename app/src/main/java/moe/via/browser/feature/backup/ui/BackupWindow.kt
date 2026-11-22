package moe.via.browser.feature.backup.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.text.TextUtils
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import moe.via.browser.feature.backup.model.BackupData
import moe.via.browser.feature.backup.process.BackupPhase
import moe.via.browser.feature.backup.process.BackupProcessOwner
import moe.via.browser.feature.backup.process.BackupSnapshot
import moe.via.browser.feature.passwords.auth.DeviceAuthentication
import moe.via.browser.ui.components.BrowserMessage
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.ui.theme.palette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Rebuilt for each Activity. No data worker or retained callback owns this window binding. */
internal class BackupWindow(private val activity: ThemedActivity, private val owner: BackupProcessOwner,
    launchDocument: (Intent, Int) -> Unit, askPermissions: (Array<String>, Int) -> Unit) {
    private val authentication = DeviceAuthentication(activity)
    private val messages = BrowserMessage(activity)
    private val documents = BackupDocuments(activity, owner.documents, owner::exportTarget, owner::importTarget, owner::cancel,
        { owner.fail() }, launchDocument, askPermissions)
    private var dialog: BrowserDialog? = null
    private var field: EditText? = null
    private var watcher: TextWatcher? = null
    private var displayed: BackupSnapshot? = null
    private var suppressDismiss = false
    private var authActive = false
    private var authSuspended = false
    private var closed = false

    fun render(state: BackupSnapshot) {
        if (closed || activity.isDestroyed || activity.isFinishing || displayed?.revision == state.revision) return
        capture(); dismissDialog(); displayed = state
        when (state.phase) {
            BackupPhase.IDLE -> Unit
            BackupPhase.SELECTION -> selection(state)
            BackupPhase.AUTHENTICATION -> authenticate()
            BackupPhase.EXPORT_PASSWORD, BackupPhase.IMPORT_PASSWORD -> password(state)
            BackupPhase.EXPORT_DOCUMENT -> if (owner.launchDocument()) chooseOutput()
            BackupPhase.IMPORT_DOCUMENT -> if (owner.launchDocument()) documents.chooseImport()
            BackupPhase.COUNTING_PASSWORDS, BackupPhase.PREPARING_IMPORT, BackupPhase.IMPORTING, BackupPhase.EXPORTING -> waiting()
            BackupPhase.RESULT -> report(state)
        }
    }
    private fun selection(state: BackupSnapshot) {
        val value = BrowserDialog(activity, "导出数据")
            .multipleChoice(BackupData.entries.map { it.title }, state.selection.map { BackupData.entries.indexOf(it) }.toSet())
            .positive("确定") { current ->
                owner.beginExport(current.selectedIndices.mapNotNull { BackupData.entries.getOrNull(it) }.toSet()); true
            }.negative { owner.cancel(); true }
        show(value, owner::cancel)
    }
    private fun authenticate() {
        if (authActive) return
        authActive = true
        authentication.authenticate("导出数据", "解锁设备来导出密码", {
            authActive = false; if (!closed && !authSuspended) owner.authenticated(true)
        }, {
            authActive = false; if (!closed && !authSuspended) owner.authenticated(false)
        }, { message ->
            authActive = false
            if (!closed && !authSuspended) { owner.authenticated(false); if (!message.isNullOrEmpty()) messages.show(message) }
        })
    }
    private fun password(state: BackupSnapshot) {
        val exporting = state.phase == BackupPhase.EXPORT_PASSWORD
        val value = BrowserDialog(activity, if (exporting) "输入密码" else "验证密码",
            if (exporting) "你正在导出隐私数据，请输入密码以加密数据。请牢记密码，如果密码丢失，你将无法导入其中加密的数据。"
            else "你导入的数据包含加密内容，请输入密码用以解密数据。如果密码错误，将只导入其中未加密的数据。")
        val editor = value.field("", "密码")
        editor.isSaveEnabled = false; editor.isSaveFromParentEnabled = false
        editor.importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO
        val draft = owner.passwordDraft()
        try { editor.setText(draft, 0, draft.size); editor.setSelection(editor.length()) } finally { draft.fill('\u0000') }
        field = editor
        watcher = object : TextWatcher {
            override fun beforeTextChanged(value: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(value: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(value: Editable?) = capturePassword()
        }.also(editor::addTextChangedListener)
        value.positive("确定") { acceptPassword(); true }.negative { owner.cancel(); true }
        show(value, owner::cancel)
    }
    private fun acceptPassword() {
        val value = fieldValue() ?: CharArray(0)
        try { owner.acceptPassword(value) } finally { value.fill('\u0000') }
    }
    private fun chooseOutput() {
        val locale = Locale.getDefault()
        val pattern = when (locale.country) { "US" -> "MMddyyyy"; "UK" -> "ddMMyyyy"; else -> "yyyyMMdd" }
        val label = activity.applicationInfo.loadLabel(activity.packageManager)
        documents.export("${label}_导出数据_${SimpleDateFormat(pattern, locale).format(Date())}.zip")
    }
    private fun waiting() {
        val content = LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(24), dp(20), dp(24), dp(20))
            addView(ProgressBar(activity), LinearLayout.LayoutParams(dp(32), dp(32)))
            addView(NativeTypography.text(activity).apply { text = "稍等片刻…"; textSize = 14f; setTextColor(activity.palette.text) },
                LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(20) })
        }
        val value = BrowserDialog(activity).custom(content)
        value.setCancelable(false); value.setCanceledOnTouchOutside(false); show(value)
    }
    private fun report(state: BackupSnapshot) {
        val result = state.outcome ?: return
        if (result.failure != null) { messages.show(result.failure); owner.acknowledge(); return }
        val errors = result.result?.errors.orEmpty()
        if (errors.isNotEmpty()) {
            show(BrowserDialog(activity, if (result.exporting) "导出失败" else "导入失败", errors.joinToString("\n") { it.error.orEmpty() })
                .positive("确定") { owner.acknowledge(); true }, owner::acknowledge)
        } else if (result.path != null) {
            show(BrowserDialog(activity, "提示", "导出数据到：${result.path}").positive("确定") { owner.acknowledge(); true }, owner::acknowledge)
        } else { messages.show(if (result.exporting) "数据已导出" else "数据已导入"); owner.acknowledge() }
    }
    private fun show(value: BrowserDialog, cancelled: (() -> Unit)? = null) {
        dialog = value
        val revision = owner.snapshot().revision
        activity.dialogScope.show(value) { if (!closed && !suppressDismiss && owner.snapshot().revision == revision) cancelled?.invoke() }
    }
    fun capture() {
        val state = displayed ?: return
        if (state.phase == BackupPhase.SELECTION) dialog?.selectedIndices?.let { indices -> owner.recordSelection(indices.mapNotNull { BackupData.entries.getOrNull(it) }.toSet()) }
        capturePassword()
    }
    private fun capturePassword() {
        val value = fieldValue() ?: return
        try { owner.recordPassword(value) } finally { value.fill('\u0000') }
    }
    private fun fieldValue(): CharArray? {
        val value = field?.text ?: return null
        return CharArray(value.length).also { TextUtils.getChars(value, 0, value.length, it, 0) }
    }
    private fun dismissDialog() {
        suppressDismiss = true
        field?.let { editor -> watcher?.let(editor::removeTextChangedListener); editor.setText("") }
        field = null; watcher = null; dialog?.dismiss(); dialog = null; suppressDismiss = false
    }
    fun result(request: Int, result: Int, data: Intent?): Boolean {
        val previous = authActive
        if (authentication.onActivityResult(request, result)) {
            if (!previous && owner.phase == BackupPhase.AUTHENTICATION) owner.authenticated(result == android.app.Activity.RESULT_OK)
            return true
        }
        return documents.onActivityResult(request, result, data)
    }
    fun permissions(request: Int, results: IntArray) = documents.onRequestPermissionsResult(request, results)
    fun interrupted() = messages.show("操作已中断，请重新选择备份文件或导出目标")
    fun pause(recreating: Boolean) {
        capture()
        if (recreating) { authSuspended = true; authActive = false; authentication.cancel() }
    }
    fun dispose() {
        if (closed) return
        capture(); closed = true
        authentication.dispose(); dismissDialog(); displayed = null; messages.close()
    }
}
