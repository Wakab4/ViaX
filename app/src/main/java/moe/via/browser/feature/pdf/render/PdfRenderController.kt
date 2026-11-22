package moe.via.browser.feature.pdf.render

import android.content.Context
import android.graphics.Matrix
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Native layout/base/high raster phases on three shared workers; observers are detachable weak values. */
internal class PdfRenderController(context: Context, listener: Listener) : AutoCloseable {
    interface Listener {
        fun loaded(sizes: List<PdfPageSize>)
        fun baseReady(current: Int, pages: List<PdfDrawingPage>)
        fun highReady(current: Int, pages: List<PdfDrawingPage>)
    }
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var listener: WeakReference<Listener>? = WeakReference(listener)
    private var document: PdfDocument? = null
    private var cache: PdfBitmapCache? = null
    private var opening: Future<*>? = null
    private var base: Future<*>? = null
    private var high: Future<*>? = null
    private var generation = 0L
    private var closed = false

    fun open(uri: Uri) {
        if (closed) return
        val ticket = ++generation
        opening?.cancel(true); base?.cancel(true); high?.cancel(true)
        document?.let { workers.execute(it::close) }; document = null
        cache?.clearMemory(); cache = null
        val owner = WeakReference(this)
        val application = app
        val dispatcher = main
        opening = workers.submit {
            val opened = runCatching {
                val doc = PdfDocument(application, uri)
                try { doc to doc.sizes() } catch (error: Throwable) { doc.close(); throw error }
            }
            dispatcher.post {
                val current = owner.get()?.takeUnless { it.closed || it.generation != ticket }
                if (current == null) { opened.getOrNull()?.first?.let { doc -> workers.execute(doc::close) }; return@post }
                opened.onSuccess { (doc, sizes) ->
                    current.document = doc; current.cache = PdfBitmapCache(application, uri.toString())
                    current.listener?.get()?.loaded(sizes)
                }.onFailure { moe.via.browser.core.logging.NativeLog.warn("PdfViewer", "Cannot open PDF", it) }
            }
        }
    }
    fun base(pages: List<PdfPageRect>, current: Int, offscreen: Int = 2) {
        val doc = document ?: return
        val bitmaps = cache ?: return
        base?.cancel(true)
        val epoch = generation
        val selected = pages.filter { it.index in max(0, current - offscreen)..minOf(pages.lastIndex, current + offscreen) }
        val owner = WeakReference(this)
        val dispatcher = main
        base = workers.submit {
            val output = mutableListOf<PdfDrawingPage>()
            runCatching {
                for (page in selected) {
                    if (Thread.currentThread().isInterrupted) return@submit
                    val bitmap = bitmaps.get(page) ?: run {
                        var width = max(1, (page.bounds.width() / page.fitScale).roundToInt())
                        var height = max(1, (page.bounds.height() / page.fitScale).roundToInt())
                        val pixels = width.toLong() * height
                        if (pixels > 6000000) { val ratio = sqrt(6000000.0 / pixels).toFloat(); width = max(1, (width * ratio).roundToInt()); height = max(1, (height * ratio).roundToInt()) }
                        doc.render(page.index, width, height).also { bitmaps.put(page, it) }
                    }
                    output += PdfDrawingPage(page, bitmap)
                }
            }.onFailure { moe.via.browser.core.logging.NativeLog.warn("PdfViewer", "Cannot raster PDF pages", it) }
            dispatcher.post { owner.get()?.takeUnless { it.closed || it.generation != epoch }?.listener?.get()?.baseReady(current, output) }
        }
    }
    fun high(pages: List<PdfPageRect>, current: Int, scale: Float, range: IntRange) {
        val doc = document ?: return
        high?.cancel(true)
        val epoch = generation
        val selected = pages.filter { it.index in range }
        val owner = WeakReference(this)
        val dispatcher = main
        high = workers.submit {
            val output = mutableListOf<PdfDrawingPage>()
            for (page in selected) {
                if (Thread.currentThread().isInterrupted) return@submit
                runCatching {
                    var factor = scale
                    var width = max(1, (page.bounds.width() * factor).roundToInt())
                    var height = max(1, (page.bounds.height() * factor).roundToInt())
                    val pixels = width.toLong() * height
                    if (pixels > 8000000) {
                        factor = max(1f, factor * sqrt(8000000.0 / pixels).toFloat())
                        width = max(1, (page.bounds.width() * factor).roundToInt()); height = max(1, (page.bounds.height() * factor).roundToInt())
                    }
                    val matrix = Matrix().apply { postScale(page.fitScale * factor, page.fitScale * factor) }
                    output += PdfDrawingPage(page, doc.render(page.index, width, height, matrix))
                }.onFailure { moe.via.browser.core.logging.NativeLog.warn("PdfViewer", "Cannot raster visible PDF page", it) }
            }
            dispatcher.post { owner.get()?.takeUnless { it.closed || it.generation != epoch }?.listener?.get()?.highReady(current, output) }
        }
    }
    fun cancelHigh() { high?.cancel(true) }
    override fun close() {
        closed = true; generation++; listener = null
        opening?.cancel(true); base?.cancel(true); high?.cancel(true)
        document?.let { workers.execute(it::close) }; document = null
        cache?.clearMemory(); cache = null
    }
    private companion object { val workers = Executors.newFixedThreadPool(3) }
}
