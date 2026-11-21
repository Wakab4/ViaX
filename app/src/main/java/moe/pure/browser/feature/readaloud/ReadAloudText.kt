package moe.pure.browser.feature.readaloud

import moe.pure.browser.core.logging.NativeLog as Log
import org.json.JSONArray
import org.json.JSONTokener

/** Accepts either the script's JSON array or evaluateJavascript's quoted JSON result. */
object ReadAloudText {
    private const val RADICALS = "一丨丶丿乙亅二亠人儿入八冂冖冫几凵刀力勹匕匚匸十卜卩厂厶又口口土士夂夊夕大女子宀寸小尢尸屮山巛工己巾干幺广廴廾弋弓彐彡彳心戈户手支攴文斗斤方无日曰月木欠止歹殳毋比毛氏气水火爪父爻爿片牙牛犬玄玉瓜瓦甘生用田疋疒癶白皮皿目矛矢石示禸禾穴立竹米糸缶网羊羽老而耒耳聿肉臣自至臼舌舛舟艮色艸虍虫血行衣襾見角言谷豆豕豸貝赤走足身車辛辰辵邑酉采里金長門阜隶隹雨青非面革韋韭音頁風飛食首香馬骨高髟鬥鬯鬲鬼魚鳥鹵鹿麥麻黃黍黑黹黽鼎鼓鼠鼻齊齒龍龜龠"
    private val separators = arrayOf("”", "」", "。", "，", "；", "？", "！", ". ", ", ", "; ", "? ", "! ", "} ", " ", ".")

    fun parse(raw: String?): List<String> {
        if (raw.isNullOrBlank() || raw == "null") return emptyList()
        return try {
            var value: Any? = JSONTokener(raw).nextValue()
            if (value is String) value = JSONTokener(value).nextValue()
            val array = value as? JSONArray ?: return emptyList()
            buildList {
                for (index in 0 until array.length()) {
                    val line = clean(array.optString(index)).trim()
                    if (line.isNotEmpty()) addAll(if (line.length < 256) listOf(line) else split(line))
                }
            }
        } catch (error: Exception) {
            Log.warn("ReadAloud", "Cannot decode page text", error)
            emptyList()
        }
    }

    fun clean(text: String): String = buildString(text.length) {
        text.forEach { character ->
            if (character != '\u200b') append(if (character in '\u2f00'..'\u2fd5') RADICALS[character.code - 0x2f00] else character)
        }
    }

    /** Retains the reference's delimiter priority and its 64/256 UTF-16 boundaries. */
    fun split(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val separator = separators.firstOrNull { text.contains(it) }
        val output = mutableListOf<String>()
        var start = 0
        if (separator == null) {
            while (start < text.length) {
                val end = minOf(start + 256, text.length)
                output.add(text.substring(start, end).trim())
                start = end
            }
            return output
        }
        var search = 0
        while (search < text.length) {
            val boundary = text.indexOf(separator, search)
            if (boundary < 0) {
                while (search < text.length) {
                    val end = minOf(start + 256, text.length)
                    output.add(text.substring(start, end).trim())
                    start = end; search = start
                }
                break
            }
            val distance = boundary - start
            search = when {
                distance >= 256 -> (start + 256).also { output.add(text.substring(start, it).trim()) }
                distance >= 64 -> (boundary + separator.length).also { output.add(text.substring(start, it).trim()) }
                else -> boundary + separator.length
            }
            start = search
        }
        return output
    }
}
