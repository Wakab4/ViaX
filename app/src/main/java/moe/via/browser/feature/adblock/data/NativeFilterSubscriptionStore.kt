package moe.via.browser.feature.adblock.data

import android.content.Context
import moe.via.browser.feature.adblock.FilterSubscription
import moe.via.browser.feature.adblock.rules.CustomRuleFiles
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.IOException
import java.util.UUID

/** z4.a keeps one process list and writes the original subscribed.txt lines directly. */
internal class NativeFilterSubscriptionStore private constructor(context: Context) {
    val directory: File = CustomRuleFiles.file(context).parentFile!!.also { it.mkdirs() }
    private val manifest = File(directory, "subscribed.txt")
    private var loaded = false
    private var records: MutableList<FilterSubscription> = ArrayList()
    val missing: Boolean get() = manifest.isDirectory || !manifest.exists()
    fun newPath(): String = File(directory, UUID.randomUUID().toString() + ".txt").absolutePath
    fun all(): MutableList<FilterSubscription> {
        if (!loaded) { load(); loaded = true }
        return records
    }
    private fun load() {
        if (missing) return
        try {
            val result = ArrayList<FilterSubscription>()
            BufferedReader(InputStreamReader(FileInputStream(manifest))).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isNotEmpty()) NativeFilterSubscriptionCodec.parse(line)?.let {
                        if (it.filePath.isNullOrEmpty()) it.filePath = newPath()
                        result.add(it)
                    }
                }
                records = result
            }
        } catch (error: Exception) { moe.via.browser.core.logging.NativeLog.warn("FilterSubscriptions", "Load list: ${error.javaClass.simpleName}") }
    }
    fun publish(items: MutableList<FilterSubscription>): Boolean {
        if (manifest.isDirectory) return false
        records = items
        loaded = true
        var writer: BufferedWriter? = null
        return try {
            writer = BufferedWriter(OutputStreamWriter(FileOutputStream(manifest)))
            records.forEach { writer.write(NativeFilterSubscriptionCodec.encode(it)); writer.newLine() }
            true
        } catch (_: IOException) { false }
        finally { try { writer?.close() } catch (_: Exception) { } }
    }
    companion object {
        @Volatile private var instance: NativeFilterSubscriptionStore? = null
        fun get(context: Context) = instance ?: synchronized(this) {
            instance ?: NativeFilterSubscriptionStore(context.applicationContext).also { instance = it }
        }
    }
}
