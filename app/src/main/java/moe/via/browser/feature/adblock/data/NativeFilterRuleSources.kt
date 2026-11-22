package moe.via.browser.feature.adblock.data

import android.content.Context
import moe.via.browser.feature.adblock.rules.FilterRule
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

/** a5.a and b5.c.t retain the parsed prefix when their source read fails. */
internal object NativeFilterRuleSources {
    fun file(file: File): MutableList<FilterRule> {
        val rules = ArrayList<FilterRule>()
        if (file.isDirectory || !file.exists()) return rules
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(InputStreamReader(FileInputStream(file)))
            read(reader, rules)
        } catch (error: Exception) { error.printStackTrace() }
        finally { close(reader) }
        return rules
    }
    fun builtin(context: Context): List<FilterRule> {
        val rules = ArrayList<FilterRule>()
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(InputStreamReader(context.assets.open("simple.txt"), "UTF-8"))
            read(reader, rules)
        } catch (error: IOException) { error.printStackTrace() }
        finally { close(reader) }
        return rules
    }
    private fun read(reader: BufferedReader, rules: MutableList<FilterRule>) {
        while (true) FilterRule.parse(reader.readLine() ?: return)?.let(rules::add)
    }
    fun close(value: Closeable?) { try { value?.close() } catch (_: IOException) { } }
}
