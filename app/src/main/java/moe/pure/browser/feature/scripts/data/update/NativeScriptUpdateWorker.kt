package moe.pure.browser.feature.scripts.data.update

import moe.pure.browser.feature.scripts.data.ScriptAssetLoader
import moe.pure.browser.feature.scripts.data.ScriptRepository
import moe.pure.browser.feature.scripts.data.ScriptSourceLoader
import moe.pure.browser.feature.scripts.model.ScriptParser
import moe.pure.browser.feature.scripts.model.ScriptUpdateTarget

/** sa.d1.j3 and sb.q.e differ in the enabled state they submit and when dependencies are attempted. */
internal class NativeScriptUpdateWorker(private val repository: ScriptRepository) {
    fun update(target: ScriptUpdateTarget, automatic: Boolean): Boolean {
        val url = target.url ?: return false
        val parsed = ScriptParser.parse(ScriptSourceLoader.load(url).orEmpty(), url) ?: return false
        val incoming = if (automatic) parsed else parsed.copy(enabled = target.enabled)
        if (!repository.updateFromSource(target.id, incoming)) return false
        if (automatic) ScriptAssetLoader().prepare(incoming.key, repository)
        return true
    }
}
