package moe.pure.browser.feature.scripts.runtime

import moe.pure.browser.feature.scripts.model.UserScript
import org.json.JSONObject

object ScriptInfo {
    fun build(script: UserScript?): String {
        val metadata = JSONObject()
        if (script != null && script.id > 0) metadata.put("version", script.version).put("downloadURL", script.downloadUrl).put("homepage", script.homepageUrl)
            .put("lastModified", script.updated).put("name", script.name).put("supportURL", script.supportUrl)
        return JSONObject().put("scriptHandler", "Via").put("version", "2.0.0").put("scriptWillUpdate", false).put("script", metadata).toString()
    }
}
