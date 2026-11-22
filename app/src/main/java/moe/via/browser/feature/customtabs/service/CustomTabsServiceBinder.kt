package moe.via.browser.feature.customtabs.service

import android.app.PendingIntent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import android.os.Parcelable
import moe.via.browser.feature.customtabs.CustomTabsContract

/** Exact GP support AIDL transaction layout (2..14), including intentionally unsupported methods. */
internal class CustomTabsServiceBinder(private val sessions: CustomTabsSessions) : Binder(), IInterface {
    init { attachInterface(this, CustomTabsContract.SERVICE_DESCRIPTOR) }
    override fun asBinder(): IBinder = this
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (code in 1..16777215) data.enforceInterface(CustomTabsContract.SERVICE_DESCRIPTOR)
        if (code == INTERFACE_TRANSACTION) { reply?.writeString(CustomTabsContract.SERVICE_DESCRIPTOR); return true }
        when (code) {
            2 -> { data.readLong(); reply.boolean(true) } // warmup
            3 -> reply.boolean(sessions.register(data.readStrongBinder(), null))
            4 -> { // mayLaunchUrl: callback, optional uri, optional extras, optional other likely bundles
                data.readStrongBinder(); data.optional(Uri.CREATOR); data.optional(Bundle.CREATOR); data.createTypedArrayList(Bundle.CREATOR)
                reply.boolean(true)
            }
            5 -> { data.readString(); data.optional(Bundle.CREATOR); reply?.writeNoException(); reply?.writeInt(0) } // extraCommand -> null Bundle
            6 -> { data.readStrongBinder(); data.optional(Bundle.CREATOR); reply.boolean(false) } // updateVisuals
            7 -> { data.readStrongBinder(); data.optional(Uri.CREATOR); reply.boolean(false) } // requestPostMessageChannel
            8 -> { data.readStrongBinder(); data.readString(); data.optional(Bundle.CREATOR); reply?.writeNoException(); reply?.writeInt(-1) }
            9 -> { data.readStrongBinder(); data.readInt(); data.optional(Uri.CREATOR); data.optional(Bundle.CREATOR); reply.boolean(true) }
            10 -> {
                val callback = data.readStrongBinder()
                val extras = data.optional(Bundle.CREATOR)
                val sessionId = extras?.pendingIntent(CustomTabsContract.SESSION_ID)
                reply.boolean(sessions.register(callback, sessionId))
            }
            11 -> { data.readStrongBinder(); data.optional(Uri.CREATOR); data.optional(Bundle.CREATOR); reply.boolean(false) }
            12 -> { data.readStrongBinder(); data.optional(Uri.CREATOR); data.readInt(); data.optional(Bundle.CREATOR); reply.boolean(false) }
            13 -> { data.readStrongBinder(); data.optional(Bundle.CREATOR); reply.boolean(false) }
            14 -> { data.readStrongBinder(); data.readStrongBinder(); data.optional(Bundle.CREATOR); reply.boolean(false) }
            else -> return super.onTransact(code, data, reply, flags)
        }
        return true
    }
    private fun Parcel?.boolean(value: Boolean) { this?.writeNoException(); this?.writeInt(if (value) 1 else 0) }
    private fun <T> Parcel.optional(creator: Parcelable.Creator<T>): T? = if (readInt() == 0) null else creator.createFromParcel(this)
    private fun Bundle.pendingIntent(key: String): PendingIntent? = runCatching {
        if (Build.VERSION.SDK_INT >= 33) getParcelable(key, PendingIntent::class.java)
        else { @Suppress("DEPRECATION") getParcelable<PendingIntent>(key) }
    }.getOrNull()
}
