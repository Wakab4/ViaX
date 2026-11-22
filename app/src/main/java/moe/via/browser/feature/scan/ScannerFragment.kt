package moe.via.browser.feature.scan

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import androidx.activity.result.contract.ActivityResultContracts
import moe.via.browser.R
import moe.via.browser.core.documents.NativeImagePickerContract
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.feature.scan.camera.LegacyScanCamera
import moe.via.browser.feature.scan.camera.ScanFrameGeometry
import moe.via.browser.feature.scan.camera.ScanFrameResult
import moe.via.browser.feature.scan.camera.ScanPreviewInfo
import moe.via.browser.feature.scan.decode.ScannerImageLoader
import moe.via.browser.feature.scan.ui.ScannerPage
import moe.via.browser.ui.components.BrowserMessage
import moe.via.browser.ui.pages.RecordPageFragment

/** fb.q uses original pause/resume bits and actual Fragment permission/photo contracts. */
class ScannerFragment : RecordPageFragment() {
    private var page: ScannerPage? = null
    private var camera: LegacyScanCamera? = null
    private var images: ScannerImageLoader? = null
    private var message: BrowserMessage? = null
    private var surfaceReady = false
    private var available = true
    private var flash = true
    private var authorized = false
    private var state = 0
    private var torch = false
    private var last: String? = null
    private var previewKey: String? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) { page?.message(null); authorized = true; resumeBarcode(); page?.torch(torch, flash) }
        else message?.show(getString(R.string.ref_scan_page_permission_denied), getString(android.R.string.ok)) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${requireContext().packageName}")))
        }
    }
    private val picker = registerForActivityResult(NativeImagePickerContract()) { uri -> if (uri != null) images?.decode(uri) { text ->
        if (text.isNullOrEmpty()) message?.show(getString(R.string.ref_scan_page_not_found)) else { last = text; complete() }
    } }
    private val surface = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) { surfaceReady = true; startCamera() }
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            surfaceReady = holder.surface.isValid; geometry(); startCamera()
        }
        override fun surfaceDestroyed(holder: SurfaceHolder) { surfaceReady = false; previewKey = null; camera?.stop(); page?.finder?.running(false) }
    }
    override fun draggable() = false
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ScannerPage(requireActivity(), ::closePage, ::toggleTorch, ::gallery, { camera?.zoom(it) }) {
            if (requireContext().checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) page?.root?.visibility = View.GONE
            else permission.launch(Manifest.permission.CAMERA)
        }.also { page = it }.root
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        images = ScannerImageLoader(requireContext()); message = BrowserMessage(requireActivity())
        val context = requireContext()
        available = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        flash = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        page!!.preview.surface.holder.addCallback(surface)
        view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> geometry(); startCamera() }
        page!!.configure(Resources.getSystem().configuration); page!!.message(null); page!!.torch(torch, false)
        view.requestFocus(); view.setOnKeyListener { _, key, event ->
            if (event.action != KeyEvent.ACTION_DOWN) false else when (key) {
                KeyEvent.KEYCODE_VOLUME_UP -> { camera?.torch(true); true }
                KeyEvent.KEYCODE_VOLUME_DOWN -> { camera?.torch(false); true }
                KeyEvent.KEYCODE_CAMERA, KeyEvent.KEYCODE_FOCUS -> true
                else -> false
            }
        }
        view.post { applyWindowAppearance() }
        if (!available) page!!.message(getString(R.string.ref_scan_page_no_camera), false)
        else if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            authorized = true; page!!.torch(torch, flash); resumeBarcode()
        } else permission.launch(Manifest.permission.CAMERA)
    }
    private fun gallery() {
        try { picker.launch(null) } catch (error: ActivityNotFoundException) { NativeLog.warn(null, error) }
    }
    private fun resumeBarcode() {
        if (!available) return
        if (state and 2 == 0) { state = state or 2; startCamera() }
        state = state and 1.inv()
    }
    private fun pauseBarcode() {
        if (!available) return
        if (state and 1 == 0) { camera?.stop(); previewKey = null; page?.finder?.running(false); state = state or 1 }
        state = state and 2.inv()
    }
    private fun startCamera() {
        val current = page ?: return
        if (!authorized || !available || state and 2 == 0 || !surfaceReady || current.preview.width <= 0 || current.preview.height <= 0) return
        val rotation = if (Build.VERSION.SDK_INT >= 30) current.root.display?.rotation ?: 0 else {
            @Suppress("DEPRECATION") requireActivity().windowManager.defaultDisplay.rotation
        }
        val key = "${current.preview.width}:${current.preview.height}:$rotation"
        if (key == previewKey) return
        previewKey = key
        val source = camera ?: LegacyScanCamera(object : LegacyScanCamera.Callbacks {
            override fun ready(info: ScanPreviewInfo) { if (state and 2 != 0) { page?.preview?.preview(info); geometry(); page?.finder?.running(true) } }
            override fun decoded(result: ScanFrameResult) {
                val text = result.text
                if (text != null && text != last) { last = text; complete() }
            }
            override fun torchChanged(enabled: Boolean) = Unit
            override fun failed(error: Throwable) { NativeLog.warn(null, error); page?.finder?.running(false); previewKey = null }
        }).also { camera = it }
        source.torch(torch); geometry(); source.start(current.preview.surface.holder, current.preview.width, current.preview.height, rotation)
    }
    private fun geometry() { page?.let { camera?.geometry(ScanFrameGeometry(it.preview.width, it.preview.height, Rect(it.finder.frame))) } }
    private fun toggleTorch() { torch = !torch; camera?.torch(torch); page?.torch(torch, flash) }
    private fun complete() {
        val text = last
        if (text.isNullOrEmpty()) return
        page?.preview?.performHapticFeedback(3, 2)
        parentFragmentManager.setFragmentResult(RESULT, Bundle().apply { putString("qrcode", text) }); closePage()
    }
    override fun onResume() {
        super.onResume(); applyWindowAppearance()
        if (authorized && state and 4 != 0 && state and 1 != 0) { state = state and 4.inv(); resumeBarcode() }
    }
    override fun onPause() { super.onPause(); if (authorized && state and 2 != 0) { state = state or 4; pauseBarcode() } }
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); page?.configure(newConfig); geometry(); startCamera() }
    @Suppress("DEPRECATION") internal override fun applyWindowAppearance(): Boolean {
        val window = activity?.window ?: return false
        window.statusBarColor = Color.BLACK; window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR).inv()
        if (Build.VERSION.SDK_INT >= 30) window.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
        return true
    }
    override fun onDestroyView() {
        page?.preview?.surface?.holder?.removeCallback(surface); camera?.close(); camera = null; images?.close(); images = null
        page?.finder?.running(false); message?.close(); message = null; page = null; surfaceReady = false; previewKey = null; super.onDestroyView()
    }
    companion object { const val RESULT = "qrcode" }
}
