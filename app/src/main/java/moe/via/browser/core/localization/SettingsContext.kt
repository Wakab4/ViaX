package moe.via.browser.core.localization

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import java.util.Locale

/** Native Shell places the selected language before the existing locale fallbacks. */
object SettingsContext {
    fun language(context: Context): String = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
        .getString("language", "").orEmpty().trim().let(AppLanguages::selected)

    fun signature(context: Context): String = language(context).let { selected ->
        if (selected.isEmpty()) "system:${Resources.getSystem().configuration.locales.toLanguageTags()}" else selected
    }

    fun wrap(base: Context): Context {
        val selected = language(base)
        val system = Resources.getSystem().configuration.locales
        val fallbacks = base.resources.configuration.locales
        val locales = if (selected.isEmpty()) system else {
            val primary = Locale.forLanguageTag(selected.replace('_', '-'))
            if (primary.language.isEmpty()) system else {
                val ordered = mutableListOf(primary)
                for (index in 0 until fallbacks.size()) fallbacks[index].takeUnless { it in ordered }?.let(ordered::add)
                LocaleList(*ordered.toTypedArray())
            }
        }
        val configuration = Configuration().apply {
            setLocales(locales)
            if (!locales.isEmpty) setLayoutDirection(locales[0])
        }
        val wrapped = base.createConfigurationContext(configuration)
        applyProcessLocale(wrapped)
        return wrapped
    }

    fun applyProcessLocale(context: Context) {
        val locales = context.resources.configuration.locales
        if (!locales.isEmpty) LocaleList.setDefault(locales)
    }
}
