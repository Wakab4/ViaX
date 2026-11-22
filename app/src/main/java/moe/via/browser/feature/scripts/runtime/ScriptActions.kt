package moe.via.browser.feature.scripts.runtime

interface ScriptActions {
    fun open(url: String, active: Boolean, insert: Int)
    fun download(url: String, name: String)
}
