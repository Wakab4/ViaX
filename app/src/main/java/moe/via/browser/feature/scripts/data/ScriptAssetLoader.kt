package moe.via.browser.feature.scripts.data

import moe.via.browser.feature.scripts.model.ScriptExecution

data class ScriptAssetPreparation(val references: Int, val skipped: Int, val reused: Int, val downloaded: Int, val failed: Int)

/** p5.a.e/I/K attempt all references, preserving duplicates and the final boolean failure result. */
class ScriptAssetLoader {
    fun prepare(identifier: String, repository: ScriptRepository): Boolean {
        val script = ScriptExecutionCache.get(identifier) ?: return true
        var successful = true
        for (url in script.requires + script.resources.values)
            if (ScriptResourceFileStore.http(url) && !repository.fetchResource(url).success) successful = false
        return successful
    }
    fun prepareRestored(scripts: List<ScriptExecution>, repository: ScriptRepository): ScriptAssetPreparation {
        var references = 0; var skipped = 0; var reused = 0; var downloaded = 0; var failed = 0
        for (script in scripts) for (url in script.requires + script.resources.values) {
            references++
            if (!ScriptResourceFileStore.http(url)) { skipped++; continue }
            when (repository.fetchResource(url)) {
                ScriptResourceFetch.REUSED -> reused++
                ScriptResourceFetch.DOWNLOADED -> downloaded++
                ScriptResourceFetch.FAILED -> failed++
            }
        }
        return ScriptAssetPreparation(references, skipped, reused, downloaded, failed)
    }
    fun download(url: String, repository: ScriptRepository): Boolean = repository.fetchResource(url).success
}
