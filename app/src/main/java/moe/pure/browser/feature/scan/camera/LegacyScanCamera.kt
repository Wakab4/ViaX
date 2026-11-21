@file:Suppress("DEPRECATION")

package moe.pure.browser.feature.scan.camera

import android.graphics.ImageFormat
import android.hardware.Camera
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import moe.pure.browser.core.logging.NativeLog
import kotlin.math.max
import kotlin.math.pow

internal data class ScanPreviewInfo(val width: Int, val height: Int, val rotation: Int, val mirrored: Boolean,
    val torchSupported: Boolean, val torchOn: Boolean)

/** Legacy Camera and decode each have one HandlerThread, matching the reference's camera/decoder ownership. */
internal class LegacyScanCamera(private val callbacks: Callbacks) : AutoCloseable {
    interface Callbacks {
        fun ready(info: ScanPreviewInfo)
        fun decoded(result: ScanFrameResult)
        fun torchChanged(enabled: Boolean)
        fun failed(error: Throwable)
    }
    private val main = Handler(Looper.getMainLooper())
    private val cameraThread = HandlerThread("scanner-camera").apply { start() }
    private val decodeThread = HandlerThread("scanner-decode").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)
    private val decoderHandler = Handler(decodeThread.looper)
    private val decoder = ScanFrameDecoder()
    private val zoom = ScanCameraZoom()
    @Volatile private var generation = 0L
    @Volatile private var closed = false
    @Volatile private var geometry: ScanFrameGeometry? = null
    @Volatile private var requestedTorch = false
    private var camera: Camera? = null
    private var runningToken = -1L
    private var previewInfo: ScanPreviewInfo? = null
    private var zoomUnavailable = false
    private val autofocus: Runnable = object : Runnable {
        override fun run() {
            val source = camera ?: return
            if (closed || runningToken != generation) return
            val mode = runCatching { source.parameters.focusMode }.getOrNull()
            if (mode !in listOf(Camera.Parameters.FOCUS_MODE_AUTO, Camera.Parameters.FOCUS_MODE_MACRO)) return
            runCatching { source.autoFocus { _, _ -> if (!closed && source === camera) cameraHandler.postDelayed(this, 2000L) } }
                .onFailure { cameraHandler.postDelayed(this, 2000L) }
        }
    }

    fun geometry(value: ScanFrameGeometry) { geometry = value.copy(frame = android.graphics.Rect(value.frame)) }

    fun start(holder: SurfaceHolder, viewportWidth: Int, viewportHeight: Int, displayRotation: Int) {
        if (closed) return
        val token = ++generation
        cameraHandler.post {
            releaseCamera()
            if (closed || token != generation || !holder.surface.isValid) return@post
            try {
                val count = Camera.getNumberOfCameras()
                check(count > 0) { "无相机" }
                val info = Camera.CameraInfo()
                val id = (0 until count).firstOrNull { index ->
                    Camera.getCameraInfo(index, info)
                    info.facing == Camera.CameraInfo.CAMERA_FACING_BACK
                } ?: 0
                Camera.getCameraInfo(id, info)
                val degrees = when (displayRotation) { Surface.ROTATION_90 -> 90; Surface.ROTATION_180 -> 180; Surface.ROTATION_270 -> 270; else -> 0 }
                val mirrored = info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT
                val rotation = if (mirrored) (360 - (info.orientation + degrees) % 360) % 360 else (info.orientation - degrees + 360) % 360
                val source = Camera.open(id) ?: error("无法打开相机")
                camera = source
                runningToken = token
                zoomUnavailable = false
                source.setDisplayOrientation(rotation)
                configure(source, viewportWidth, viewportHeight, rotation)
                val params = source.parameters
                val size = params.previewSize ?: error("相机没有可用预览")
                val modes = params.supportedFlashModes.orEmpty()
                val torch = Camera.Parameters.FLASH_MODE_TORCH in modes || Camera.Parameters.FLASH_MODE_ON in modes
                source.setErrorCallback { error, _ ->
                    cameraHandler.post {
                        if (token == generation && source === camera) {
                            releaseCamera(); report(token, IllegalStateException("相机不可用 ($error)"))
                        }
                    }
                }
                source.setPreviewDisplay(holder)
                source.startPreview()
                previewInfo = ScanPreviewInfo(size.width, size.height, rotation, mirrored, torch, false)
                applyTorch(source, requestedTorch)
                val ready = previewInfo ?: error("预览初始化失败")
                main.post { if (!closed && token == generation) callbacks.ready(ready) }
                cameraHandler.post(autofocus)
                requestFrame(source, token)
            } catch (error: Exception) {
                releaseCamera(); report(token, error)
            }
        }
    }

    private fun configure(source: Camera, targetWidth: Int, targetHeight: Int, rotation: Int) {
        val original = source.parameters.flatten()
        try {
            val params = source.parameters
            val sizes = params.supportedPreviewSizes.orEmpty()
            sizes.maxByOrNull { size -> previewScore(size.width, size.height, targetWidth, targetHeight, rotation) }?.let { params.setPreviewSize(it.width, it.height) }
            if (ImageFormat.NV21 in params.supportedPreviewFormats.orEmpty()) params.previewFormat = ImageFormat.NV21
            val focus = params.supportedFocusModes.orEmpty()
            params.focusMode = when {
                Camera.Parameters.FOCUS_MODE_AUTO in focus -> Camera.Parameters.FOCUS_MODE_AUTO
                Camera.Parameters.FOCUS_MODE_MACRO in focus -> Camera.Parameters.FOCUS_MODE_MACRO
                else -> params.focusMode
            }
            if (Camera.Parameters.FLASH_MODE_OFF in params.supportedFlashModes.orEmpty()) params.flashMode = Camera.Parameters.FLASH_MODE_OFF
            zoom.restore(params)
            source.parameters = params
        } catch (_: RuntimeException) {
            // Native retries with safe parameters when a device rejects its preferred settings.
            runCatching {
                val safe = source.parameters
                safe.unflatten(original)
                val sizes = safe.supportedPreviewSizes.orEmpty()
                sizes.maxByOrNull { previewScore(it.width, it.height, targetWidth, targetHeight, rotation) }?.let { safe.setPreviewSize(it.width, it.height) }
                zoom.restore(safe)
                source.parameters = safe
            }
        }
    }

    private fun previewScore(rawWidth: Int, rawHeight: Int, targetWidth: Int, targetHeight: Int, rotation: Int): Double {
        val width = if (rotation % 180 == 0) rawWidth else rawHeight
        val height = if (rotation % 180 == 0) rawHeight else rawWidth
        if (width <= 0 || height <= 0 || targetWidth <= 0 || targetHeight <= 0) return 0.0
        val scale = max(targetWidth.toDouble() / width, targetHeight.toDouble() / height)
        val score = if (scale > 1) (1 / scale).pow(1.1) else scale
        val coverage = width * scale / targetWidth + height * scale / targetHeight
        return score / (coverage * coverage)
    }

    private fun requestFrame(source: Camera, token: Long) {
        if (closed || token != generation || source !== camera) return
        runCatching {
            source.setOneShotPreviewCallback { bytes, _ ->
                val info = previewInfo
                val crop = geometry
                if (bytes == null || info == null || crop == null) {
                    cameraHandler.postDelayed({ requestFrame(source, token) }, 100L)
                    return@setOneShotPreviewCallback
                }
                decoderHandler.post {
                    if (closed || token != generation) return@post
                    val result = runCatching { decoder.decode(bytes, info.width, info.height, info.rotation, info.mirrored, crop) }
                        .getOrDefault(ScanFrameResult(null, emptyList()))
                    main.post { if (!closed && token == generation) callbacks.decoded(result) }
                    cameraHandler.post { requestFrame(source, token) }
                }
            }
        }.onFailure { error -> releaseCamera(); report(token, error) }
    }

    fun torch(enabled: Boolean) {
        requestedTorch = enabled
        val token = generation
        cameraHandler.post {
            val source = camera ?: return@post
            if (closed || token != generation) return@post
            runCatching {
                cameraHandler.removeCallbacks(autofocus)
                runCatching { source.cancelAutoFocus() }
                applyTorch(source, enabled)
            }
            cameraHandler.post(autofocus)
            val actual = previewInfo?.torchOn == true
            main.post { if (!closed && token == generation) callbacks.torchChanged(actual) }
        }
    }

    fun zoom(factor: Float) {
        if (closed || !factor.isFinite() || factor <= 0f) return
        val token = generation
        cameraHandler.post {
            val source = camera ?: return@post
            if (closed || token != generation || zoomUnavailable) return@post
            try {
                val params = source.parameters
                if (!zoom.change(params, factor)) return@post
                cameraHandler.removeCallbacks(autofocus)
                runCatching { source.cancelAutoFocus() }
                try { source.parameters = params } finally { cameraHandler.post(autofocus) }
            } catch (error: RuntimeException) {
                zoomUnavailable = true
                NativeLog.warn("ScannerZoom", error)
            }
        }
    }

    private fun applyTorch(source: Camera, enabled: Boolean) {
        val params = source.parameters
        val modes = params.supportedFlashModes.orEmpty()
        val mode = if (enabled) listOf(Camera.Parameters.FLASH_MODE_TORCH, Camera.Parameters.FLASH_MODE_ON).firstOrNull(modes::contains)
        else Camera.Parameters.FLASH_MODE_OFF.takeIf(modes::contains)
        if (mode != null) { params.flashMode = mode; source.parameters = params }
        val current = source.parameters.flashMode
        previewInfo = previewInfo?.copy(torchOn = current == Camera.Parameters.FLASH_MODE_TORCH || current == Camera.Parameters.FLASH_MODE_ON)
    }

    fun stop() {
        generation++
        cameraHandler.post { releaseCamera() }
    }

    private fun releaseCamera() {
        cameraHandler.removeCallbacks(autofocus)
        val source = camera
        camera = null; previewInfo = null; runningToken = -1L
        if (source != null) {
            runCatching { source.setOneShotPreviewCallback(null) }
            runCatching { source.cancelAutoFocus() }
            runCatching { source.stopPreview() }
            runCatching { source.release() }
        }
    }

    private fun report(token: Long, error: Throwable) {
        main.post { if (!closed && token == generation) callbacks.failed(error) }
    }

    override fun close() {
        if (closed) return
        closed = true; generation++
        main.removeCallbacksAndMessages(null)
        decoderHandler.removeCallbacksAndMessages(null)
        cameraHandler.post { releaseCamera(); cameraThread.quitSafely() }
        decodeThread.quitSafely()
    }
}
