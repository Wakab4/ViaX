package moe.pure.browser.web.permissions

import android.Manifest
import android.app.Activity
import moe.pure.browser.R
import moe.pure.browser.ui.dialog.BrowserDialog
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.feature.site.SiteDecision
import moe.pure.browser.feature.site.SiteDomains
import moe.pure.browser.feature.site.SiteFeature
import moe.pure.browser.feature.site.SiteSettingsRepository

class WebPermissionsController(private val activity: Activity, private val settings: BrowserSettings,
    private val sites: SiteSettingsRepository,
    canPresentExternalApp: () -> Boolean = { true },
    currentExternalOrigin: (() -> String?)? = null) {
    private val runtime = AndroidPermissionQueue(activity)
    private val prompts = SitePermissionDialog(activity)
    private val externalApps = ExternalAppPermissionController(activity, settings, sites,
        canPresentExternalApp, currentExternalOrigin)
    private val active = mutableSetOf<PermissionRequest>()
    private val dialogs = mutableMapOf<PermissionRequest, BrowserDialog>()
    private data class Location(val origin: String, val callback: GeolocationPermissions.Callback)
    private var location: Location? = null
    private var locationDialog: BrowserDialog? = null

    fun media(request: PermissionRequest) {
        if (activity.isFinishing || activity.isDestroyed) { request.deny(); return }
        active.add(request)
        val origin = request.origin.toString()
        val resources = request.resources.filter { it == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID ||
            it == PermissionRequest.RESOURCE_AUDIO_CAPTURE || it == PermissionRequest.RESOURCE_VIDEO_CAPTURE }
        val allowed = resources.filter { decision(origin, feature(it)) == SiteDecision.ALLOW }.toMutableList()
        val ask = resources.filter { decision(origin, feature(it)) == SiteDecision.ASK }
        if (ask.isEmpty()) { grantMedia(request, allowed); return }
        dialogs[request] = prompts.show(origin, ask.map(::label)) { allow, remember ->
            dialogs.remove(request)
            if (request !in active) return@show
            if (remember) rememberDecision(origin, ask.mapNotNull(::feature), allow)
            if (allow) allowed.addAll(ask)
            grantMedia(request, allowed)
        }
    }

    private fun grantMedia(request: PermissionRequest, resources: List<String>) {
        if (resources.isEmpty()) { if (active.remove(request)) request.deny(); return }
        val permissions = resources.mapNotNull(::platformPermission)
        runtime.ask(permissions) { granted ->
            if (!active.remove(request)) return@ask
            val accepted = resources.filter { platformPermission(it)?.let { permission -> permission in granted } != false }
            if (accepted.isEmpty()) request.deny() else request.grant(accepted.toTypedArray())
        }
    }
    fun cancel(request: PermissionRequest) { active.remove(request); dialogs.remove(request)?.dismiss() }

    fun geolocation(origin: String, callback: GeolocationPermissions.Callback) {
        if (activity.isFinishing || activity.isDestroyed) { callback.invoke(origin, false, false); return }
        cancelGeolocation()
        val pending = Location(origin, callback)
        location = pending
        when (sites.resolve(origin, SiteFeature.LOCATION, settings)) {
            SiteDecision.DENY -> finishLocation(pending, false)
            SiteDecision.ALLOW -> grantLocation(pending)
            else -> locationDialog = prompts.show(origin, emptyList(), SitePermissionDialog.Kind.LOCATION) { allow, remember ->
                locationDialog = null
                if (location !== pending) return@show
                if (remember) rememberDecision(origin, listOf(SiteFeature.LOCATION), allow)
                if (allow) grantLocation(pending) else finishLocation(pending, false)
            }
        }
    }
    private fun grantLocation(pending: Location) {
        runtime.ask(listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) { granted ->
            if (location === pending) finishLocation(pending, granted.isNotEmpty())
        }
    }
    private fun finishLocation(pending: Location, allow: Boolean) {
        if (location !== pending) return
        location = null
        pending.callback.invoke(pending.origin, allow, false)
    }
    fun cancelGeolocation() {
        val pending = location
        location = null
        locationDialog?.dismiss(); locationDialog = null
        pending?.callback?.invoke(pending.origin, false, false)
    }

    fun popup(origin: String, userGesture: Boolean, result: (Boolean) -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) { result(false); return }
        val mode = sites.resolve(origin, SiteFeature.POPUPS, settings)
        if (mode == SiteDecision.ALLOW || mode == SiteDecision.ASK && userGesture) result(true)
        else if (mode == SiteDecision.DENY) result(false)
        else prompts.show(origin, listOf("弹出式窗口")) { allow, remember ->
            if (remember) rememberDecision(origin, listOf(SiteFeature.POPUPS), allow)
            result(allow)
        }
    }

    fun externalApp(request: ExternalAppRequest, result: (Boolean) -> Unit) = externalApps.request(request, result)

    private fun rememberDecision(origin: String, features: List<SiteFeature>, allow: Boolean) {
        val domain = SiteDomains.fromUrl(origin) ?: return
        sites.setEnabled(domain, true)
        features.forEach { sites.set(domain, it, if (allow) SiteDecision.ALLOW else SiteDecision.DENY) }
    }
    private fun decision(origin: String, feature: SiteFeature?) = feature?.let { sites.resolve(origin, it, settings) } ?: SiteDecision.ASK
    private fun feature(resource: String): SiteFeature? = when (resource) {
        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> SiteFeature.CAMERA
        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> SiteFeature.MICROPHONE
        else -> null
    }
    private fun platformPermission(resource: String): String? = when (resource) {
        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Manifest.permission.RECORD_AUDIO
        else -> null
    }
    private fun label(resource: String): String = when (resource) {
        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> activity.getString(R.string.ref_site_permission_resource,
            activity.getString(R.string.ref_site_permission_camera), activity.getString(R.string.ref_site_permission_camera_detail))
        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> activity.getString(R.string.ref_site_permission_resource,
            activity.getString(R.string.ref_site_permission_microphone), activity.getString(R.string.ref_site_permission_microphone_detail))
        PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> "受保护的媒体标识"
        PermissionRequest.RESOURCE_MIDI_SYSEX -> "MIDI 设备"
        else -> resource
    }
    fun onRequestPermissionsResult(requestCode: Int): Boolean = runtime.onResult(requestCode)
    fun platform(permissions: List<String>, result: (Set<String>) -> Unit) = runtime.ask(permissions, result)
    fun dispose() {
        active.toList().forEach { runCatching { it.deny() } }; active.clear()
        dialogs.values.toList().forEach { it.dismiss() }; dialogs.clear()
        cancelGeolocation(); runtime.dispose(); prompts.dispose(); externalApps.dispose()
    }
}
