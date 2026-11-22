package moe.via.browser.feature.adblock.data

import moe.via.browser.feature.adblock.rules.FilterRule
import moe.via.browser.feature.adblock.rules.NetworkRule
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/** y4.a keeps parsed custom rules and uses the original append/rewrite file operations. */
internal class NativeCustomFilterSource(private val path: File) {
    private var cached: MutableList<FilterRule>? = null

    fun load(): List<FilterRule> = NativeFilterRuleSources.file(path).also { cached = it }
    private fun file(): File? {
        val parent = path.parentFile
        return path.takeIf { parent == null || parent.exists() || parent.mkdirs() }
    }
    fun text(): String {
        val source = file()?.takeIf(File::exists) ?: return ""
        val result = StringBuilder()
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(InputStreamReader(FileInputStream(source)))
            val separator = System.getProperty("line.separator")
            while (true) {
                val line = reader.readLine()?.trim { it <= ' ' } ?: break
                if (line.isNotEmpty() && line[0] != '!') result.append(line).append(separator)
            }
        } catch (error: Exception) { error.printStackTrace() }
        finally { NativeFilterRuleSources.close(reader) }
        return result.toString()
    }
    fun contains(raw: String): Boolean {
        val rule = NetworkRule.parse(raw)?.let { FilterRule.Network(it) } ?: return false
        return cached!!.contains(rule)
    }
    fun add(raw: String): FilterRule? {
        if (raw.isEmpty() || raw[0] == '!') return null
        val source = file() ?: return null
        val rule = FilterRule.parse(raw) ?: return null
        val rules = cached ?: load().toMutableList().also { cached = it }
        if (rule in rules) return null
        rules.add(rule)
        var writer: BufferedWriter? = null
        try {
            writer = BufferedWriter(OutputStreamWriter(FileOutputStream(source, true)))
            writer.newLine()
            writer.write(raw)
            writer.flush()
        } catch (error: IOException) { error.printStackTrace() }
        finally { NativeFilterRuleSources.close(writer) }
        return rule
    }
    fun remove(raw: String): FilterRule? {
        if (raw.isEmpty() || raw[0] == '!') return null
        val source = file() ?: return null
        val rule = FilterRule.parse(raw) ?: return null
        val rules = cached ?: load().toMutableList().also { cached = it }
        if (!rules.remove(rule)) return null
        val temporary = File(source.parent, ".${source.name}")
        var reader: BufferedReader? = null
        var writer: BufferedWriter? = null
        try {
            reader = BufferedReader(InputStreamReader(FileInputStream(source)))
            writer = BufferedWriter(OutputStreamWriter(FileOutputStream(temporary)))
            while (true) {
                val line = reader.readLine()?.trim { it <= ' ' } ?: break
                if (line != raw && line.isNotEmpty()) { writer.write(line); writer.newLine() }
            }
            temporary.renameTo(source)
        } catch (error: IOException) { error.printStackTrace() }
        finally { NativeFilterRuleSources.close(reader); NativeFilterRuleSources.close(writer) }
        return rule
    }
    fun replace(raw: String): Boolean {
        val source = file() ?: return false
        val text = raw.trim { it <= ' ' }
        if (text.isEmpty()) return source.delete()
        var writer: BufferedWriter? = null
        return try {
            writer = BufferedWriter(OutputStreamWriter(FileOutputStream(source, false)))
            writer.write(text)
            true
        } catch (error: IOException) { error.printStackTrace(); false }
        finally { NativeFilterRuleSources.close(writer) }
    }
}
