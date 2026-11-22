package moe.via.browser.ui.pages

internal interface PredictiveBackPreview {
    fun progress(value: Float)
    fun cancel()
    fun complete()
}
