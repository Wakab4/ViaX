package moe.via.browser.feature.backup.model

/** The caller supplies the envelope format; individual site records never change it. */
enum class SettingsImportKind {
    CURRENT_LINES,
    LEGACY_CONF
}
