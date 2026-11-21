package moe.pure.browser.web.permissions

import android.content.Intent

/** J9 keeps the original target text for the action's long-press details. */
data class ExternalAppRequest(val origin: String, val targetUrl: String, val intent: Intent)
