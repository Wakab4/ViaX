package moe.via.browser.core.localization

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import java.util.Locale

object SettingsContext {
    fun language(context: Context): String = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
        .getString("language", "").orEmpty().trim().let(AppLanguages::selected)

    fun signature(context: Context): String = language(context).let { selected ->
        if (selected.isEmpty()) "system:${Resources.getSystem().configuration.locales.toLanguageTags()}" else selected
    }

    fun wrap(base: Context, night: Boolean? = null): Context {
        val selected = language(base)
        val locales = if (selected.isEmpty()) Resources.getSystem().configuration.locales
            else LocaleList(Locale.forLanguageTag(selected))
        val configuration = Configuration().apply {
            setLocales(locales)
            if (night != null) {
                val mode = if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                uiMode = (base.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
            }
        }
        return base.createConfigurationContext(configuration)
    }

    fun applyProcessLocale(context: Context) {
        val locales = context.resources.configuration.locales
        if (!locales.isEmpty) LocaleList.setDefault(locales)
    }
}
