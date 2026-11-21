# FragmentManager persists subclass names in saved state; keep them stable across app updates.
-keepnames class moe.pure.browser.** extends androidx.fragment.app.Fragment

# g6.y.J's native pager sensitivity uses this AndroidX field through reflection.
-keepclassmembers class androidx.recyclerview.widget.RecyclerView {
    int mTouchSlop;
}

# These are the only methods reachable from page JavaScript; keep their JS-facing names.
-keepclassmembers,allowoptimization class moe.pure.browser.feature.scripts.runtime.UserScriptBridge$Receiver {
    @android.webkit.JavascriptInterface public java.lang.String call(java.lang.String,java.lang.String);
    @android.webkit.JavascriptInterface public boolean applies(java.lang.String,java.lang.String,java.lang.String);
}
-keepclassmembers,allowoptimization class moe.pure.browser.feature.translation.PageTranslationBridge$Receiver {
    @android.webkit.JavascriptInterface public void fallback(java.lang.String);
}
