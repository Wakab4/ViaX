package moe.pure.browser.feature.home.ui.customize

import java.util.Locale

/** h9.a/b/c/d: raw numeric values, mutable command summary and boolean-dependent labels. */
internal sealed class DesignTool(val id: Int, val icon: Int) {
    abstract val title: String?
    abstract val summary: String?
    class Number(id: Int, override val title: String?, icon: Int, val range: IntRange, var value: Int,
        private val format: String?, val specialValue: Int = -1, private val specialText: String? = null) : DesignTool(id, icon) {
        override val summary: String? get() {
            if (value == specialValue) return specialText
            return if (format != null && format.contains('%')) String.format(Locale.getDefault(), format, value) else format
        }
    }
    class Command(id: Int, override val title: String?, icon: Int, override var summary: String? = null) : DesignTool(id, icon)
    class Toggle(id: Int, icon: Int, private val trueTitle: String?, private val trueSummary: String?,
        private val falseTitle: String?, private val falseSummary: String?, var value: Boolean) : DesignTool(id, icon) {
        override val title: String? get() = if (value || falseTitle == null) trueTitle else falseTitle
        override val summary: String? get() = if (value || falseSummary == null) trueSummary else falseSummary
    }
}

internal enum class DesignPage { LOGO, SEARCH, BACKGROUND, FAVORITES, ADVANCED }
