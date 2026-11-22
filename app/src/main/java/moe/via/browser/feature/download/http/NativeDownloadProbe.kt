package moe.via.browser.feature.download.http

import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.UnknownHostException
import java.security.GeneralSecurityException
import moe.via.browser.feature.download.transfer.DownloadControl
import moe.via.browser.feature.download.model.DownloadMetadata
import moe.via.browser.feature.download.data.DownloadRepository
import moe.via.browser.feature.download.model.DownloadTask
import moe.via.browser.feature.download.transfer.DownloadFailure

internal data class ProbedDownload(val task: DownloadTask, val metadata: DownloadMetadata)

/** m5.e.j/a: ordered Referrer/UA negotiation, with the current URL retained across redirects. */
internal class NativeDownloadProbe(private val repository: DownloadRepository, private val control: DownloadControl) {
    fun run(task: DownloadTask): ProbedDownload {
        val source = task.url
        val referer = task.headers["Referer"]
        val referers: List<String?> = if (referer.isNullOrEmpty()) listOf(source, null)
            else if (referer == source) listOf(null, referer) else listOf(null, source, referer)
        val agent = task.headers["User-Agent"]
        val agents: List<String?> = if (agent == null) listOf(null) else listOf(null, agent)
        var refIndex = referers.lastIndex
        var agentIndex = agents.lastIndex
        var lastFailure: DownloadFailure? = null
        val factory = NativeDownloadConnection(control)
        while (refIndex >= 0 && agentIndex >= 0) {
            control.check()
            val selectedRef = referers[refIndex]
            val selectedAgent = agents[agentIndex]
            repository.requestUrl(task.id, source)
            var connection: HttpURLConnection? = null
            lastFailure = null
            try {
                val response = factory.open(source, { NativeDownloadHeaders.probe(it, task, selectedRef, selectedAgent) },
                    redirected = { url, _ -> repository.requestUrl(task.id, url) })
                connection = response
                control.check()
                refIndex--
                if (refIndex < 0) { agentIndex--; if (agentIndex >= 0) refIndex = referers.lastIndex }
                val code = response.responseCode
                if (code != 200 && code != 206) lastFailure = DownloadFailure.http(code, response.responseMessage)
                else NativeDownloadProbeMetadata.accept(response, task)?.let { accepted ->
                    val headers = task.headers.toMutableMap()
                    accepted.metadata.etag?.takeIf(String::isNotEmpty)?.let { headers["ETag"] = it }
                    if (selectedRef == null) headers.remove("Referer") else headers["Referer"] = selectedRef
                    if (selectedAgent == null) headers.remove("User-Agent") else headers["User-Agent"] = selectedAgent
                    repository.acceptProbe(task.id, response.url.toString(), headers, accepted.name, accepted.mime, accepted.metadata)
                    return ProbedDownload(repository.get(task.id) ?: throw DownloadFailure(1), accepted.metadata)
                }
            } catch (error: Exception) {
                val failure = DownloadFailure.from(error)
                lastFailure = failure
                if (failure.code == 1 || error is UnknownHostException) throw failure
                // Native's pre-response IO/redirect failure keeps the same header indices until cancellation.
                if (error !is MalformedURLException && error !is GeneralSecurityException && failure.code !in setOf(20, 25)) throw failure
            } finally { connection?.let(control::detach) }
        }
        lastFailure?.let { throw it }
        val current = repository.get(task.id) ?: throw DownloadFailure(1)
        return ProbedDownload(current, DownloadMetadata(current.total, current.nativeFlags and 1 != 0,
            current.etag, current.lastModified, current.mime, ready = false))
    }
}
