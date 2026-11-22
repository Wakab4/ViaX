package moe.via.browser.feature.network

import java.util.Locale

/** Compatibility data from the reference APK's resource classifier (i6.i0.r). */
object ResourceTypes {
    fun extension(url: String): String {
        val path = url.substringBefore('#').substringBefore('?').substringAfterLast('/')
        return if ('.' in path) path.substringAfterLast('.').lowercase(Locale.ROOT) else ""
    }
    fun isResource(extension: String): Boolean {
        if (extension.length !in 2..5) return false
        var hash = 0
        extension.toByteArray(Charsets.UTF_8).forEach { byte ->
            hash = ((hash xor (byte.toInt() and 255)) * 1540483477) and Int.MAX_VALUE
            hash = hash xor (hash ushr 15)
        }
        return hash in resourceIds
    }
    // The upstream classifier publishes hashes rather than names. Retaining that table also
    // preserves uncommon container extensions without a broad MIME guess changing behavior.
    private val resourceIds = setOf(
        3847354,9568862,81264632,81930561,107123353,126179678,140994747,172819050,190100404,
        313906034,324664358,349563905,394057630,401942050,414455207,415430052,428139183,479113676,
        512218191,521295666,560628411,608526472,622106444,687482999,718140131,733429268,807282548,
        831243175,838714884,843086363,846958475,851561760,855761738,912646323,917017690,930611925,
        937672157,957034884,985168431,994678331,1000779705,1014739318,1103121146,1108586424,
        1123614135,1155674324,1161994020,1215239210,1234200878,1319876930,1321096783,1387028997,
        1413036331,1458270773,1558932844,1583569252,1625637477,1734170793,1739555145,1740189627,
        1768878331,1778226052,1792002250,1809528708,1810343100,1876522621,1944662444,1990761014,
        1999275047,2015764437,2022745637,2061805411,2072937858,2075848807,2093577775)
}
