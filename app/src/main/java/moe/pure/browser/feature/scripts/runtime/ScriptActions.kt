package moe.pure.browser.feature.scripts.runtime

interface ScriptActions {
    fun open(url: String, active: Boolean, insert: Int)
    fun download(url: String, name: String)
}
